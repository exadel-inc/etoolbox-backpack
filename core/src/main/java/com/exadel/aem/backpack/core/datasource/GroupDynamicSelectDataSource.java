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

package com.exadel.aem.backpack.core.datasource;

import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.wcm.datasources.DataSourceBuilder;
import com.adobe.acs.commons.wcm.datasources.DataSourceOption;
import com.exadel.aem.backpack.core.services.PackageService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.List;
import java.util.stream.Collectors;

import static javax.jcr.query.Query.JCR_SQL2;

/**
 * Servlet that implements {@code datasource} pattern for populating a TouchUI {@code select} widget
 * with present packages' groups' names
 */
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.resourceTypes=backpack/data-sources/group-dynamic-select",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        }
)
public class GroupDynamicSelectDataSource extends SlingSafeMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(GroupDynamicSelectDataSource.class);
    private static final String PN_DROP_DOWN_QUERY_LANGUAGE = "dropDownQueryLanguage";
    private static final String PN_DROP_DOWN_QUERY = "dropDownQuery";
    private static final String DATASOURCE = "datasource";
    private static final String DEFAULT_PATH_KEY = "/etc/packages/backpack";
    private static final String ROOT_KEY = "/etc/packages";
    private static final String ROOT_TEXT = "All packages";

    @Reference
    private transient DataSourceBuilder dataSourceBuilder;

    @Reference
    private transient QueryHelper queryHelper;

    @Reference
    private transient PackageService packageService;

    /**
     * Processes {@code GET} requests to the current endpoint to add to the {@code SlingHttpServletRequest}
     * a {@code datasource} object filled with names of present packages' groups
     * @param request {@code SlingHttpServletRequest} instance
     * @param response {@code SlingHttpServletResponse} instance
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        ResourceResolver resolver = request.getResourceResolver();
        ValueMap properties = getProperties(request.getResource());
        try {
            String queryLanguage = properties.get(PN_DROP_DOWN_QUERY_LANGUAGE, JCR_SQL2);
            String queryStatement = properties.get(PN_DROP_DOWN_QUERY, StringUtils.EMPTY);

            if (StringUtils.isNotBlank(queryStatement)) {
                // perform the query
                List<Resource> results = queryHelper.findResources(resolver, queryLanguage, queryStatement, StringUtils.EMPTY);
                List<DataSourceOption> options = results.stream()
                        .map(this::createDataOption)
                        .collect(Collectors.toList());
                RequestParameter groupParam = request.getRequestParameter("group");
                if (groupParam != null) {
                    DataSourceOption firstDataSourceOption = new DataSourceOption(getOptionText(groupParam.getString()), groupParam.getString());
                    options.add(0, firstDataSourceOption);
                } else {
                    DataSourceOption firstDataSourceOption = new DataSourceOption(getOptionText(DEFAULT_PATH_KEY), DEFAULT_PATH_KEY);
                    options.add(0, firstDataSourceOption);
                }
                DataSourceOption rootDataSourceOption = new DataSourceOption(ROOT_TEXT, ROOT_KEY);
                options.add(1, rootDataSourceOption);
                dataSourceBuilder.addDataSource(request, options);
            }

        } catch (Exception e) {
            LOG.error("Unable to collect the information to populate the dynamic-select drop-down.", e);
        }
    }

    /**
     * Called from {@link GroupDynamicSelectDataSource#doGet(SlingHttpServletRequest, SlingHttpServletResponse)} to
     * map a {@code Resource} containing datasource option requisites to a {@code DataSourceOption} instance
     * @param resource {@code Resource} object
     * @return {@code DataSourceOption} object
     */
    private DataSourceOption createDataOption(Resource resource) {
        return new DataSourceOption(getOptionText(resource.getPath()), resource.getPath());
    }

    /**
     * Gets {@code datasource} option label by returning the trailing element of an underlying JCR path
     * @param resourcePath String representing a JCR path
     * @return String value
     */
    private String getOptionText(String resourcePath) {
        String[] path = resourcePath.split("/");
        return path[path.length - 1];
    }
}
