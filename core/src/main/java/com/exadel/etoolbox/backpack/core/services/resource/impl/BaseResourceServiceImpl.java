package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.resource.BaseResourceService;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import com.exadel.etoolbox.backpack.core.servlets.model.v2.PathModel;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@Component(service = BaseResourceService.class)
public class BaseResourceServiceImpl implements BaseResourceService {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, service = BaseHandler.class,
            bind = "bindHandlerMap", unbind = "unbindHandlerMap",
            policy = ReferencePolicy.DYNAMIC)
    private Map<String, BaseHandler> handlerMap;

    @Reference
    private BasePackageService basePackageService;

    @Override
    public PackageInfo getPackageInfo(ResourceResolver resourceResolver, PathModel pathModel) {

        PackageInfo packageInfo = basePackageService.getPackageInfos().asMap().get(pathModel.getPackagePath());

        //move to servlet
        if (packageInfo == null) {
            PackageInfo errorPackageInfo = new PackageInfo();
            errorPackageInfo.setLog(Collections.singletonList("Path isn't valid"));
            errorPackageInfo.setPackageStatus(PackageStatus.ERROR);
            return errorPackageInfo;
        }

        if (handlerMap.containsKey(pathModel.getType())) {
            handlerMap.get(pathModel.getType()).process(resourceResolver, pathModel.getPayload(), packageInfo);
            packageInfo.setDataSize(packageInfo.getPaths().stream().mapToLong(value -> getAssetSize(resourceResolver.getResource(value))).sum());
            packageInfo.setPackageStatus(PackageStatus.MODIFIED);
            basePackageService.getPackageInfos().asMap().put(pathModel.getPackagePath(), packageInfo);
        } else {
            packageInfo.setPackageStatus(PackageStatus.ERROR);
        }

        return packageInfo;
    }


    protected void bindHandlerMap(final BaseHandler searchService) {
        if (handlerMap == null) {
            handlerMap = new HashMap<>();
        }

        handlerMap.put(searchService.getActionType(), searchService);
    }

    protected void unbindHandlerMap(final BaseHandler searchService) {
        handlerMap.remove(searchService.getActionType());
    }

    private long getAssetSize(Resource resource) {
        long totalSize = 0L;
        if (resource == null) {
            return totalSize;
        }
        for (Resource child : resource.getChildren()) {
            totalSize += getAssetSize(child);
        }
        Resource childResource = resource.getChild("jcr:content/jcr:data");
        if (childResource != null && childResource.getResourceMetadata().containsKey("sling.contentLength")) {
            totalSize += (Long) childResource.getResourceMetadata().get("sling.contentLength");
        }
        return totalSize;
    }
}
