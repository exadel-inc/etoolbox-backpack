package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Component(service = BaseResourceService.class)
public class AddChildResourceService implements BaseResourceService<PathInfo> {

    @Reference
    private BasePackageService basePackageService;

    @Reference
    private PackageInfoService packageInfoService;

    @Reference
    private LiveCopySearchService liveCopySearchService;

    @Reference
    private ReferencesSearchService referencesSearchService;

    @Override
    public ResponseWrapper<PathInfo> process(ResourceResolver resourceResolver, PathModel pathModel) {

        PackageInfo packageInfo = packageInfoService.getPackageInfo(resourceResolver, pathModel.getPackagePath());

        if (packageInfo == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList(BackpackConstants.PACKAGE_NOT_FOUND + pathModel.getPackagePath()));
        }

        Resource resource = resourceResolver.getResource(pathModel.getPayload().get(0));

        if (resource == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList(BackpackConstants.PACKAGE_NOT_FOUND + pathModel.getPayload()));
        }

        ResponseWrapper<PathInfo> responseWrapper = new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList(BackpackConstants.UNKNOWN_ACTION_TYPE + pathModel.getType()));

        switch (pathModel.getType()) {
            case "add/children":
                responseWrapper = processAddChild(resourceResolver, resource.getPath(), packageInfo);
                break;
            case "add/liveCopies":
                responseWrapper = processAddLiveCopies(resourceResolver, resource.getPath(), packageInfo);
                break;
            case "add/pages":
                responseWrapper = processAddPages(resourceResolver, resource.getPath(), packageInfo);
                break;
            case "add/assets":
                responseWrapper = processAddAssets(resourceResolver, resource.getPath(), packageInfo);
                break;
            case "add/tags":
                responseWrapper = processAddTags(resourceResolver, resource.getPath(), packageInfo);
                break;
            default:
                return responseWrapper;
        }

        basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), pathModel.getPackagePath(), packageInfo);

        return responseWrapper;
    }

    private ResponseWrapper<PathInfo> processAddChild(ResourceResolver resourceResolver, String path, PackageInfo packageInfo) {
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Page manager not found"));
        }
        Iterator<Page> iterator = pageManager.getPage(path).listChildren();
        while (iterator.hasNext()) {
            Page page = iterator.next();
            packageInfo.getPathInfo(path).getChildren().add(page.getPath());
        }
        basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), packageInfo.getPackagePath(), packageInfo);
        return new ResponseWrapper<>(packageInfo.getPathInfo(path), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processAddLiveCopies(ResourceResolver resourceResolver, String path, PackageInfo packageInfo) {
        List<String> paths = liveCopySearchService.getLiveCopies(resourceResolver, path, StringUtils.EMPTY);
        packageInfo.getPathInfo(path).getLiveCopies().addAll(paths);
        basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), packageInfo.getPackagePath(), packageInfo);
        return new ResponseWrapper<>(packageInfo.getPathInfo(path), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processAddPages(ResourceResolver resourceResolver, String path, PackageInfo packageInfo) {
        referencesSearchService.getPageReferences(resourceResolver, path)
                .forEach(page -> packageInfo.getPathInfo(path).getPages().add(page.getPath()));
        basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), packageInfo.getPackagePath(), packageInfo);
        return new ResponseWrapper<>(packageInfo.getPathInfo(path), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processAddAssets(ResourceResolver resourceResolver, String path, PackageInfo packageInfo) {
        referencesSearchService.getAssetReferences(resourceResolver, path)
                .forEach(asset -> packageInfo.getPathInfo(path).getAssets().add(asset.getPath()));
        basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), packageInfo.getPackagePath(), packageInfo);
        return new ResponseWrapper<>(packageInfo.getPathInfo(path), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processAddTags(ResourceResolver resourceResolver, String path, PackageInfo packageInfo) {
        referencesSearchService.getTagReferences(resourceResolver, path)
                .forEach(tag -> packageInfo.getPathInfo(path).getTags().add(tag.getPath()));
        basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), packageInfo.getPackagePath(), packageInfo);
        return new ResponseWrapper<>(packageInfo.getPathInfo(path), ResponseWrapper.ResponseStatus.SUCCESS);
    }
}
