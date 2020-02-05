
package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.services.PackageService;
import com.google.gson.Gson;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;


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
		String packagePath = request.getParameter("path");
		String [] referencedResources = request.getParameterValues("referencedResources");
		boolean testBuild = Boolean.parseBoolean(request.getParameter("testBuild"));

		PackageInfo packageInfo;
		Collection<String> referencedResList = Collections.emptyList();

		if(ArrayUtils.isNotEmpty(referencedResources)) {
			referencedResList = Arrays.asList(referencedResources);
		}

		if (testBuild) {
			packageInfo = packageService.testBuildPackage(request.getResourceResolver(), packagePath, referencedResList);
		} else {
			packageInfo = packageService.buildPackage(request.getResourceResolver(), packagePath, referencedResList);
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

