
package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.services.PackageService;
import com.google.gson.Gson;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;

import static com.exadel.aem.backpack.core.servlets.BuildPackageServlet.APPLICATION_JSON;
import static com.exadel.aem.backpack.core.servlets.BuildPackageServlet.PATH;


@Component(service = Servlet.class,
		property = {
				"sling.servlet.paths=" + "/services/backpack/packageInfo",
				"sling.servlet.methods=[get]",

		})
public class PackageInfoServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	private static final Gson GSON = new Gson();

	@Reference
	private transient PackageService packageService;

	@Override
	protected void doGet(final SlingHttpServletRequest request,
						 final SlingHttpServletResponse response) throws IOException {
		String pathToPackage = request.getParameter(PATH);


		PackageInfo packageInfo = packageService.getPackageInfo(request.getResourceResolver(), pathToPackage);
		response.setContentType(APPLICATION_JSON);
		response.getWriter().write(GSON.toJson(packageInfo));
	}
}

