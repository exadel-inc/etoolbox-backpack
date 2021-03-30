/*
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.etoolbox.backpack.core.servlets;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.EditPackageService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageModel;
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

/**
 * Serves as the network endpoint for user requests that trigger package modification<br><br>
 * <p>
 * See also:<br>
 * {@link BuildPackageServlet} - endpoint for requests for building of created package and reporting package status<br>
 * {@link PackageInfoServlet} - endpoint for requests for information on previously built packages
 * {@link CreatePackageServlet} - endpoint for requests for package creation
 */
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/services/backpack/editPackage",
                "sling.servlet.methods=post"
        })
@SuppressWarnings("PackageAccessibility")
// because Servlet and HttpServletResponse classes reported as a non-bundle dependency
public class EditPackageServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Gson GSON = new Gson();

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient RequestAdapter requestAdapter;


    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient EditPackageService editPackageService;

    /**
     * Processes {@code POST} requests to the current endpoint. Attempts to edit a package for a consequential build
     * according to the request parameters.
     * Request parameters are parsed to a {@link PackageModel} which is validated and passed
     * to the corresponding {@link EditPackageService} routine if proven valid; otherwise, the {@code HTTP status 400} reported
     *
     * @param request  {@code SlingHttpServletRequest} instance
     * @param response {@code SlingHttpServletResponse} instance
     * @throws IOException in case writing data to the {@code SlingHttpServletResponse} fails
     */
    @Override
    protected void doPost(final SlingHttpServletRequest request,
                          final SlingHttpServletResponse response) throws IOException {
        response.setContentType(BuildPackageServlet.APPLICATION_JSON);
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


