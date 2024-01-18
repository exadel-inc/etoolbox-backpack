/*
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.exadel.etoolbox.backpack.core.dto.repository.ReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.CreatePackageService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageModel;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
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
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Implements {@link CreatePackageService} to provide create package operation
 */
//@Component(service = CreatePackageService.class)
public class CreatePackageServiceImpl implements CreatePackageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreatePackageServiceImpl.class);

    @Reference
    private BasePackageService basePackageService;

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo createPackage(final ResourceResolver resourceResolver, final PackageModel packageModel) {
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

        Set<ReferencedItem> referencedAssets = basePackageService.getReferencedResources(resourceResolver, packageInfo.getPaths());
        Collection<String> resultingPaths = basePackageService.initAssets(packageInfo.getPaths(), referencedAssets, packageInfo);
        DefaultWorkspaceFilter filter = basePackageService.getWorkspaceFilter(resultingPaths);
        createPackage(session, packageInfo, packageModel.getPaths(), filter);

        if (PackageStatus.CREATED.equals(packageInfo.getPackageStatus())) {
            basePackageService.getPackageInfos().asMap().put(packageInfo.getPackagePath(), packageInfo);
        }

        return packageInfo;
    }

    /**
     * Called by {@link CreatePackageService#createPackage(ResourceResolver, PackageModel)} to implement package
     * creation on the standard {@link JcrPackage} package layer and report package status upon completion
     *
     * @param userSession Current user {@code Session} as adapted from the acting {@code ResourceResolver}
     * @param packageInfo {@code PackageInfo} object to store status information in
     * @param paths       {@code List} of {@code PathModel} will be stored in package metadata information and used in future package modifications
     * @param filter      {@code DefaultWorkspaceFilter} instance representing resource selection mechanism for the package
     */
    private void createPackage(final Session userSession,
                               final PackageInfo packageInfo,
                               final List<PathModel> paths,
                               final DefaultWorkspaceFilter filter) {
        JcrPackage jcrPackage = null;
        try {
            JcrPackageManager packMgr = PackagingService.getPackageManager(userSession);
            if (!filter.getFilterSets().isEmpty()) {
                jcrPackage = packMgr.create(packageInfo.getGroupName(), packageInfo.getPackageName(), packageInfo.getVersion());
                JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
                if (jcrPackageDefinition != null) {
                    basePackageService.setPackageInfo(jcrPackageDefinition, userSession, packageInfo, paths, filter);
                    packageInfo.setPackageStatus(PackageStatus.CREATED);
                    Node packageNode = jcrPackage.getNode();
                    if (packageNode != null) {
                        packageInfo.setPackageNodeName(packageNode.getName());
                        packageInfo.setPackagePath(packageNode.getPath());
                    }
                }
            } else {
                packageInfo.setPackageStatus(PackageStatus.ERROR);
                packageInfo.addLogMessage(BasePackageServiceImpl.ERROR + "Package does not contain any valid filters.");
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
