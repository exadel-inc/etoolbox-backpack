package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PathInfo;
import com.exadel.etoolbox.backpack.core.dto.response.ResponseWrapper;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.resource.BaseResourceService;
import com.exadel.etoolbox.backpack.core.services.util.constants.BackpackConstants;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Session;
import java.util.Collections;
import java.util.LinkedList;

@Component(service = BaseResourceService.class)
public class DeleteChildResourceService implements BaseResourceService<PathInfo> {

    private static final Gson GSON = new Gson();

    @Reference
    private BasePackageService basePackageService;

    @Reference
    private PackageInfoService packageInfoService;

    @Override
    public ResponseWrapper<PathInfo> process(ResourceResolver resourceResolver, PathModel pathModel) {

        PackageInfo packageInfo = packageInfoService.getPackageInfo(resourceResolver, pathModel.getPackagePath());

        if (packageInfo == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList(BackpackConstants.PACKAGE_NOT_FOUND + pathModel.getPackagePath()));
        }

        LinkedList<String> params = parsePayloadToList(pathModel.getPayload());

        if (params.size() != 2 || params.getFirst() == null || params.getLast() == null) {
            return new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList("Invalid payload: " + pathModel.getPayload()));
        }

        ResponseWrapper<PathInfo> responseWrapper = new ResponseWrapper<>(null, ResponseWrapper.ResponseStatus.ERROR, Collections.singletonList(BackpackConstants.UNKNOWN_ACTION_TYPE + pathModel.getType()));

        switch (pathModel.getType()) {
            case "delete/child":
                responseWrapper = processDeleteChild(params.getFirst(), params.getLast(), packageInfo);
                break;
            case "delete/liveCopy":
                responseWrapper = processDeleteLiveCopies(params.getFirst(), params.getLast(), packageInfo);
                break;
            case "delete/page":
                responseWrapper = processDeletePage(params.getFirst(), params.getLast(), packageInfo);
                break;
            case "delete/asset":
                responseWrapper = processDeleteAsset(params.getFirst(), params.getLast(), packageInfo);
                break;
            case "delete/tag":
                responseWrapper = processDeleteTag(params.getFirst(), params.getLast(), packageInfo);
                break;
            default:
                return responseWrapper;
        }

        basePackageService.modifyPackage(resourceResolver.adaptTo(Session.class), pathModel.getPackagePath(), packageInfo);

        return responseWrapper;
    }

    private ResponseWrapper<PathInfo> processDeleteChild(String root, String child, PackageInfo packageInfo) {
        packageInfo.getPathInfo(root)
                .getChildren()
                .remove(child);
        return new ResponseWrapper<>(packageInfo.getPathInfo(root), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processDeleteLiveCopies(String root, String child, PackageInfo packageInfo) {
        packageInfo.getPathInfo(root)
                .getLiveCopies()
                .remove(child);
        return new ResponseWrapper<>(packageInfo.getPathInfo(root), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processDeletePage(String root, String child, PackageInfo packageInfo) {
        packageInfo.getPathInfo(root)
                .getPages()
                .remove(child);
        return new ResponseWrapper<>(packageInfo.getPathInfo(root), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processDeleteAsset(String root, String child, PackageInfo packageInfo) {
        packageInfo.getPathInfo(root)
                .getAssets()
                .remove(child);
        return new ResponseWrapper<>(packageInfo.getPathInfo(root), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private ResponseWrapper<PathInfo> processDeleteTag(String root, String child, PackageInfo packageInfo) {
        packageInfo.getPathInfo(root)
                .getTags()
                .remove(child);
        return new ResponseWrapper<>(packageInfo.getPathInfo(root), ResponseWrapper.ResponseStatus.SUCCESS);
    }

    private LinkedList<String> parsePayloadToList(String payload) throws JsonSyntaxException {
        return GSON.fromJson(payload, new TypeToken<LinkedList<String>>() {
        }.getType());
    }
}
