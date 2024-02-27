package com.exadel.etoolbox.backpack.core.servlets;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.pckg.ReplicatePackageService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageInfoModel;
import com.exadel.etoolbox.backpack.core.util.CalendarAdapter;
import com.exadel.etoolbox.backpack.core.util.ServletUtils;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.validator.ValidatorResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;

/**
 * Serves as the network endpoint for user requests that trigger start of package replication
 */
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/services/backpack/replicatePackage",
                "sling.servlet.methods=[post]"
        }
)
public class ReplicatePackageServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Gson GSON = new GsonBuilder().registerTypeHierarchyAdapter(Calendar.class, new CalendarAdapter()).create();

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient ReplicatePackageService replicatePackageService;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient PackageInfoService packageInfoService;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient RequestAdapter requestAdapter;

    /**
     * Processes {@code POST} requests to the current endpoint. Attempts to replicate a package according to the request parameters.
     * Request parameters are parsed to a {@link PathModel} which is validated and passed
     * to the corresponding {@link ReplicatePackageService} routine if proven valid; otherwise, the {@code HTTP status 400} reported
     *
     * @param request  {@code SlingHttpServletRequest} instance
     * @param response {@code SlingHttpServletResponse} instance
     * @throws IOException in case writing data to the {@code SlingHttpServletResponse} fails
     */
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType(ServletUtils.APPLICATION_JSON_CONTENT_TYPE);
        ValidatorResponse<PackageInfoModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), PackageInfoModel.class);
        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse.getLog()));
        } else {
            PackageInfo packageInfo = replicatePackageService.replicatePackage(request.getResourceResolver(), validatorResponse.getModel());
            response.getWriter().write(GSON.toJson(packageInfo));
        }
    }
}
