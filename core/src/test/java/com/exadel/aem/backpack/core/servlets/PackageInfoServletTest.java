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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
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
    protected PackageInfo packageInfo = getPackageInfo();

    @Before
    public void beforeTest() {
        when(packageServiceMock.getPackageInfo(any(ResourceResolver.class), any(PackageRequestInfo.class))).thenReturn(packageInfo);
        context.registerService(PackageService.class, packageServiceMock);
        servlet = context.registerInjectActivateService(new PackageInfoServlet());
        GSON = new Gson();
    }

    @Test
    public void shouldReturnBadRequestWithNonExistingPathParameter() throws IOException {
        PackageRequestInfo.PackageRequestInfoBuilder builder = PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo();

        builder.withInvalidMessage(PATH_PARAM + " is mandatory field!");

        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertEquals(GSON.toJson(builder.build()), context.response().getOutputAsString());
    }

    @Test
    public void shouldReturnOkWithExistingPathParameter() throws IOException {
        context.request().addRequestParameter(PATH_PARAM, PACKAGE_PATH);

        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals(GSON.toJson(packageInfo), context.response().getOutputAsString());
    }

    private PackageInfo getPackageInfo() {
        PackageInfo info = new PackageInfo();
        info.setPackagePath(PACKAGE_PATH);
        info.setPackageStatus(PackageStatus.CREATED);
        info.setDataSize(0L);
        info.setGroupName("test");
        info.setPackageName("package");
        info.setVersion("1");
        return info;
    }
}