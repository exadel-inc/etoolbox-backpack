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

package com.exadel.aem.backpack.core.services.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.services.ReferenceService;
import com.exadel.aem.backpack.core.servlets.model.BuildPackageModel;
import com.exadel.aem.backpack.core.servlets.model.CreatePackageModel;
import com.exadel.aem.backpack.core.servlets.model.LatestPackageInfoModel;
import com.exadel.aem.backpack.core.servlets.model.PackageInfoModel;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jackrabbit.vault.fs.api.FilterSet;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.*;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
/**
 * Implements {@link PackageService} to facilitate routines for managing packages and reporting packages' status info
 */
@Component(service = PackageService.class)
@Designate(ocd = PackageServiceImpl.Configuration.class)
public class PackageServiceImpl implements PackageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageServiceImpl.class);

    private static final String SERVICE_NAME = "backpack-service";
    private static final String DEFAULT_PACKAGE_GROUP = "backpack";
    private static final String DEFAULT_THUMBNAILS_LOCATION = "/apps/backpack/assets/";
    private static final String THUMBNAIL_PATH_TEMPLATE = DEFAULT_THUMBNAILS_LOCATION + "backpack_%s.png";
    private static final String THUMBNAIL_FILE = "thumbnail.png";
    private static final String ERROR = "ERROR: ";
    private static final String JCR_CONTENT_NODE = "/" + JcrConstants.JCR_CONTENT;
    private static final Gson GSON = new Gson();
    private static final String REFERENCED_RESOURCES = "referencedResources";
    private static final String GENERAL_RESOURCES = "generalResources";
    private static final String PACKAGE_DOES_NOT_EXIST_MESSAGE = "Package by this path %s doesn't exist in the repository.";

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private ReferenceService referenceService;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private SlingRepository slingRepository;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private ResourceResolverFactory resourceResolverFactory;

    @SuppressWarnings("UnstableApiUsage") // sticking to Guava Cache version bundled in uber-jar; still safe to use
    private Cache<String, PackageInfo> packageInfos;
    private boolean enableStackTrace;

    /**
     * Run upon this OSGi service activation to initialize cache storage of collected {@link PackageInfo} objects
     *
     * @param config {@link Configuration} instance representing this OSGi service's starting configuration
     */
    @Activate
    @SuppressWarnings("unused") // run internally by the OSGi mechanism
    private void activate(Configuration config) {
        enableStackTrace = config.enableStackTraceShowing();
        packageInfos = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(config.buildInfoTTL(), TimeUnit.DAYS)
                .build();
    }

    /**
     * Represents this OSGi service's configuration
     */
    @ObjectClassDefinition(name = "BackPack PackageService configuration")
    @interface Configuration {
        @AttributeDefinition(
                name = "Package Build Info TTL",
                description = "Specify TTL for package build information cache (in days).",
                type = AttributeType.INTEGER
        )
        int buildInfoTTL() default 1;

        @AttributeDefinition(
                name = "Enable stack traces",
                description = "Show exceptions stack traces in the packages build log",
                type = AttributeType.BOOLEAN
        )
        boolean enableStackTraceShowing() default true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo testBuildPackage(final ResourceResolver resourceResolver,
                                        final BuildPackageModel requestInfo) {
        PackageInfo packageInfo = new PackageInfo();

        final Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            packageInfo.addLogMessage(ERROR + " session is null");
            return packageInfo;
        }
        JcrPackageManager packMgr = getPackageManager(session);
        Node packageNode;
        JcrPackage jcrPackage = null;
        AtomicLong totalSize = new AtomicLong();

        try {
            packageNode = session.getNode(requestInfo.getPackagePath());

            if (packageNode != null) {
                jcrPackage = packMgr.open(packageNode);
                if (jcrPackage != null) {
                    JcrPackageDefinition definition = jcrPackage.getDefinition();
                    if (definition == null) {
                        packageInfo.addLogMessage(ERROR + " package definition is null");
                        return packageInfo;
                    }
                    includeGeneralResources(definition, s -> packageInfo.addLogMessage("A " + s));
                    includeReferencedResources(requestInfo.getReferencedResources(), definition, s -> {
                        packageInfo.addLogMessage("A " + s);
                        totalSize.addAndGet(getAssetSize(resourceResolver, s));
                    });
                    packageInfo.setDataSize(totalSize.get());
                    packageInfo.setPackageBuilt(definition.getLastWrapped());
                }
            }
        } catch (RepositoryException e) {
            addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during package opening", e);
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
        return packageInfo;
    }

    /**
     * Add information about the exception to {@link PackageInfo}
     *
     * @param packageInfo {@code PackageInfo} object to store status information in
     * @param e Exception to log
     */
    private void addExceptionToLog(final PackageInfo packageInfo, final Exception e) {
        packageInfo.addLogMessage(ERROR + e.getMessage());
        if (enableStackTrace) {
            packageInfo.addLogMessage(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Called by {@link PackageServiceImpl#testBuildPackage(ResourceResolver, BuildPackageModel)} to compute size
     * of the asset specified by path
     *
     * @param resourceResolver {@code ResourceResolver} used to retrieve path-specified {@code Resource}s
     * @param path             JCR path of the required resource
     * @return Asset size in bytes, or 0 if the asset is not found
     */
    private long getAssetSize(ResourceResolver resourceResolver, String path) {
        Resource rootResource = resourceResolver.getResource(path);
        return getAssetSize(rootResource);
    }

    /**
     * Called by {@link PackageServiceImpl#getAssetSize(ResourceResolver, String)} to recursively compute the size of
     * the current resource and its child resources, summed up
     *
     * @param resource The {@code Resource} to compute size for
     * @return Resource size in bytes, or 0 if the resource is a null value
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo buildPackage(final ResourceResolver resourceResolver,
                                    final BuildPackageModel requestInfo) {
        PackageInfo packageInfo = getPackageInfo(resourceResolver, requestInfo);
        if (!PackageStatus.BUILD_IN_PROGRESS.equals(packageInfo.getPackageStatus())) {
            packageInfo.setPackageStatus(PackageStatus.BUILD_IN_PROGRESS);
            packageInfo.clearLog();
            packageInfos.put(requestInfo.getPackagePath(), packageInfo);
            buildPackageAsync(resourceResolver.getUserID(), packageInfo, requestInfo.getReferencedResources());
        }

        return packageInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo createPackage(final ResourceResolver resourceResolver, final CreatePackageModel createPackageModel) {
        final Session session = resourceResolver.adaptTo(Session.class);

        List<String> actualPaths = createPackageModel.getPaths().stream()
                .filter(s -> resourceResolver.getResource(s) != null)
                .map(path -> getActualPath(path, createPackageModel.isExcludeChildren(), resourceResolver))
                .collect(Collectors.toList());

        JcrPackageManager packMgr = getPackageManager(session);
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageName(createPackageModel.getPackageName());
        packageInfo.setPaths(actualPaths);
        packageInfo.setVersion(createPackageModel.getVersion());
        packageInfo.setThumbnailPath(createPackageModel.getThumbnailPath());

        String pkgGroupName = DEFAULT_PACKAGE_GROUP;

        if (StringUtils.isNotBlank(createPackageModel.getGroup())) {
            pkgGroupName = createPackageModel.getGroup();
        }
        packageInfo.setGroupName(pkgGroupName);
        try {
            if (isPkgExists(packMgr, createPackageModel.getPackageName(), pkgGroupName, createPackageModel.getVersion())) {
                String packageExistMsg = "Package with such name already exists in the " + pkgGroupName + " group.";

                packageInfo.addLogMessage(ERROR + packageExistMsg);
                packageInfo.setPackageStatus(PackageStatus.ERROR);
                LOGGER.error(packageExistMsg);
                return packageInfo;
            }
        } catch (RepositoryException e) {
            addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during existing packages check", e);
            return packageInfo;
        }

        Set<AssetReferencedItem> referencedAssets = getReferencedAssets(resourceResolver, actualPaths);
        Collection<String> resultingPaths = initAssets(actualPaths, referencedAssets, packageInfo);
        DefaultWorkspaceFilter filter = getWorkspaceFilter(resultingPaths);
        createPackage(session, packageInfo, filter);

        if (PackageStatus.CREATED.equals(packageInfo.getPackageStatus())) {
            packageInfos.asMap().put(packageInfo.getPackagePath(), packageInfo);
        }

        return packageInfo;
    }

    /**
     * Called by {@link PackageServiceImpl#createPackage(ResourceResolver, CreatePackageModel)} to implement package
     * creation on the standard {@link JcrPackage} package layer and report package status upon completion
     *
     * @param userSession Current user {@code Session} as adapted from the acting {@code ResourceResolver}
     * @param packageInfo {@code PackageInfo} object to store status information in
     * @param filter      {@code DefaultWorkspaceFilter} instance representing resource selection mechanism for the package
     */
    private void createPackage(final Session userSession,
                               final PackageInfo packageInfo,
                               final DefaultWorkspaceFilter filter) {
        JcrPackage jcrPackage = null;
        try {
            JcrPackageManager packMgr = PackagingService.getPackageManager(userSession);
            if (!filter.getFilterSets().isEmpty()) {
                jcrPackage = packMgr.create(packageInfo.getGroupName(), packageInfo.getPackageName(), packageInfo.getVersion());
                JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
                if (jcrPackageDefinition != null) {
                    jcrPackageDefinition.set(REFERENCED_RESOURCES, GSON.toJson(packageInfo.getReferencedResources()), true);
                    jcrPackageDefinition.set(GENERAL_RESOURCES, GSON.toJson(packageInfo.getPaths()), true);
                    jcrPackageDefinition.setFilter(filter, true);

                    String thumbnailPath = StringUtils.defaultIfBlank(packageInfo.getThumbnailPath(), getDefaultThumbnailPath(true));
                    addThumbnail(jcrPackageDefinition.getNode(), thumbnailPath, userSession);
                    packageInfo.setPackageStatus(PackageStatus.CREATED);
                    Node packageNode = jcrPackage.getNode();
                    if (packageNode != null) {
                        packageInfo.setPackageNodeName(packageNode.getName());
                        packageInfo.setPackagePath(packageNode.getPath());
                    }
                }
            } else {
                packageInfo.setPackageStatus(PackageStatus.ERROR);
                packageInfo.addLogMessage(ERROR + "Package does not contain any valid filters.");
            }
        } catch (RepositoryException | IOException e) {
            packageInfo.setPackageStatus(PackageStatus.ERROR);
            addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during package creation", e);
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo getPackageInfo(final ResourceResolver resourceResolver, final PackageInfoModel packageInfoModel) {
        String packagePath = packageInfoModel.getPackagePath();
        PackageInfo packageInfo = packageInfos.asMap().get(packagePath);
        if (packageInfo != null) {
            return packageInfo;
        }

        final Session session = resourceResolver.adaptTo(Session.class);
        JcrPackageManager packMgr = getPackageManager(session);

        packageInfo = new PackageInfo();

        JcrPackage jcrPackage = null;

        try {
            if (!isPkgExists(packMgr, packagePath)) {
                packageNotExistInfo(packagePath, packageInfo);
            } else if (session != null) {
                Node packageNode = session.getNode(packagePath);
                if (packageNode != null) {
                    jcrPackage = packMgr.open(packageNode);
                    packageExistInfo(packageInfo, jcrPackage, packageNode);
                }
            }
        } catch (RepositoryException e) {
            addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during package opening", e);
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
        return packageInfo;
    }

    /**
     * Called by {@link PackageServiceImpl#getPackageInfo(ResourceResolver, PackageInfoModel)} to populate a preliminarily
     * initialized {@link PackageInfo} object as it represents an <i>actual</i> storage item, with information on
     * package specifics
     *
     * @param packageInfo {@code PackageInfo} object to store information in
     * @param jcrPackage  The standard {@link JcrPackage} model used to retrieve information for the {@code PackageInfo} object
     * @param packageNode The corresponding JCR {@code Node} used to retrieve path requisite
     *                    for the {@code PackageInfo} object
     * @throws RepositoryException in case retrieving of JCR node detail fails
     */
    private void packageExistInfo(final PackageInfo packageInfo, final JcrPackage jcrPackage, final Node packageNode) throws RepositoryException {
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
                    packageInfo.setReferencedResources(GSON.fromJson(definition.get(REFERENCED_RESOURCES), mapType));
                    packageInfo.setPaths(filterSets.stream().map(FilterSet::getRoot).collect(Collectors.toList()));
                    packageInfo.setDataSize(jcrPackage.getSize());
                    packageInfo.setPackageBuilt(definition.getLastWrapped());
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
     * Called by {@link PackageServiceImpl#getPackageInfo(ResourceResolver, PackageInfoModel)} to populate a preliminarily
     * initialized {@link PackageInfo} object as it represents a <i>non-existing</i> JCR storage item
     *
     * @param pathToPackage Path to a package as supplied by user
     * @param packageInfo   {@code PackageInfo} object to store information in
     */
    private void packageNotExistInfo(final String pathToPackage, final PackageInfo packageInfo) {
        String packageNotExistMsg = String.format(PACKAGE_DOES_NOT_EXIST_MESSAGE, pathToPackage);
        packageInfo.setPackagePath(pathToPackage);
        packageInfo.addLogMessage(ERROR + packageNotExistMsg);
        packageInfo.setPackageStatus(PackageStatus.ERROR);
        LOGGER.error(packageNotExistMsg);
    }

    /**
     * Called by {@link PackageServiceImpl#createPackage(ResourceResolver, CreatePackageModel)} to populate a preliminarily
     * initialized {@link PackageInfo} object, as it represents an <i>actual</i> JCR storage item, with data reflecting
     * assets referenced by resources of this package
     *
     * @param initialPaths     Collections of strings representing paths of resources to be included in the package
     * @param referencedAssets Collection of unique {@link AssetReferencedItem} objects matching assets referenced
     *                         by resources of this package
     * @param packageInfo      {@code PackageInfo} object to store information in
     * @return {@code List<String>} object containing paths of package entries
     */
    private Collection<String> initAssets(final Collection<String> initialPaths,
                                          final Set<AssetReferencedItem> referencedAssets,
                                          final PackageInfo packageInfo) {
        Collection<String> resultingPaths = new ArrayList<>(initialPaths);
        referencedAssets.forEach(packageInfo::addAssetReferencedItem);
        return resultingPaths;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo getLatestPackageBuildInfo(final LatestPackageInfoModel latestPackageInfoModel) {
        String packagePath = latestPackageInfoModel.getPackagePath();
        String packageNotExistMsg = String.format(PACKAGE_DOES_NOT_EXIST_MESSAGE, packagePath);
        PackageInfo completeBuildInfo = packageInfos.asMap().get(packagePath);
        PackageInfo partialBuildInfo;

        if (completeBuildInfo != null) {
            partialBuildInfo = new PackageInfo(completeBuildInfo);
            partialBuildInfo.setLog(completeBuildInfo.getLatestBuildInfo(latestPackageInfoModel.getLatestLogIndex()));
        } else {
            partialBuildInfo = new PackageInfo();
            partialBuildInfo.setPackagePath(packagePath);
            partialBuildInfo.addLogMessage(ERROR + packageNotExistMsg);
            partialBuildInfo.setPackageStatus(PackageStatus.ERROR);
            LOGGER.error(packageNotExistMsg);
        }
        return partialBuildInfo;
    }

    /**
     * Gets {@link JcrPackageManager} instance associated with the current {@code Session}
     *
     * @param userSession {@code Session} object to retrieve package manager for
     * @return {@code JcrPackageManager} instance
     */
    JcrPackageManager getPackageManager(final Session userSession) {
        return PackagingService.getPackageManager(userSession);
    }

    /**
     * Called from {@link PackageServiceImpl#buildPackage(ResourceResolver, BuildPackageModel)}.
     * Encapsulates building package in a separate execution thread
     *
     * @param userId                  User ID per the effective {@code ResourceResolver}
     * @param packageBuildInfo        {@link PackageInfo} object to store package building status information in
     * @param referencedResourceTypes Collection of strings representing resource types to be embedded
     *                                in the resulting package
     */
    private void buildPackageAsync(final String userId,
                                   final PackageInfo packageBuildInfo,
                                   final List<String> referencedResourceTypes) {
        new Thread(() -> buildPackage(userId, packageBuildInfo, referencedResourceTypes)).start();
    }

    /**
     * Performs the internal package building procedure and stores status information
     *
     * @param userId                  User ID per the effective {@code ResourceResolver}
     * @param packageBuildInfo        {@link PackageInfo} object to store package building status information in
     * @param referencedResourceTypes Collection of strings representing resource types to be embedded
     *                                in the resulting package
     */
    void buildPackage(final String userId,
                                final PackageInfo packageBuildInfo,
                                final List<String> referencedResourceTypes) {
        Session userSession = null;
        try {
            userSession = getUserImpersonatedSession(userId);
            JcrPackageManager packMgr = getPackageManager(userSession);
            JcrPackage jcrPackage = packMgr.open(userSession.getNode(packageBuildInfo.getPackagePath()));
            if (jcrPackage != null) {
                JcrPackageDefinition definition = Objects.requireNonNull(jcrPackage.getDefinition());
                DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
                includeGeneralResources(definition, s -> filter.add(new PathFilterSet(s)));
                includeReferencedResources(referencedResourceTypes, definition, s -> filter.add(new PathFilterSet(s)));
                definition.setFilter(filter, true);
                String thumbnailPath = StringUtils.defaultIfBlank(packageBuildInfo.getThumbnailPath(), getDefaultThumbnailPath(false));
                addThumbnail(definition.getNode(), thumbnailPath, userSession);
                packageBuildInfo.setPackageStatus(PackageStatus.BUILD_IN_PROGRESS);
                packMgr.assemble(jcrPackage, new ProgressTrackerListener() {
                    @Override
                    public void onMessage(final Mode mode, final String statusCode, final String path) {
                        packageBuildInfo.addLogMessage(statusCode + " " + path);
                    }

                    @Override
                    public void onError(final Mode mode, final String s, final Exception e) {
                        packageBuildInfo.addLogMessage(s + " " + e.getMessage());
                    }
                });
                packageBuildInfo.setPackageBuilt(Calendar.getInstance());
                packageBuildInfo.setPackageStatus(PackageStatus.BUILT);
                packageBuildInfo.setDataSize(jcrPackage.getSize());
            } else {
                packageBuildInfo.setPackageStatus(PackageStatus.ERROR);
                packageBuildInfo.addLogMessage(ERROR + String.format(PACKAGE_DOES_NOT_EXIST_MESSAGE, packageBuildInfo.getPackagePath()));
            }
        } catch (RepositoryException | PackageException | IOException e) {
            packageBuildInfo.setPackageStatus(PackageStatus.ERROR);
            addExceptionToLog(packageBuildInfo, e);
            LOGGER.error("Error during package generation", e);
        } finally {
            closeSession(userSession);
        }
    }

    /**
     * Called by {@link PackageServiceImpl#buildPackage(String, PackageInfo, List)} to get the {@code Session} instance
     * with required rights for package creation
     *
     * @param userId User ID per the effective {@code ResourceResolver}
     * @return {@code Session} object
     * @throws RepositoryException in case of a Sling repository failure
     */
    Session getUserImpersonatedSession(final String userId) throws RepositoryException {
        return slingRepository.impersonateFromService(SERVICE_NAME,
                new SimpleCredentials(userId, StringUtils.EMPTY.toCharArray()),
                null);
    }

    /**
     * Called by {@link PackageServiceImpl#createPackage(ResourceResolver, CreatePackageModel)} to adjust paths to resources
     * intended for the package, Whether a resource does not require its children to be included, its path is brought down
     * to the underlying {@code jcr:content} node
     *
     * @param path             Resource path to inspect
     * @param excludeChildren  Flag indicating if this resource's children must be excluded
     * @param resourceResolver Current {@code ResourceResolver} object
     * @return Source path, or the adjusted resource path
     */
    private String getActualPath(final String path, final boolean excludeChildren, final ResourceResolver resourceResolver) {
        Resource res = resourceResolver.getResource(path);

        if (!excludeChildren) {
            return path;
        }
        if (res != null && res.getChild(JcrConstants.JCR_CONTENT) != null) {
            return path + JCR_CONTENT_NODE;
        }
        return path;
    }

    /**
     * Called from {@link PackageServiceImpl#buildPackage(ResourceResolver, BuildPackageModel)} or
     * {@link PackageServiceImpl#testBuildPackage(ResourceResolver, BuildPackageModel)} to facilitate including directly
     * specified resources into the current package
     *
     * @param definition   {@code JcrPackageDefinition} object
     * @param pathConsumer A routine executed over each resources' path value (mainly for logging purposes
     *                     and statistics gathering)
     */
    private void includeGeneralResources(final JcrPackageDefinition definition, final Consumer<String> pathConsumer) {
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        List<String> pkgGeneralResources = GSON.fromJson(definition.get(GENERAL_RESOURCES), listType);
        if (pkgGeneralResources != null) {
            pkgGeneralResources.forEach(pathConsumer);
        }
    }

    /**
     * Called from {@link PackageServiceImpl#buildPackage(ResourceResolver, BuildPackageModel)} and
     * {@link PackageServiceImpl#testBuildPackage(ResourceResolver, BuildPackageModel)} to facilitate including referenced
     * resources (assets) into the current package
     *
     * @param referencedResourceTypes Collection of strings representing resource types to be embedded
     *                                in the resulting package
     * @param definition              {@code JcrPackageDefinition} object
     * @param pathConsumer            A routine executed over each resources' path value (mainly for logging purposes
     *                                and statistics gathering)
     */
    private void includeReferencedResources(final Collection<String> referencedResourceTypes,
                                            final JcrPackageDefinition definition,
                                            final Consumer<String> pathConsumer) {
        Type mapType = new TypeToken<Map<String, List<String>>>() {
        }.getType();
        Map<String, List<String>> pkgReferencedResources = GSON.fromJson(definition.get(REFERENCED_RESOURCES), mapType);

        if (pkgReferencedResources != null && referencedResourceTypes != null) {
            List<String> includeResources = referencedResourceTypes.stream()
                    .map(pkgReferencedResources::get)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            includeResources.forEach(pathConsumer);
        }
    }

    /**
     * Called from {@link PackageServiceImpl#buildPackage(String, PackageInfo, List)} to perform impersonated session
     * closing
     *
     * @param userSession {@code Session} object used for package building
     */
    private void closeSession(final Session userSession) {
        if (userSession != null && userSession.isLive()) {
            userSession.logout();
        }
    }

    /**
     * Gets the collection of unique {@link AssetReferencedItem}s matching the collection of provided resource paths
     * applying for the {@link ReferenceService} instance
     *
     * @param resourceResolver {@code ResourceResolver} used to collect assets details
     * @param paths            Collection of JCR paths of resources to gather references for
     * @return {@code Set<AssetReferencedItem>} object
     */
    private Set<AssetReferencedItem> getReferencedAssets(final ResourceResolver resourceResolver, final Collection<String> paths) {
        Set<AssetReferencedItem> assetLinks = new HashSet<>();
        paths.forEach(path -> {
            Set<AssetReferencedItem> assetReferences = referenceService.getAssetReferences(resourceResolver, path);
            assetLinks.addAll(assetReferences);
        });
        return assetLinks;
    }

    /**
     * Gets a {@link DefaultWorkspaceFilter} instance populated with the specified JCR paths
     *
     * @param paths Collection of JCR paths of resources
     * @return {@code DefaultWorkspaceFilter} object
     */
    private DefaultWorkspaceFilter getWorkspaceFilter(final Collection<String> paths) {
        DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
        paths.forEach(path -> {
            PathFilterSet pathFilterSet = new PathFilterSet(path);
            filter.add(pathFilterSet);
        });

        return filter;
    }

    /**
     * Called from {@link PackageServiceImpl#createPackage(ResourceResolver, CreatePackageModel)} to get whether
     * a package with specified own name, group name, and version exists
     *
     * @param pkgMgr       Standard {@link JcrPackageManager} object associated with the current user session
     * @param newPkgName   String representing package name to check
     * @param pkgGroupName String representing package group name to check
     * @param version      String representing package version to check
     * @return True or false
     * @throws RepositoryException in case {@code JcrPackageManager} could not enumerated existing packages
     *                             or retrieve a packages's info
     */
    private boolean isPkgExists(final JcrPackageManager pkgMgr,
                                final String newPkgName,
                                final String pkgGroupName,
                                final String version) throws RepositoryException {
        List<JcrPackage> packages = pkgMgr.listPackages(pkgGroupName, false);
        for (JcrPackage jcrpackage : packages) {
            JcrPackageDefinition definition = jcrpackage.getDefinition();
            if (definition != null) {
                String packageName = definition.getId().toString();
                if (packageName.equalsIgnoreCase(getPackageId(pkgGroupName, newPkgName, version))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Called from {@link PackageServiceImpl#getPackageInfo(ResourceResolver, PackageInfoModel)} to get whether
     * a package exists at specified path
     *
     * @param pkgMgr Standard {@link JcrPackageManager} object associated with the current user session
     * @return True or false
     * @throws RepositoryException in case {@code JcrPackageManager} could not enumerated existing packages
     *                             or retrieve a packages's info
     */
    private boolean isPkgExists(final JcrPackageManager pkgMgr,
                                final String path) throws RepositoryException {
        List<JcrPackage> packages = pkgMgr.listPackages();
        for (JcrPackage jcrpackage : packages) {
            Node packageNode = jcrpackage.getNode();
            if (packageNode != null) {
                String packagePath = packageNode.getPath();
                if (packagePath.equals(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Generates package identifier string for the specified package own name, group name, and version
     *
     * @param pkgGroupName String representing package group name to check
     * @param packageName  String representing package name to check
     * @param version      String representing package version to check
     * @return Package identifier string
     */
    private String getPackageId(final String pkgGroupName, final String packageName, final String version) {
        return pkgGroupName + ":" + packageName + (StringUtils.isNotBlank(version) ? ":" + version : StringUtils.EMPTY);
    }

    /**
     * Generates a path for a default (built-in) package thumbnail depending on whether this package is empty (has just
     * been created) or contains data
     *
     * @param isEmpty Flag indicating whether this package is empty
     * @return Path to the thumbnail
     */
    private String getDefaultThumbnailPath(boolean isEmpty) {
        return String.format(THUMBNAIL_PATH_TEMPLATE, isEmpty ? "empty" : "full");
    }

    /**
     * Called by {@link PackageServiceImpl#createPackage(ResourceResolver, CreatePackageModel)} or
     * {@link PackageServiceImpl#buildPackage(ResourceResolver, BuildPackageModel)} to add a thumbnail to package
     *
     * @param packageNode   {@code Node} representing content package as a JCR storage item
     * @param thumbnailPath Path to the thumbnail
     * @param session       {@code Session} object used for package building
     */
    private void addThumbnail(Node packageNode, final String thumbnailPath, Session session) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("user.jcr.session", session);

        try (ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(paramMap)) {
            addThumbnail(packageNode, thumbnailPath, resourceResolver);
        } catch (LoginException e) {
            LOGGER.error("Could not get Resource resolver", e);
        }
    }

    /**
     * Encapsulates JCr operations needed to embed a thumbnail resource to the package
     *
     * @param packageNode      {@code Node} representing content package as a JCR storage item
     * @param thumbnailPath    Path to the thumbnail in JCR
     * @param resourceResolver {@code ResourceResolver} used to resolve <i>thumbnailPath</i> to a thumbnail resource
     */
    private void addThumbnail(Node packageNode, final String thumbnailPath, ResourceResolver resourceResolver) {
        if (packageNode == null || StringUtils.isBlank(thumbnailPath)) {
            LOGGER.warn("Could not add package thumbnail.");
            return;
        }

        Resource thumbnailResource = resourceResolver.getResource(thumbnailPath);
        if (thumbnailResource == null) {
            LOGGER.warn("The provided thumbnail does not exist in the repository.");
            return;
        }

        try {
            Asset asset = thumbnailResource.adaptTo(Asset.class);
            Node thumbnailNode = (asset != null) ?
                    asset.getImagePreviewRendition().adaptTo(Node.class) :
                    thumbnailResource.adaptTo(Node.class);

            if (thumbnailNode == null) {
                LOGGER.warn("Thumbnail node can not be retrieved. Could not add package thumbnail.");
                return;
            }

            JcrUtil.copy(thumbnailNode, packageNode, THUMBNAIL_FILE);
            packageNode.getSession().save();
        } catch (RepositoryException e) {
            LOGGER.error("A repository exception occurred: ", e);
        }
    }

    /**
     * Gets current {@link PackageInfo} objects cache
     *
     * @return {@code Cache<String, PackageInfo>} object
     */
    @SuppressWarnings("UnstableApiUsage") // sticking to Guava Cache version bundled in uber-jar; still safe to use
    Cache<String, PackageInfo> getPackageInfos() {
        return packageInfos;
    }

}