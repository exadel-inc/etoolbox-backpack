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
package com.exadel.aem.backpack.core.services.pckg.impl;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.pckg.BasePackageService;
import com.exadel.aem.backpack.core.services.pckg.BuildPackageService;
import com.exadel.aem.backpack.core.services.pckg.PackageInfoService;
import com.exadel.aem.backpack.core.servlets.model.BuildPackageModel;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.exadel.aem.backpack.core.services.pckg.impl.BasePackageServiceImpl.*;

/**
 * Implements {@link BuildPackageService} to provide build package operations
 */
@Component(service = BuildPackageService.class)
public class BuildPackageImpl implements BuildPackageService {
    private static final String SERVICE_NAME = "backpack-service";

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildPackageImpl.class);

    protected static final Gson GSON = new Gson();

    @Reference
    private PackageInfoService packageInfoService;

    @Reference
    private BasePackageService basePackageService;

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    protected SlingRepository slingRepository;

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
        JcrPackageManager packMgr = basePackageService.getPackageManager(session);
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
                    includeGeneralResources(definition, s -> {
                        packageInfo.addLogMessage("A " + s);
                        totalSize.addAndGet(getAssetSize(resourceResolver, s));
                    });
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
     * Performs the internal package building procedure and stores status information
     *
     * @param userId              User ID per the effective {@code ResourceResolver}
     * @param packageBuildInfo    {@link PackageInfo} object to store package building status information in
     * @param referencedResources Collection of strings representing resource types to be embedded
     *                            in the resulting package
     */

    void buildPackage(final String userId,
                      final PackageInfo packageBuildInfo,
                      final String referencedResources) {
        Session userSession = null;
        try {
            userSession = getUserImpersonatedSession(userId);
            JcrPackageManager packMgr = basePackageService.getPackageManager(userSession);
            JcrPackage jcrPackage = packMgr.open(userSession.getNode(packageBuildInfo.getPackagePath()));
            if (jcrPackage != null) {
                JcrPackageDefinition definition = Objects.requireNonNull(jcrPackage.getDefinition());
                DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
                includeGeneralResources(definition, s -> filter.add(new PathFilterSet(s)));
                includeReferencedResources(referencedResources, definition, s -> filter.add(new PathFilterSet(s)));
                definition.setFilter(filter, true);
                String thumbnailPath = StringUtils.defaultIfBlank(packageBuildInfo.getThumbnailPath(), basePackageService.getDefaultThumbnailPath(false));
                basePackageService.addThumbnail(definition.getNode(), thumbnailPath, userSession);
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
                packageBuildInfo.setPaths(filter.getFilterSets().stream().map(pathFilterSet -> pathFilterSet.seal().getRoot()).collect(Collectors.toList()));
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
     * {@inheritDoc}
     */

    @Override
    public PackageInfo buildPackage(final ResourceResolver resourceResolver,
                                    final BuildPackageModel requestInfo) {
        PackageInfo packageInfo = packageInfoService.getPackageInfo(resourceResolver, requestInfo);
        if (!PackageStatus.BUILD_IN_PROGRESS.equals(packageInfo.getPackageStatus())) {
            packageInfo.setPackageStatus(PackageStatus.BUILD_IN_PROGRESS);
            packageInfo.clearLog();
            basePackageService.getPackageInfos().put(requestInfo.getPackagePath(), packageInfo);
            buildPackageAsync(resourceResolver.getUserID(), packageInfo, requestInfo.getReferencedResources());
        }

        return packageInfo;
    }

    /**
     * Called from {@link BuildPackageImpl#buildPackage(ResourceResolver, BuildPackageModel)}.
     * Encapsulates building package in a separate execution thread
     *
     * @param userId              User ID per the effective {@code ResourceResolver}
     * @param packageBuildInfo    {@link PackageInfo} object to store package building status information in
     * @param referencedResources Collection of strings representing resource types to be embedded
     *                            in the resulting package
     */

    private void buildPackageAsync(final String userId,
                                   final PackageInfo packageBuildInfo,
                                   final String referencedResources) {
        new Thread(() -> buildPackage(userId, packageBuildInfo, referencedResources)).start();
    }

    /**
     * Called from {@link BuildPackageImpl#buildPackage(String, PackageInfo, String)} to perform impersonated session
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
     * Called by {@link BuildPackageImpl#buildPackage(String, PackageInfo, String)} to get the {@code Session} instance
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
     * Called from {@link BuildPackageImpl#buildPackage(ResourceResolver, BuildPackageModel)} and
     * {@link BuildPackageImpl#testBuildPackage(ResourceResolver, BuildPackageModel)} to facilitate including referenced
     * resources (assets) into the current package
     *
     * @param includeReferencedResources Collection of strings representing resource types to be embedded
     *                                   in the resulting package
     * @param definition                 {@code JcrPackageDefinition} object
     * @param pathConsumer               A routine executed over each resources' path value (mainly for logging purposes
     *                                   and statistics gathering)
     */

    private void includeReferencedResources(final String includeReferencedResources,
                                            final JcrPackageDefinition definition,
                                            final Consumer<String> pathConsumer) {
        Type mapType = new TypeToken<Map<String, List<String>>>() {
        }.getType();
        Map<String, List<String>> packageReferencedResources = GSON.fromJson(definition.get(REFERENCED_RESOURCES), mapType);
        Map<String, List<String>> includeResources = GSON.fromJson(includeReferencedResources, mapType);
        if (packageReferencedResources != null && includeResources != null) {
            for (Map.Entry<String, List<String>> entry : includeResources.entrySet()) {
                List<String> packageResourceList = packageReferencedResources.get(entry.getKey());
                List<String> includeResourceList = entry.getValue();
                includeResourceList.stream().filter(s -> packageResourceList.contains(s)).forEach(pathConsumer);
            }
        }
    }


    /**
     * Called from {@link BuildPackageImpl#buildPackage(ResourceResolver, BuildPackageModel)} or
     * {@link BuildPackageImpl#testBuildPackage(ResourceResolver, BuildPackageModel)} to facilitate including directly
     * specified resources into the current package
     *
     * @param definition   {@code JcrPackageDefinition} object
     * @param pathConsumer A routine executed over each resources' path value (mainly for logging purposes
     *                     and statistics gathering)
     */

    private void includeGeneralResources(final JcrPackageDefinition definition, final Consumer<String> pathConsumer) {
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        List<String> packageGeneralResources = GSON.fromJson(definition.get(GENERAL_RESOURCES), listType);
        if (packageGeneralResources != null) {
            packageGeneralResources.forEach(pathConsumer);
        }
    }

    /**
     * Add information about the exception to {@link PackageInfo}
     *
     * @param packageInfo {@code PackageInfo} object to store status information in
     * @param e           Exception to log
     */

    protected void addExceptionToLog(final PackageInfo packageInfo, final Exception e) {
        packageInfo.addLogMessage(ERROR + e.getMessage());
        if (basePackageService.isEnableStackTrace()) {
            packageInfo.addLogMessage(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Called by {@link BuildPackageImpl#testBuildPackage(ResourceResolver, BuildPackageModel)} to compute size
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
     * Called by {@link BuildPackageImpl#getAssetSize(ResourceResolver, String)} to recursively compute the size of
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

}
