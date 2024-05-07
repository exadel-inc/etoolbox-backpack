package com.exadel.etoolbox.backpack.core.services.resource;

import com.exadel.etoolbox.backpack.core.dto.response.ResponseWrapper;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import org.apache.sling.api.resource.ResourceResolver;

public interface BaseResourceService<T> {

    ResponseWrapper<T> process(ResourceResolver resourceResolver, PathModel pathModel);
}
