package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.ResponseWrapper;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.resource.BaseResourceService;
import com.exadel.etoolbox.backpack.core.services.resource.QuerySearchService;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Session;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = BaseResourceService.class)
public class RootResourceService implements BaseResourceService<PackageInfo> {

    @Reference
    private BasePackageService basePackageService;

    @Reference
    private PackageInfoService packageInfoService;

    @Reference
    private QuerySearchService querySearchService;

    @Override
    public ResponseWrapper<PackageInfo> process(ResourceResolver resourceResolver, PathModel pathModel) {

        PackageInfo packageInfo = packageInfoService.getPackageInfo(resourceResolver, pathModel.getPackagePath());

        if (packageInfo == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Package not found: " + pathModel.getPackagePath()));
        }

        ResponseWrapper<PackageInfo> responseWrapper = new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Unknown action type: " + pathModel.getType()));

        switch (pathModel.getType()) {
            case "add/path":
                responseWrapper = processPath(resourceResolver, pathModel.getPayload(), packageInfo);
                break;
            case "add/list":
                responseWrapper = processList(resourceResolver, pathModel.getPayload(), packageInfo);
                break;
            case "add/query":
                responseWrapper = processQuery(resourceResolver, pathModel.getPayload(), packageInfo);
                break;
            case "delete":
                responseWrapper = processDelete(pathModel.getPayload(), packageInfo);
                break;
            default:
                return responseWrapper;
        }

        basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), pathModel.getPackagePath(), packageInfo);

        return responseWrapper;
    }

    private ResponseWrapper<PackageInfo> processPath(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        Resource resource = resourceResolver.getResource(payload);
        if (resource == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Resource not found: " + payload));
        }
        packageInfo.setPaths(Stream.of(
                        Collections.singletonList(resource.getPath()),
                        packageInfo.getPaths())
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));
        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PackageInfo> processList(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        List<String> parseLog = new ArrayList<>();

        Set<String> paths = parseStringToList(payload, parseLog).stream()
                .filter(item -> resourceResolver.getResource(item) != null)
                .collect(Collectors.toSet());

        packageInfo.setPaths(Stream.of(paths, packageInfo.getPaths()).flatMap(Collection::stream).collect(Collectors.toSet()));

        if (!parseLog.isEmpty()) {
            return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.WARNING, parseLog);
        }

        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PackageInfo> processQuery(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        List<String> parseLog = new ArrayList<>();
        List<String> paths = querySearchService.getResourcesPathsFromQuery(resourceResolver, payload, parseLog);

        if (!parseLog.isEmpty()) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, parseLog);
        }

        packageInfo.setPaths(Stream.of(paths, packageInfo.getPaths()).flatMap(Collection::stream).collect(Collectors.toSet()));

        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PackageInfo> processDelete(String payload, PackageInfo packageInfo) {
        packageInfo.deletePath(payload);
        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private List<String> parseStringToList(String payload, List<String> log) {
        //todo log errors and processing
        return Arrays.stream(payload.split(",")).map(String::trim).collect(Collectors.toList());
    }
}
