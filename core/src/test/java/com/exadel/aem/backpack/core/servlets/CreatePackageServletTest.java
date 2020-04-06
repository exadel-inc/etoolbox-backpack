package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.servlets.model.CreatePackageModel;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreatePackageServletTest {

    public static final String APPLICATION_JSON = "application/json";
    private static final String PAGE_1 = "/content/site/pages/page1";
    private static final String PACKAGE_NAME = "test-package";
    private static final String PACKAGE_NAME_PARAM = "packageName";
    private static final String PACKAGE_PATHS_PARAM = "paths";

    @Rule
    public AemContext context = new AemContext();
    protected CreatePackageServlet servlet;
    protected PackageService packageServiceMock = mock(PackageService.class);
    private PackageInfo packageInfoWithCreatedStatus;
    protected PackageInfo packageInfoWithErrorStatus;

    @Before
    public void beforeTest() {
        packageInfoWithCreatedStatus = getPackageInfoWithCreatedStatus();
        packageInfoWithErrorStatus = getPackageInfoWithErrorStatus();
        context.registerService(PackageService.class, packageServiceMock);
        servlet = context.registerInjectActivateService(new CreatePackageServlet());

        context.create().page(PAGE_1);
    }

    @Test
    public void shouldReturnBadRequestWhenRequestIsEmpty() throws IOException {
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertEquals(APPLICATION_JSON, context.response().getContentType());
    }

    @Test
    public void shouldReturnOkWhenRequestIsValid() throws IOException {
        createBaseRequest();
        when(packageServiceMock.createPackage(any(ResourceResolver.class), any(CreatePackageModel.class))).thenReturn(packageInfoWithCreatedStatus);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals(APPLICATION_JSON, context.response().getContentType());
    }

    @Test
    public void shouldReturnConflictWhenPackageAlreadyExist() throws IOException {
        createBaseRequest();
        when(packageServiceMock.createPackage(any(ResourceResolver.class), any(CreatePackageModel.class))).thenReturn(packageInfoWithErrorStatus);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_CONFLICT, context.response().getStatus());
        assertEquals(APPLICATION_JSON, context.response().getContentType());
    }

    private void createBaseRequest() {
        context.request().addRequestParameter(PACKAGE_NAME_PARAM, PACKAGE_NAME);
        context.request().addRequestParameter(PACKAGE_PATHS_PARAM, PAGE_1);
    }

    private PackageInfo getPackageInfoWithErrorStatus() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageStatus(PackageStatus.ERROR);
        return packageInfo;
    }

    private PackageInfo getPackageInfoWithCreatedStatus() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageStatus(PackageStatus.CREATED);
        return packageInfo;
    }
}