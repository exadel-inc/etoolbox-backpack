package com.exadel.etoolbox.backpack.core.servlets;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.UploadPackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.impl.UploadPackageServiceImpl;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.impl.RequestAdapterImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UploadPackageServletTest {

    private static final String PARAM_FORCE_UPDATE = "forceUpdate";
    private static final String PARAM_FILEUPLOAD = "fileupload";

    @Rule
    public final AemContext context = new AemContext();
    private final UploadPackageServiceImpl uploadPackageService = mock(UploadPackageServiceImpl.class);
    private UploadPackageServlet servlet;

    @Before
    public void beforeTest() {
        context.registerService(UploadPackageService.class, uploadPackageService);
        context.registerService(RequestAdapter.class, new RequestAdapterImpl());
        servlet = context.registerInjectActivateService(new UploadPackageServlet());
    }

    @Test
    public void shouldReturnBadRequest_whenPackageServiceGetPackageInfoReturnNull() throws IOException {
        when(uploadPackageService.uploadPackage(null, null, false)).thenReturn(getPackageInfoObj());

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals("application/json;charset=UTF-8", context.response().getContentType());
        assertEquals("{\"packageName\":\"some-name.zip\",\"version\":\"some-version\",\"packageStatus\":\"BUILT\",\"packagePath\":\"/some/path\",\"referencedResources\":{},\"log\":[]}", context.response().getOutputAsString());
    }

    @Test
    public void shouldReturnSuccessRequest() throws IOException {
        when(uploadPackageService.uploadPackage(null, null, false)).thenReturn(getPackageInfoObj());

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals("application/json;charset=UTF-8", context.response().getContentType());
        assertEquals("{\"packageName\":\"some-name.zip\",\"version\":\"some-version\",\"packageStatus\":\"BUILT\",\"packagePath\":\"/some/path\",\"referencedResources\":{},\"log\":[]}", context.response().getOutputAsString());
    }


    @Test
    public void shouldReturnBadRequest_whenRequestIsEmpty() throws IOException {
        when(uploadPackageService.uploadPackage(null, null, false)).thenReturn(getErrorPackageInfoObj());

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertEquals("application/json;charset=UTF-8", context.response().getContentType());
        assertEquals("{\"packageStatus\":\"ERROR\",\"referencedResources\":{},\"log\":[]}", context.response().getOutputAsString());
    }

    private PackageInfo getErrorPackageInfoObj() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageStatus(PackageStatus.ERROR);
        return packageInfo;
    }


    private PackageInfo getPackageInfoObj() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackagePath("/some/path");
        packageInfo.setPackageName("some-name.zip");
        packageInfo.setPackageStatus(PackageStatus.BUILT);
        packageInfo.setVersion("some-version");

        return packageInfo;
    }
}