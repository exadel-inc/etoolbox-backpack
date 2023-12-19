package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.resource.ReferenceService;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = BaseHandler.class)
public class ReferenceHandler implements BaseHandler {

    private static final String REFERENCE = "reference";

    @Reference
    private ReferenceService referenceService;

    @Override
    public void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {

    }

    @Override
    public String getType() {
        return REFERENCE;
    }

}
