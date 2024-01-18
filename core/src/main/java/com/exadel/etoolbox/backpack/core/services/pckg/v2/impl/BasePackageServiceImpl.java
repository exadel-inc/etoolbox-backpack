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

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import com.exadel.etoolbox.backpack.core.dto.repository.ReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.resource.ReferenceService;
import com.exadel.etoolbox.backpack.core.servlets.model.v2.PackageModel;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implements {@link BasePackageService} to provide base operation with package
 */
@Component(immediate = true)
@Designate(ocd = BasePackageServiceImpl.Configuration.class)
public class BasePackageServiceImpl implements BasePackageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePackageServiceImpl.class);

    protected static final String PACKAGE_METADATA = "metadata";
    protected static final String GENERAL_RESOURCES = "generalResources";
    protected static final String DEFAULT_PACKAGE_GROUP = "EToolbox_BackPack";

    public static final String ERROR = "ERROR: ";


    protected static final String REFERENCED_RESOURCES = "referencedResources";

    protected static final String PACKAGE_DOES_NOT_EXIST_MESSAGE = "Package by this path %s doesn't exist in the repository.";
    private static final String THUMBNAIL_FILE = "thumbnail.png";
    private static final String DEFAULT_THUMBNAILS_LOCATION = "/apps/etoolbox-backpack/assets/";
    private static final String THUMBNAIL_PATH_TEMPLATE = DEFAULT_THUMBNAILS_LOCATION + "backpack_%s.png";

    protected static final String QUERY_PARAMETER = "queryPackage";
    protected static final String SWITCH_PARAMETER = "toggle";
    protected static final String THUMBNAIL_PATH_PARAMETER = "thumbnailPath";

    protected static final Gson GSON = new Gson();

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private SlingRepository slingRepository;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private ReferenceService referenceService;

    @SuppressWarnings("UnstableApiUsage") // sticking to Guava Cache version bundled in uber-jar; still safe to use
    protected Cache<String, PackageInfo> packageInfos;

    protected boolean enableStackTrace;

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

    @Override
    public boolean isEnableStackTrace() {
        return enableStackTrace;
    }

    /**
     * Represents this OSGi service's configuration
     */
    @ObjectClassDefinition(name = "EToolbox BackPack PackageService configuration")
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
    public PackageInfo getPackageInfo(final ResourceResolver resourceResolver, final PackageModel packageModel) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageName(packageModel.getPackageName());
        packageInfo.setVersion(packageModel.getVersion());
        packageInfo.setThumbnailPath(packageModel.getThumbnailPath());

        if (packageModel.getInitialResources() != null) {
            packageInfo.setPaths(packageModel
                    .getInitialResources().stream()
                    .filter(path -> resourceResolver.getResource(path) != null)
                    .collect(Collectors.toSet())
            );
        } else {
            packageInfo.setPaths(Collections.emptySet());
        }

        String packageGroupName = DEFAULT_PACKAGE_GROUP;

        if (StringUtils.isNotBlank(packageModel.getGroup())) {
            packageGroupName = packageModel.getGroup();
        }

        packageInfo.setGroupName(packageGroupName);

        return packageInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JcrPackageManager getPackageManager(final Session userSession) {
        return PackagingService.getPackageManager(userSession);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addThumbnail(Node packageNode, final String thumbnailPath, Session session) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("user.jcr.session", session);

        try (ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(paramMap)) {
            addThumbnail(packageNode, thumbnailPath, resourceResolver);
        } catch (LoginException e) {
            LOGGER.error("Could not get Resource resolver", e);
        }
    }

    /**
     * Encapsulates JCR operations needed to embed a thumbnail resource to the package
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
     * {@inheritDoc}
     */
    @Override
    public String getDefaultThumbnailPath(boolean isEmpty) {
        return String.format(THUMBNAIL_PATH_TEMPLATE, isEmpty ? "empty" : "full");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPackageInfo(final JcrPackageDefinition jcrPackageDefinition,
                               final Session userSession,
                               final PackageInfo packageInfo,
                               final DefaultWorkspaceFilter filter) {
        //todo move to jcr nodes??
        jcrPackageDefinition.set(GENERAL_RESOURCES, GSON.toJson(packageInfo.getPaths()), true);
        jcrPackageDefinition.set(PACKAGE_METADATA, GSON.toJson(packageInfo.getPathInfoMap()), true);
        jcrPackageDefinition.set(THUMBNAIL_PATH_PARAMETER, GSON.toJson(packageInfo.getThumbnailPath()), true);
        jcrPackageDefinition.setFilter(filter, true);

        String thumbnailPath = StringUtils.defaultIfBlank(packageInfo.getThumbnailPath(), getDefaultThumbnailPath(true));
        addThumbnail(jcrPackageDefinition.getNode(), thumbnailPath, userSession);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addExceptionToLog(final PackageInfo packageInfo, final Exception e) {
        packageInfo.addLogMessage(ERROR + e.getMessage());
        if (enableStackTrace) {
            packageInfo.addLogMessage(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ReferencedItem> getReferencedResources(final ResourceResolver resourceResolver, final Collection<String> paths) {
        Set<ReferencedItem> assetLinks = new HashSet<>();
        paths.forEach(path -> {
            Set<ReferencedItem> assetReferences = referenceService.getReferences(resourceResolver, path);
            assetLinks.addAll(assetReferences);
        });
        return assetLinks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultWorkspaceFilter getWorkspaceFilter(final Collection<String> paths) {
        DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
        paths.forEach(path -> {
            PathFilterSet pathFilterSet = new PathFilterSet(path);
            filter.add(pathFilterSet);
        });

        return filter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> initAssets(final Collection<String> initialPaths,
                                         final Set<ReferencedItem> referencedAssets,
                                         final PackageInfo packageInfo) {
        referencedAssets.forEach(packageInfo::addAssetReferencedItem);
        Collection<String> resultingPaths = new ArrayList<>(initialPaths);
        return resultingPaths;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPackageExist(final JcrPackageManager packageMgr,
                                  final String newPackageName,
                                  final String packageGroupName,
                                  final String version) throws RepositoryException {
        List<JcrPackage> packages = packageMgr.listPackages(packageGroupName, false);
        for (JcrPackage jcrpackage : packages) {
            JcrPackageDefinition definition = jcrpackage.getDefinition();
            if (definition != null) {
                String packageName = definition.getId().toString();
                if (packageName.equalsIgnoreCase(getPackageId(packageGroupName, newPackageName, version))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Called by {@link BasePackageService#getPackageInfo(ResourceResolver, PackageModel)} to compute size
     * of the asset specified by path
     *
     * @param resourceResolver {@code ResourceResolver} used to retrieve path-specified {@code Resource}s
     * @param path             JCR path of the required resource
     * @return Asset size in bytes, or 0 if the asset is not found
     */
    @Override
    public long getAssetSize(ResourceResolver resourceResolver, String path) {
        Resource rootResource = resourceResolver.getResource(path);
        return getAssetSize(rootResource);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("UnstableApiUsage")
    // sticking to Guava Cache version bundled in uber-jar; still safe to use
    @Override
    public Cache<String, PackageInfo> getPackageInfos() {
        return packageInfos;
    }

    /**
     * Called by {@link BasePackageService#getAssetSize(ResourceResolver, String)} to recursively compute the size of
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
     * Generates package identifier string for the specified package own name, group name, and version
     *
     * @param packageGroupName String representing package group name to check
     * @param packageName      String representing package name to check
     * @param version          String representing package version to check
     * @return Package identifier string
     */
    private String getPackageId(final String packageGroupName, final String packageName, final String version) {
        return packageGroupName + ":" + packageName + (StringUtils.isNotBlank(version) ? ":" + version : StringUtils.EMPTY);
    }
}
