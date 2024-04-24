package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.day.cq.wcm.api.WCMException;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.CreatePackageService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageModel;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.junit.Assert;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CreatePackageServiceImplTest extends Base {

    private CreatePackageService createPackage;

    @Override
    public void beforeTest() throws IOException, RepositoryException, WCMException {
        super.beforeTest();
        createPackage = context.registerInjectActivateService(new CreatePackageServiceImpl());
    }

    @Test
    public void shouldCreatePackage() throws RepositoryException {
        PackageModel packageModel = new PackageModel();

        initBasePackageInfo(packageModel);
        packageModel.setGroup(TEST_GROUP);
        resourceResolver = context.resourceResolver();
        PackageInfo aPackage = createPackage.createPackage(resourceResolver, packageModel);

        Assert.assertEquals(PackageStatus.CREATED, aPackage.getPackageStatus());
        assertNotNull("testPackage-1.zip", aPackage.getPackageNodeName());
        assertNotNull(resourceResolver.getResource("/etc/packages/testGroup/testPackage-1.zip"));
        Node packageNode = session.getNode("/etc/packages/testGroup/testPackage-1.zip");
        assertNotNull(packageNode);
    }

    @Test
    public void shouldCreatePackageWithDefaultGroup() throws RepositoryException {
        PackageModel packageModel = new PackageModel();
        initBasePackageInfo(packageModel);
        PackageInfo aPackage = createPackage.createPackage(resourceResolver, packageModel);

        Assert.assertEquals(PackageStatus.CREATED, aPackage.getPackageStatus());
        assertNotNull("testPackage-1.zip", aPackage.getPackageNodeName());
        Node packageNode = session.getNode("/etc/packages/my_packages/testPackage-1.zip");
        assertNotNull(packageNode);

    }

    @Test
    public void shouldNotCreatePackageIfSuchPackageAlreadyExist() throws RepositoryException, IOException {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setGroupName(MY_PACKAGES_GROUP);
        packageInfo.setPackageName(TEST_PACKAGE);
        packageInfo.setVersion(PACKAGE_VERSION);
        packageInfo.setPaths(new ArrayList<>());
        createPackage(packageInfo, new DefaultWorkspaceFilter());

        PackageModel packageModel = new PackageModel();
        initBasePackageInfo(packageModel);
        PackageInfo aPackage = createPackage.createPackage(resourceResolver, packageModel);

        assertEquals(PackageStatus.ERROR, aPackage.getPackageStatus());
        assertEquals("Package with this name already exists. Consider changing package name or package group", aPackage.getLog().get(0));

        session.removeItem("/etc/packages/my_packages/testPackage-1.zip");
    }

    private void initBasePackageInfo(final PackageModel model) {
        model.setPackageName(TEST_PACKAGE);
        model.setThumbnailPath(THUMBNAIL);
        model.setVersion(PACKAGE_VERSION);
    }
}