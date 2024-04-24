package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.day.cq.wcm.api.WCMException;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.ResponseWrapper;
import com.exadel.etoolbox.backpack.core.services.pckg.impl.Base;
import com.exadel.etoolbox.backpack.core.services.util.constants.BackpackConstants;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AddChildResourceServiceTest extends Base {

    private static final String PACKAGE_PATH = "/etc/packages/my_packages/testPackage-1.0.zip";
    private static final String TEST_PACKAGE = "testPackage";
    private static final String PAGE1 = "/content/site/pages/page1";
    private static final String PAGE2 = "/content/site/pages/page2";

    private AddChildResourceService addChildResourceService;
    private PathModel pathModel;

    @Before
    public void beforeTest() throws RepositoryException, IOException, WCMException {
        super.beforeTest();
        addChildResourceService = context.registerInjectActivateService(new AddChildResourceService());

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setGroupName(BackpackConstants.DEFAULT_PACKAGE_GROUP);
        packageInfo.setPackageName(TEST_PACKAGE);
        packageInfo.setPackagePath(PACKAGE_PATH);
        packageInfo.setVersion("1.0");
        DefaultWorkspaceFilter defaultWorkspaceFilter = new DefaultWorkspaceFilter();
        defaultWorkspaceFilter.add(new PathFilterSet(PAGE1));
        defaultWorkspaceFilter.add(new PathFilterSet(PAGE2 + BackpackConstants.JCR_CONTENT));
        createPackage(packageInfo, defaultWorkspaceFilter);
    }

    @Test
    public void shouldProcessAddChildWhenPackageExists() {
        pathModel = new PathModel(PACKAGE_PATH, Collections.singletonList(PAGE2), "add/children");

        ResponseWrapper<PackageInfo> result = addChildResourceService.process(resourceResolver, pathModel);

        assertEquals(ResponseWrapper.ResponseStatus.SUCCESS, result.getStatus());
        assertTrue(result.getData().getPathInfoMap().containsKey(PAGE2));
    }

    @Test
    public void shouldReturnErrorWhenPackageDoesNotExist() {
        pathModel = new PathModel("/unknownPath", Collections.singletonList(PAGE2), "add/children");

        ResponseWrapper<PackageInfo> result = addChildResourceService.process(resourceResolver, pathModel);

        assertEquals(ResponseWrapper.ResponseStatus.ERROR, result.getStatus());
    }

    @Test
    public void shouldReturnErrorWhenUnknownActionType() {
        pathModel = new PathModel(PACKAGE_PATH, Collections.singletonList(PAGE2), "unknownActionType");

        ResponseWrapper<PackageInfo> result = addChildResourceService.process(resourceResolver, pathModel);

        assertEquals(ResponseWrapper.ResponseStatus.ERROR, result.getStatus());
    }

}