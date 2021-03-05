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
package com.exadel.aembox.backpack.core.services.pckg.impl;

import com.exadel.aembox.backpack.core.dto.response.PackageInfo;
import com.exadel.aembox.backpack.core.dto.response.PackageStatus;
import com.exadel.aembox.backpack.core.services.pckg.CreatePackageService;
import com.exadel.aembox.backpack.core.servlets.model.PackageModel;
import com.exadel.aembox.backpack.core.servlets.model.PathModel;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.junit.Assert;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CreatePackageServiceImplTest extends Base {

    private List<PathModel> expectedInitialFiltersModels = Collections.singletonList(new PathModel(PAGE_1, false));

    private CreatePackageService createPackage;

    @Override
    public void beforeTest() throws IOException, RepositoryException {
        super.beforeTest();
        createPackage = context.registerInjectActivateService(new CreatePackageServiceImpl());
    }

    @Test
    public void shouldCreatePackage() throws RepositoryException {
        PackageModel packageModel = new PackageModel();

        initBasePackageInfo(packageModel, Collections.singletonList(PAGE_1), false);
        packageModel.setGroup(TEST_GROUP);
        resourceResolver = context.resourceResolver();
        PackageInfo aPackage = createPackage.createPackage(resourceResolver, packageModel);

        Assert.assertEquals(PackageStatus.CREATED, aPackage.getPackageStatus());
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

        PackageInfo aPackage = createPackage.createPackage(resourceResolver, packageModel);

        Assert.assertEquals(PackageStatus.CREATED, aPackage.getPackageStatus());
        assertNotNull("testPackage-1.zip", aPackage.getPackageNodeName());
        Node packageNode = session.getNode("/etc/packages/testGroup/testPackage-1.zip");
        assertNotNull(packageNode);
        verifyPackageFilters(packageNode, Collections.singletonList(PAGE_1 + "/jcr:content"), Collections.singletonList(new PathModel(PAGE_1, true)), referencedResources);
    }

    @Test
    public void shouldCreatePackageWithDefaultGroup() throws RepositoryException {
        PackageModel packageModel = new PackageModel();
        initBasePackageInfo(packageModel, Collections.singletonList(PAGE_1), false);
        PackageInfo aPackage = createPackage.createPackage(resourceResolver, packageModel);

        Assert.assertEquals(PackageStatus.CREATED, aPackage.getPackageStatus());
        assertNotNull("testPackage-1.zip", aPackage.getPackageNodeName());
        Node packageNode = session.getNode("/etc/packages/AEMBox_BackPack/testPackage-1.zip");
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
        PackageInfo aPackage = createPackage.createPackage(resourceResolver, packageModel);

        assertEquals(PackageStatus.ERROR, aPackage.getPackageStatus());
        assertEquals("ERROR: Package with this name already exists in the AEMBox_BackPack group.", aPackage.getLog().get(0));

        session.removeItem("/etc/packages/AEMBox_BackPack/testPackage-1.zip");
    }

    @Test
    public void shouldNotCreatePackageWithoutFilters() {
        PackageModel packageModel = new PackageModel();
        initBasePackageInfo(packageModel, Collections.emptyList(), false);
        PackageInfo aPackage = createPackage.createPackage(resourceResolver, packageModel);

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
