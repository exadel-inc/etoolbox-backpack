package com.exadel.etoolbox.backpack.core.servlets;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.CreatePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.EditPackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.impl.RequestAdapterImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageServletTest {
    private static final String PACKAGE_PATH = "/etc/packages/testGroup/testPackage-1.zip";
    private static final String PATH_PARAM = "path";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
    private final CreatePackageService createPackageService = mock(CreatePackageService.class);
    private final EditPackageService editPackageService = mock(EditPackageService.class);
    private final PackageInfoService packageInfoService = mock(PackageInfoService.class);
    private PackageServlet servlet;
    private PackageInfo packageInfoWithCreatedStatus;

    @Mock
    private LiveRelationshipManager liveRelationshipManager;

    @Before
    public void beforeTest() throws WCMException {
        context.registerService(CreatePackageService.class, createPackageService);
        context.registerService(EditPackageService.class, editPackageService);
        context.registerService(PackageInfoService.class, packageInfoService);
        context.registerService(RequestAdapter.class, new RequestAdapterImpl());
        servlet = context.registerInjectActivateService(new PackageServlet());
        packageInfoWithCreatedStatus = getPackageInfoWithCreatedStatus();
    }

    @Test
    public void doGetShouldReturnBadRequestWhenRequestIsEmpty() throws IOException {
        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    public void doGetShouldReturnNotFoundWhenPackageNotExist() throws IOException {
        createBaseRequest();
        context.request().addRequestParameter("packagePath", PACKAGE_PATH);
        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_NOT_FOUND, context.response().getStatus());
    }

    @Test
    public void doPostShouldReturnBadRequestWhenRequestIsEmpty() throws IOException {
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    public void doPostShouldReturnOkWhenRequestIsValid() throws IOException {
        createBaseRequest();
        context.request().addRequestParameter("packagePath", PACKAGE_PATH);
        context.request().addRequestParameter("packageName", "testPackage");
        when(createPackageService.createPackage(any(), any())).thenReturn(packageInfoWithCreatedStatus);
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    public void doPostShouldReturnConflictWhenPackageAlreadyExist() throws IOException {
        createBaseRequest();
        context.request().addRequestParameter("packagePath", PACKAGE_PATH);
        context.request().addRequestParameter("packageName", "testPackage");
        PackageInfo packageInfo = packageInfoWithCreatedStatus;
        packageInfo.setPackageStatus(PackageStatus.ERROR);
        when(createPackageService.createPackage(any(), any())).thenReturn(packageInfo);
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_CONFLICT, context.response().getStatus());
    }

    @Test
    public void doPutShouldReturnBadRequestWhenRequestIsEmpty() throws IOException {
        servlet.doPut(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    public void doPutShouldReturnOkWhenRequestIsValid() throws IOException {
        createBaseRequest();
        context.request().addRequestParameter("packagePath", PACKAGE_PATH);
        context.request().addRequestParameter("packageName", "testPackage");
        PackageInfo packageInfo = packageInfoWithCreatedStatus;
        packageInfo.setPackageStatus(PackageStatus.MODIFIED);
        when(editPackageService.editPackage(any(), any())).thenReturn(packageInfo);
        servlet.doPut(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    public void doPutShouldReturnConflictWhenPackageAlreadyExist() throws IOException {
        createBaseRequest();
        context.request().addRequestParameter("packagePath", PACKAGE_PATH);
        context.request().addRequestParameter("packageName", "testPackage");
        when(editPackageService.editPackage(any(), any())).thenReturn(packageInfoWithCreatedStatus);
        servlet.doPut(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_CONFLICT, context.response().getStatus());
    }

    private void createBaseRequest() {
        context.request().addRequestParameter(PATH_PARAM, PACKAGE_PATH);
    }

    private PackageInfo getPackageInfoWithCreatedStatus() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackagePath(PACKAGE_PATH);
        packageInfo.setPackageName("testPackage");
        packageInfo.setPackageStatus(PackageStatus.CREATED);
        return packageInfo;
    }

}