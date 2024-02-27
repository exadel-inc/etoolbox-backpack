package com.exadel.etoolbox.backpack.core.servlets;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.ResponseWrapper;
import com.exadel.etoolbox.backpack.core.services.resource.BaseResourceService;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import com.exadel.etoolbox.backpack.core.util.ServletUtils;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.validator.ValidatorResponse;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
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
                "sling.servlet.paths=/services/backpack/delete/child",
                "sling.servlet.paths=/services/backpack/delete/liveCopy",
                "sling.servlet.paths=/services/backpack/delete/tag",
                "sling.servlet.paths=/services/backpack/delete/asset",
                "sling.servlet.paths=/services/backpack/delete/page",
                "sling.servlet.methods=post"
        })
@SuppressWarnings("PackageAccessibility")
public class DeleteChildResourceServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Gson GSON = new Gson();

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient RequestAdapter requestAdapter;

    @Reference(target = "(component.name=com.exadel.etoolbox.backpack.core.services.resource.impl.DeleteChildResourceService)")
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient BaseResourceService<PackageInfo> baseResourceService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws IOException {

        response.setContentType(ServletUtils.APPLICATION_JSON_CONTENT_TYPE);

        ValidatorResponse<PathModel> validatorResponse = requestAdapter
                .adaptValidate(ServletUtils.addActionTypeToParameterMap(request.getParameterMap(), StringUtils.substringAfter(request.getPathInfo(), ServletUtils.SERVLET_PATH_BASE)), PathModel.class);

        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse));
        } else {

            final ResponseWrapper<PackageInfo> responseWrapper = baseResourceService.process(
                    request.getResourceResolver(),
                    validatorResponse.getModel()
            );

            response.getWriter().write(GSON.toJson(responseWrapper));
        }
    }
}
