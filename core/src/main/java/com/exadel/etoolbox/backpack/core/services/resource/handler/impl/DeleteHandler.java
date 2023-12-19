package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.List;

@Component(service = BaseHandler.class)
public class DeleteHandler implements BaseHandler {

    @Override
    public void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        List<String> paths = new ArrayList<>(packageInfo.getPaths());
        paths.remove(payload);
        packageInfo.setPaths(paths);
    }

    @Override
    public String getType() {
        return "delete";
    }
}
