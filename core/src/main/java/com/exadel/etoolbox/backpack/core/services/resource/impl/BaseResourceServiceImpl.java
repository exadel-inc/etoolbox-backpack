package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.resource.BaseResourceService;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import com.exadel.etoolbox.backpack.core.servlets.model.v2.PathModel;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;


@Component(service = BaseResourceService.class)
public class BaseResourceServiceImpl implements BaseResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseResourceServiceImpl.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, service = BaseHandler.class,
            bind = "bindHandlerMap", unbind = "unbindHandlerMap",
            policy = ReferencePolicy.DYNAMIC)
    private Map<String, BaseHandler> handlerMap;

    @Reference
    private BasePackageService basePackageService;

    @Reference
    private PackageInfoService packageInfoService;

    @Override
    public PackageInfo getPackageInfo(ResourceResolver resourceResolver, PathModel pathModel) {

        PackageInfo packageInfo = packageInfoService.getPackageInfo(resourceResolver, pathModel.getPackagePath());
        //todo add check for packageInfo

        if (handlerMap.containsKey(pathModel.getType())) {
            handlerMap.get(pathModel.getType()).process(resourceResolver, pathModel.getPayload(), packageInfo);

            packageInfo.setDataSize(packageInfo.getPaths().stream().mapToLong(value -> getAssetSize(resourceResolver.getResource(value))).sum());

            Session session = resourceResolver.adaptTo(Session.class);
            modifyPackage(session, pathModel.getPackagePath(), packageInfo, basePackageService.buildWorkspaceFilter(packageInfo.getPaths()));

            packageInfo.setPackageStatus(PackageStatus.MODIFIED);

            basePackageService.getPackageCacheAsMap().put(pathModel.getPackagePath(), packageInfo);

        } else {
            packageInfo.setPackageStatus(PackageStatus.ERROR);
            packageInfo.addLogMessage("Handler for this type isn't exist");
        }

        return packageInfo;
    }


    protected void bindHandlerMap(final BaseHandler handler) {
        if (handlerMap == null) {
            handlerMap = new HashMap<>();
        }
        handlerMap.put(handler.bindActionType(), handler);
    }

    protected void unbindHandlerMap(final BaseHandler handler) {
        handlerMap.remove(handler.bindActionType());
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

    private void modifyPackage(final Session userSession,
                               final String packagePath,
                               final PackageInfo packageInfo,
                               final DefaultWorkspaceFilter filter) {

        JcrPackage jcrPackage = null;

        try {
            JcrPackageManager packMgr = PackagingService.getPackageManager(userSession);
            Node packageNode = userSession.getNode(packagePath);
            if (packageNode != null) {
                jcrPackage = packMgr.open(packageNode);
                JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
                if (jcrPackageDefinition != null) {
                    basePackageService.setPackageInfo(jcrPackageDefinition, userSession, packageInfo, filter);
                    packageInfo.setPackageStatus(PackageStatus.MODIFIED);
                }
            }
        } catch (RepositoryException e) {
            packageInfo.setPackageStatus(PackageStatus.ERROR);
            basePackageService.addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during package modification", e);
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
    }
}
