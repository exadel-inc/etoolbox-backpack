package com.exadel.etoolbox.backpack.core.servlets;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.BuildPackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.impl.BasePackageServiceImpl;
import com.exadel.etoolbox.backpack.core.services.pckg.impl.PackageInfoServiceImpl;
import com.exadel.etoolbox.backpack.core.services.resource.ReferencesSearchService;
import com.exadel.etoolbox.backpack.core.services.resource.impl.LiveCopySearchServiceImpl;
import com.exadel.etoolbox.backpack.core.services.resource.impl.QuerySearchServiceImpl;
import com.exadel.etoolbox.backpack.core.services.resource.impl.ReferencesSearchServiceImpl;
import com.exadel.etoolbox.backpack.core.servlets.model.BuildPackageModel;
import com.exadel.etoolbox.backpack.core.util.CalendarAdapter;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.impl.RequestAdapterImpl;
import com.exadel.etoolbox.backpack.request.validator.ValidatorResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.RangeIterator;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildPackageServletTest {

    private static final String APPLICATION_JSON = "application/json";
    private static final String PACKAGE_PATH = "/etc/packages/testGroup/testPackage-1.zip";
    private static final String PATH_PARAM = "path";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
    private final BuildPackageService buildPackageServiceMock = mock(BuildPackageService.class);
    private BuildPackageServlet servlet;
    private PackageInfo packageInfoTestBuilt;
    private PackageInfo packageInfoWithBuiltStatus;
    private Gson GSON;

    @Mock
    private LiveRelationshipManager liveRelationshipManager;

    @Before
    public void beforeTest() throws WCMException {
        context.registerInjectActivateService(new QuerySearchServiceImpl());
        liveRelationshipManager = mock(LiveRelationshipManager.class);
        context.registerService(LiveRelationshipManager.class, liveRelationshipManager);
        RangeIterator relationships = mock(RangeIterator.class);
        when(relationships.hasNext()).thenReturn(false);
        when(liveRelationshipManager.getLiveRelationships(any(Resource.class), any(), any())).thenReturn(relationships);
        context.registerInjectActivateService(new LiveCopySearchServiceImpl());
        context.registerService(BuildPackageService.class, buildPackageServiceMock);
        context.registerService(RequestAdapter.class, new RequestAdapterImpl());
        context.registerService(ReferencesSearchService.class, new ReferencesSearchServiceImpl());
        Map<String, Object> properties = new HashMap<>();
        properties.put("buildInfoTTL", 1);
        context.registerInjectActivateService(new BasePackageServiceImpl(), properties);
        context.registerInjectActivateService(new PackageInfoServiceImpl());
        servlet = context.registerInjectActivateService(new BuildPackageServlet());
        packageInfoTestBuilt = getTestBuildPackageInfo();
        packageInfoWithBuiltStatus = getPackageInfoWithBuiltStatus();
        GSON = new GsonBuilder().registerTypeHierarchyAdapter(Calendar.class, new CalendarAdapter()).create();
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
        context.request().addRequestParameter("referencedResources", "[]");
        context.request().addRequestParameter("packagePath", PACKAGE_PATH);

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
        when(buildPackageServiceMock.testBuildPackage(any(ResourceResolver.class), any(BuildPackageModel.class))).thenReturn(packageInfoTestBuilt);

        context.request().addRequestParameter("testBuild", "true");
        context.request().addRequestParameter("referencedResources", "[]");
        context.request().addRequestParameter("packagePath", PACKAGE_PATH);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals(GSON.toJson(packageInfoTestBuilt), context.response().getOutputAsString());

    }

    @Test
    public void doPostShouldReturnOkWhenRequestIsValid() throws IOException {
        createBaseRequest();
        when(buildPackageServiceMock.buildPackage(any(ResourceResolver.class), any(BuildPackageModel.class))).thenReturn(packageInfoWithBuiltStatus);

        context.request().addRequestParameter("referencedResources", "[]");
        context.request().addRequestParameter("packagePath", PACKAGE_PATH);

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