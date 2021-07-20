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

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.servlets.model.LatestPackageInfoModel;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageInfoModel;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageModel;
import com.exadel.etoolbox.backpack.core.servlets.model.PathModel;
import com.google.gson.reflect.TypeToken;
import org.apache.jackrabbit.vault.fs.api.FilterSet;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implements {@link PackageInfoService} to provide info about the package
 */
@Component(service = PackageInfoService.class)
public class PackageInfoServiceImpl implements PackageInfoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageInfoServiceImpl.class);
    protected static final String SNAPSHOT_FOLDER = ".snapshot";

    @Reference
    private BasePackageService basePackageService;

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo getPackageInfo(final ResourceResolver resourceResolver, final PackageInfoModel packageInfoModel) {
        String packagePath = packageInfoModel.getPackagePath();
        PackageInfo packageInfo = basePackageService.getPackageInfos().asMap().get(packagePath);
        if (packageInfo != null) {
            return packageInfo;
        }

        final Session session = resourceResolver.adaptTo(Session.class);
        JcrPackageManager packMgr = basePackageService.getPackageManager(session);

        packageInfo = new PackageInfo();

        JcrPackage jcrPackage = null;

        try {
            if (session != null) {
                Node packageNode = session.getNode(packagePath);
                if (packageNode != null) {
                    jcrPackage = packMgr.open(packageNode);
                    getPackageInfo(packageInfo, jcrPackage, packageNode);
                }
            }
        } catch (RepositoryException e) {
            basePackageService.addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during package opening", e);
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
        return packageInfo;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo getPackageInfo(final JcrPackage jcrPackage) {
        PackageInfo packageInfo = new PackageInfo();
        try {
            if (jcrPackage != null) {
                getPackageInfo(packageInfo, jcrPackage, jcrPackage.getNode());

                return packageInfo;
            }
        } catch (RepositoryException e) {
            basePackageService.addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during package opening", e);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageModel getPackageModelByPath(final String packagePath, final ResourceResolver resourceResolver) {
        final Session session = resourceResolver.adaptTo(Session.class);
        JcrPackageManager packMgr = basePackageService.getPackageManager(session);

        JcrPackage jcrPackage = null;
        if (session == null) {
            return null;
        }
        try {

            Node packageNode = session.getNode(packagePath);
            if (packageNode != null) {
                jcrPackage = packMgr.open(packageNode);
                return getPackageModel(jcrPackage);
            }

        } catch (RepositoryException e) {
            LOGGER.error("Error during package opening", e);
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean packageExists(ResourceResolver resourceResolver, PackageInfoModel packageInfoModel) {
        final Session session = resourceResolver.adaptTo(Session.class);
        final JcrPackageManager packageMgr = basePackageService.getPackageManager(session);
        try {
            List<Node> nodes = packageMgr.listPackages().stream().map(JcrPackage::getNode)
                    .filter(Objects::nonNull).collect(Collectors.toList());
            for (Node node : nodes) {
                if (node.getPath().equals(packageInfoModel.getPackagePath())) {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error(String.format(BasePackageServiceImpl.PACKAGE_DOES_NOT_EXIST_MESSAGE, packageInfoModel.getPackagePath()));
        }
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<Resource> getPackageFolders(final ResourceResolver resourceResolver) {
        List<Resource> packageGroups = new ArrayList<>();
        Resource resource = resourceResolver.getResource(BasePackageServiceImpl.PACKAGES_ROOT_PATH);
        return getFolderResources(packageGroups, resource);
    }


    /**
     * Called from {@link PackageInfoService#getPackageFolders(ResourceResolver)} for getting folder resources recursively
     *
     * @param packageGroups {@code List} of folder resources
     * @param resource      {@code Resource} under which search occur
     * @return {@code List} of folder resources
     */
    private List<Resource> getFolderResources(final List<Resource> packageGroups, final Resource resource) {
        resource.listChildren().forEachRemaining(r -> {
            if (isFolder(r.getResourceType()) && !SNAPSHOT_FOLDER.equals(r.getName())) {
                packageGroups.add(r);
                getFolderResources(packageGroups, r);
            }
        });
        return packageGroups;
    }

    /**
     * Called from {@link PackageInfoServiceImpl#getFolderResources(List, Resource)} in order to check that resource type is folder
     *
     * @param resourceType {@code String} resource type to check
     * @return true or false
     */
    private boolean isFolder(String resourceType) {
        return resourceType.equals(JcrResourceConstants.NT_SLING_FOLDER) ||
                resourceType.equals(JcrResourceConstants.NT_SLING_ORDERED_FOLDER) ||
                resourceType.equals(org.apache.jackrabbit.JcrConstants.NT_FOLDER);
    }

    /**
     * Called by {@link PackageInfoServiceImpl#getPackageModelByPath(String, ResourceResolver)} to get
     * {@link PackageModel} from repository
     *
     * @param jcrPackage {@code JcrPackage} package object used for {@link PackageModel} generation
     * @return {@link PackageModel} instance
     * @throws RepositoryException in case {@code JcrPackageManager} could not  retrieve a packages's info
     */
    private PackageModel getPackageModel(final JcrPackage jcrPackage) throws RepositoryException {
        if (jcrPackage != null) {
            JcrPackageDefinition definition = jcrPackage.getDefinition();
            if (definition != null) {
                WorkspaceFilter filter = definition.getMetaInf().getFilter();
                Type listType = new TypeToken<ArrayList<PathModel>>() {
                }.getType();
                if (filter != null) {
                    PackageModel packageModel = new PackageModel();
                    packageModel.setPackageName(definition.get(JcrPackageDefinition.PN_NAME));
                    packageModel.setGroup(definition.get(JcrPackageDefinition.PN_GROUP));
                    packageModel.setVersion(definition.get(JcrPackageDefinition.PN_VERSION));

                    if (definition.get(BasePackageServiceImpl.QUERY) != null) {
                        packageModel.setQuery(BasePackageServiceImpl.GSON.fromJson(definition.get(BasePackageServiceImpl.QUERY), String.class));
                    }
                    if (definition.get(BasePackageServiceImpl.INITIAL_FILTERS) != null) {
                        packageModel.setPaths(BasePackageServiceImpl.GSON.fromJson(definition.get(BasePackageServiceImpl.INITIAL_FILTERS), listType));
                    } else {
                        List<PathFilterSet> filterSets = filter.getFilterSets();
                        packageModel.setPaths(filterSets.stream().map(pathFilterSet -> new PathModel(pathFilterSet.getRoot(), false)).collect(Collectors.toList()));
                    }
                    return packageModel;
                }
            }
        }
        return null;
    }

    /**
     * Called by {@link PackageInfoService#getPackageInfo(ResourceResolver, PackageInfoModel)} to populate a preliminarily
     * initialized {@link PackageInfo} object as it represents an <i>actual</i> storage item, with information on
     * package specifics
     *
     * @param packageInfo {@code PackageInfo} object to store information in
     * @param jcrPackage  The standard {@link JcrPackage} model used to retrieve information for the {@code PackageInfo} object
     * @param packageNode The corresponding JCR {@code Node} used to retrieve path requisite
     *                    for the {@code PackageInfo} object
     * @throws RepositoryException in case retrieving of JCR node detail fails
     */
    private void getPackageInfo(final PackageInfo packageInfo, final JcrPackage jcrPackage, final Node packageNode) throws RepositoryException {
        if (jcrPackage != null) {
            JcrPackageDefinition definition = jcrPackage.getDefinition();
            if (definition != null) {
                WorkspaceFilter filter = definition.getMetaInf().getFilter();
                if (filter != null) {
                    List<PathFilterSet> filterSets = filter.getFilterSets();
                    Type mapType = new TypeToken<Map<String, List<String>>>() {
                    }.getType();

                    packageInfo.setPackagePath(packageNode.getPath());
                    packageInfo.setPackageName(definition.get(JcrPackageDefinition.PN_NAME));
                    packageInfo.setGroupName(definition.get(JcrPackageDefinition.PN_GROUP));
                    packageInfo.setVersion(definition.get(JcrPackageDefinition.PN_VERSION));
                    packageInfo.setReferencedResources(BasePackageServiceImpl.GSON.fromJson(definition.get(BasePackageServiceImpl.REFERENCED_RESOURCES), mapType));
                    packageInfo.setPaths(filterSets.stream().map(FilterSet::getRoot).collect(Collectors.toList()));
                    packageInfo.setDataSize(jcrPackage.getSize());
                    packageInfo.setPackageBuilt(definition.getLastWrapped());
                    packageInfo.setQuery(BasePackageServiceImpl.GSON.fromJson(definition.get(BasePackageServiceImpl.QUERY), String.class));
                    if (definition.getLastWrapped() != null) {
                        packageInfo.setPackageStatus(PackageStatus.BUILT);
                    } else {
                        packageInfo.setPackageStatus(PackageStatus.CREATED);
                    }
                    packageInfo.setPackageNodeName(packageNode.getName());
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo getLatestPackageBuildInfo(final LatestPackageInfoModel latestPackageInfoModel) {
        String packagePath = latestPackageInfoModel.getPackagePath();
        String packageNotExistMsg = String.format(BasePackageServiceImpl.PACKAGE_DOES_NOT_EXIST_MESSAGE, packagePath);
        PackageInfo completeBuildInfo = basePackageService.getPackageInfos().asMap().get(packagePath);
        PackageInfo partialBuildInfo;

        if (completeBuildInfo != null) {
            partialBuildInfo = new PackageInfo(completeBuildInfo);
            partialBuildInfo.setLog(completeBuildInfo.getLatestBuildInfo(latestPackageInfoModel.getLatestLogIndex()));
        } else {
            partialBuildInfo = new PackageInfo();
            partialBuildInfo.setPackagePath(packagePath);
            partialBuildInfo.addLogMessage(BasePackageServiceImpl.ERROR + packageNotExistMsg);
            partialBuildInfo.setPackageStatus(PackageStatus.ERROR);
            LOGGER.error(packageNotExistMsg);
        }
        return partialBuildInfo;
    }

}
