package com.exadel.etoolbox.backpack.core.services.resource;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

public interface LiveCopySearchService {

    List<String> getLiveCopies(ResourceResolver resourceResolver, String path, String sourceSyncPath);
}
