package com.exadel.etoolbox.backpack.core.servlets.v2;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.InstallPackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.PackageInfoService;
import com.exadel.etoolbox.backpack.core.servlets.model.InstallPackageModel;
import com.exadel.etoolbox.backpack.core.util.CalendarAdapter;
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
 * Serves as the network endpoint for user requests that trigger start of package installation <br><br>
 *
 */
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/services/backpack/package/install",
                "sling.servlet.methods=[post]"
        }
)
public class InstallPackageServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 1L;

    private static final Gson GSON = new GsonBuilder().registerTypeHierarchyAdapter(Calendar.class, new CalendarAdapter()).create();
    private static final String APPLICATION_JSON = "application/json";

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient RequestAdapter requestAdapter;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient InstallPackageService installPackageService;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient PackageInfoService packageInfoService;

    /**
     * Processes {@code POST} requests to the current endpoint. Attempts to install a package according to the request parameters.
     * Request parameters are parsed to a {@link InstallPackageModel} which is validated and passed
     * to the corresponding {@link InstallPackageService} routine if proven valid; otherwise, the {@code HTTP status 400} reported
     * @param request {@code SlingHttpServletRequest} instance
     * @param response {@code SlingHttpServletResponse} instance
     * @throws IOException in case writing data to the {@code SlingHttpServletResponse} fails
     */
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        ValidatorResponse<InstallPackageModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), InstallPackageModel.class);
        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse.getLog()));
        } else {
            PackageInfo packageInfo = installPackageService.installPackage(request.getResourceResolver(), validatorResponse.getModel());
            response.getWriter().write(GSON.toJson(packageInfo));
        }
    }
}
