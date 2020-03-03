package com.exadel.aem.backpack.core.services.impl;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.services.ReferenceService;
import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import com.google.common.cache.Cache;
import com.google.gson.Gson;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.*;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.*;

import static com.exadel.aem.backpack.core.dto.response.PackageStatus.CREATED;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class PackageServiceImplTest {
    private static final String PAGE_1 = "/content/site/pages/page1";
    private static final String PICTURE_1 = "/content/dam/picture1.jpg";
    private static final String PICTURE_2 = "/content/dam/picture2.png";
    private static final String IMAGE_JPEG = "image/jpeg";
    private static final String IMAGE_PNG = "image/png";
    private static final String BACKPACK = "backpack";
    private static final String TEST_PACKAGE = "testPackage";
    private static final String PACKAGE_VERSION = "1";
    private static final String TEST_GROUP = "testGroup";


    private static final String REFERENCED_RESOURCES = "referencedResources";
    private static final String GENERAL_RESOURCES = "generalResources";

    private static final Gson GSON = new Gson();

    @Ignore
    public static class Base {

        @Rule
        public AemContext context = new AemContext(ResourceResolverType.JCR_OAK);
        protected PackageService packageService;
        protected ResourceResolver resourceResolver;
        protected JcrPackageManager packMgr;
        protected Session session;
        protected ReferenceService referenceServiceMock = mock(ReferenceService.class);
        protected Map<String, List<String>> referencedResources;

        @Before
        public void beforeTest() {
            referencedResources = new HashMap<>();
            referencedResources.put(IMAGE_JPEG, Arrays.asList(PICTURE_1));
            referencedResources.put(IMAGE_PNG, Arrays.asList(PICTURE_2));

            HashSet<AssetReferencedItem> assetReferenceItems = new HashSet<>();
            assetReferenceItems.add(new AssetReferencedItem(PICTURE_1, IMAGE_JPEG));
            assetReferenceItems.add(new AssetReferencedItem(PICTURE_2, IMAGE_PNG));
            referenceServiceMock = mock(ReferenceService.class);
            when(referenceServiceMock.getAssetReferences(any(ResourceResolver.class), any(String.class))).thenReturn(assetReferenceItems);

            context.registerService(ReferenceService.class, referenceServiceMock);
            packageService = context.registerInjectActivateService(new PackageServiceImpl());

            context.create().page(PAGE_1);
            context.create().asset(PICTURE_1, 100, 100, IMAGE_JPEG);
            context.create().asset(PICTURE_2, 100, 100, IMAGE_PNG);
            resourceResolver = context.resourceResolver();
            session = resourceResolver.adaptTo(Session.class);
            packMgr = PackagingService.getPackageManager(session);
        }

        protected PackageInfo getDefaultPackageInfo() {
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.setGroupName(BACKPACK);
            packageInfo.setPackageName(TEST_PACKAGE);
            packageInfo.setVersion(PACKAGE_VERSION);
            packageInfo.setReferencedResources(referencedResources);
            packageInfo.setPaths(Arrays.asList(PAGE_1));
            return packageInfo;
        }

        protected JcrPackage createPackage(final PackageInfo packageInfo,
                                           final DefaultWorkspaceFilter filter) throws RepositoryException, IOException {
            JcrPackage jcrPackage = null;
            try {
                jcrPackage = packMgr.create(packageInfo.getGroupName(), packageInfo.getPackageName(), packageInfo.getVersion());
                JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
                if (jcrPackageDefinition != null) {
                    jcrPackageDefinition.set(REFERENCED_RESOURCES, GSON.toJson(packageInfo.getReferencedResources()), true);
                    jcrPackageDefinition.set(GENERAL_RESOURCES, GSON.toJson(packageInfo.getPaths()), true);
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


        protected void verifyPackageFilters(final Node packageNode,
                                            final List<String> expectedPaths,
                                            final Map<String, List<String>> expectedReferencedResources) throws RepositoryException {
            JcrPackage jcrPackage = null;
            try {
                jcrPackage = packMgr.open(packageNode);
                JcrPackageDefinition definition = jcrPackage.getDefinition();
                WorkspaceFilter filter = definition.getMetaInf().getFilter();
                List<String> pkgGeneralResources = (List<String>) GSON.fromJson(definition.get(GENERAL_RESOURCES), List.class);
                Map<String, List<String>> pkgReferencedResources = (Map<String, List<String>>) GSON.fromJson(definition.get(REFERENCED_RESOURCES), Map.class);

                expectedReferencedResources.entrySet().stream().forEach(listEntry -> {
                    List<String> expectedPath = expectedReferencedResources.get(listEntry.getKey());
                    expectedPath.forEach(s -> assertTrue("Referenced resources from the package metadata must be as expected map", listEntry.getValue().contains(s)));
                });

                List<PathFilterSet> filterSets = filter.getFilterSets();

                assertEquals("Count of filters must be the same as expected list size", expectedPaths.size(), filterSets.size());
                assertEquals("Count of general resources from metadata must be the same as expected list size", pkgGeneralResources.size(), filterSets.size());

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
        @Test
        public void shouldCreatePackage() throws RepositoryException {
            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();

            initBasePackageInfo(builder, Arrays.asList(PAGE_1));
            builder.withPackageGroup(TEST_GROUP);
            resourceResolver = context.resourceResolver();
            PackageInfo aPackage = packageService.createPackage(resourceResolver, builder.build());

            assertEquals(CREATED, aPackage.getPackageStatus());
            assertNotNull("testPackage-1.zip", aPackage.getPackageNodeName());
            assertNotNull(resourceResolver.getResource("/etc/packages/testGroup/testPackage-1.zip"));
            Node packageNode = session.getNode("/etc/packages/testGroup/testPackage-1.zip");
            assertNotNull(packageNode);
            verifyPackageFilters(packageNode, Arrays.asList(PAGE_1), referencedResources);
        }

        @Test
        public void shouldCreatePackageWithJcrContentInCaseOfExcludedChildren() throws RepositoryException {
            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();

            initBasePackageInfo(builder, Arrays.asList(PAGE_1));
            builder.withPackageGroup(TEST_GROUP);
            builder.withExcludeChildren(true);

            PackageInfo aPackage = packageService.createPackage(resourceResolver, builder.build());

            assertEquals(CREATED, aPackage.getPackageStatus());
            assertNotNull("testPackage-1.zip", aPackage.getPackageNodeName());
            Node packageNode = session.getNode("/etc/packages/testGroup/testPackage-1.zip");
            assertNotNull(packageNode);
            verifyPackageFilters(packageNode, Arrays.asList(PAGE_1 + "/jcr:content"), referencedResources);
        }

        @Test
        public void shouldCreatePackageWithDefaultGroup() throws RepositoryException {
            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();
            initBasePackageInfo(builder, Arrays.asList(PAGE_1));
            PackageInfo aPackage = packageService.createPackage(resourceResolver, builder.build());

            assertEquals(CREATED, aPackage.getPackageStatus());
            assertNotNull("testPackage-1.zip", aPackage.getPackageNodeName());
            assertNotNull(resourceResolver.getResource("/etc/packages/backpack/testPackage-1.zip"));
            Node packageNode = session.getNode("/etc/packages/backpack/testPackage-1.zip");
            assertNotNull(packageNode);
            verifyPackageFilters(packageNode, Arrays.asList(PAGE_1), referencedResources);

        }

        @Test
        public void shouldNotCreatePackageIfSuchPackageAlreadyExist() throws RepositoryException, IOException {
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.setGroupName(BACKPACK);
            packageInfo.setPackageName(TEST_PACKAGE);
            packageInfo.setVersion(PACKAGE_VERSION);
            packageInfo.setReferencedResources(new HashMap<>());
            packageInfo.setPaths(new ArrayList<>());
            createPackage(packageInfo, new DefaultWorkspaceFilter());

            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();
            initBasePackageInfo(builder, Arrays.asList(PAGE_1));
            PackageInfo aPackage = packageService.createPackage(resourceResolver, builder.build());

            assertEquals(PackageStatus.ERROR, aPackage.getPackageStatus());
            assertEquals("ERROR: Package with such name already exist in the backpack group.", aPackage.getLog().get(0));

            session.removeItem("/etc/packages/backpack/testPackage-1.zip");
        }

        @Test
        public void shouldNotCreatePackageWithoutFilters() throws RepositoryException, IOException {
            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();
            initBasePackageInfo(builder, Collections.emptyList());
            PackageInfo aPackage = packageService.createPackage(resourceResolver, builder.build());

            assertEquals(PackageStatus.ERROR, aPackage.getPackageStatus());
            assertEquals("ERROR: Package does not contain any valid filters.", aPackage.getLog().get(0));
        }

        private void initBasePackageInfo(final PackageRequestInfo.PackageRequestInfoBuilder builder, final List<String> strings) {
            builder.withPaths(strings);
            builder.withPackageName(TEST_PACKAGE);
            builder.withVersion(PACKAGE_VERSION);
        }
    }

    public static class GetPackageInfo extends Base {

        private static final String TEST_ZIP = "/etc/packages/backpack/test.zip";
        private static final String TEST = "test";
        private static final String PACKAGE_PATH = "/etc/packages/backpack/testPackage-1.zip";

        @Test
        public void shouldReturnInMemoryPackageInfo() {
            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();
            builder.withPackagePath(TEST_ZIP);
            Cache<String, PackageInfo> packageInfos = ((PackageServiceImpl) packageService).getPackageInfos();
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.setPackageName(TEST);
            packageInfos.put(TEST_ZIP, packageInfo);

            PackageInfo result = packageService.getPackageInfo(resourceResolver, builder.build());

            assertEquals(TEST, result.getPackageName());
        }

        @Test
        public void shouldReturnNonExistingPackageInfo() {
            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();
            builder.withPackagePath(TEST_ZIP);

            PackageInfo result = packageService.getPackageInfo(resourceResolver, builder.build());

            assertEquals(PackageStatus.ERROR, result.getPackageStatus());
            assertEquals("ERROR: Package by this path " + TEST_ZIP + " doesn't exist in the repository.", result.getLog().get(0));
        }

        @Test
        public void shouldReturnExistingPackageInfo() throws IOException, RepositoryException {
            //create package inside the repository
            PackageInfo packageInfo = getDefaultPackageInfo();
            DefaultWorkspaceFilter defaultWorkspaceFilter = new DefaultWorkspaceFilter();
            defaultWorkspaceFilter.add(new PathFilterSet(PAGE_1));
            createPackage(packageInfo, defaultWorkspaceFilter);

            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();
            builder.withPackagePath(PACKAGE_PATH);

            PackageInfo result = packageService.getPackageInfo(resourceResolver, builder.build());

            assertEquals(PACKAGE_PATH, result.getPackagePath());
            assertEquals(BACKPACK, result.getGroupName());
            assertEquals(TEST_PACKAGE, result.getPackageName());
            assertEquals(PACKAGE_VERSION, result.getVersion());
            assertEquals(referencedResources, result.getReferencedResources());
            assertEquals(PAGE_1, result.getPaths().stream().findFirst().get());
            assertNotNull(result.getDataSize());
            assertEquals(CREATED, result.getPackageStatus());
            assertEquals("testPackage-1.zip", result.getPackageNodeName());
        }
    }
    public static class TestBuildPackage extends Base {

        private static final String PACKAGE_PATH = "/etc/packages/testGroup/testPackage-1.zip";

        @Test
        public void shouldReturnZeroSizeWhenNoReferencesFound() {
            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();

            builder.withPackagePath(PACKAGE_PATH);

            PackageInfo aPackage = packageService.testBuildPackage(resourceResolver, builder.build());

            assertEquals((Long) 0L, aPackage.getDataSize());
            assertEquals(Collections.singletonList("A " + PAGE_1), aPackage.getLog());
            assertNull(aPackage.getPackageBuilt());
        }

        @Test
        public void shouldReturnNonZeroSizeWhenReferencesFound() {
            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();

            builder.withPackagePath(PACKAGE_PATH);
            builder.withReferencedResourceTypes(Collections.singletonList(IMAGE_JPEG));

            PackageInfo aPackage = packageService.testBuildPackage(resourceResolver, builder.build());

            assertNotEquals((Long) 0L, aPackage.getDataSize());
            assertEquals(Arrays.asList("A " + PAGE_1, "A " + PICTURE_1), aPackage.getLog());
            assertNull(aPackage.getPackageBuilt());
        }

        @Test
        public void shouldReturnNonEqualSizeWithDifferentReferences() {
            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();

            builder.withPackagePath(PACKAGE_PATH);
            builder.withReferencedResourceTypes(Collections.singletonList(IMAGE_JPEG));

            PackageInfo firstPackage = packageService.testBuildPackage(resourceResolver, builder.build());

            builder.withReferencedResourceTypes(Arrays.asList(IMAGE_JPEG, IMAGE_PNG));

            PackageInfo secondPackage = packageService.testBuildPackage(resourceResolver, builder.build());

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
            packageInfo.setPaths(Arrays.asList(PAGE_1));
            createPackage(packageInfo, new DefaultWorkspaceFilter());
        }
    }

    public static class getLatestPackageBuildInfo extends Base {
        private static final String PACKAGE_PATH = "/etc/packages/backpack/testPackage-1.zip";
        private static final String TEST = "test_log";
        private static final List<String> TEST_LOG = new ArrayList<>(Arrays.asList(TEST));
        private static final List<String> TEST_EMPTY_LOG = Collections.EMPTY_LIST;
        private static final int LATEST_INDEX = 1;

        @Test
        public void shouldReturnPackageInfoWithLatestLogsIfExist(){
            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();
            builder.withPackagePath(PACKAGE_PATH);
            builder.withLatestLogIndex(LATEST_INDEX);

            Cache<String, PackageInfo> packageInfos = ((PackageServiceImpl) packageService).getPackageInfos();
            PackageInfo packageInfo = new PackageInfo();

            packageInfo.addLogMessage(TEST);
            packageInfo.addLogMessage(TEST);
            packageInfos.put(PACKAGE_PATH, packageInfo);

            PackageInfo result = packageService.getLatestPackageBuildInfo(builder.build());

            assertEquals(TEST_LOG, result.getLog());
        }

        @Test
        public void shouldReturnPackageInfoWithoutLatestLogsIfNotExist() {
            PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();
            builder.withPackagePath(PACKAGE_PATH);

            Cache<String, PackageInfo> packageInfos = ((PackageServiceImpl) packageService).getPackageInfos();
            PackageInfo packageInfo = new PackageInfo();
            packageInfos.put(PACKAGE_PATH, packageInfo);

            PackageInfo result = packageService.getLatestPackageBuildInfo(builder.build());

            assertEquals(TEST_EMPTY_LOG, result.getLog());
        }
    }
}