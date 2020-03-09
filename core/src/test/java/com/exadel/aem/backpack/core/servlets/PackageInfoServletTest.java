package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import com.google.gson.Gson;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageInfoServletTest {

    private static final String PACKAGE_PATH = "/etc/package/test/package-1";
    private static final String PATH_PARAM = "path";

    @Rule
    public AemContext context = new AemContext();
    protected PackageInfoServlet servlet;
    protected PackageService packageServiceMock = mock(PackageService.class);
    protected Gson GSON;

    @Before
    public void beforeTest() {
        when(packageServiceMock.getPackageInfo(any(ResourceResolver.class), any(PackageRequestInfo.class))).then(getResult());
        context.registerService(PackageService.class, packageServiceMock);
        servlet = context.registerInjectActivateService(new PackageInfoServlet());
        GSON = new Gson();
    }

    @Test
    public void shouldReturnBadRequestWithEmptyRequest() throws IOException {
        PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();

        builder.withInvalidMessage(PATH_PARAM + " is mandatory field!");

        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertEquals(GSON.toJson(builder.build()), context.response().getOutputAsString());
    }

    @Test
    public void shouldReturnOkWithExistingPackage() throws IOException {
        context.request().addRequestParameter(PATH_PARAM, PACKAGE_PATH);

        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals(GSON.toJson(getPackageExistInfo(PACKAGE_PATH)), context.response().getOutputAsString());
    }

    @Test
    public void shouldReturnOkWithNonExistingPackage() throws IOException {
        context.request().addRequestParameter(PATH_PARAM, "non-existing-path");

        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals(GSON.toJson(getPackageNotExistInfo("non-existing-path")), context.response().getOutputAsString());
    }

    private PackageInfo getPackageExistInfo(String path) {
        PackageInfo info = new PackageInfo();
        info.setPackagePath(path);
        info.setPackageStatus(PackageStatus.CREATED);
        info.setDataSize(0L);
        info.setGroupName("test");
        info.setPackageName("package");
        info.setVersion("1");
        return info;
    }

    private PackageInfo getPackageNotExistInfo(String path) {
        PackageInfo info = new PackageInfo();
        info.setPackagePath(path);
        info.addLogMessage("ERROR: Package by this path " + path + " doesn't exist in the repository.");
        info.setPackageStatus(PackageStatus.ERROR);
        return info;
    }

    private Answer<PackageInfo> getResult() {
        return invocationOnMock -> {
            PackageRequestInfo requestInfo = invocationOnMock.getArgument(1);
            String path = requestInfo.getPackagePath();
            return path.equals(PACKAGE_PATH) ? getPackageExistInfo(path) : getPackageNotExistInfo(path);
        };
    }

}