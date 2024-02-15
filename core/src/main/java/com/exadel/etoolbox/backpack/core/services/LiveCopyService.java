package com.exadel.etoolbox.backpack.core.services;

import com.exadel.etoolbox.backpack.core.dto.response.ResourceRelationships;
import org.apache.sling.api.resource.ResourceResolver;

public interface LiveCopyService {

    ResourceRelationships getResourceRelationships(ResourceResolver resourceResolver, String path, boolean includeLiveCopies);
}
