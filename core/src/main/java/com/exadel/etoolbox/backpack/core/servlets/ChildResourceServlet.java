package com.exadel.etoolbox.backpack.core.servlets;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.ResponseWrapper;
import com.exadel.etoolbox.backpack.core.services.resource.BaseResourceService;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import com.exadel.etoolbox.backpack.core.util.RequestUtils;
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
import java.util.Map;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/services/backpack/add/children",
                "sling.servlet.paths=/services/backpack/add/liveCopies",
                "sling.servlet.paths=/services/backpack/add/tags",
                "sling.servlet.paths=/services/backpack/add/assets",
                "sling.servlet.paths=/services/backpack/add/pages",
                "sling.servlet.paths=/services/backpack/delete/child",
                "sling.servlet.paths=/services/backpack/delete/children",
                "sling.servlet.paths=/services/backpack/delete/liveCopy",
                "sling.servlet.paths=/services/backpack/delete/tag",
                "sling.servlet.paths=/services/backpack/delete/asset",
                "sling.servlet.paths=/services/backpack/delete/page",
                "sling.servlet.methods=post"
        })
@SuppressWarnings("PackageAccessibility")
public class ChildResourceServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Gson GSON = new Gson();

    private static final String SERVLET_PATH_BASE = "services/backpack/";

    private static final String APPLICATION_JSON = "application/json";

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient RequestAdapter requestAdapter;

    @Reference(target = "(component.name=com.exadel.etoolbox.backpack.core.services.resource.impl.ChildResourceService)")
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient BaseResourceService<PackageInfo> baseResourceService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws IOException {

        response.setContentType(APPLICATION_JSON);

        Map<String, String[]> parameterMap = RequestUtils
                .modifyParameterMap(request.getParameterMap(), "type", StringUtils.substringAfter(request.getPathInfo(), SERVLET_PATH_BASE));

        ValidatorResponse<PathModel> validatorResponse = requestAdapter.adaptValidate(parameterMap, PathModel.class);

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
