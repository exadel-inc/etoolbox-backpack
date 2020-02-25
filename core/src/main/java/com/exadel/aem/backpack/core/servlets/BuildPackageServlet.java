package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import com.exadel.aem.backpack.core.servlets.validation.LatestIndexProcessor;
import com.exadel.aem.backpack.core.servlets.validation.PathProcessor;
import com.exadel.aem.backpack.core.servlets.validation.ReferencedResourceTypesProcessor;
import com.exadel.aem.backpack.core.servlets.validation.TestBuildProcessor;
import com.google.gson.Gson;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component(service = Servlet.class,
        property = {
                "sling.servlet.paths=" + "/services/backpack/buildPackage",
                "sling.servlet.methods=[get,post]",

        })
public class BuildPackageServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Gson GSON = new Gson();
    public static final String APPLICATION_JSON = "application/json";

    @Reference
    private transient PackageService packageService;

    @Override
    protected void doPost(final SlingHttpServletRequest request,
                          final SlingHttpServletResponse response) throws IOException {

        ReferencedResourceTypesProcessor referencedResourcesProcessor = new ReferencedResourceTypesProcessor(null, false);
        TestBuildProcessor testBuildProcessor = new TestBuildProcessor(referencedResourcesProcessor, false);
        PathProcessor packagePathsProcessor = new PathProcessor(testBuildProcessor, true);
        PackageRequestInfo requestInfo = packagePathsProcessor.processRequest(request, PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo());
        response.setContentType(APPLICATION_JSON);

        if (requestInfo.isInvalid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(requestInfo));
        } else {
            PackageInfo packageInfo;
            if (requestInfo.isTestBuild()) {
                packageInfo = packageService.testBuildPackage(request.getResourceResolver(), requestInfo);
            } else {
                packageInfo = packageService.buildPackage(request.getResourceResolver(), requestInfo);
            }
            response.getWriter().write(GSON.toJson(packageInfo));
        }
    }

    @Override
    protected void doGet(final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response) throws IOException {
        LatestIndexProcessor latestIndexProcessor = new LatestIndexProcessor(null, true);
        PathProcessor pathProcessor = new PathProcessor(latestIndexProcessor, true);
        PackageRequestInfo requestInfo = pathProcessor.processRequest(request, PackageRequestInfo.PackageRequestInfoBuilder.aPackageRequestInfo());

        response.setContentType(APPLICATION_JSON);

        if (requestInfo.isInvalid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(requestInfo));
        } else {
            final PackageInfo latestPackageBuildInfo = packageService.getLatestPackageBuildInfo(requestInfo);
            response.getWriter().write(GSON.toJson(latestPackageBuildInfo));
        }
    }
}

