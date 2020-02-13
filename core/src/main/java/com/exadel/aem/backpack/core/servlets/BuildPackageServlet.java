
package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.services.PackageService;
import com.google.gson.Gson;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
	private static final String REFERENCED_RESOURCES = "referencedResources";
	public static final String PATH = "path";
	private static final String TEST_BUILD = "testBuild";
	public static final String APPLICATION_JSON = "application/json";
	private static final String LATEST_LOG_INDEX = "latestLogIndex";

	@Reference
	private transient PackageService packageService;

	@Override
	protected void doPost(final SlingHttpServletRequest request,
						  final SlingHttpServletResponse response) throws IOException {
		String packagePath = URLDecoder.decode(request.getParameter(PATH), StandardCharsets.UTF_8.displayName());
		String [] referencedResources = request.getParameterValues(REFERENCED_RESOURCES);
		boolean testBuild = Boolean.parseBoolean(request.getParameter(TEST_BUILD));

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
		response.setContentType(APPLICATION_JSON);
		response.getWriter().write(GSON.toJson(packageInfo));
	}

	@Override
	protected void doGet(final SlingHttpServletRequest request,
						 final SlingHttpServletResponse response) throws IOException {
		String paths = URLDecoder.decode(request.getParameter(PATH), StandardCharsets.UTF_8.displayName());
		int latestLogIndex = NumberUtils.toInt(request.getParameter(LATEST_LOG_INDEX), 0);


		final PackageInfo latestPackageBuildInfo = packageService.getLatestPackageBuildInfo(paths, latestLogIndex);

		response.setContentType(APPLICATION_JSON);
		response.getWriter().write(GSON.toJson(latestPackageBuildInfo));
	}
}

