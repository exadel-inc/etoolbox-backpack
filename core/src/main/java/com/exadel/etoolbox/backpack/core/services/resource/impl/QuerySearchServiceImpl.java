package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.resource.QuerySearchService;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component(service = QuerySearchService.class)
public class QuerySearchServiceImpl implements QuerySearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuerySearchServiceImpl.class);

    @Override
    public List<String> getResourcesPathsFromQuery(ResourceResolver resourceResolver, String queryString, List<String> logMessages) {
        Query query = getQuery(resourceResolver, queryString);
        if (query == null) {
            logMessages.add("ERROR: Query execution has not returned any results");
            LOGGER.info("Execution query {} has not returned any results", queryString);
            return Collections.emptyList();
        }
        List<String> paths = new ArrayList<>();
        try {
            NodeIterator nodes = query.execute().getNodes();
            while (nodes.hasNext()) {
                paths.add(nodes.nextNode().getPath());
            }
        } catch (RepositoryException e) {
            logMessages.add("ERROR: Query is not valid");
            LOGGER.error("Cannot get resources");
        }
        return paths;
    }

    /**
     * Called by {@link QueryService#getResourcesPathsFromQuery(ResourceResolver, String, PackageInfo)} to get the {@code Query} instance
     * from SQL2 query
     *
     * @param resourceResolver {@code ResourceResolver} instance used to build the package
     * @param query            {@code String} SQL2 query
     * @return {@code Query} object
     */
    private Query getQuery(ResourceResolver resourceResolver, String query) {
        try {
            Session session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                LOGGER.error("Cannot get Session");
                return null;
            }
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            return queryManager.createQuery(query, Query.JCR_SQL2);
        } catch (InvalidQueryException e) {
            LOGGER.error("Cannot build query from {}", query, e);
        } catch (RepositoryException e) {
            LOGGER.error("Cannot get QueryManager", e);
        }
        return null;
    }
}
