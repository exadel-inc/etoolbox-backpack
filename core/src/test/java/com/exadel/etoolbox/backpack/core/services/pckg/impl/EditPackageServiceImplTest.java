package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.day.cq.wcm.api.WCMException;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.EditPackageService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageModel;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.junit.Assert;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EditPackageServiceImplTest extends Base {
    private static final String TEST_GROUP_2 = "testGroup2";
    private static final String TEST_PACKAGE_2 = "testPackage2";

    private PackageInfo packageInfo;
    private EditPackageService editPackageService;

    @Override
    public void beforeTest() throws IOException, RepositoryException, WCMException {
        super.beforeTest();
        packageInfo = new PackageInfo();
        packageInfo.setGroupName(TEST_GROUP);
        packageInfo.setPackageName(TEST_PACKAGE);
        packageInfo.setVersion(PACKAGE_VERSION);
        packageInfo.setPaths(Collections.singletonList(PAGE_1));
        packageInfo.setPackagePath(PACKAGE_PATH);
        resourceResolver = context.resourceResolver();
        createPackage(packageInfo, new DefaultWorkspaceFilter());
        editPackageService = context.registerInjectActivateService(new EditPackageServiceImpl());
    }

    @Test
    public void shouldEditPackage() throws RepositoryException {
        PackageModel packageModel = new PackageModel();
        initBasePackageModel(packageModel);

        PackageInfo aPackage = editPackageService.editPackage(resourceResolver, packageModel);

        Assert.assertEquals(PackageStatus.MODIFIED, aPackage.getPackageStatus());
        assertNotNull(PACKAGE_2_2_ZIP, aPackage.getPackageNodeName());
        Node packageNode = session.getNode("/etc/packages/testGroup2/testPackage2-2.zip");
        assertNotNull(packageNode);
    }

    @Test
    public void shouldSkipPackageEditingIfSuchPackageAlreadyExist() throws RepositoryException, IOException {
        packageInfo.setGroupName(TEST_GROUP_2);
        packageInfo.setPackageName(TEST_PACKAGE_2);
        packageInfo.setVersion(PACKAGE_VERSION_2);
        createPackage(packageInfo, new DefaultWorkspaceFilter());
        PackageModel packageModel = new PackageModel();
        initBasePackageModel(packageModel);

        PackageInfo aPackage = editPackageService.editPackage(resourceResolver, packageModel);

        Assert.assertEquals(PackageStatus.ERROR, aPackage.getPackageStatus());
        assertEquals("Package with this name already exists. Consider changing package name or package group", aPackage.getLog().get(0));
    }

    private void initBasePackageModel(final PackageModel model) {
        model.setPackageName(TEST_PACKAGE_2);
        model.setGroup(TEST_GROUP_2);
        model.setThumbnailPath(THUMBNAIL);
        model.setVersion(PACKAGE_VERSION_2);
        model.setPackagePath("/etc/packages/testGroup/testPackage-1.zip");
    }
}