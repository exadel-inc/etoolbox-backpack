package com.exadel.aem.backpack.core.datasource;

import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.wcm.datasources.DataSourceBuilder;
import com.adobe.acs.commons.wcm.datasources.DataSourceOption;
import com.day.crx.JcrConstants;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static javax.jcr.query.Query.JCR_SQL2;

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
    private static final String ROOT_KEY = "/etc/packages";
    private static final String ROOT_TEXT = "All packages";

    @Reference
    private DataSourceBuilder dataSourceBuilder;

    @Reference
    private QueryHelper queryHelper;

    @Reference
    private transient PackageService packageService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        ResourceResolver resolver = request.getResourceResolver();
        ValueMap properties = getProperties(request.getResource(), resolver);
        try {
            String queryLanguage = properties.get(PN_DROP_DOWN_QUERY_LANGUAGE, JCR_SQL2);
            String queryStatement = properties.get(PN_DROP_DOWN_QUERY, StringUtils.EMPTY);

            if (StringUtils.isNotBlank(queryStatement)) {
                // perform the query
                List<Resource> results = queryHelper.findResources(resolver, queryLanguage, queryStatement, StringUtils.EMPTY);
                List<DataSourceOption> options = results.stream()
                        .map(resource -> createDataOption(resource))
                        .filter(Objects::nonNull).collect(Collectors.toList());
                RequestParameter groupParam = request.getRequestParameter("group");
                if (groupParam != null) {
                    DataSourceOption firstDataSourceOption = new DataSourceOption(getOptionText(groupParam.getString()), getOptionKey(groupParam.getString()));
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

    private ValueMap getProperties(Resource resource, ResourceResolver resourceResolver) {
        Resource datasource = resource.getChild(DATASOURCE);
        if (datasource == null) {
            return ValueMap.EMPTY;
        }
        ValueMap properties = datasource.getValueMap();
        return properties;
    }

    private DataSourceOption createDataOption(Resource resource) {
        Map<String, Object> params = new HashMap<>();
        params.putAll(resource.getValueMap());
        params.put(JcrConstants.JCR_PATH, resource.getPath());
        return new DataSourceOption(getOptionText(resource.getPath()), getOptionKey(resource.getPath()));
    }

    private String getOptionKey(String resourcePath) {
        return resourcePath;
    }

    private String getOptionText(String resourcePath) {
        String[] path = resourcePath.split("/");
        return path[path.length - 1];
    }
}
