package com.exadel.etoolbox.backpack.core.services.resource.handler;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import org.apache.sling.api.resource.ResourceResolver;

public interface BaseHandler {

    void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo);

    String getType();
}
