package com.exadel.etoolbox.backpack.core.services;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

public interface LiveCopyService {

    List<String> getPaths(ResourceResolver resourceResolver, java.lang.String path, boolean includeLiveCopies);
}