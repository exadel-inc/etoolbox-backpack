package com.exadel.etoolbox.backpack.core.services.pckg.v2.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.CreatePackageService;
import com.exadel.etoolbox.backpack.core.servlets.model.v2.PackageModel;
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
import java.util.Collections;

@Component(service = CreatePackageService.class)
public class CreatePackageServiceImpl implements CreatePackageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.exadel.etoolbox.backpack.core.services.pckg.impl.CreatePackageServiceImpl.class);

    @Reference
    private BasePackageService basePackageService;

    @Override
    public PackageInfo createPackage(ResourceResolver resourceResolver, PackageModel packageModel) {
        final Session session = resourceResolver.adaptTo(Session.class);

        PackageInfo packageInfo = basePackageService.getPackageInfo(resourceResolver, packageModel);
        if (packageInfo.getPackageStatus() != null) {
            return packageInfo;
        }

        try {
            JcrPackageManager packMgr = basePackageService.getPackageManager(session);
            if (basePackageService.isPackageExist(packMgr, packageModel.getPackageName(), packageInfo.getGroupName(), packageModel.getVersion())) {
                String packageExistMsg = "Package with this name already exists in the " + packageInfo.getGroupName() + " group.";

                packageInfo.addLogMessage(BasePackageServiceImpl.ERROR + packageExistMsg);
                packageInfo.setPackageStatus(PackageStatus.ERROR);
                LOGGER.error(packageExistMsg);
                return packageInfo;
            }
        } catch (RepositoryException e) {
            basePackageService.addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during existing packages check", e);
            return packageInfo;
        }

//        Set<ReferencedItem> referencedAssets = basePackageService.getReferencedResources(resourceResolver, packageInfo.getPaths());
//        Collection<String> resultingPaths = basePackageService.initAssets(packageInfo.getPaths(), referencedAssets, packageInfo);
//        DefaultWorkspaceFilter filter = basePackageService.getWorkspaceFilter(resultingPaths);

        //todo init filters
        DefaultWorkspaceFilter filter = basePackageService.getWorkspaceFilter(Collections.EMPTY_SET);

        createPackage(session, packageInfo, filter);

        if (PackageStatus.CREATED.equals(packageInfo.getPackageStatus())) {
            basePackageService.getPackageInfos().asMap().put(packageInfo.getPackagePath(), packageInfo);
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
                //EMPTY_LIST
                basePackageService.setPackageInfo(jcrPackageDefinition, userSession, packageInfo, Collections.EMPTY_LIST, filter);
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
