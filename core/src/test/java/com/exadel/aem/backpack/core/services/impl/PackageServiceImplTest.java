/*
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.aem.backpack.core.services.impl;


import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.aem.backpack.core.dto.response.JcrPackageWrapper;
import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.services.ReferenceService;
import com.exadel.aem.backpack.core.servlets.model.*;
import com.google.common.cache.Cache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.exadel.aem.backpack.core.dto.response.PackageStatus.*;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class PackageServiceImplTest {
    private static final String PAGE_1 = "/content/site/pages/page1";
    private static final String PAGE_2 = "/content/site/pages/page2";

    private static final String PICTURE_1 = "/content/dam/picture1.jpg";
    private static final String PICTURE_2 = "/content/dam/picture2.png";
    private static final String PICTURE_3 = "/content/dam/picture3.jpg";
    private static final String PACKAGE_PATH = "/etc/packages/testGroup/testPackage-1.zip";
    private static final String THUMBNAIL = "/content/dam/thumbnail.png";
    private static final String IMAGE_JPEG = "image/jpeg";
    private static final String IMAGE_PNG = "image/png";
    private static final String BACKPACK = "backpack";
    private static final String TEST_PACKAGE = "testPackage";
    private static final String PACKAGE_VERSION = "1";
    private static final String PACKAGE_VERSION_2 = "2";

    private static final String TEST_GROUP = "testGroup";

    private static final String REFERENCED_RESOURCES = "referencedResources";
    private static final String GENERAL_RESOURCES = "generalResources";

    private static final Gson GSON = new Gson();

    public abstract static class Base {

        protected static final String INITIAL_FILTERS = "initialFilters";
        @Rule
        public AemContext context = new AemContext(ResourceResolverType.JCR_OAK);
        PackageService packageService;
        ResourceResolver resourceResolver;
        JcrPackageManager packMgr;
        Session session;
        ReferenceService referenceServiceMock = mock(ReferenceService.class);
        Map<String, List<String>> referencedResources;

        @Before
        public void beforeTest() {
            referencedResources = new HashMap<>();
            referencedResources.put(IMAGE_JPEG, Collections.singletonList(PICTURE_1));
            referencedResources.put(IMAGE_PNG, Collections.singletonList(PICTURE_2));

            HashSet<AssetReferencedItem> assetReferenceItems = new HashSet<>();
            assetReferenceItems.add(new AssetReferencedItem(PICTURE_1, IMAGE_JPEG));
            assetReferenceItems.add(new AssetReferencedItem(PICTURE_2, IMAGE_PNG));
            referenceServiceMock = mock(ReferenceService.class);
            when(referenceServiceMock.getAssetReferences(any(ResourceResolver.class), any(String.class))).thenReturn(assetReferenceItems);

            context.registerService(ReferenceService.class, referenceServiceMock);
            packageService = context.registerInjectActivateService(new PackageServiceImpl());

            context.create().page(PAGE_1);
            context.create().page(PAGE_2);
            context.create().asset(PICTURE_1, 100, 100, IMAGE_JPEG);
            context.create().asset(PICTURE_2, 100, 100, IMAGE_PNG);
            resourceResolver = context.resourceResolver();
            session = resourceResolver.adaptTo(Session.class);
            packMgr = PackagingService.getPackageManager(session);
        }

        PackageInfo getDefaultPackageInfo() {
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.setGroupName(BACKPACK);
            packageInfo.setPackageName(TEST_PACKAGE);
            packageInfo.setVersion(PACKAGE_VERSION);
            packageInfo.setReferencedResources(referencedResources);
            packageInfo.setPaths(Collections.singletonList(PAGE_1));
            return packageInfo;
        }

        JcrPackage createPackage(final PackageInfo packageInfo,
                                 final List<PathModel> pathModels,
                                 final DefaultWorkspaceFilter filter) throws RepositoryException, IOException {
            JcrPackage jcrPackage = null;
            try {
                jcrPackage = packMgr.create(packageInfo.getGroupName(), packageInfo.getPackageName(), packageInfo.getVersion());
                JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
                if (jcrPackageDefinition != null) {
                    jcrPackageDefinition.set(REFERENCED_RESOURCES, GSON.toJson(packageInfo.getReferencedResources()), true);
                    jcrPackageDefinition.set(GENERAL_RESOURCES, GSON.toJson(packageInfo.getPaths()), true);
                    if (pathModels != null) {
                        jcrPackageDefinition.set(INITIAL_FILTERS, GSON.toJson(pathModels), true);
                    }
                    jcrPackageDefinition.setFilter(filter, true);


                    Node packageNode = jcrPackage.getNode();
                    if (packageNode != null) {
                        packageInfo.setPackageNodeName(packageNode.getName());
                    }
                }
            } finally {
                if (jcrPackage != null) {
                    jcrPackage.close();
                }
            }
            return jcrPackage;
        }


        void verifyPackageFilters(final Node packageNode,
                                  final List<String> expectedPaths,
                                  final List<PathModel> expectedInitialFiltersModels,
                                  final Map<String, List<String>> expectedReferencedResources) throws RepositoryException {
            JcrPackage jcrPackage = null;
            Type listType = new TypeToken<List<String>>() {
            }.getType();
            Type mapType = new TypeToken<Map<String, List<String>>>() {
            }.getType();

            Type patModelListType = new TypeToken<List<PathModel>>() {
            }.getType();
            try {
                jcrPackage = Objects.requireNonNull(packMgr.open(packageNode));
                JcrPackageDefinition definition = Objects.requireNonNull(jcrPackage.getDefinition());
                WorkspaceFilter filter = Objects.requireNonNull(definition.getMetaInf().getFilter());
                List<String> pkgGeneralResources = Objects.requireNonNull(GSON.fromJson(definition.get(GENERAL_RESOURCES), listType));
                Map<String, List<String>> pkgReferencedResources = Objects.requireNonNull(GSON.fromJson(definition.get(REFERENCED_RESOURCES), mapType));
                List<PathModel> initialFiltersModels = Objects.requireNonNull(GSON.fromJson(definition.get(INITIAL_FILTERS), patModelListType));

                pkgReferencedResources.forEach((key, value) -> {
                    List<String> expectedPath = expectedReferencedResources.get(key);
                    expectedPath.forEach(s -> assertTrue("Referenced resources from the package metadata must be as expected map",
                            value.contains(s)));
                });

                for (int i = 0; i < initialFiltersModels.size(); i++) {
                    assertEquals("Initial path must be as in expected list", expectedInitialFiltersModels.get(i).getPath(), initialFiltersModels.get(i).getPath());
                    assertEquals("Initial excludeChildren flag must be as in expected list", expectedInitialFiltersModels.get(i).isExcludeChildren(), initialFiltersModels.get(i).isExcludeChildren());

                }

                List<PathFilterSet> filterSets = filter.getFilterSets();

                assertEquals("Count of filters must be the same as expected list size",
                        expectedPaths.size(),
                        filterSets.size());
                assertEquals("Count of general resources from metadata must be the same as expected list size",
                        pkgGeneralResources.size(),
                        filterSets.size());

                filterSets.forEach(pathFilterSet -> {
                    assertTrue(expectedPaths.contains(pathFilterSet.getRoot()));
                    assertTrue(pkgGeneralResources.contains(pathFilterSet.getRoot()));
                });
            } finally {
                if (jcrPackage != null) {
                    jcrPackage.close();
                }
            }
        }
    }

    public static class CreatePackage extends Base {

        private List<PathModel> expectedInitialFiltersModels = Collections.singletonList(new PathModel(PAGE_1, false));


        @Test
        public void shouldCreatePackage() throws RepositoryException {
            PackageModel packageModel = new PackageModel();

            initBasePackageInfo(packageModel, Collections.singletonList(PAGE_1), false);
            packageModel.setGroup(TEST_GROUP);
            resourceResolver = context.resourceResolver();
            PackageInfo aPackage = packageService.createPackage(resourceResolver, packageModel);

            assertEquals(CREATED, aPackage.getPackageStatus());
            assertNotNull("testPackage-1.zip", aPackage.getPackageNodeName());
            assertNotNull(resourceResolver.getResource("/etc/packages/testGroup/testPackage-1.zip"));
            Node packageNode = session.getNode("/etc/packages/testGroup/testPackage-1.zip");
            assertNotNull(packageNode);
            verifyPackageFilters(packageNode, Collections.singletonList(PAGE_1), expectedInitialFiltersModels, referencedResources);
        }

        @Test
        public void shouldCreatePackageWithJcrContentInCaseOfExcludedChildren() throws RepositoryException {
            PackageModel packageModel = new PackageModel();

            initBasePackageInfo(packageModel, Collections.singletonList(PAGE_1), true);
            packageModel.setGroup(TEST_GROUP);

            PackageInfo aPackage = packageService.createPackage(resourceResolver, packageModel);

            assertEquals(CREATED, aPackage.getPackageStatus());
            assertNotNull("testPackage-1.zip", aPackage.getPackageNodeName());
            Node packageNode = session.getNode("/etc/packages/testGroup/testPackage-1.zip");
            assertNotNull(packageNode);
            verifyPackageFilters(packageNode, Collections.singletonList(PAGE_1 + "/jcr:content"), Collections.singletonList(new PathModel(PAGE_1, true)), referencedResources);
        }

        @Test
        public void shouldCreatePackageWithDefaultGroup() throws RepositoryException {
            PackageModel packageModel = new PackageModel();
            initBasePackageInfo(packageModel, Collections.singletonList(PAGE_1), false);
            PackageInfo aPackage = packageService.createPackage(resourceResolver, packageModel);

            assertEquals(CREATED, aPackage.getPackageStatus());
            assertNotNull("testPackage-1.zip", aPackage.getPackageNodeName());
            Node packageNode = session.getNode("/etc/packages/backpack/testPackage-1.zip");
            assertNotNull(packageNode);
            verifyPackageFilters(packageNode, Collections.singletonList(PAGE_1), expectedInitialFiltersModels, referencedResources);

        }

        @Test
        public void shouldNotCreatePackageIfSuchPackageAlreadyExist() throws RepositoryException, IOException {
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.setGroupName(BACKPACK);
            packageInfo.setPackageName(TEST_PACKAGE);
            packageInfo.setVersion(PACKAGE_VERSION);
            packageInfo.setReferencedResources(new HashMap<>());
            packageInfo.setPaths(new ArrayList<>());
            createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, false)), new DefaultWorkspaceFilter());

            PackageModel packageModel = new PackageModel();
            initBasePackageInfo(packageModel, Collections.singletonList(PAGE_1), false);
            PackageInfo aPackage = packageService.createPackage(resourceResolver, packageModel);

            assertEquals(PackageStatus.ERROR, aPackage.getPackageStatus());
            assertEquals("ERROR: Package with such name already exists in the backpack group.", aPackage.getLog().get(0));

            session.removeItem("/etc/packages/backpack/testPackage-1.zip");
        }

        @Test
        public void shouldNotCreatePackageWithoutFilters() {
            PackageModel packageModel = new PackageModel();
            initBasePackageInfo(packageModel, Collections.emptyList(), false);
            PackageInfo aPackage = packageService.createPackage(resourceResolver, packageModel);

            assertEquals(PackageStatus.ERROR, aPackage.getPackageStatus());
            assertEquals("ERROR: Package does not contain any valid filters.", aPackage.getLog().get(0));
        }

        private void initBasePackageInfo(final PackageModel model, final List<String> strings, final boolean excludeChildren) {
            model.setPaths(strings.stream().map(s -> new PathModel(s, excludeChildren)).collect(Collectors.toList()));
            model.setPackageName(TEST_PACKAGE);
            model.setThumbnailPath(THUMBNAIL);
            model.setVersion(PACKAGE_VERSION);
        }
    }

    public static class EditPackage extends Base {
        private static final String TEST_GROUP_2 = "testGroup2";
        private static final String TEST_PACKAGE_2 = "testPackage2";
        private JcrPackage aPackage;
        private List<PathModel> expectedInitialFiltersModels = Collections.singletonList(new PathModel(PAGE_1, false));

        private PackageInfo packageInfo;

        @Before
        public void before() throws IOException, RepositoryException {
            packageInfo = new PackageInfo();
            packageInfo.setGroupName(TEST_GROUP);
            packageInfo.setPackageName(TEST_PACKAGE);
            packageInfo.setVersion(PACKAGE_VERSION);
            packageInfo.setReferencedResources(referencedResources);
            packageInfo.setPaths(Collections.singletonList(PAGE_1));
            packageInfo.setPackagePath(PACKAGE_PATH);
            aPackage = createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, false)), new DefaultWorkspaceFilter());
            resourceResolver = context.resourceResolver();
        }


        @Test
        public void shouldEditPackage() throws RepositoryException {
            PackageModel packageModel = new PackageModel();
            HashSet<AssetReferencedItem> assetReferenceItems = new HashSet<>();
            assetReferenceItems.add(new AssetReferencedItem(PICTURE_3, IMAGE_JPEG));
            when(referenceServiceMock.getAssetReferences(any(ResourceResolver.class), any(String.class))).thenReturn(assetReferenceItems);

            Map<String, List<String>> modifiedReferencedResources = new HashMap<>();
            modifiedReferencedResources.put(IMAGE_JPEG, Collections.singletonList(PICTURE_3));

            initBasePackageModel(packageModel, Collections.singletonList(PAGE_2), false);


            PackageInfo aPackage = packageService.editPackage(resourceResolver, packageModel);


            assertEquals(MODIFIED, aPackage.getPackageStatus());
            assertNotNull("testPackage2-2.zip", aPackage.getPackageNodeName());
            Node packageNode = session.getNode("/etc/packages/testGroup2/testPackage2-2.zip");
            assertNotNull(packageNode);
            verifyPackageFilters(packageNode, Collections.singletonList(PAGE_2), Collections.singletonList(new PathModel(PAGE_2, false)), modifiedReferencedResources);
        }

        @Test
        public void shouldSkipPackageEditingIfSuchPackageAlreadyExist() throws RepositoryException, IOException {
            packageInfo.setGroupName(TEST_GROUP_2);
            packageInfo.setPackageName(TEST_PACKAGE_2);
            packageInfo.setVersion(PACKAGE_VERSION_2);
            createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, false)), new DefaultWorkspaceFilter());
            PackageModel packageModel = new PackageModel();
            initBasePackageModel(packageModel, Collections.singletonList(PAGE_2), false);

            PackageInfo aPackage = packageService.editPackage(resourceResolver, packageModel);

            assertEquals(ERROR, aPackage.getPackageStatus());
            assertEquals("ERROR: Package with such name already exists in the testGroup2 group.", aPackage.getLog().get(0));
        }

        @Test
        public void shouldModifyPackageFilters() throws RepositoryException {
            PackageModel packageModel = new PackageModel();

            initBasePackageModel(packageModel, Collections.singletonList(PAGE_1), true);

            PackageInfo aPackage = packageService.editPackage(resourceResolver, packageModel);

            assertEquals(MODIFIED, aPackage.getPackageStatus());
            assertNotNull("testPackage2-2.zip", aPackage.getPackageNodeName());
            Node packageNode = session.getNode("/etc/packages/testGroup2/testPackage2-2.zip");
            assertNotNull(packageNode);
            verifyPackageFilters(packageNode, Collections.singletonList(PAGE_1 + "/jcr:content"), Collections.singletonList(new PathModel(PAGE_1, true)), referencedResources);
        }

        @Test
        public void shouldNotModifyPackageWithoutFilters() {
            PackageModel packageModel = new PackageModel();
            initBasePackageModel(packageModel, Collections.emptyList(), false);
            PackageInfo aPackage = packageService.editPackage(resourceResolver, packageModel);

            assertEquals(PackageStatus.ERROR, aPackage.getPackageStatus());
            assertEquals("ERROR: Package does not contain any valid filters.", aPackage.getLog().get(0));
        }

        private void initBasePackageModel(final PackageModel model, final List<String> strings, final boolean excludeChildren) {
            model.setPaths(strings.stream().map(s -> new PathModel(s, excludeChildren)).collect(Collectors.toList()));
            model.setPackageName(TEST_PACKAGE_2);
            model.setGroup(TEST_GROUP_2);
            model.setThumbnailPath(THUMBNAIL);
            model.setVersion(PACKAGE_VERSION_2);
            model.setPackagePath("/etc/packages/testGroup/testPackage-1.zip");
        }
    }

    public static class GetPackageModelByPath extends Base {

        private JcrPackage aPackage;
        private JcrPackageManager jcrPackageManagerMock;

        private PackageServiceImpl packageServiceSpy;
        private PackageInfo packageInfo;

        @Before
        public void before() throws IOException, RepositoryException {
            packageInfo = new PackageInfo();
            packageInfo.setGroupName(TEST_GROUP);
            packageInfo.setPackageName(TEST_PACKAGE);
            packageInfo.setVersion(PACKAGE_VERSION);
            packageInfo.setReferencedResources(referencedResources);
            packageInfo.setPaths(Collections.singletonList(PAGE_1));
            packageInfo.setPackagePath(PACKAGE_PATH);
            aPackage = createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, true)), new DefaultWorkspaceFilter());

        }

        @Test
        public void shouldReturnPackageModelByPath() {
            PackageModel packageModelByPath = packageService.getPackageModelByPath(PACKAGE_PATH, resourceResolver);

            assertEquals(TEST_PACKAGE, packageModelByPath.getPackageName());
            assertEquals(PACKAGE_VERSION, packageModelByPath.getVersion());
            assertEquals(TEST_GROUP, packageModelByPath.getGroup());
            packageModelByPath.getPaths().forEach(pathModel -> {
                assertEquals(PAGE_1, pathModel.getPath());
                assertEquals(true, pathModel.isExcludeChildren());
            });
        }

        @Test
        public void shouldReturnDefaultFiltersWhenInitialNotSpecified() throws IOException, RepositoryException {
            packageInfo.setVersion(PACKAGE_VERSION_2);
            aPackage = createPackage(packageInfo, null, new DefaultWorkspaceFilter());

            PackageModel packageModelByPath = packageService.getPackageModelByPath("/etc/packages/testGroup/testPackage-2.zip", resourceResolver);


            assertEquals(TEST_PACKAGE, packageModelByPath.getPackageName());
            assertEquals(PACKAGE_VERSION_2, packageModelByPath.getVersion());
            assertEquals(TEST_GROUP, packageModelByPath.getGroup());
            packageModelByPath.getPaths().forEach(pathModel -> {
                assertEquals(PAGE_1, pathModel.getPath());
                assertEquals(false, pathModel.isExcludeChildren());
            });
        }

        @Test
        public void shouldReturnNullWhenPackageNotExist() throws IOException, RepositoryException {
            PackageModel packageModelByPath = packageService.getPackageModelByPath("/etc/packages/testGroup/testPackage-3.zip", resourceResolver);

            assertNull(packageModelByPath);
        }

    }

    public static class GetPackageGroups extends Base {


        private PackageServiceImpl packageServiceSpy;
        private Node rootNode;
        private List<String> expectedPath;


        @Before
        public void before() throws RepositoryException {

            rootNode = context.create().resource("/etc/packages").adaptTo(Node.class);
            Node slingFolder = rootNode.addNode("slingFolder", JcrResourceConstants.NT_SLING_FOLDER);
            Node nestedFolder = slingFolder.addNode("nestedFolder", JcrConstants.NT_FOLDER);
            Node slingOrderedFolder = rootNode.addNode("slingOrderedFolder", JcrResourceConstants.NT_SLING_ORDERED_FOLDER);
            rootNode.addNode(".snapshot", JcrResourceConstants.NT_SLING_ORDERED_FOLDER);
            expectedPath = new ArrayList<>();
            expectedPath.add(slingFolder.getPath());
            expectedPath.add(nestedFolder.getPath());
            expectedPath.add(slingOrderedFolder.getPath());
        }

        @Test
        public void shouldReturnAppropriateGroups() {
            List<Resource> packageGroups = packageService.getPackageFolders(resourceResolver);

            assertEquals(expectedPath.size(), packageGroups.size());
            for (int i = 0; i < packageGroups.size(); i++) {
                assertEquals(expectedPath.get(i), packageGroups.get(i).getPath());
            }
        }
    }

    public static class GetPackageInfo extends Base {

        private static final String TEST_ZIP = "/etc/packages/backpack/test.zip";
        private static final String TEST = "test";
        private static final String PACKAGE_PATH = "/etc/packages/backpack/testPackage-1.zip";

        @Test
        @SuppressWarnings("UnstableApiUsage") // sticking to Guava Cache version bundled in uber-jar; still safe to use
        public void shouldReturnInMemoryPackageInfo() {
            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(TEST_ZIP);
            Cache<String, PackageInfo> packageInfos = ((PackageServiceImpl) packageService).getPackageInfos();
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.setPackageName(TEST);
            packageInfos.put(TEST_ZIP, packageInfo);

            PackageInfo result = packageService.getPackageInfo(resourceResolver, packageInfoModel);

            assertEquals(TEST, result.getPackageName());
        }

        @Test
        public void shouldReturnNullWithNonExistingPackage() {
            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(TEST_ZIP);

            PackageInfo result = packageService.getPackageInfo(resourceResolver, packageInfoModel);

            assertNull(result.getPackageStatus());
        }

        @Test
        public void shouldReturnExistingPackageInfo() throws IOException, RepositoryException {
            //create package inside the repository
            PackageInfo packageInfo = getDefaultPackageInfo();
            DefaultWorkspaceFilter defaultWorkspaceFilter = new DefaultWorkspaceFilter();
            defaultWorkspaceFilter.add(new PathFilterSet(PAGE_1));
            createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, false)), defaultWorkspaceFilter);

            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(PACKAGE_PATH);

            PackageInfo result = packageService.getPackageInfo(resourceResolver, packageInfoModel);

            assertEquals(PACKAGE_PATH, result.getPackagePath());
            assertEquals(BACKPACK, result.getGroupName());
            assertEquals(TEST_PACKAGE, result.getPackageName());
            assertEquals(PACKAGE_VERSION, result.getVersion());
            assertEquals(referencedResources, result.getReferencedResources());
            assertEquals(PAGE_1, result.getPaths().stream().findFirst().orElse(null));
            assertNotNull(result.getDataSize());
            assertEquals(CREATED, result.getPackageStatus());
            assertEquals("testPackage-1.zip", result.getPackageNodeName());
        }

        @Test
        public void shouldReturnRightInfoAfterDeletingPackage() throws IOException, RepositoryException {
            PackageInfo packageInfo = getDefaultPackageInfo();
            DefaultWorkspaceFilter defaultWorkspaceFilter = new DefaultWorkspaceFilter();
            defaultWorkspaceFilter.add(new PathFilterSet(PAGE_1));
            createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, false)), defaultWorkspaceFilter);

            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(PACKAGE_PATH);

            PackageInfo result1 = packageService.getPackageInfo(resourceResolver, packageInfoModel);

            packMgr.remove(packMgr.listPackages().get(0));
            defaultWorkspaceFilter.add(new PathFilterSet(PICTURE_1));
            createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, false)), defaultWorkspaceFilter);

            PackageInfo result2 = packageService.getPackageInfo(resourceResolver, packageInfoModel);

            assertEquals(result1.getPackageStatus(), result2.getPackageStatus());
            assertEquals(result1.getPackageName(), result2.getPackageName());
            assertEquals(result1.getGroupName(), result2.getGroupName());
            assertEquals(result1.getVersion(), result2.getVersion());
            assertEquals(result1.getPackagePath(), result2.getPackagePath());
            assertNotEquals(result1.getPaths(), result2.getPaths());
        }
    }

    public static class TestBuildPackage extends Base {

        private static final String PACKAGE_PATH = "/etc/packages/testGroup/testPackage-1.zip";

        @Test
        public void shouldReturnZeroSizeWhenNoReferencesFound() {
            BuildPackageModel buildPackageModel = new BuildPackageModel();

            buildPackageModel.setPackagePath(PACKAGE_PATH);

            PackageInfo aPackage = packageService.testBuildPackage(resourceResolver, buildPackageModel);

            assertEquals((Long) 0L, aPackage.getDataSize());
            assertEquals(Collections.singletonList("A " + PAGE_1), aPackage.getLog());
            assertNull(aPackage.getPackageBuilt());
        }

        @Test
        public void shouldReturnNonZeroSizeWhenReferencesFound() {
            BuildPackageModel buildPackageModel = new BuildPackageModel();

            buildPackageModel.setPackagePath(PACKAGE_PATH);
            buildPackageModel.setReferencedResources(Collections.singletonList(IMAGE_JPEG));

            PackageInfo aPackage = packageService.testBuildPackage(resourceResolver, buildPackageModel);

            assertNotEquals((Long) 0L, aPackage.getDataSize());
            assertEquals(Arrays.asList("A " + PAGE_1, "A " + PICTURE_1), aPackage.getLog());
            assertNull(aPackage.getPackageBuilt());
        }

        @Test
        public void shouldReturnNonEqualSizeWithDifferentReferences() {
            BuildPackageModel buildPackageModel = new BuildPackageModel();

            buildPackageModel.setPackagePath(PACKAGE_PATH);
            buildPackageModel.setReferencedResources(Collections.singletonList(IMAGE_JPEG));

            PackageInfo firstPackage = packageService.testBuildPackage(resourceResolver, buildPackageModel);

            buildPackageModel.setReferencedResources(Arrays.asList(IMAGE_JPEG, IMAGE_PNG));

            PackageInfo secondPackage = packageService.testBuildPackage(resourceResolver, buildPackageModel);

            assertNotEquals((Long) 0L, firstPackage.getDataSize());
            assertNotEquals((Long) 0L, secondPackage.getDataSize());
            assertNotEquals(firstPackage.getDataSize(), secondPackage.getDataSize());
            assertEquals(Arrays.asList("A " + PAGE_1, "A " + PICTURE_1), firstPackage.getLog());
            assertEquals(Arrays.asList("A " + PAGE_1, "A " + PICTURE_1, "A " + PICTURE_2), secondPackage.getLog());
            assertNull(firstPackage.getPackageBuilt());
            assertNull(secondPackage.getPackageBuilt());
        }

        @Before
        public void createBasePackage() throws IOException, RepositoryException {
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.setGroupName(TEST_GROUP);
            packageInfo.setPackageName(TEST_PACKAGE);
            packageInfo.setVersion(PACKAGE_VERSION);
            packageInfo.setReferencedResources(referencedResources);
            packageInfo.setPaths(Collections.singletonList(PAGE_1));
            createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, false)), new DefaultWorkspaceFilter());
        }
    }

    public static class BuildPackage extends Base {


        private static final String PACKAGE_PATH = "/etc/packages/testGroup/testPackage-1.zip";
        private PackageServiceImpl packageServiceSpy;
        private PackageInfo packageInfo;
        private JcrPackage aPackage;
        private JcrPackageManager jcrPackageManagerMock;


        @Before
        public void before() throws IOException, RepositoryException {
            packageInfo = new PackageInfo();
            packageInfo.setGroupName(TEST_GROUP);
            packageInfo.setPackageName(TEST_PACKAGE);
            packageInfo.setVersion(PACKAGE_VERSION);
            packageInfo.setReferencedResources(referencedResources);
            packageInfo.setPaths(Collections.singletonList(PAGE_1));
            packageInfo.setPackagePath(PACKAGE_PATH);
            aPackage = spy(createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, false)), new DefaultWorkspaceFilter()));


            packageServiceSpy = (PackageServiceImpl) spy(packageService);
            jcrPackageManagerMock = mock(JcrPackageManager.class);
            doReturn(jcrPackageManagerMock).when(packageServiceSpy).getPackageManager(any(Session.class));
            doReturn(resourceResolver.adaptTo(Session.class)).when(packageServiceSpy).getUserImpersonatedSession(any(String.class));
        }

        @Test
        public void shouldBuildPackage() throws RepositoryException {
            doReturn(aPackage).when(jcrPackageManagerMock).open(any(Node.class));
            doReturn(100L).when(aPackage).getSize();

            List<String> referencedResourceTypes = Arrays.asList(IMAGE_JPEG, IMAGE_PNG);

            packageServiceSpy.buildPackage("admin", packageInfo, referencedResourceTypes);

            assertEquals(BUILT, packageInfo.getPackageStatus());
            assertNotNull(packageInfo.getPackageBuilt());
        }

        @Test
        public void shouldHandleError() {
            List<String> referencedResourceTypes = Arrays.asList(IMAGE_JPEG, IMAGE_PNG);

            packageServiceSpy.buildPackage("admin", packageInfo, referencedResourceTypes);

            assertEquals(ERROR, packageInfo.getPackageStatus());
            assertEquals("ERROR: Package by this path /etc/packages/testGroup/testPackage-1.zip doesn't exist in the repository.", packageInfo.getLog().get(0));
        }
    }

    public static class GetLatestPackageBuildInfo extends Base {
        private static final String PACKAGE_PATH = "/etc/packages/backpack/testPackage-1.zip";
        private static final String TEST = "test_log";
        private static final List<String> TEST_LOG = new ArrayList<>(Collections.singletonList(TEST));
        private static final List<String> TEST_EMPTY_LOG = Collections.emptyList();
        private static final int LATEST_INDEX = 1;

        @Test
        public void shouldReturnPackageInfoWithLatestLogsIfExist() {
            LatestPackageInfoModel latestPackageInfoModel = new LatestPackageInfoModel();
            latestPackageInfoModel.setPackagePath(PACKAGE_PATH);
            latestPackageInfoModel.setLatestLogIndex(LATEST_INDEX);

            @SuppressWarnings("UnstableApiUsage") // sticking to Guava Cache version bundled in uber-jar; still safe to use
                    Cache<String, PackageInfo> packageInfos = ((PackageServiceImpl) packageService).getPackageInfos();
            PackageInfo packageInfo = new PackageInfo();

            packageInfo.addLogMessage(TEST);
            packageInfo.addLogMessage(TEST);
            packageInfos.put(PACKAGE_PATH, packageInfo);

            PackageInfo result = packageService.getLatestPackageBuildInfo(latestPackageInfoModel);

            assertEquals(TEST_LOG, result.getLog());
        }

        @Test
        public void shouldReturnPackageInfoWithoutLatestLogsIfNotExist() {
            LatestPackageInfoModel latestPackageInfoModel = new LatestPackageInfoModel();
            latestPackageInfoModel.setPackagePath(PACKAGE_PATH);

            @SuppressWarnings("UnstableApiUsage") // sticking to Guava Cache version bundled in uber-jar; still safe to use
                    Cache<String, PackageInfo> packageInfos = ((PackageServiceImpl) packageService).getPackageInfos();
            PackageInfo packageInfo = new PackageInfo();
            packageInfos.put(PACKAGE_PATH, packageInfo);

            PackageInfo result = packageService.getLatestPackageBuildInfo(latestPackageInfoModel);

            assertEquals(TEST_EMPTY_LOG, result.getLog());
        }

        @Test
        public void shouldReturnNonExistingLatestPackageBuildInfo() {
            LatestPackageInfoModel latestPackageInfoModel = new LatestPackageInfoModel();
            latestPackageInfoModel.setPackagePath(PACKAGE_PATH);

            PackageInfo result = packageService.getLatestPackageBuildInfo(latestPackageInfoModel);

            assertEquals(PackageStatus.ERROR, result.getPackageStatus());
            assertEquals("ERROR: Package by this path " + PACKAGE_PATH + " doesn't exist in the repository.", result.getLog().get(0));
        }
    }

    public static class PackageExists extends Base {

        private static final String PACKAGE_PATH = "/etc/packages/backpack/testPackage-1.zip";

        @Test
        public void shouldReturnFalseWithNonExistingPackage() {
            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(TEST_PACKAGE);

            boolean result = packageService.packageExists(resourceResolver, packageInfoModel);

            assertFalse(result);
        }

        @Test
        public void shouldReturnTrueWithExistingPackage() throws IOException, RepositoryException {
            PackageInfo packageInfo = getDefaultPackageInfo();
            DefaultWorkspaceFilter defaultWorkspaceFilter = new DefaultWorkspaceFilter();
            defaultWorkspaceFilter.add(new PathFilterSet(PAGE_1));
            createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, false)), defaultWorkspaceFilter);

            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(PACKAGE_PATH);

            boolean result = packageService.packageExists(resourceResolver, packageInfoModel);

            assertTrue(result);
        }

        @Test
        public void shouldReturnJcrPackageWrapperWithEmptyJcrPackage_whenByteArrayEmptyOfZipFile() {

            JcrPackageWrapper jcrPackageWrapper = packageService.uploadPackage(session, "".getBytes(), false);

            Assert.assertNotNull(jcrPackageWrapper);
            assertEquals("zip file is empty", jcrPackageWrapper.getMessage());
            assertEquals(SC_CONFLICT, jcrPackageWrapper.getStatusCode());
        }

        @Test
        public void shouldReturnJcrPackageWrapperWithEmptyJcrPackage_whenByteArrayNullOfZipFile() {

            JcrPackageWrapper jcrPackageWrapper = packageService.uploadPackage(session, null, false);

            Assert.assertNotNull(jcrPackageWrapper);
            assertEquals("An incorrect value of parameter(s)", jcrPackageWrapper.getMessage());
            assertEquals(SC_CONFLICT, jcrPackageWrapper.getStatusCode());
        }

        @Test
        public void shouldReturnJcrPackageWrapperWithEmptyJcrPackage_whenByteArrayNullOfZipFileAndSessionNull() {

            JcrPackageWrapper jcrPackageWrapper = packageService.uploadPackage(null, null, false);

            Assert.assertNotNull(jcrPackageWrapper);
            assertEquals("An incorrect value of parameter(s)", jcrPackageWrapper.getMessage());
            assertEquals(SC_CONFLICT, jcrPackageWrapper.getStatusCode());
        }

        @Test
        public void shouldReturnJcrPackageWrapperWithJcrPackage_whenByteArrayNoEmptyAndZipFileValid() throws IOException, RepositoryException {
            byte[] bytes = PackageServiceImplTest.readByteArrayFromFile("/com/exadel/aem/backpack/core/services/impl/test_back_pack.zip");
            JcrPackageWrapper jcrPackageWrapper = packageService.uploadPackage(session, bytes, false);

            Assert.assertNotNull(jcrPackageWrapper);
            assertEquals(0, jcrPackageWrapper.getStatusCode());
            assertEquals("{\"statusCode\":0}", jcrPackageWrapper.getJson());
            Assert.assertNull(jcrPackageWrapper.getMessage());

            PackageInfo packageInfo = jcrPackageWrapper.getPackageInfo();
            Assert.assertNotNull(packageInfo);
            assertEquals("test_back_pack", packageInfo.getPackageName());
            assertEquals("backpack", packageInfo.getGroupName());
        }

        @Test
        public void shouldReturnPackageInfoWithJcrPackage_whenByteArrayNoEmptyAndZipFileValid() {
            byte[] bytes = PackageServiceImplTest.readByteArrayFromFile("/com/exadel/aem/backpack/core/services/impl/test_back_pack.zip");
            JcrPackageWrapper jcrPackageWrapper = packageService.uploadPackage(session, bytes, false);

            Assert.assertNotNull(jcrPackageWrapper);
            Assert.assertNull(jcrPackageWrapper.getMessage());

            PackageInfo packageInfo = jcrPackageWrapper.getPackageInfo();

            String packageInfoJson = GSON.toJson(packageInfo);
            assertEquals("{\"packageName\":\"test_back_pack\",\"packageNodeName\":\"test_back_pack.zip\",\"groupName\":\"backpack\",\"version\":\"\",\"packageBuilt\":{\"year\":2020,\"month\":11,\"dayOfMonth\":7,\"hourOfDay\":15,\"minute\":43,\"second\":45},\"packageStatus\":\"BUILT\",\"packagePath\":\"/etc/packages/backpack/test_back_pack.zip\",\"paths\":[\"/content/we-retail/ca/en/about-us\"],\"referencedResources\":{\"image/jpeg\":[\"/content/dam/we-retail/en/activities/hiking-camping/trekker-ama-dablam.jpg\"]},\"log\":[],\"dataSize\":22283}", packageInfoJson);
        }

        @Test
        public void shouldReturnWrapperWithErrorMsgPackageAlreadyExist_whenUploadArchiveTwiceWithForceUpdateFalse() {
            byte[] bytes = PackageServiceImplTest.readByteArrayFromFile("/com/exadel/aem/backpack/core/services/impl/test_back_pack.zip");
            JcrPackageWrapper jcrPackageWrapper = packageService.uploadPackage(session, bytes, false);

            Assert.assertNotNull(jcrPackageWrapper);
            Assert.assertNull(jcrPackageWrapper.getMessage());

            JcrPackageWrapper jcrPackageWrapper2 = packageService.uploadPackage(session, bytes, false);

            assertEquals("Package already exists: /etc/packages/backpack/test_back_pack.zip", jcrPackageWrapper2.getMessage());
            assertEquals(SC_CONFLICT, jcrPackageWrapper2.getStatusCode());

        }

        @Test
        public void shouldReturnRewritePackage_whenUploadArchiveTwiceWithForceUpdateTrue() {
            byte[] bytes = PackageServiceImplTest.readByteArrayFromFile("/com/exadel/aem/backpack/core/services/impl/test_back_pack.zip");
            JcrPackageWrapper jcrPackageWrapper = packageService.uploadPackage(session, bytes, false);

            Assert.assertNotNull(jcrPackageWrapper);
            Assert.assertNull(jcrPackageWrapper.getMessage());

            JcrPackageWrapper jcrPackageWrapper2 = packageService.uploadPackage(session, bytes, true);

            PackageInfo packageInfo2 = jcrPackageWrapper2.getPackageInfo();

            String packageInfoJson = GSON.toJson(packageInfo2);
            assertEquals("{\"packageName\":\"test_back_pack\",\"packageNodeName\":\"test_back_pack.zip\",\"groupName\":\"backpack\",\"version\":\"\",\"packageBuilt\":{\"year\":2020,\"month\":11,\"dayOfMonth\":7,\"hourOfDay\":15,\"minute\":43,\"second\":45},\"packageStatus\":\"BUILT\",\"packagePath\":\"/etc/packages/backpack/test_back_pack.zip\",\"paths\":[\"/content/we-retail/ca/en/about-us\"],\"referencedResources\":{\"image/jpeg\":[\"/content/dam/we-retail/en/activities/hiking-camping/trekker-ama-dablam.jpg\"]},\"log\":[],\"dataSize\":22283}", packageInfoJson);

        }
    }

    public static byte[] readByteArrayFromFile(final String classpathResource) {
        String filePath = "src/test/resources".concat(classpathResource);
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException var11) {
            return null;
        }
    }
}