
package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.PackageService;
import com.google.gson.Gson;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

import static com.exadel.aem.backpack.core.servlets.BuildPackageServlet.APPLICATION_JSON;


@Component(service = Servlet.class,
		property = {
				"sling.servlet.paths=" + "/services/backpack/createPackage",
				"sling.servlet.methods=[post]",

		})
public class CreatePackageServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	private static final Gson GSON = new Gson();
	private static final String PATHS = "paths";
	private static final String PACKAGE_NAME = "packageName";
	private static final String PACKAGE_GROUP = "packageGroup";
	private static final String VERSION = "version";
	private static final String EXCLUDE_CHILDREN = "excludeChildren";


	@Reference
	private transient PackageService packageService;

	@Override
	protected void doPost(final SlingHttpServletRequest request,
						  final SlingHttpServletResponse response) throws IOException {
		String[] paths = request.getParameterValues(PATHS);

		String packageName = request.getParameter(PACKAGE_NAME);
		String packageGroup = request.getParameter(PACKAGE_GROUP);
		String version = request.getParameter(VERSION);
		boolean excludeChildren = BooleanUtils.toBoolean(request.getParameter(EXCLUDE_CHILDREN));

		final PackageInfo packageInfo = packageService.createPackage(
				request.getResourceResolver(),
				Arrays.asList(paths),
				excludeChildren,
				packageName,
				packageGroup,
				version
		);

		response.setContentType(APPLICATION_JSON);
		response.getWriter().write(GSON.toJson(packageInfo));
		if (!PackageStatus.CREATED.equals(packageInfo.getPackageStatus())) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
		}
	}
}

