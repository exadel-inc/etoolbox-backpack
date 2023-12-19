package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.resource.QueryService;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = BaseHandler.class)
public class QueryHandler implements BaseHandler {

    private static final String QUERY = "query";

    @Reference
    private QueryService queryService;

    @Override
    public void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        List<String> paths = queryService.getResourcesPathsFromQuery(resourceResolver, payload, packageInfo);
        packageInfo.setPaths(Stream.of(paths, packageInfo.getPaths()).flatMap(Collection::stream).collect(Collectors.toSet()));
    }

    @Override
    public String getType() {
        return QUERY;
    }
}
