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

import com.day.cq.wcm.api.WCMException;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.SessionService;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.BuildPackageService;
import com.exadel.etoolbox.backpack.core.servlets.model.BuildPackageModel;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class BuildPackageImplTest extends Base {


    private static final String PACKAGE_PATH = "/etc/packages/testGroup/testPackage-1.zip";
    private BuildPackageImpl buildPackageServiceSpy;
    private PackageInfo packageInfo;
    private JcrPackage aPackage;
    private JcrPackageManager jcrPackageManagerMock;
    private BuildPackageService buildPackage;
    private BasePackageService basePackageServiceSpy;
    private SessionService sessionService;

    @Override
    public void beforeTest() throws IOException, RepositoryException, WCMException {
        super.beforeTest();
        buildPackage = context.registerInjectActivateService(new BuildPackageImpl());
    }

    @Test
    public void shouldReturnZeroSizeWhenNoReferencesFound() {
        BuildPackageModel buildPackageModel = new BuildPackageModel();

        buildPackageModel.setPackagePath(PACKAGE_PATH);

        PackageInfo aPackage = buildPackage.testBuildPackage(resourceResolver, buildPackageModel);

        assertEquals((Long) 0L, aPackage.getDataSize());
        assertEquals("A " + PAGE_1, aPackage.getLog().get(0));
        assertNull(aPackage.getPackageBuilt());
    }

    @Test
    public void shouldReturnNonZeroSizeWhenReferencesFound() {
        BuildPackageModel buildPackageModel = new BuildPackageModel();

        buildPackageModel.setPackagePath(PACKAGE_PATH);
        Map<String, List<String>> referencedResourceTypes = new HashMap<>();
        referencedResourceTypes.put(IMAGE_JPEG, Arrays.asList(PICTURE_1));
        buildPackageModel.setReferencedResources(gson.toJson(referencedResourceTypes));
        buildPackageModel.setReferencedResources(gson.toJson(referencedResourceTypes));

        PackageInfo aPackage = buildPackage.testBuildPackage(resourceResolver, buildPackageModel);

        assertNotEquals((Long) 0L, aPackage.getDataSize());
        assertEquals("A " + PAGE_1, aPackage.getLog().get(0));
        assertEquals("A " + PICTURE_1, aPackage.getLog().get(1));
        assertNull(aPackage.getPackageBuilt());
    }

    @Test
    public void shouldReturnNonEqualSizeWithDifferentReferences() {
        BuildPackageModel buildPackageModel = new BuildPackageModel();

        buildPackageModel.setPackagePath(PACKAGE_PATH);
        Map<String, List<String>> referencedResourceTypes = new HashMap<>();
        referencedResourceTypes.put(IMAGE_JPEG, Arrays.asList(PICTURE_1));
        buildPackageModel.setReferencedResources(gson.toJson(referencedResourceTypes));

        PackageInfo firstPackage = buildPackage.testBuildPackage(resourceResolver, buildPackageModel);

        Map<String, List<String>> referencedResourceTypes2 = new LinkedHashMap<>();
        referencedResourceTypes2.put(IMAGE_JPEG, Arrays.asList(PICTURE_1));
        referencedResourceTypes2.put(IMAGE_PNG, Arrays.asList(PICTURE_2));
        buildPackageModel.setReferencedResources(gson.toJson(referencedResourceTypes2));

        PackageInfo secondPackage = buildPackage.testBuildPackage(resourceResolver, buildPackageModel);

        assertNotEquals((Long) 0L, firstPackage.getDataSize());
        assertNotEquals((Long) 0L, secondPackage.getDataSize());
        assertNotEquals(firstPackage.getDataSize(), secondPackage.getDataSize());
        assertEquals("A " + PAGE_1, firstPackage.getLog().get(0));
        assertEquals("A " + PICTURE_1, firstPackage.getLog().get(1));
        assertEquals("A " + PAGE_1, secondPackage.getLog().get(0));
        assertEquals("A " + PICTURE_1, secondPackage.getLog().get(1));
        assertEquals("A " + PICTURE_2, secondPackage.getLog().get(2));
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
        createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, true, false)), new DefaultWorkspaceFilter());
    }


    @Before
    public void before() throws IOException, RepositoryException {
        packageInfo = new PackageInfo();
        packageInfo.setGroupName(TEST_GROUP);
        packageInfo.setPackageName(TEST_PACKAGE);
        packageInfo.setVersion(PACKAGE_VERSION);
        packageInfo.setReferencedResources(referencedResources);
        packageInfo.setPaths(Collections.singletonList(PAGE_1));
        packageInfo.setPackagePath(PACKAGE_PATH);
        aPackage = spy(createPackage(packageInfo, Collections.singletonList(new PathModel(PAGE_1, true, false)), new DefaultWorkspaceFilter()));


        buildPackageServiceSpy = (BuildPackageImpl) spy(buildPackage);
        basePackageServiceSpy = (BasePackageServiceImpl) Mockito.spy(basePackageService);
        jcrPackageManagerMock = mock(JcrPackageManager.class);
        doReturn(jcrPackageManagerMock).when(basePackageServiceSpy).getPackageManager(any(Session.class));
        doReturn(resourceResolver.adaptTo(Session.class)).when(sessionService).getUserImpersonatedSession(any(String.class));
    }

    @Test
    public void shouldBuildPackage() throws RepositoryException {
        doReturn(aPackage).when(jcrPackageManagerMock).open(any(Node.class));
        doReturn(100L).when(aPackage).getSize();

        Map<String, List<String>> referencedResourceTypes = new HashMap<>();
        referencedResourceTypes.put(IMAGE_JPEG, new ArrayList<>());

        buildPackageServiceSpy.buildPackage("admin", packageInfo, gson.toJson(referencedResourceTypes));

        Assert.assertEquals(PackageStatus.BUILT, packageInfo.getPackageStatus());
        assertNotNull(packageInfo.getPackageBuilt());
    }

    @Test
    public void shouldHandleError() {
        Map<String, List<String>> referencedResourceTypes = new HashMap<>();
        referencedResourceTypes.put(IMAGE_JPEG, new ArrayList<>());

        buildPackageServiceSpy.buildPackage("admin", packageInfo, gson.toJson(referencedResourceTypes));

        Assert.assertEquals(PackageStatus.ERROR, packageInfo.getPackageStatus());
        assertEquals("ERROR: Package by this path /etc/packages/testGroup/testPackage-1.zip doesn't exist in the repository.", packageInfo.getLog().get(0));
    }

}
