package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = BaseHandler.class)
public class PathHandler implements BaseHandler {

    private static final String PATH = "path";

    @Override
    public void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        Resource resource = resourceResolver.getResource(payload);
        if (resource != null) {
            packageInfo.setPaths(Stream.of(
                    Collections.singletonList(resource.getPath()),
                    packageInfo.getPaths())
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));
        } else {
            packageInfo.setPackageStatus(PackageStatus.ERROR);
            packageInfo.setLog(Collections.singletonList("Resource is empty"));
        }
    }

    @Override
    public String getType() {
        return PATH;
    }
}
