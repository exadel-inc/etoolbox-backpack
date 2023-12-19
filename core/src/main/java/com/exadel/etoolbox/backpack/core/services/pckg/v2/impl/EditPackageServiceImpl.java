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
package com.exadel.etoolbox.backpack.core.services.pckg.v2.impl;

import com.exadel.etoolbox.backpack.core.dto.repository.ReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.EditPackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.PackageInfoService;
import com.exadel.etoolbox.backpack.core.servlets.model.v2.PackageModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.*;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Implements {@link EditPackageService} to provide edit package operations
 */
@Component(service = EditPackageService.class)
public class EditPackageServiceImpl implements EditPackageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditPackageServiceImpl.class);


    @Reference
    private PackageInfoService packageInfoService;

    @Reference
    private BasePackageService basePackageService;

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo editPackage(final ResourceResolver resourceResolver, final PackageModel modificationPackageModel) {
        final Session session = resourceResolver.adaptTo(Session.class);

        PackageInfo packageInfo = basePackageService.getPackageInfo(resourceResolver, modificationPackageModel);

        PackageModel oldPackageModel = packageInfoService.getPackageModelByPath(modificationPackageModel.getPackagePath(), resourceResolver);
        try {
            JcrPackageManager packMgr = basePackageService.getPackageManager(session);
            if (isPackageLocationUpdated(modificationPackageModel, oldPackageModel)
                    && basePackageService.isPackageExist(packMgr, packageInfo.getPackageName(), packageInfo.getGroupName(), packageInfo.getVersion())) {
                String packageExistMsg = "Package with this name already exists in the " + packageInfo.getGroupName() + " group.";

                packageInfo.addLogMessage(com.exadel.etoolbox.backpack.core.services.pckg.impl.BasePackageServiceImpl.ERROR + packageExistMsg);
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

        modifyPackage(session, modificationPackageModel.getPackagePath(), packageInfo, filter);

        if (PackageStatus.MODIFIED.equals(packageInfo.getPackageStatus())) {
            basePackageService.getPackageInfos().asMap().remove(modificationPackageModel.getPackagePath());
            basePackageService.getPackageInfos().asMap().put(packageInfo.getPackagePath(), packageInfo);
        }

        return packageInfo;
    }

    private boolean isPackageLocationUpdated(final PackageModel newPackage, final PackageModel oldPackage) {
        return !StringUtils.equals(oldPackage.getPackageName(), newPackage.getPackageName()) ||
                !StringUtils.equals(oldPackage.getGroup(), newPackage.getGroup()) ||
                !StringUtils.equals(oldPackage.getVersion(), newPackage.getVersion());
    }


    private void modifyPackage(final Session userSession,
                               final String packagePath,
                               final PackageInfo packageInfo,
                               final DefaultWorkspaceFilter filter) {

//        if (filter.getFilterSets().isEmpty()) {
//            packageInfo.setPackageStatus(PackageStatus.ERROR);
//            packageInfo.addLogMessage(BasePackageServiceImpl.ERROR + "Package does not contain any valid filters.");
//            return;
//
//        }
        JcrPackage jcrPackage = null;

        try {
            JcrPackageManager packMgr = PackagingService.getPackageManager(userSession);
            Node packageNode = userSession.getNode(packagePath);
            if (packageNode != null) {
                jcrPackage = packMgr.open(packageNode);
                jcrPackage = packMgr.rename(jcrPackage, packageInfo.getGroupName(), packageInfo.getPackageName(), packageInfo.getVersion());
                JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
                if (jcrPackageDefinition != null) {
                    //todo empty list
                    basePackageService.setPackageInfo(jcrPackageDefinition, userSession, packageInfo, Collections.EMPTY_LIST, filter);
                    packageInfo.setPackageStatus(PackageStatus.MODIFIED);
                    Node movedPackageNode = jcrPackage.getNode();
                    if (movedPackageNode != null) {
                        packageInfo.setPackageNodeName(movedPackageNode.getName());
                        packageInfo.setPackagePath(movedPackageNode.getPath());
                    }
                }
            }
        } catch (RepositoryException | PackageException e) {
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
