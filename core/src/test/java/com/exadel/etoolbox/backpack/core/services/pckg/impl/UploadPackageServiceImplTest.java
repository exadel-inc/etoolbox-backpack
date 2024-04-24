package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.day.cq.wcm.api.WCMException;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.UploadPackageService;
import org.junit.Assert;
import org.junit.Test;

import javax.jcr.RepositoryException;
import java.io.IOException;

import static org.junit.Assert.*;

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

        PackageInfo packageInfo = uploadPackage.uploadPackage(session, null, false);

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
        assertEquals("{\"packageName\":\"test_back_pack\",\"packageNodeName\":\"test_back_pack.zip\",\"groupName\":\"EToolbox_BackPack\",\"version\":\"\",\"packageBuilt\":{\"year\":2023,\"month\":1,\"dayOfMonth\":19,\"hourOfDay\":20,\"minute\":26,\"second\":44},\"packagePath\":\"/etc/packages/EToolbox_BackPack/test_back_pack.zip\",\"pathInfoMap\":{\"/content/we-retail/ca/en/about-us\":{\"liveCopies\":[],\"pages\":[],\"tags\":[],\"assets\":[]}},\"packageStatus\":\"BUILT\",\"log\":[],\"dataSize\":13016,\"lastModifiedBy\":\"admin\"}", packageInfoJson);
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
        assertEquals("{\"packageName\":\"test_back_pack\",\"packageNodeName\":\"test_back_pack.zip\",\"groupName\":\"EToolbox_BackPack\",\"version\":\"\",\"packageBuilt\":{\"year\":2023,\"month\":1,\"dayOfMonth\":19,\"hourOfDay\":20,\"minute\":26,\"second\":44},\"packagePath\":\"/etc/packages/EToolbox_BackPack/test_back_pack.zip\",\"pathInfoMap\":{\"/content/we-retail/ca/en/about-us\":{\"liveCopies\":[],\"pages\":[],\"tags\":[],\"assets\":[]}},\"packageStatus\":\"BUILT\",\"log\":[],\"dataSize\":13016,\"lastModifiedBy\":\"admin\"}", packageInfoJson);
    }
}