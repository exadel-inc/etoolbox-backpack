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

import com.exadel.aem.backpack.core.dto.response.JcrPackageWrapper;
import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.services.pckg.UploadPackageService;
import org.junit.Assert;
import org.junit.Test;

import javax.jcr.RepositoryException;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static org.junit.Assert.assertEquals;

public class UploadPackageServiceImplTest extends Base {

    private UploadPackageService uploadPackage;

    @Override
    public void beforeTest() throws IOException, RepositoryException {
        super.beforeTest();
        uploadPackage = context.registerInjectActivateService(new UploadPackageServiceImpl());
    }

    @Test
    public void shouldReturnJcrPackageWrapperWithEmptyJcrPackage_whenByteArrayEmptyOfZipFile() {

        JcrPackageWrapper jcrPackageWrapper = uploadPackage.uploadPackage(session, "".getBytes(), false);

        Assert.assertNotNull(jcrPackageWrapper);
        assertEquals("zip file is empty", jcrPackageWrapper.getMessage());
        assertEquals(SC_CONFLICT, jcrPackageWrapper.getStatusCode());
    }

    @Test
    public void shouldReturnJcrPackageWrapperWithEmptyJcrPackage_whenByteArrayNullOfZipFile() {

        JcrPackageWrapper jcrPackageWrapper = uploadPackage.uploadPackage(session, null, false);

        Assert.assertNotNull(jcrPackageWrapper);
        assertEquals("An incorrect value of parameter(s)", jcrPackageWrapper.getMessage());
        assertEquals(SC_CONFLICT, jcrPackageWrapper.getStatusCode());
    }

    @Test
    public void shouldReturnJcrPackageWrapperWithEmptyJcrPackage_whenByteArrayNullOfZipFileAndSessionNull() {

        JcrPackageWrapper jcrPackageWrapper = uploadPackage.uploadPackage(null, null, false);

        Assert.assertNotNull(jcrPackageWrapper);
        assertEquals("An incorrect value of parameter(s)", jcrPackageWrapper.getMessage());
        assertEquals(SC_CONFLICT, jcrPackageWrapper.getStatusCode());
    }

    @Test
    public void shouldReturnJcrPackageWrapperWithJcrPackage_whenByteArrayNoEmptyAndZipFileValid() throws IOException, RepositoryException {
        byte[] bytes = PackageInfoServiceImplTest.readByteArrayFromFile("/com/exadel/aem/backpack/core/services/impl/test_back_pack.zip");
        JcrPackageWrapper jcrPackageWrapper = uploadPackage.uploadPackage(session, bytes, false);

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
        byte[] bytes = PackageInfoServiceImplTest.readByteArrayFromFile("/com/exadel/aem/backpack/core/services/impl/test_back_pack.zip");
        JcrPackageWrapper jcrPackageWrapper = uploadPackage.uploadPackage(session, bytes, false);

        Assert.assertNotNull(jcrPackageWrapper);
        Assert.assertNull(jcrPackageWrapper.getMessage());

        PackageInfo packageInfo = jcrPackageWrapper.getPackageInfo();

        String packageInfoJson = GSON.toJson(packageInfo);
        assertEquals("{\"packageName\":\"test_back_pack\",\"packageNodeName\":\"test_back_pack.zip\",\"groupName\":\"backpack\",\"version\":\"\",\"packageBuilt\":{\"year\":2020,\"month\":11,\"dayOfMonth\":7,\"hourOfDay\":15,\"minute\":43,\"second\":45},\"packageStatus\":\"BUILT\",\"packagePath\":\"/etc/packages/backpack/test_back_pack.zip\",\"paths\":[\"/content/we-retail/ca/en/about-us\"],\"referencedResources\":{\"image/jpeg\":[\"/content/dam/we-retail/en/activities/hiking-camping/trekker-ama-dablam.jpg\"]},\"log\":[],\"dataSize\":22283}", packageInfoJson);
    }

    @Test
    public void shouldReturnWrapperWithErrorMsgPackageAlreadyExist_whenUploadArchiveTwiceWithForceUpdateFalse() {
        byte[] bytes = PackageInfoServiceImplTest.readByteArrayFromFile("/com/exadel/aem/backpack/core/services/impl/test_back_pack.zip");
        JcrPackageWrapper jcrPackageWrapper = uploadPackage.uploadPackage(session, bytes, false);

        Assert.assertNotNull(jcrPackageWrapper);
        Assert.assertNull(jcrPackageWrapper.getMessage());

        JcrPackageWrapper jcrPackageWrapper2 = uploadPackage.uploadPackage(session, bytes, false);

        assertEquals("Package already exists: /etc/packages/backpack/test_back_pack.zip", jcrPackageWrapper2.getMessage());
        assertEquals(SC_CONFLICT, jcrPackageWrapper2.getStatusCode());

    }

    @Test
    public void shouldReturnRewritePackage_whenUploadArchiveTwiceWithForceUpdateTrue() {
        byte[] bytes = PackageInfoServiceImplTest.readByteArrayFromFile("/com/exadel/aem/backpack/core/services/impl/test_back_pack.zip");
        JcrPackageWrapper jcrPackageWrapper = uploadPackage.uploadPackage(session, bytes, false);

        Assert.assertNotNull(jcrPackageWrapper);
        Assert.assertNull(jcrPackageWrapper.getMessage());

        JcrPackageWrapper jcrPackageWrapper2 = uploadPackage.uploadPackage(session, bytes, true);

        PackageInfo packageInfo2 = jcrPackageWrapper2.getPackageInfo();

        String packageInfoJson = GSON.toJson(packageInfo2);
        assertEquals("{\"packageName\":\"test_back_pack\",\"packageNodeName\":\"test_back_pack.zip\",\"groupName\":\"backpack\",\"version\":\"\",\"packageBuilt\":{\"year\":2020,\"month\":11,\"dayOfMonth\":7,\"hourOfDay\":15,\"minute\":43,\"second\":45},\"packageStatus\":\"BUILT\",\"packagePath\":\"/etc/packages/backpack/test_back_pack.zip\",\"paths\":[\"/content/we-retail/ca/en/about-us\"],\"referencedResources\":{\"image/jpeg\":[\"/content/dam/we-retail/en/activities/hiking-camping/trekker-ama-dablam.jpg\"]},\"log\":[],\"dataSize\":22283}", packageInfoJson);

    }
}
