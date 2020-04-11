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

package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.servlets.model.PackageInfoModel;
import com.exadel.aem.request.RequestAdapter;
import com.exadel.aem.request.validator.ValidatorResponse;
import com.google.gson.Gson;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.exadel.aem.backpack.core.servlets.BuildPackageServlet.APPLICATION_JSON;

/**
 * Serves as the network endpoint for user requests polling for information about previously created and/or built
 * packages<br><br>
 *
 * See also:<br>
 *     {@link CreatePackageServlet} - endpoint for requests for package creation<br>
 *     {@link BuildPackageServlet} - endpoint for requests for building of created package and reporting package status<br>
 */
@Component(service = Servlet.class,
        property = {
                "sling.servlet.paths=" + "/services/backpack/packageInfo",
                "sling.servlet.methods=[get]",

        })
public class PackageInfoServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Gson GSON = new Gson();

    @Reference
    private transient RequestAdapter requestAdapter;
    @Reference
    private transient PackageService packageService;

    /**
     * Processes {@code GET} requests to the current endpoint. Reports information on the specified previously
     * created/built package. Request parameters are parsed to a {@link PackageInfoModel} which is validated and passed
     * to the corresponding {@link PackageService} routine if proven valid; otherwise, the {@code HTTP status 400} reported
     * @param request {@code SlingHttpServletRequest} instance
     * @param response {@code SlingHttpServletResponse} instance
     * @throws IOException in case writing data to the {@code SlingHttpServletResponse} fails
     */
    @Override
    protected void doGet(final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response) throws IOException {
        ValidatorResponse<PackageInfoModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), PackageInfoModel.class);

        response.setContentType(APPLICATION_JSON);

        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse));
        } else {
            PackageInfo packageInfo = packageService.getPackageInfo(request.getResourceResolver(), validatorResponse.getModel());
            response.getWriter().write(GSON.toJson(packageInfo));
        }
    }
}

