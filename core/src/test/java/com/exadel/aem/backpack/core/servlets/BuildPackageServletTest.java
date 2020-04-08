package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.servlets.model.BuildPackageModel;
import com.exadel.aem.request.RequestAdapter;
import com.exadel.aem.request.impl.RequestAdapterImpl;
import com.exadel.aem.request.validator.ValidatorResponse;
import com.google.gson.Gson;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildPackageServletTest {

    public static final String APPLICATION_JSON = "application/json";
    private static final String PACKAGE_PATH = "/etc/packages/testGroup/testPackage-1.zip";
    private static final String PATH_PARAM = "path";

    @Rule
    public AemContext context = new AemContext();
    protected BuildPackageServlet servlet;
    protected PackageService packageServiceMock = mock(PackageService.class);
    private PackageInfo packageInfoTestBuilt;
    private PackageInfo packageInfoWithBuiltStatus;
    protected Gson GSON;

    @Before
    public void beforeTest() {
        context.registerService(PackageService.class, packageServiceMock);
        context.registerService(RequestAdapter.class, new RequestAdapterImpl());
        servlet = context.registerInjectActivateService(new BuildPackageServlet());
        packageInfoTestBuilt = getTestBuildPackageInfo();
        packageInfoWithBuiltStatus = getPackageInfoWithBuiltStatus();
        GSON = new Gson();
    }

    @Test
    public void doGetShouldReturnBadRequestWhenRequestIsEmpty() throws IOException {
        ValidatorResponse validatorResponse = new ValidatorResponse();
        validatorResponse.setLog(Arrays.asList("Path field is required", "Latest log index field is required"));

        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertEquals(GSON.toJson(validatorResponse), context.response().getOutputAsString());
    }

    @Test
    public void doGetShouldReturnOkWithLatestPackageBuildInfo() throws IOException {
        createBaseRequest();
        context.request().addRequestParameter("latestLogIndex", "1");

        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    public void doPostShouldReturnBadRequestWhenRequestIsEmpty() throws IOException {
        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    public void doPostShouldReturnOkWhenTestBuildPackage() throws IOException {
        createBaseRequest();
        when(packageServiceMock.testBuildPackage(any(ResourceResolver.class), any(BuildPackageModel.class))).thenReturn(packageInfoTestBuilt);

        context.request().addRequestParameter("testBuild", "true");

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals(GSON.toJson(packageInfoTestBuilt), context.response().getOutputAsString());

    }

    @Test
    public void doPostShouldReturnOkWhenRequestIsValid() throws IOException {
        createBaseRequest();
        when(packageServiceMock.buildPackage(any(ResourceResolver.class), any(BuildPackageModel.class))).thenReturn(packageInfoWithBuiltStatus);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals(APPLICATION_JSON, context.response().getContentType());
    }

    private void createBaseRequest() {
        context.request().addRequestParameter(PATH_PARAM, PACKAGE_PATH);
    }

    private PackageInfo getPackageInfoWithBuiltStatus() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageStatus(PackageStatus.BUILT);
        return packageInfo;
    }

    private PackageInfo getTestBuildPackageInfo() {
        PackageInfo info = new PackageInfo();
        info.addLogMessage("testLog");
        info.setPackageName("testPackage");
        return info;
    }
}