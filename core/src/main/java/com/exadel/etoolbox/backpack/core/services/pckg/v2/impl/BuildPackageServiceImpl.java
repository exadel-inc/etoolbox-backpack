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

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.BuildPackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.util.LoggerService;
import com.exadel.etoolbox.backpack.core.services.util.SessionService;
import com.exadel.etoolbox.backpack.core.servlets.model.BuildPackageModel;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Implements {@link BuildPackageService} to provide build package operations
 */
@Component(service = BuildPackageService.class)
public class BuildPackageServiceImpl implements BuildPackageService {
    private static final String SERVICE_NAME = "backpack-service";

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildPackageServiceImpl.class);

    protected static final Gson GSON = new Gson();

    @Reference
    private PackageInfoService packageInfoService;

    @Reference
    private BasePackageService basePackageService;

    @Reference
    private SessionService sessionService;

    @Reference
    private LoggerService loggerService;

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo testBuildPackage(final ResourceResolver resourceResolver,
                                        final BuildPackageModel requestInfo) {
        PackageInfo packageInfo = new PackageInfo();

        final Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            packageInfo.addLogMessage(BasePackageServiceImpl.ERROR + " session is null");
            return packageInfo;
        }
        JcrPackageManager packMgr = basePackageService.getPackageManager(session);
        Node packageNode;
        JcrPackage jcrPackage = null;
        AtomicLong totalSize = new AtomicLong();

        try {
            packageNode = session.getNode(requestInfo.getPackagePath());

            if (packageNode != null) {
                long start = System.currentTimeMillis();
                jcrPackage = packMgr.open(packageNode);
                if (jcrPackage != null) {
                    JcrPackageDefinition definition = jcrPackage.getDefinition();
                    if (definition == null) {
                        packageInfo.addLogMessage(BasePackageServiceImpl.ERROR + " package definition is null");
                        return packageInfo;
                    }
                    includeGeneralResources(definition, s -> {
                        packageInfo.addLogMessage("A " + s);
                        totalSize.addAndGet(basePackageService.getAssetSize(resourceResolver, s));
                    });
                    includeReferencedResources(requestInfo.getReferencedResources(), definition, s -> {
                        packageInfo.addLogMessage("A " + s);
                        totalSize.addAndGet(basePackageService.getAssetSize(resourceResolver, s));
                    });
                    packageInfo.setDataSize(totalSize.get());
                    packageInfo.setPackageBuilt(definition.getLastWrapped());
                    long finish = System.currentTimeMillis();
                    packageInfo.addLogMessage("Package test built in " + (finish - start) + " milliseconds");
                }
            }
        } catch (RepositoryException e) {
            loggerService.addExceptionToLog(packageInfo, e);
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
     * @param referencedResources JSON string representing resources to be embedded in the resulting package
     */

    void buildPackage(final String userId,
                      final PackageInfo packageBuildInfo,
                      final String referencedResources) {
        Session userSession = null;
        try {
            userSession = sessionService.getUserImpersonatedSession(userId);
            JcrPackageManager packMgr = basePackageService.getPackageManager(userSession);
            JcrPackage jcrPackage = packMgr.open(userSession.getNode(packageBuildInfo.getPackagePath()));
            if (jcrPackage != null) {
                StopWatch stopWatch = StopWatch.createStarted();
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
                packageBuildInfo.setPackageReplicated(null);
                packageBuildInfo.setPackageBuilt(Calendar.getInstance());
                packageBuildInfo.setPackageStatus(PackageStatus.BUILT);
                packageBuildInfo.setDataSize(jcrPackage.getSize());
                packageBuildInfo.setPaths(filter.getFilterSets().stream().map(pathFilterSet -> pathFilterSet.seal().getRoot()).collect(Collectors.toList()));
                packageBuildInfo.addLogMessage("Package built in " + stopWatch);
            } else {
                packageBuildInfo.setPackageStatus(PackageStatus.ERROR);
                packageBuildInfo.addLogMessage(BasePackageServiceImpl.ERROR + String.format(BasePackageServiceImpl.PACKAGE_DOES_NOT_EXIST_MESSAGE, packageBuildInfo.getPackagePath()));
            }
        } catch (RepositoryException | PackageException | IOException e) {
            packageBuildInfo.setPackageStatus(PackageStatus.ERROR);
            loggerService.addExceptionToLog(packageBuildInfo, e);
            LOGGER.error("Error during package generation", e);
        } finally {
            sessionService.closeSession(userSession);
        }
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public PackageInfo buildPackage(final ResourceResolver resourceResolver,
                                    final BuildPackageModel requestInfo) {
        PackageInfo packageInfo = packageInfoService.getPackageInfo(resourceResolver, requestInfo.getPackagePath());
        if (!PackageStatus.BUILD_IN_PROGRESS.equals(packageInfo.getPackageStatus()) && !PackageStatus.INSTALL_IN_PROGRESS.equals(packageInfo.getPackageStatus())) {
            packageInfo.setPackageStatus(PackageStatus.BUILD_IN_PROGRESS);
            packageInfo.clearLog();
            basePackageService.getPackageCacheAsMap().put(requestInfo.getPackagePath(), packageInfo);
            buildPackageAsync(resourceResolver.getUserID(), packageInfo, requestInfo.getReferencedResources());
        }

        return packageInfo;
    }

    /**
     * Called from {@link BuildPackageServiceImpl#buildPackage(ResourceResolver, BuildPackageModel)}.
     * Encapsulates building package in a separate execution thread
     *
     * @param userId              User ID per the effective {@code ResourceResolver}
     * @param packageBuildInfo    {@link PackageInfo} object to store package building status information in
     * @param referencedResources JSON string representing resources to be embedded in the resulting package
     */

    private void buildPackageAsync(final String userId,
                                   final PackageInfo packageBuildInfo,
                                   final String referencedResources) {
        new Thread(() -> buildPackage(userId, packageBuildInfo, referencedResources)).start();
    }

    /**
     * Called from {@link BuildPackageServiceImpl#buildPackage(ResourceResolver, BuildPackageModel)} and
     * {@link BuildPackageServiceImpl#testBuildPackage(ResourceResolver, BuildPackageModel)} to facilitate including referenced
     * resources (assets) into the current package
     *
     * @param includeReferencedResources JSON string representing resources to be embedded in the resulting package
     * @param definition                 {@code JcrPackageDefinition} object
     * @param pathConsumer               A routine executed over each resources' path value (mainly for logging purposes
     *                                   and statistics gathering)
     */

    private void includeReferencedResources(final String includeReferencedResources,
                                            final JcrPackageDefinition definition,
                                            final Consumer<String> pathConsumer) {
        Type mapType = new TypeToken<Map<String, List<String>>>() {
        }.getType();
        Map<String, List<String>> packageReferencedResources = GSON.fromJson(definition.get(BasePackageServiceImpl.REFERENCED_RESOURCES), mapType);
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
     * Called from {@link BuildPackageServiceImpl#buildPackage(ResourceResolver, BuildPackageModel)} or
     * {@link BuildPackageServiceImpl#testBuildPackage(ResourceResolver, BuildPackageModel)} to facilitate including directly
     * specified resources into the current package
     *
     * @param definition   {@code JcrPackageDefinition} object
     * @param pathConsumer A routine executed over each resources' path value (mainly for logging purposes
     *                     and statistics gathering)
     */

    private void includeGeneralResources(final JcrPackageDefinition definition, final Consumer<String> pathConsumer) {
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        List<String> packageGeneralResources = GSON.fromJson(definition.get(BasePackageServiceImpl.GENERAL_RESOURCES), listType);
        if (packageGeneralResources != null) {
            packageGeneralResources.forEach(pathConsumer);
        }
    }
}
