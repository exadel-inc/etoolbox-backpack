package com.exadel.etoolbox.backpack.core.services.resource;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

/**
 *  Retrieves resources via a JCR-SQL2 query
 */
public interface QueryService {

    /**
     * The method to get Resources paths by SQL2 query String
     *
     * @param resourceResolver {@code ResourceResolver} instance used to build the package
     * @param queryString {@code String} SQL2 query
     * @return {@code List} of Resources paths
     */
    List<String> getResourcesPathsFromQuery(ResourceResolver resourceResolver, String queryString, PackageInfo packageInfo);
}
