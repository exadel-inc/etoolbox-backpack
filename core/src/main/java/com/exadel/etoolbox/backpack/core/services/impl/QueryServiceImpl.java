package com.exadel.etoolbox.backpack.core.services.impl;

import com.exadel.etoolbox.backpack.core.services.QueryService;
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
import java.util.NoSuchElementException;

/**
 * Implements {@link QueryService} to provide operations for getting Resources paths from SQL2 query
 */
@Component(service = QueryService.class)
public class QueryServiceImpl implements QueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryServiceImpl.class);

    /**
     * Called by {@link QueryService#getResourcesPathsFromQuery(ResourceResolver, String)} to get the {@code Query} instance
     * from SQL2 query
     * @param resourceResolver {@code ResourceResolver} instance used to build the package
     * @param query {@code String} SQL2 query
     * @return {@code Query} object
     */
    private Query getQuery(ResourceResolver resourceResolver, String query) {
        try {
            Session session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                throw new NoSuchElementException();
            }
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            return queryManager.createQuery(query, Query.JCR_SQL2);
        } catch (InvalidQueryException e) {
            LOGGER.error("cannot build query from {}", query, e);
        } catch (RepositoryException e) {
            LOGGER.error("cannot get QueryManager");
        } catch (NoSuchElementException e) {
            LOGGER.error("cannot get Session");
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getResourcesPathsFromQuery(ResourceResolver resourceResolver, String queryString) {
        Query query = getQuery(resourceResolver, queryString);
        if (query == null) {
            return Collections.emptyList();
        }
        List<String> paths = new ArrayList<>();
        try {
            NodeIterator nodes = query.execute().getNodes();
            while (nodes.hasNext()) {
                paths.add(nodes.nextNode().getPath());
            }
        } catch (RepositoryException e) {
            LOGGER.error("cannot get resources");
        }
        return paths;
    }
}
