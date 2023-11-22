package com.exadel.etoolbox.backpack.core.servlets;

import com.exadel.etoolbox.backpack.core.services.pckg.DeletePackageService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves as the network endpoint for user requests that delete package<br><br>
 * <p>
 * See also:<br>
 * {@link BuildPackageServlet} - endpoint for requests for building of created package and reporting package status<br>
 * {@link PackageInfoServlet} - endpoint for requests for information on previously built packages
 * {@link CreatePackageServlet} - endpoint for requests for package creation
 */
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/services/backpack/deletePackage",
                "sling.servlet.methods=delete"
        })
@SuppressWarnings("PackageAccessibility")
public class DeletePackageServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final String PACKAGE_PATH_REQUEST_PARAMETER = "path";

    @Reference
    @SuppressWarnings("UnusedDeclaration")
    private transient DeletePackageService deletePackageService;

    /**
     * Processes {@code DELETE} requests to the current endpoint. Attempts to delete a package and clear cache info about package
     * Request parameters are parsed to a {@link String} with path to package and passed
     * to the corresponding {@link DeletePackageService} routine if proven not null; otherwise, the {@code HTTP status 404} reported
     *
     * @param request  {@code SlingHttpServletRequest} instance
     * @param response {@code SlingHttpServletResponse} instance
     */
    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        RequestParameter requestParameter = request.getRequestParameter(PACKAGE_PATH_REQUEST_PARAMETER);
        if (requestParameter != null) {
            deletePackageService.delete(request.getResourceResolver(), requestParameter.getString());
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
