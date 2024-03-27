package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.CreatePackageService;
import com.exadel.etoolbox.backpack.core.services.util.constants.BackpackConstants;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageModel;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;

@Component(service = CreatePackageService.class)
public class CreatePackageServiceImpl implements CreatePackageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreatePackageServiceImpl.class);

    @Reference
    private BasePackageService basePackageService;

    @Override
    public PackageInfo createPackage(ResourceResolver resourceResolver, PackageModel packageModel) {
        final Session session = resourceResolver.adaptTo(Session.class);

        PackageInfo packageInfo = basePackageService.initPackageInfo(resourceResolver, packageModel);

        try {
            JcrPackageManager packMgr = basePackageService.getPackageManager(session);
            if (basePackageService.isPackageExist(packMgr, packageModel.getPackageName(), packageInfo.getGroupName(), packageModel.getVersion())) {
                String packageExistMsg = "Package with this name already exists. Consider changing package name or package group";

                packageInfo.addLogMessage(packageExistMsg);
                packageInfo.setPackageStatus(PackageStatus.ERROR);
                LOGGER.error(packageExistMsg);
                return packageInfo;
            }
        } catch (RepositoryException e) {
            basePackageService.addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during existing packages check", e);
            return packageInfo;
        }

        createPackage(session, packageInfo, basePackageService.buildWorkspaceFilter(packageInfo.getPaths()));

        if (PackageStatus.CREATED.equals(packageInfo.getPackageStatus())) {
            basePackageService.getPackageCacheAsMap().put(packageInfo.getPackagePath(), packageInfo);
        }

        return packageInfo;
    }


    private void createPackage(final Session userSession,
                               final PackageInfo packageInfo,
                               final DefaultWorkspaceFilter filter) {
        JcrPackage jcrPackage = null;
        try {
            JcrPackageManager packMgr = PackagingService.getPackageManager(userSession);
            jcrPackage = packMgr.create(packageInfo.getGroupName(), packageInfo.getPackageName(), packageInfo.getVersion());
            JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
            if (jcrPackageDefinition != null) {
                basePackageService.setPackageInfo(jcrPackageDefinition, userSession, packageInfo, filter);
                packageInfo.setPackageStatus(PackageStatus.CREATED);
                Node packageNode = jcrPackage.getNode();
                if (packageNode != null) {
                    packageInfo.setPackageNodeName(packageNode.getName());
                    packageInfo.setPackagePath(packageNode.getPath());
                }
            }
        } catch (RepositoryException | IOException e) {
            packageInfo.setPackageStatus(PackageStatus.ERROR);
            basePackageService.addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during package creation", e);
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
    }
}
