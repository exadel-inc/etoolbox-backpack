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
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.UploadPackageService;
import org.junit.Assert;
import org.junit.Test;

import javax.jcr.RepositoryException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class UploadPackageServiceImplTest extends Base {

    private UploadPackageService uploadPackage;

    @Override
    public void beforeTest() throws IOException, RepositoryException, WCMException {
        super.beforeTest();
        uploadPackage = context.registerInjectActivateService(new UploadPackageServiceImpl());
    }

    @Test
    public void shouldReturnJcrPackageWrapperWithEmptyJcrPackage_whenByteArrayEmptyOfZipFile() {

        PackageInfo packageInfo = uploadPackage.uploadPackage(session, "".getBytes(), false);

        Assert.assertNotNull(packageInfo);
        assertEquals("zip file is empty", packageInfo.getLog().get(0));
        assertEquals(PackageStatus.ERROR, packageInfo.getPackageStatus());
    }

    @Test
    public void shouldReturnJcrPackageWrapperWithEmptyJcrPackage_whenByteArrayNullOfZipFile() {

        PackageInfo packageInfo = uploadPackage.uploadPackage(session, null, false);

        Assert.assertNotNull(packageInfo);
        assertEquals("An incorrect value of parameter(s)", packageInfo.getLog().get(0));
        assertEquals(PackageStatus.ERROR, packageInfo.getPackageStatus());
    }

    @Test
    public void shouldReturnJcrPackageWrapperWithEmptyJcrPackage_whenByteArrayNullOfZipFileAndSessionNull() {

        PackageInfo packageInfo = uploadPackage.uploadPackage(null, null, false);

        Assert.assertNotNull(packageInfo);
        assertEquals("An incorrect value of parameter(s)", packageInfo.getLog().get(0));
        assertEquals(PackageStatus.ERROR, packageInfo.getPackageStatus());
    }

    @Test
    public void shouldReturnJcrPackageWrapperWithJcrPackage_whenByteArrayNoEmptyAndZipFileValid() throws IOException, RepositoryException {
        byte[] bytes = PackageInfoServiceImplTest.readByteArrayFromFile("/com/exadel/etoolbox/backpack/core/services/impl/test_back_pack.zip");
        PackageInfo packageInfo = uploadPackage.uploadPackage(session, bytes, false);

        Assert.assertNotNull(packageInfo);

        assertEquals(PackageStatus.BUILT, packageInfo.getPackageStatus());
        assertEquals("test_back_pack", packageInfo.getPackageName());
        assertEquals("EToolbox_BackPack", packageInfo.getGroupName());
    }

    @Test
    public void shouldReturnPackageInfoWithJcrPackage_whenByteArrayNoEmptyAndZipFileValid() {
        byte[] bytes = PackageInfoServiceImplTest.readByteArrayFromFile("/com/exadel/etoolbox/backpack/core/services/impl/test_back_pack.zip");

        PackageInfo packageInfo = uploadPackage.uploadPackage(session, bytes, false);

        Assert.assertNotNull(packageInfo);
        String packageInfoJson = GSON.toJson(packageInfo);
        assertEquals("{\"packageName\":\"test_back_pack\",\"packageNodeName\":\"test_back_pack.zip\",\"groupName\":\"EToolbox_BackPack\",\"version\":\"\",\"packageBuilt\":{\"year\":2023,\"month\":1,\"dayOfMonth\":19,\"hourOfDay\":20,\"minute\":26,\"second\":44},\"packageStatus\":\"BUILT\",\"packagePath\":\"/etc/packages/EToolbox_BackPack/test_back_pack.zip\",\"paths\":[\"/content/we-retail/ca/en/about-us\"],\"referencedResources\":{},\"log\":[],\"dataSize\":13016,\"toggle\":false}", packageInfoJson);
    }

    @Test
    public void shouldReturnWrapperWithErrorMsgPackageAlreadyExist_whenUploadArchiveTwiceWithForceUpdateFalse() {
        byte[] bytes = PackageInfoServiceImplTest.readByteArrayFromFile("/com/exadel/etoolbox/backpack/core/services/impl/test_back_pack.zip");
        PackageInfo packageInfo = uploadPackage.uploadPackage(session, bytes, false);

        Assert.assertNotNull(packageInfo);

        PackageInfo packageInfo2 = uploadPackage.uploadPackage(session, bytes, false);
        assertEquals("Package already exists: /etc/packages/EToolbox_BackPack/test_back_pack.zip", packageInfo2.getLog().get(0));
        assertEquals(PackageStatus.ERROR, packageInfo2.getPackageStatus());

    }

    @Test
    public void shouldReturnRewritePackage_whenUploadArchiveTwiceWithForceUpdateTrue() {
        byte[] bytes = PackageInfoServiceImplTest.readByteArrayFromFile("/com/exadel/etoolbox/backpack/core/services/impl/test_back_pack.zip");
        PackageInfo packageInfo = uploadPackage.uploadPackage(session, bytes, false);

        Assert.assertNotNull(packageInfo);

        PackageInfo packageInfo2 = uploadPackage.uploadPackage(session, bytes, true);
        String packageInfoJson = GSON.toJson(packageInfo2);
        assertEquals("{\"packageName\":\"test_back_pack\",\"packageNodeName\":\"test_back_pack.zip\",\"groupName\":\"EToolbox_BackPack\",\"version\":\"\",\"packageBuilt\":{\"year\":2023,\"month\":1,\"dayOfMonth\":19,\"hourOfDay\":20,\"minute\":26,\"second\":44},\"packageStatus\":\"BUILT\",\"packagePath\":\"/etc/packages/EToolbox_BackPack/test_back_pack.zip\",\"paths\":[\"/content/we-retail/ca/en/about-us\"],\"referencedResources\":{},\"log\":[],\"dataSize\":13016,\"toggle\":false}", packageInfoJson);
    }
}
