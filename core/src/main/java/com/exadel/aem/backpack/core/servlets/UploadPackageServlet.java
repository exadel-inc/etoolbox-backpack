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

import com.exadel.aem.backpack.core.dto.response.JcrPackageWrapper;
import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.servlets.model.PackageModel;
import com.google.gson.Gson;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Session;
import javax.servlet.Servlet;
import java.io.IOException;
import java.io.PrintWriter;

import static com.exadel.aem.backpack.core.servlets.BuildPackageServlet.APPLICATION_JSON;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;

/**
 * Serves as the network endpoint for user requests that trigger package creation<br><br>
 * <p>
 * See also:<br>
 * {@link BuildPackageServlet} - endpoint for requests for building of created package and reporting package status<br>
 * {@link PackageInfoServlet} - endpoint for requests for information on previously built packages
 */
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/services/backpack/uploadPackage",
                "sling.servlet.methods=[get,post]"
        })
@SuppressWarnings("PackageAccessibility")
// because Servlet and HttpServletResponse classes reported as a non-bundle dependency
public class UploadPackageServlet extends SlingAllMethodsServlet {

    private static final String PARAM_FORCE_UPDATE = "forceUpdate";
    private static final String PARAM_FILEUPLOAD = "fileupload";
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private transient PackageService packageService;

    /**
     * Processes {@code POST} requests to the current endpoint. Attempts to create a package for a consequential build
     * according to the request parameters.
     * Request parameters are parsed to a {@link PackageModel} which is validated and passed
     * to the corresponding {@link PackageService} routine if proven valid; otherwise, the {@code HTTP status 400} reported
     *
     * @param request  {@code SlingHttpServletRequest} instance
     * @param response {@code SlingHttpServletResponse} instance
     * @throws IOException in case writing data to the {@code SlingHttpServletResponse} fails
     */
    @Override
    protected void doPost(final SlingHttpServletRequest request,
                          final SlingHttpServletResponse response) throws IOException {
        Session session = request.getResourceResolver().adaptTo(Session.class);
        byte[] fileUploadBytesArray = getFileUploadBytesArray(request);
        boolean forceUpdate = getForceUpdate(request);

        JcrPackageWrapper jcrPackageWrapper = packageService.uploadPackage(session, fileUploadBytesArray, forceUpdate);

        writeResponse(response, jcrPackageWrapper);
    }

    private byte[] getFileUploadBytesArray(final SlingHttpServletRequest request) {
        String parameter = request.getParameter(PARAM_FILEUPLOAD);
        if (parameter != null) {
            return parameter.getBytes(ISO_8859_1);
        }
        return null;
    }

    private void writeResponse(final SlingHttpServletResponse response, final JcrPackageWrapper jcrPackageWrapper) throws IOException {
        response.setContentType(APPLICATION_JSON);
        response.setCharacterEncoding(UTF_8.name());
        if (jcrPackageWrapper.isExist()) {
            PackageInfo packageInfo = packageService.getPackageInfo(jcrPackageWrapper.getJcrPackage());
            if (packageInfo != null) {
                writeSuccess(response, packageInfo);
                return;
            } else {
                jcrPackageWrapper.setStatusCode(SC_CONFLICT);
                jcrPackageWrapper.setMessage("Something went wrong! Please ask administrator for assistance.");
            }
        }

        writeError(response, jcrPackageWrapper);
    }

    private void writeError(final SlingHttpServletResponse response, JcrPackageWrapper jcrPackageWrapper) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.write(jcrPackageWrapper.getJson());
    }

    private void writeSuccess(final SlingHttpServletResponse response, final PackageInfo packageInfo) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.write(GSON.toJson(packageInfo));
    }

    private boolean getForceUpdate(final SlingHttpServletRequest request) {
        String forceUpdate = request.getParameter(PARAM_FORCE_UPDATE);

        return Boolean.parseBoolean(forceUpdate);
    }
}


