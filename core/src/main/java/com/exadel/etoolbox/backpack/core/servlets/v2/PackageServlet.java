package com.exadel.etoolbox.backpack.core.servlets.v2;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.CreatePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.EditPackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.PackageInfoService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageInfoModel;
import com.exadel.etoolbox.backpack.core.servlets.model.v2.PackageModel;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.validator.ValidatorResponse;
import com.google.gson.Gson;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/services/backpack/package",
                "sling.servlet.methods=[get,post,put]"
        })
@SuppressWarnings("PackageAccessibility")
public class PackageServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final String APPLICATION_JSON = "application/json";

    private static final Gson GSON = new Gson();

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient RequestAdapter requestAdapter;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient PackageInfoService packageInfoService;


    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient CreatePackageService createPackageService;


    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient EditPackageService editPackageService;

    @Override
    protected void doGet(final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response) throws IOException {
        response.setContentType(APPLICATION_JSON);
        ValidatorResponse<PackageInfoModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), PackageInfoModel.class);

        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse));
        } else {
            if (!packageInfoService.packageExists(request.getResourceResolver(), validatorResponse.getModel())) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else {
                PackageInfo packageInfo = packageInfoService.getPackageInfo(request.getResourceResolver(), validatorResponse.getModel().getPackagePath());
                response.getWriter().write(GSON.toJson(packageInfo));
            }
        }
    }

    @Override
    protected void doPost(final SlingHttpServletRequest request,
                          final SlingHttpServletResponse response) throws IOException {
        response.setContentType(APPLICATION_JSON);
        ValidatorResponse<PackageModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), PackageModel.class);
        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse));
        } else {
            final PackageInfo packageInfo = createPackageService.createPackage(
                    request.getResourceResolver(),
                    validatorResponse.getModel()
            );
            response.getWriter().write(GSON.toJson(packageInfo));
            if (!PackageStatus.CREATED.equals(packageInfo.getPackageStatus())) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
            }
        }
    }

    @Override
    protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType(APPLICATION_JSON);
        ValidatorResponse<PackageModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), PackageModel.class);
        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse));
        } else {
            final PackageInfo packageInfo = editPackageService.editPackage(
                    request.getResourceResolver(),
                    validatorResponse.getModel()
            );
            response.getWriter().write(GSON.toJson(packageInfo));
            if (!PackageStatus.MODIFIED.equals(packageInfo.getPackageStatus())) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
            }
        }
    }
}
