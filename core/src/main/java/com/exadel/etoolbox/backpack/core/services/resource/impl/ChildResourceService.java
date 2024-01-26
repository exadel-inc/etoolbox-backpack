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
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
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
public class ChildResourceService implements BaseResourceService<PathInfo> {

    private static final Gson GSON = new Gson();

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
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Package not found: " + pathModel.getPackagePath()));
        }

        ResponseWrapper<PathInfo> responseWrapper = new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Unknown action type: " + pathModel.getType()));

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
            case "delete/child":
                responseWrapper = processDeleteChild(pathModel.getPayload(), packageInfo);
                break;
            case "delete/children":
                responseWrapper = processDeleteChildren(resourceResolver, pathModel.getPayload(), packageInfo);
                break;
            case "delete/liveCopy":
                responseWrapper = processDeleteLiveCopies(pathModel.getPayload(), packageInfo);
                break;
            case "delete/page":
                responseWrapper = processDeletePage(pathModel.getPayload(), packageInfo);
                break;
            case "delete/asset":
                responseWrapper = processDeleteAsset(pathModel.getPayload(), packageInfo);
                break;
            case "delete/tag":
                responseWrapper = processDeleteTag(pathModel.getPayload(), packageInfo);
                break;
            default:
                return responseWrapper;
        }

        basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), pathModel.getPackagePath(), packageInfo);

        return responseWrapper;
    }

    private ResponseWrapper<PathInfo> processAddChild(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        Resource resource = resourceResolver.getResource(payload);
        if (resource == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Resource not found: " + payload));
        }
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Page manager not found"));
        }
        Iterator<Page> iterator = pageManager.getPage(resource.getPath()).listChildren();
        while (iterator.hasNext()) {
            Page page = iterator.next();
            packageInfo.getPathInfo(resource.getPath()).getChildren().add(page.getPath());
        }
        return new ResponseWrapper<>(packageInfo.getPathInfo(resource.getPath()), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processAddLiveCopies(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        Resource resource = resourceResolver.getResource(payload);
        if (resource == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Resource not found: " + payload));
        }
        List<String> paths = liveCopySearchService.getLiveCopies(resourceResolver, payload, StringUtils.EMPTY);
        packageInfo.getPathInfo(payload).getLiveCopies().addAll(paths);
        return new ResponseWrapper<>(packageInfo.getPathInfo(resource.getPath()), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processAddPages(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        Resource resource = resourceResolver.getResource(payload);
        if (resource == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Resource not found: " + payload));
        }
        referencesSearchService.getPageReferences(resourceResolver, payload)
                .forEach(page -> packageInfo.getPathInfo(payload).getPages().add(page.getPath()));
        return new ResponseWrapper<>(packageInfo.getPathInfo(resource.getPath()), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processAddAssets(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        Resource resource = resourceResolver.getResource(payload);
        if (resource == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Resource not found: " + payload));
        }
        referencesSearchService.getAssetReferences(resourceResolver, payload)
                .forEach(page -> packageInfo.getPathInfo(payload).getAssets().add(page.getPath()));
        return new ResponseWrapper<>(packageInfo.getPathInfo(resource.getPath()), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processAddTags(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        Resource resource = resourceResolver.getResource(payload);
        if (resource == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Resource not found: " + payload));
        }
        referencesSearchService.getTagReferences(resourceResolver, payload)
                .forEach(page -> packageInfo.getPathInfo(payload).getTags().add(page.getPath()));
        return new ResponseWrapper<>(packageInfo.getPathInfo(resource.getPath()), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processDeleteChildren(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        Resource resource = resourceResolver.getResource(payload);
        if (resource == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Resource not found: " + payload));
        }
        packageInfo.getPathInfo(payload)
                .getChildren()
                .clear();
        return new ResponseWrapper<>(packageInfo.getPathInfo(resource.getPath()), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processDeleteChild(String payload, PackageInfo packageInfo) {
        try {
            List<String> params = parsePayloadToList(payload);
            packageInfo.getPathInfo(params.get(0))
                    .getChildren()
                    .remove(params.get(1));
            return new ResponseWrapper<>(packageInfo.getPathInfo(params.get(0)), ResponseWrapper.ResponseStatus.SUCCESS);
        } catch (JsonSyntaxException | IndexOutOfBoundsException ex) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Invalid payload: " + payload));
        }
    }

    private ResponseWrapper<PathInfo> processDeleteLiveCopies(String payload, PackageInfo packageInfo) {
        try {
            List<String> params = parsePayloadToList(payload);
            packageInfo.getPathInfo(params.get(0))
                    .getLiveCopies()
                    .remove(params.get(1));
            return new ResponseWrapper<>(packageInfo.getPathInfo(params.get(0)), ResponseWrapper.ResponseStatus.SUCCESS);
        } catch (JsonSyntaxException | IndexOutOfBoundsException ex) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Invalid payload: " + payload));
        }
    }

    private ResponseWrapper<PathInfo> processDeletePage(String payload, PackageInfo packageInfo) {
        try {
            List<String> params = parsePayloadToList(payload);
            packageInfo.getPathInfo(params.get(0))
                    .getPages()
                    .remove(params.get(1));
            return new ResponseWrapper<>(packageInfo.getPathInfo(params.get(0)), ResponseWrapper.ResponseStatus.SUCCESS);
        } catch (JsonSyntaxException | IndexOutOfBoundsException ex) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Invalid payload: " + payload));
        }
    }

    private ResponseWrapper<PathInfo> processDeleteAsset(String payload, PackageInfo packageInfo) {
        try {
            List<String> params = parsePayloadToList(payload);
            packageInfo.getPathInfo(params.get(0))
                    .getAssets()
                    .remove(params.get(1));
            return new ResponseWrapper<>(packageInfo.getPathInfo(params.get(0)), ResponseWrapper.ResponseStatus.SUCCESS);
        } catch (JsonSyntaxException | IndexOutOfBoundsException ex) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Invalid payload: " + payload));
        }
    }

    private ResponseWrapper<PathInfo> processDeleteTag(String payload, PackageInfo packageInfo) {
        try {
            List<String> params = parsePayloadToList(payload);
            packageInfo.getPathInfo(params.get(0))
                    .getTags()
                    .remove(params.get(1));
            return new ResponseWrapper<>(packageInfo.getPathInfo(params.get(0)), ResponseWrapper.ResponseStatus.SUCCESS);
        } catch (JsonSyntaxException | IndexOutOfBoundsException ex) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Invalid payload: " + payload));
        }
    }

    private List<String> parsePayloadToList(String payload) throws JsonSyntaxException {
        return GSON.fromJson(payload, new TypeToken<List<String>>() {
        }.getType());
    }
}
