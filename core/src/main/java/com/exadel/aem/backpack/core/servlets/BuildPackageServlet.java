
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
import java.util.Arrays;


@Component(service = Servlet.class,
		property = {
				"sling.servlet.paths=" + "/services/backpack/buildPackage",
				"sling.servlet.methods=[get,post]",

		})
public class BuildPackageServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	private static final Gson GSON = new Gson();

	@Reference
	private transient PackageService packageService;

	@Override
	protected void doPost(final SlingHttpServletRequest request,
						  final SlingHttpServletResponse response) throws IOException {
		String paths = request.getParameter("path");
/*		String packageName = request.getParameter("packageName");
		String packageGroup = request.getParameter("packageGroup");
		String version = request.getParameter("version");*/
		String testBuild = request.getParameter("testBuild");

		PackageInfo packageInfo;

		if (testBuild != null) {
			packageInfo = packageService.testBuild(request.getResourceResolver(), Arrays.asList(paths));
		} else {
			packageInfo = packageService.buildPackage(request.getResourceResolver(), paths);
		}
		response.setContentType("application/json");
		response.getWriter().write(GSON.toJson(packageInfo));
	}

	@Override
	protected void doGet(final SlingHttpServletRequest request,
						 final SlingHttpServletResponse response) throws IOException {
		String paths = request.getParameter("path");

		final PackageInfo latestPackageBuildInfo = packageService.getLatestPackageBuildInfo(paths);

		response.setContentType("application/json");
		response.getWriter().write(GSON.toJson(latestPackageBuildInfo));
	}
}

