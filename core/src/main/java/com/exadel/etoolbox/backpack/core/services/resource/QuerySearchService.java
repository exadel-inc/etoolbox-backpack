package com.exadel.etoolbox.backpack.core.services.resource;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

public interface QuerySearchService {
    List<String> getResourcesPathsFromQuery(ResourceResolver resourceResolver, String queryString, List<String> logMessages);
}
