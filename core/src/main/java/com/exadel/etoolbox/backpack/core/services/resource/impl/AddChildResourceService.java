package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PathInfo;
import com.exadel.etoolbox.backpack.core.dto.response.ResponseWrapper;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.resource.BaseResourceService;
import com.exadel.etoolbox.backpack.core.services.resource.LiveCopySearchService;
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
public class AddChildResourceService implements BaseResourceService<PackageInfo> {

    @Reference
    private BasePackageService basePackageService;

    @Reference
    private PackageInfoService packageInfoService;

    @Reference
    private LiveCopySearchService liveCopySearchService;

    @Reference
    private ReferencesSearchService referencesSearchService;

    @Override
    public ResponseWrapper<PackageInfo> process(ResourceResolver resourceResolver, PathModel pathModel) {

        PackageInfo packageInfo = packageInfoService.getPackageInfo(resourceResolver, pathModel.getPackagePath());

        if (packageInfo == null || packageInfo.getPackagePath() == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList(BackpackConstants.PACKAGE_NOT_FOUND + pathModel.getPackagePath()));
        }

        ResponseWrapper<PackageInfo> responseWrapper = new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList(BackpackConstants.UNKNOWN_ACTION_TYPE + pathModel.getType()));

        switch (pathModel.getType()) {
            case "add/children":
                responseWrapper = processAddChild(resourceResolver, pathModel.getPayload(), packageInfo);
                break;
            case "add/liveCopies":
                responseWrapper = processAddLiveCopies(resourceResolver, pathModel.getPayload(), packageInfo);
                break;
            case "add/pages":
                responseWrapper = processAddPages(resourceResolver, pathModel.getPayload(), packageInfo);
                break;
            case "add/assets":
                responseWrapper = processAddAssets(resourceResolver, pathModel.getPayload(), packageInfo);
                break;
            case "add/tags":
                responseWrapper = processAddTags(resourceResolver, pathModel.getPayload(), packageInfo);
                break;
            default:
                return responseWrapper;
        }

        basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), pathModel.getPackagePath(), packageInfo);

        return responseWrapper;
    }

    private ResponseWrapper<PackageInfo> processAddChild(ResourceResolver resourceResolver, List<String> paths, PackageInfo packageInfo) {
        for (String path : paths) {
            Resource resource = resourceResolver.getResource(StringUtils.substringBefore(path, BackpackConstants.JCR_CONTENT));
            if (resource == null) {
                continue;
            }
            packageInfo.setPaths(Stream.of(
                            Collections.singletonList(resource.getPath()),
                            packageInfo.getPaths())
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));
            PathInfo oldPathInfo = packageInfo.getPathInfoMap().getOrDefault(path, new PathInfo());
            Set<String> tags = oldPathInfo.getTags();
            Set<String> assets = oldPathInfo.getAssets();
            Set<String> pages = oldPathInfo.getPages();
            Set<String> liveCopies = oldPathInfo.getLiveCopies();
            PathInfo newPathInfo = packageInfo.getPathInfoMap().getOrDefault(resource.getPath(), new PathInfo());
            newPathInfo.getTags().addAll(tags);
            newPathInfo.getAssets().addAll(assets);
            newPathInfo.getPages().addAll(pages);
            tags.forEach(packageInfo::deletePath);
            assets.forEach(packageInfo::deletePath);
            pages.forEach(packageInfo::deletePath);
            liveCopies.forEach(packageInfo::deletePath);
            packageInfo.getPathInfoMap().remove(path);
            List<String> LiveCopiesPaths = liveCopies.stream().map(liveCopyPath -> liveCopyPath.replace(BackpackConstants.JCR_CONTENT, StringUtils.EMPTY)).collect(Collectors.toList());
            packageInfo.getPathInfo(resource.getPath()).getLiveCopies().addAll(LiveCopiesPaths);
        }
        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PackageInfo> processAddLiveCopies(ResourceResolver resourceResolver, List<String> paths, PackageInfo packageInfo) {
        for (String path : paths) {
            List<String> LiveCopiesPaths = liveCopySearchService.getLiveCopies(resourceResolver, path, StringUtils.EMPTY);
            packageInfo.getPathInfo(path).getLiveCopies().addAll(LiveCopiesPaths);
            basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), packageInfo.getPackagePath(), packageInfo);
        }
        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PackageInfo> processAddPages(ResourceResolver resourceResolver, List<String> paths, PackageInfo packageInfo) {
        for (String path : paths) {
            referencesSearchService.getPageReferences(resourceResolver, path)
                    .forEach(page -> packageInfo.getPathInfo(path).getPages().add(page.getPath()));
            basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), packageInfo.getPackagePath(), packageInfo);
        }
        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PackageInfo> processAddAssets(ResourceResolver resourceResolver, List<String> paths, PackageInfo packageInfo) {
        for (String path : paths) {
            referencesSearchService.getAssetReferences(resourceResolver, path)
                    .forEach(asset -> packageInfo.getPathInfo(path).getAssets().add(asset.getPath()));
            basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), packageInfo.getPackagePath(), packageInfo);
        }
        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PackageInfo> processAddTags(ResourceResolver resourceResolver, List<String> paths, PackageInfo packageInfo) {
        for (String path : paths) {
            referencesSearchService.getTagReferences(resourceResolver, path)
                    .forEach(tag -> packageInfo.getPathInfo(path).getTags().add(tag.getPath()));
            basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), packageInfo.getPackagePath(), packageInfo);
        }
        return new ResponseWrapper<>(packageInfo, ResponseWrapper.ResponseStatus.SUCCESS);
    }
}
