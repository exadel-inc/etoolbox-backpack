package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = BaseHandler.class)
public class ListHandler implements BaseHandler {

    private static final String ACTION_LIST = "list";

    @Override
    public void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        List<String> parseLog = new ArrayList<>();

        List<String> paths = parseString(payload, parseLog).stream()
                .filter(path -> resourceResolver.getResource(path) != null)
                .collect(Collectors.toList());

        packageInfo.setLog(parseLog);
        packageInfo.setPaths(Stream.of(paths, packageInfo.getPaths()).flatMap(Collection::stream).collect(Collectors.toSet()));
    }

    @Override
    public String bindActionType() {
        return ACTION_LIST;
    }

    private List<String> parseString(String payload, List<String> log) {
        //todo log errors and processing
        return Arrays.stream(payload.split(",")).map(String::trim).collect(Collectors.toList());
    }
}
