/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.exadel.aem.backpack.core.servlets;

import com.day.cq.commons.jcr.JcrConstants;
import com.exadel.aem.backpack.core.dto.BuildPackageInfo;
import com.exadel.aem.backpack.core.services.PackageService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;


@Component(service = Servlet.class,
		property = {
				"sling.servlet.methods=" + HttpConstants.METHOD_POST,
				"sling.servlet.paths=" + "/services/backpack/createPackage",
				"sling.servlet.methods=post",
		})
public class BuildPackageServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	private static final Gson GSON = new Gson();

	@Reference
	private PackageService packageService;

	@Override
	protected void doPost(final SlingHttpServletRequest request,
						  final SlingHttpServletResponse response) throws IOException {
		String[] paths = request.getParameterValues("paths");
		String packageName = request.getParameter("packageName");
		String packageGroup = request.getParameter("packageGroup");

		final BuildPackageInfo buildPackageInfo = packageService.createPackage(
				request.getResourceResolver(),
				Arrays.asList(paths),
				packageName,
				packageGroup
		);

		response.setContentType("application/json");
		response.getWriter().write(GSON.toJson(buildPackageInfo));
	}
}

