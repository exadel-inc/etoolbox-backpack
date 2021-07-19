package com.exadel.etoolbox.backpack.core.services;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

/**
 *  Represents a service running in an AEM instance responsible for get Resources paths by SQL2 query String
 */
public interface QueryService {

    /**
     * The method to get Resources paths by SQL2 query String
     *
     * @param resourceResolver {@code ResourceResolver} instance used to build the package
     * @param queryString {@code String} SQL2 query
     * @return {@code List} of Resources paths
     */
    List<String> getResourcesPathsFromQuery(ResourceResolver resourceResolver, String queryString);
}
