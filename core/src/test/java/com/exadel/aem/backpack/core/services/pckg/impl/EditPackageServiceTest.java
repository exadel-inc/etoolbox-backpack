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
package com.exadel.aem.backpack.core.services.pckg.impl;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.aem.backpack.core.dto.repository.ReferencedItem;
import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.pckg.EditPackageService;
import com.exadel.aem.backpack.core.servlets.model.PackageModel;
import com.exadel.aem.backpack.core.servlets.model.PathModel;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.exadel.aem.backpack.core.dto.response.PackageStatus.ERROR;
import static com.exadel.aem.backpack.core.dto.response.PackageStatus.MODIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class EditPackageServiceTest extends Base {
    private static final String TEST_GROUP_2 = "testGroup2";
    private static final String TEST_PACKAGE_2 = "testPackage2";

    private PackageInfo packageInfo;
    private EditPackageService editPackageService;

    @Override
    public void beforeTest() throws IOException, RepositoryException {
        super.beforeTest();
        packageInfo = new PackageInfo();
        packageInfo.setGroupName(TEST_GROUP);
        packageInfo.setPackageName(TEST_PACKAGE);
        packageInfo.setVersion(PACKAGE_VERSION);
        packageInfo.setReferencedResources(referencedResources);
        packageInfo.setPaths(Collections.singletonList(PAGE_1));
        packageInfo.setPackagePath(PACKAGE_PATH);
        resourceResolver = context.resourceResolver();
        createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, false)), new DefaultWorkspaceFilter());
        editPackageService = context.registerInjectActivateService(new EditPackageServiceImpl());
    }

    @Test
    public void shouldEditPackage() throws RepositoryException {
        PackageModel packageModel = new PackageModel();
        HashSet<ReferencedItem> assetReferenceItems = new HashSet<>();
        assetReferenceItems.add(new AssetReferencedItem(PICTURE_3, IMAGE_JPEG));
        when(referenceServiceMock.getReferences(any(ResourceResolver.class), any(String.class))).thenReturn(assetReferenceItems);

        Map<String, List<String>> modifiedReferencedResources = new HashMap<>();
        modifiedReferencedResources.put(IMAGE_JPEG, Collections.singletonList(PICTURE_3));

        initBasePackageModel(packageModel, Collections.singletonList(PAGE_2), false);


        PackageInfo aPackage = editPackageService.editPackage(resourceResolver, packageModel);


        assertEquals(MODIFIED, aPackage.getPackageStatus());
        assertNotNull(PACKAGE_2_2_ZIP, aPackage.getPackageNodeName());
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

        PackageInfo aPackage = editPackageService.editPackage(resourceResolver, packageModel);

        assertEquals(ERROR, aPackage.getPackageStatus());
        assertEquals("ERROR: Package with this name already exists in the testGroup2 group.", aPackage.getLog().get(0));
    }

    @Test
    public void shouldModifyPackageFilters() throws RepositoryException {
        PackageModel packageModel = new PackageModel();

        initBasePackageModel(packageModel, Collections.singletonList(PAGE_1), true);

        PackageInfo aPackage = editPackageService.editPackage(resourceResolver, packageModel);

        assertEquals(MODIFIED, aPackage.getPackageStatus());
        assertNotNull(PACKAGE_2_2_ZIP, aPackage.getPackageNodeName());
        Node packageNode = session.getNode("/etc/packages/testGroup2/testPackage2-2.zip");
        assertNotNull(packageNode);
        verifyPackageFilters(packageNode, Collections.singletonList(PAGE_1 + "/jcr:content"), Collections.singletonList(new PathModel(PAGE_1, true)), referencedResources);
    }

    @Test
    public void shouldNotModifyPackageWithoutFilters() {
        PackageModel packageModel = new PackageModel();
        initBasePackageModel(packageModel, Collections.emptyList(), false);
        PackageInfo aPackage = editPackageService.editPackage(resourceResolver, packageModel);

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
