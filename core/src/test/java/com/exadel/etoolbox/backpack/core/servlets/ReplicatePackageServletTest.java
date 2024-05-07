package com.exadel.etoolbox.backpack.core.servlets;

import com.day.cq.wcm.api.WCMException;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.ReplicatePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.impl.BasePackageServiceImpl;
import com.exadel.etoolbox.backpack.core.services.pckg.impl.PackageInfoServiceImpl;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageInfoModel;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.impl.RequestAdapterImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReplicatePackageServletTest {

    private static final String APPLICATION_JSON = "application/json";
    private static final String PACKAGE_PATH = "/etc/packages/testGroup/testPackage-1.zip";
    private static final String PATH_PARAM = "path";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
    private final ReplicatePackageService replicatePackageService = mock(ReplicatePackageService.class);
    private ReplicatePackageServlet servlet;
    private PackageInfo packageInfoWithInstallStatus;

    @Before
    public void beforeTest() throws WCMException {
        context.registerService(ReplicatePackageService.class, replicatePackageService);
        context.registerService(RequestAdapter.class, new RequestAdapterImpl());
        Map<String, Object> properties = new HashMap<>();
        properties.put("buildInfoTTL", 1);
        context.registerInjectActivateService(new BasePackageServiceImpl(), properties);
        context.registerInjectActivateService(new PackageInfoServiceImpl());
        servlet = context.registerInjectActivateService(new ReplicatePackageServlet());
        packageInfoWithInstallStatus = getPackageInfoWithInstallStatus();
    }

    @Test
    public void doPostShouldReturnBadRequestWhenRequestIsEmpty() throws IOException, ServletException {
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    public void doPostShouldReturnOkWithLatestPackageBuildInfo() throws IOException, ServletException {
        createBaseRequest();
        context.request().addRequestParameter("latestLogIndex", "1");
        context.request().addRequestParameter("referencedResources", "[]");
        context.request().addRequestParameter("packagePath", PACKAGE_PATH);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    public void doPostShouldReturnOkWhenRequestIsValid() throws IOException, ServletException {
        createBaseRequest();
        when(replicatePackageService.replicatePackage(any(ResourceResolver.class), any(PackageInfoModel.class))).thenReturn(packageInfoWithInstallStatus);

        context.request().addRequestParameter("referencedResources", "[]");
        context.request().addRequestParameter("packagePath", PACKAGE_PATH);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals(APPLICATION_JSON, context.response().getContentType());
    }

    private void createBaseRequest() {
        context.request().addRequestParameter(PATH_PARAM, PACKAGE_PATH);
    }

    private PackageInfo getPackageInfoWithInstallStatus() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageStatus(PackageStatus.INSTALL);
        return packageInfo;
    }

}