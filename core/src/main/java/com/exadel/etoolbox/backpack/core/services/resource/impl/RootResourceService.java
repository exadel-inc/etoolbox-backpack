package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PathInfo;
import com.exadel.etoolbox.backpack.core.dto.response.ResponseWrapper;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.resource.BaseResourceService;
import com.exadel.etoolbox.backpack.core.services.resource.QuerySearchService;
import com.exadel.etoolbox.backpack.core.services.resource.ReferencesSearchService;
import com.exadel.etoolbox.backpack.core.services.util.constants.BackpackConstants;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import org.apache.commons.lang3.StringUtils;
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
    private ReferencesSearchService referencesSearchService;

    @Reference
    private QuerySearchService querySearchService;

    @Override
    public ResponseWrapper<PackageInfo> process(ResourceResolver resourceResolver, PathModel pathModel) {

        PackageInfo packageInfo = packageInfoService.getPackageInfo(resourceResolver, pathModel.getPackagePath());

        if (packageInfo == null || packageInfo.getPackagePath() == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList(BackpackConstants.PACKAGE_NOT_FOUND + pathModel.getPackagePath()));
        }

        ResponseWrapper<PackageInfo> responseWrapper = new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList(BackpackConstants.UNKNOWN_ACTION_TYPE + pathModel.getType()));

        switch (pathModel.getType()) {
            case "add/path":
                responseWrapper = processPath(resourceResolver, pathModel.getPayload().get(0), packageInfo);
                break;
            case "add/list":
                responseWrapper = processList(resourceResolver, pathModel.getPayload().get(0), packageInfo);
                break;
            case "add/query":
                responseWrapper = processQuery(resourceResolver, pathModel.getPayload().get(0), packageInfo);
                break;
            case "delete/children":
                responseWrapper = processDeleteChildren(resourceResolver, pathModel.getPayload(), packageInfo);
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
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList(BackpackConstants.RESOURCE_NOT_FOUND + payload));
        }
        packageInfo.setPaths(Stream.of(
                        Collections.singletonList(resource.getPath()),
                        packageInfo.getPaths())
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));
        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PackageInfo> processList(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        List<String> log = new ArrayList<>();

        Set<String> paths = parseStringToList(payload).stream()
                .filter(item -> {
                    if (resourceResolver.getResource(item) == null) {
                        log.add(BackpackConstants.RESOURCE_NOT_FOUND + item);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toSet());

        packageInfo.setPaths(Stream.of(paths, packageInfo.getPaths()).flatMap(Collection::stream).collect(Collectors.toSet()));

        if (!log.isEmpty()) {
            return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.WARNING, log);
        }

        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PackageInfo> processQuery(ResourceResolver resourceResolver, String path, PackageInfo packageInfo) {
        List<String> log = new ArrayList<>();
        List<String> paths = querySearchService.getResourcesPathsFromQuery(resourceResolver, path, log);

        if (!log.isEmpty()) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, log);
        }

        packageInfo.setPaths(Stream.of(paths, packageInfo.getPaths()).flatMap(Collection::stream).collect(Collectors.toSet()));
        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PackageInfo> processDelete(List<String> paths, PackageInfo packageInfo) {
        for (String path : paths) {
            if (path.startsWith(BackpackConstants.OPEN_BRACKET) && path.endsWith(BackpackConstants.CLOSE_BRACKET)) {
                String[] split = StringUtils.substringBetween(path, BackpackConstants.OPEN_BRACKET, BackpackConstants.CLOSE_BRACKET).split(BackpackConstants.COMMA);
                if (split.length == 2) {
                    packageInfo.getPathInfoMap().getOrDefault(split[0], new PathInfo()).getAssets().remove(split[1]);
                    packageInfo.getPathInfoMap().getOrDefault(split[0], new PathInfo()).getLiveCopies().remove(split[1]);
                    packageInfo.getPathInfoMap().getOrDefault(split[0], new PathInfo()).getPages().remove(split[1]);
                    packageInfo.getPathInfoMap().getOrDefault(split[0], new PathInfo()).getTags().remove(split[1]);
                    packageInfo.deletePath(split[1]);
                }
            } else {
                packageInfo.deletePath(path);
            }
        }
        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PackageInfo> processDeleteChildren(ResourceResolver resourceResolver, List<String> paths, PackageInfo packageInfo) {
        for (String path : paths) {
            Resource resource = resourceResolver.getResource(path + BackpackConstants.JCR_CONTENT);
            if (resource == null) {
                continue;
            }
            Set<String> assets = packageInfo.getPathInfoMap().getOrDefault(path, new PathInfo()).getAssets();
            assets.forEach(packageInfo::deletePath);
            Set<String> pages = packageInfo.getPathInfoMap().getOrDefault(path, new PathInfo()).getPages();
            pages.forEach(packageInfo::deletePath);
            Set<String> tags = packageInfo.getPathInfoMap().getOrDefault(path, new PathInfo()).getTags();
            tags.forEach(packageInfo::deletePath);
            Set<String> liveCopies = packageInfo.getPathInfoMap().getOrDefault(path, new PathInfo()).getLiveCopies();
            liveCopies.forEach(packageInfo::deletePath);
            packageInfo.getPathInfoMap().remove(path);

            packageInfo.setPaths(Stream.of(
                            Collections.singletonList(resource.getPath()),
                            packageInfo.getPaths())
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));

            referencesSearchService.getAssetReferences(resourceResolver, resource.getPath())
                    .stream().filter(asset -> assets.contains(asset.getPath())).forEach(asset -> packageInfo.getPathInfo(resource.getPath()).getAssets().add(asset.getPath()));
            referencesSearchService.getPageReferences(resourceResolver, resource.getPath())
                    .stream().filter(page -> pages.contains(page.getPath())).forEach(page -> packageInfo.getPathInfo(resource.getPath()).getPages().add(page.getPath()));
            referencesSearchService.getTagReferences(resourceResolver, resource.getPath())
                    .stream().filter(tag -> tags.contains(tag.getPath())).forEach(tag -> packageInfo.getPathInfo(resource.getPath()).getTags().add(tag.getPath()));
            List<String> LiveCopiesPaths = liveCopies.stream().map(liveCopyPath -> liveCopyPath + BackpackConstants.JCR_CONTENT).collect(Collectors.toList());
            packageInfo.getPathInfo(resource.getPath()).getLiveCopies().addAll(LiveCopiesPaths);
        }
    return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
}

    private List<String> parseStringToList(String payload) {
        return Arrays.stream(payload.split(",")).map(String::trim).collect(Collectors.toList());
    }
}
