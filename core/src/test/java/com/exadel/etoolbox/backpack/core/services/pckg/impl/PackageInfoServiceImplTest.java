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
package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.servlets.model.LatestPackageInfoModel;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageInfoModel;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageModel;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import com.google.common.cache.Cache;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class PackageInfoServiceImplTest extends Base {

    public static class GetPackageModelByPath extends Base {

        private JcrPackage aPackage;
        private JcrPackageManager jcrPackageManagerMock;

        private PackageInfoService packageServiceSpy;
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
            aPackage = createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, false, false)), new DefaultWorkspaceFilter());

        }

        @Test
        public void shouldReturnPackageModelByPath() {
            PackageModel packageModelByPath = packageInfoService.getPackageModelByPath(PACKAGE_PATH, resourceResolver);

            assertEquals(TEST_PACKAGE, packageModelByPath.getPackageName());
            assertEquals(PACKAGE_VERSION, packageModelByPath.getVersion());
            assertEquals(TEST_GROUP, packageModelByPath.getGroup());
            packageModelByPath.getPaths().forEach(pathModel -> {
                assertEquals(PAGE_1, pathModel.getPath());
                assertEquals(false, pathModel.includeChildren());
            });
        }

        @Test
        public void shouldReturnDefaultFiltersWhenInitialNotSpecified() throws IOException, RepositoryException {
            packageInfo.setVersion(PACKAGE_VERSION_2);
            aPackage = createPackage(packageInfo, null, new DefaultWorkspaceFilter());

            PackageModel packageModelByPath = packageInfoService.getPackageModelByPath("/etc/packages/testGroup/testPackage-2.zip", resourceResolver);


            assertEquals(TEST_PACKAGE, packageModelByPath.getPackageName());
            assertEquals(PACKAGE_VERSION_2, packageModelByPath.getVersion());
            assertEquals(TEST_GROUP, packageModelByPath.getGroup());
            packageModelByPath.getPaths().forEach(pathModel -> {
                assertEquals(PAGE_1, pathModel.getPath());
                assertEquals(false, pathModel.includeChildren());
            });
        }

        @Test
        public void shouldReturnNullWhenPackageNotExist() throws IOException, RepositoryException {
            PackageModel packageModelByPath = packageInfoService.getPackageModelByPath("/etc/packages/testGroup/testPackage-3.zip", resourceResolver);

            assertNull(packageModelByPath);
        }

    }

    public static class GetPackageGroups extends Base {


        private PackageInfoService packageServiceSpy;
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
            List<Resource> packageGroups = packageInfoService.getPackageFolders(resourceResolver);

            assertEquals(expectedPath.size(), packageGroups.size());
            for (int i = 0; i < packageGroups.size(); i++) {
                assertEquals(expectedPath.get(i), packageGroups.get(i).getPath());
            }
        }
    }

    public static class GetPackageInfo extends Base {

        private static final String TEST_ZIP = "/etc/packages/EToolbox_BackPack/test.zip";
        private static final String TEST = "test";
        private static final String PACKAGE_PATH = "/etc/packages/EToolbox_BackPack/testPackage-1.zip";

        @Test
        @SuppressWarnings("UnstableApiUsage") // sticking to Guava Cache version bundled in uber-jar; still safe to use
        public void shouldReturnInMemoryPackageInfo() {
            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(TEST_ZIP);
            Cache<String, PackageInfo> packageInfos = basePackageService.getPackageInfos();
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.setPackageName(TEST);
            packageInfos.put(TEST_ZIP, packageInfo);

            PackageInfo result = packageInfoService.getPackageInfo(resourceResolver, packageInfoModel);

            assertEquals(TEST, result.getPackageName());
        }

        @Test
        public void shouldReturnNullWithNonExistingPackage() {
            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(TEST_ZIP);

            PackageInfo result = packageInfoService.getPackageInfo(resourceResolver, packageInfoModel);

            assertNull(result.getPackageStatus());
        }

        @Test
        public void shouldReturnExistingPackageInfo() throws IOException, RepositoryException {
            //create package inside the repository
            PackageInfo packageInfo = getDefaultPackageInfo();
            DefaultWorkspaceFilter defaultWorkspaceFilter = new DefaultWorkspaceFilter();
            defaultWorkspaceFilter.add(new PathFilterSet(PAGE_1));
            createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, true, false)), defaultWorkspaceFilter);

            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(PACKAGE_PATH);

            PackageInfo result = packageInfoService.getPackageInfo(resourceResolver, packageInfoModel);

            assertEquals(PACKAGE_PATH, result.getPackagePath());
            assertEquals(BACKPACK, result.getGroupName());
            assertEquals(TEST_PACKAGE, result.getPackageName());
            assertEquals(PACKAGE_VERSION, result.getVersion());
            assertEquals(referencedResources, result.getReferencedResources());
            assertEquals(PAGE_1, result.getPaths().stream().findFirst().orElse(null));
            assertNotNull(result.getDataSize());
            Assert.assertEquals(PackageStatus.CREATED, result.getPackageStatus());
            assertEquals("testPackage-1.zip", result.getPackageNodeName());
        }

        @Test
        public void shouldReturnRightInfoAfterDeletingPackage() throws IOException, RepositoryException {
            PackageInfo packageInfo = getDefaultPackageInfo();
            DefaultWorkspaceFilter defaultWorkspaceFilter = new DefaultWorkspaceFilter();
            defaultWorkspaceFilter.add(new PathFilterSet(PAGE_1));
            createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, true, false)), defaultWorkspaceFilter);

            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(PACKAGE_PATH);

            PackageInfo result1 = packageInfoService.getPackageInfo(resourceResolver, packageInfoModel);

            packMgr.remove(packMgr.listPackages().get(0));
            defaultWorkspaceFilter.add(new PathFilterSet(PICTURE_1));
            createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, true, false)), defaultWorkspaceFilter);

            PackageInfo result2 = packageInfoService.getPackageInfo(resourceResolver, packageInfoModel);

            assertEquals(result1.getPackageStatus(), result2.getPackageStatus());
            assertEquals(result1.getPackageName(), result2.getPackageName());
            assertEquals(result1.getGroupName(), result2.getGroupName());
            assertEquals(result1.getVersion(), result2.getVersion());
            assertEquals(result1.getPackagePath(), result2.getPackagePath());
            assertNotEquals(result1.getPaths(), result2.getPaths());
        }
    }

    public static class GetLatestPackageBuildInfo extends Base {
        private static final String PACKAGE_PATH = "/etc/packages/EToolbox_BackPack/testPackage-1.zip";
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
                    Cache<String, PackageInfo> packageInfos = basePackageService.getPackageInfos();
            PackageInfo packageInfo = new PackageInfo();

            packageInfo.addLogMessage(TEST);
            packageInfo.addLogMessage(TEST);
            packageInfos.put(PACKAGE_PATH, packageInfo);

            PackageInfo result = packageInfoService.getLatestPackageBuildInfo(latestPackageInfoModel);

            assertEquals(TEST_LOG, result.getLog());
        }

        @Test
        public void shouldReturnPackageInfoWithoutLatestLogsIfNotExist() {
            LatestPackageInfoModel latestPackageInfoModel = new LatestPackageInfoModel();
            latestPackageInfoModel.setPackagePath(PACKAGE_PATH);

            @SuppressWarnings("UnstableApiUsage") // sticking to Guava Cache version bundled in uber-jar; still safe to use
                    Cache<String, PackageInfo> packageInfos = basePackageService.getPackageInfos();
            PackageInfo packageInfo = new PackageInfo();
            packageInfos.put(PACKAGE_PATH, packageInfo);

            PackageInfo result = packageInfoService.getLatestPackageBuildInfo(latestPackageInfoModel);

            assertEquals(TEST_EMPTY_LOG, result.getLog());
        }

        @Test
        public void shouldReturnNonExistingLatestPackageBuildInfo() {
            LatestPackageInfoModel latestPackageInfoModel = new LatestPackageInfoModel();
            latestPackageInfoModel.setPackagePath(PACKAGE_PATH);

            PackageInfo result = packageInfoService.getLatestPackageBuildInfo(latestPackageInfoModel);

            assertEquals(PackageStatus.ERROR, result.getPackageStatus());
            assertEquals("ERROR: Package by this path " + PACKAGE_PATH + " doesn't exist in the repository.", result.getLog().get(0));
        }
    }

    public static class PackageExists extends Base {

        private static final String PACKAGE_PATH = "/etc/packages/EToolbox_BackPack/testPackage-1.zip";

        @Test
        public void shouldReturnFalseWithNonExistingPackage() {
            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(TEST_PACKAGE);

            boolean result = packageInfoService.packageExists(resourceResolver, packageInfoModel);

            assertFalse(result);
        }

        @Test
        public void shouldReturnTrueWithExistingPackage() throws IOException, RepositoryException {
            PackageInfo packageInfo = getDefaultPackageInfo();
            DefaultWorkspaceFilter defaultWorkspaceFilter = new DefaultWorkspaceFilter();
            defaultWorkspaceFilter.add(new PathFilterSet(PAGE_1));
            createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, true, false)), defaultWorkspaceFilter);

            PackageInfoModel packageInfoModel = new PackageInfoModel();
            packageInfoModel.setPackagePath(PACKAGE_PATH);

            boolean result = packageInfoService.packageExists(resourceResolver, packageInfoModel);

            assertTrue(result);
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
