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
import com.exadel.etoolbox.backpack.core.services.pckg.BuildPackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.servlets.model.BuildPackageModel;
import com.exadel.etoolbox.backpack.core.servlets.model.LatestPackageInfoModel;
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
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;

/**
 * Serves as the network endpoint for user requests that trigger start of package building or else poll information
 * on package building progress<br><br>
 * <p>
 * See also:<br>
 * {@link CreatePackageServlet} - endpoint for requests for package creation<br>
 * {@link PackageInfoServlet} - endpoint for requests for information on previously built packages
 */
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/services/backpack/package/build",
                "sling.servlet.methods=[get,post]"
        })
@SuppressWarnings("PackageAccessibility")
// because Servlet and HttpServletResponse classes reported as a non-bundle dependency
public class BuildPackageServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Calendar.class, new CalendarAdapter())
            .create();

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient BuildPackageService buildPackageService;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient PackageInfoService packageInfoService;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient RequestAdapter requestAdapter;

    /**
     * Processes {@code POST} requests to the current endpoint. Attempts to build a package according to the request parameters.
     * Request parameters are parsed to a {@link BuildPackageModel} which is validated and passed
     * to the corresponding {@link BuildPackageService} routine if proven valid; otherwise, the {@code HTTP status 400} reported
     *
     * @param request  {@code SlingHttpServletRequest} instance
     * @param response {@code SlingHttpServletResponse} instance
     * @throws IOException in case writing data to the {@code SlingHttpServletResponse} fails
     */
    @Override
    protected void doPost(final SlingHttpServletRequest request,
                          final SlingHttpServletResponse response) throws IOException {
        response.setContentType(ServletUtils.APPLICATION_JSON_CONTENT_TYPE);
        ValidatorResponse<BuildPackageModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), BuildPackageModel.class);
        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse.getLog()));
        } else {
            PackageInfo packageInfo;
            BuildPackageModel model = validatorResponse.getModel();
            if (model.isTestBuild()) {
                packageInfo = buildPackageService.testBuildPackage(request.getResourceResolver(), model);
            } else {
                packageInfo = buildPackageService.buildPackage(request.getResourceResolver(), model);
            }
            response.getWriter().write(GSON.toJson(packageInfo));
        }
    }

    /**
     * Processes {@code GET} requests to the current endpoint. Reports information on the latest package build status.
     * Request parameters are parsed to a {@link LatestPackageInfoModel} which is validated and passed
     * to the corresponding {@link PackageInfoService} routine if proven valid; otherwise, the {@code HTTP status 400} reported
     *
     * @param request  {@code SlingHttpServletRequest} instance
     * @param response {@code SlingHttpServletResponse} instance
     * @throws IOException in case writing data to the {@code SlingHttpServletResponse} fails
     */
    @Override
    protected void doGet(final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response) throws IOException {
        ValidatorResponse<LatestPackageInfoModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), LatestPackageInfoModel.class);

        response.setContentType(ServletUtils.APPLICATION_JSON_CONTENT_TYPE);

        if (!validatorResponse.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(GSON.toJson(validatorResponse));
        } else {
            final PackageInfo latestPackageBuildInfo = packageInfoService.getLatestPackageBuildInfo(validatorResponse.getModel());
            response.getWriter().write(GSON.toJson(latestPackageBuildInfo));
        }
    }
}

