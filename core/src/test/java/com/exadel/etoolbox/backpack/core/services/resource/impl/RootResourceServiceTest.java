package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.day.cq.wcm.api.WCMException;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.ResponseWrapper;
import com.exadel.etoolbox.backpack.core.services.pckg.impl.Base;
import com.exadel.etoolbox.backpack.core.services.util.constants.BackpackConstants;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RootResourceServiceTest extends Base {

    private static final String PACKAGE_PATH = "/etc/packages/my_packages/testPackage-1.0.zip";
    private static final String TEST_PACKAGE = "testPackage";
    private static final String PAGE1 = "/content/site/pages/page1";
    private static final String PAGE2 = "/content/site/pages/page2";

    private RootResourceService rootResourceService;
    private PathModel pathModel;

    @Before
    public void beforeTest() throws RepositoryException, IOException, WCMException {
        super.beforeTest();
        rootResourceService = context.registerInjectActivateService(new RootResourceService());

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setGroupName(BackpackConstants.DEFAULT_PACKAGE_GROUP);
        packageInfo.setPackageName(TEST_PACKAGE);
        packageInfo.setPackagePath(PACKAGE_PATH);
        packageInfo.setVersion("1.0");
        createPackage(packageInfo, new DefaultWorkspaceFilter());
    }

    @Test
    public void shouldProcessAddChildWhenPackageExists() {
        pathModel = new PathModel(PACKAGE_PATH, Collections.singletonList(PAGE2), "add/path");

        ResponseWrapper<PackageInfo> result = rootResourceService.process(resourceResolver, pathModel);

        assertEquals(ResponseWrapper.ResponseStatus.SUCCESS, result.getStatus());
        assertTrue(result.getData().getPathInfoMap().containsKey(PAGE2));
    }

    @Test
    public void shouldReturnErrorWhenPackageDoesNotExist() {
        pathModel = new PathModel("/unknownPath", Collections.singletonList(PAGE2), "add/path");

        ResponseWrapper<PackageInfo> result = rootResourceService.process(resourceResolver, pathModel);

        assertEquals(ResponseWrapper.ResponseStatus.ERROR, result.getStatus());
    }

    @Test
    public void shouldReturnErrorWhenUnknownActionType() {
        pathModel = new PathModel(PACKAGE_PATH, Collections.singletonList(PAGE2), "unknownActionType");

        ResponseWrapper<PackageInfo> result = rootResourceService.process(resourceResolver, pathModel);

        assertEquals(ResponseWrapper.ResponseStatus.ERROR, result.getStatus());
    }
}