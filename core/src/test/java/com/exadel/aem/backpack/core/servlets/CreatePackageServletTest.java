package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    protected Set<String> createdPackages = new HashSet<>();

    @Before
    public void beforeTest() {
        when(packageServiceMock.createPackage(any(ResourceResolver.class), any(PackageRequestInfo.class))).thenAnswer(getPackageInfo());
        context.registerService(PackageService.class, packageServiceMock);
        servlet = context.registerInjectActivateService(new CreatePackageServlet());

        context.create().page(PAGE_1);
    }

    @Test
    public void shouldReturnBadRequestWhenRequestIsEmpty() throws IOException {
        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    public void shouldReturnBadRequestWhenRequestHasNonExistingPath() throws IOException {
        createBaseRequest();
        context.request().addRequestParameter(PACKAGE_PATHS_PARAM, "/content/site/pages/page2");

        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    public void shouldReturnOkWhenRequestIsGood() throws IOException {
        createBaseRequest();

        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    public void shouldReturnConflictWhenPackageAlreadyExist() throws IOException {
        createBaseRequest();

        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        createdPackages.add(context.response().getOutputAsString());
        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_CONFLICT, context.response().getStatus());
    }

    @Test
    public void shouldReturnOkWithDifferentGroupAndVersionAndName() throws IOException {
        createBaseRequest();

        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        context.request().addRequestParameter("group", "test");
        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        context.request().addRequestParameter("version", "1");
        servlet.doPost(context.request(), context.response());
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());

        context.request().removeAttribute(PACKAGE_PATHS_PARAM);
        context.request().addRequestParameter(PACKAGE_NAME_PARAM, "test-package2");
        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    private void createBaseRequest() {
        context.request().addRequestParameter(PACKAGE_NAME_PARAM, PACKAGE_NAME);
        context.request().addRequestParameter(PACKAGE_PATHS_PARAM, PAGE_1);
    }

    private String createPackageName(PackageRequestInfo info) {
        return info.getPackageGroup() + "/" + info.getPackageName() + "/" + info.getVersion();
    }

    private Answer<PackageInfo> getPackageInfo() {
        return invocationOnMock -> {
            PackageInfo info = new PackageInfo();
            PackageRequestInfo requestInfo = invocationOnMock.getArgument(1);
            String packagePath = createPackageName(requestInfo);
            if (createdPackages.contains(packagePath)) {
                info.setPackageStatus(PackageStatus.ERROR);
            } else {
                createdPackages.add(packagePath);
                info.setPackageStatus(PackageStatus.CREATED);
            }
            assertFalse(requestInfo.isInvalid());
            assertEquals(context.response().getContentType(), APPLICATION_JSON);
            return info;
        };
    }
}