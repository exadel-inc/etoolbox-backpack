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
package com.exadel.etoolbox.backpack.core.services.pckg;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.servlets.model.BuildPackageModel;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageModel;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents a service running in an AEM instance responsible for base operation with package
 */
public interface BasePackageService {

    /**
     * Return true in the case when exception stack trace should be enabled otherwise return false
     *
     * @return True or false
     */
    boolean isEnableStackTrace();

    /**
     * Called from {@link com.exadel.etoolbox.backpack.core.services.pckg.CreatePackageService#createPackage(ResourceResolver, PackageModel)} and {@link EditPackageService#editPackage(ResourceResolver, PackageModel)}
     * in order to convert {@link PackageModel} into {@link PackageInfo}
     *
     * @param resourceResolver {@code ResourceResolver} used to convert the model
     * @param packageModel     {@code PackageModel} that will be converted
     * @return {@link PackageInfo} instance
     */
    PackageInfo initPackageInfo(ResourceResolver resourceResolver, PackageModel packageModel);

    /**
     * Gets {@link JcrPackageManager} instance associated with the current {@code Session}
     *
     * @param userSession {@code Session} object to retrieve package manager for
     * @return {@code JcrPackageManager} instance
     */
    JcrPackageManager getPackageManager(Session userSession);

    /**
     * Called by {@link com.exadel.etoolbox.backpack.core.services.pckg.CreatePackageService#createPackage(ResourceResolver, PackageModel)} or
     * {@link BuildPackageService#buildPackage(ResourceResolver, BuildPackageModel)} to add a thumbnail to package
     *
     * @param packageNode   {@code Node} representing content package as a JCR storage item
     * @param thumbnailPath Path to the thumbnail
     * @param session       {@code Session} object used for package building
     */
    void addThumbnail(Node packageNode, String thumbnailPath, Session session);

    /**
     * Generates a path for a default (built-in) package thumbnail depending on whether this package is empty (has just
     * been created) or contains data
     *
     * @param isEmpty Flag indicating whether this package is empty
     * @return Path to the thumbnail
     */
    String getDefaultThumbnailPath(boolean isEmpty);

    /**
     * Called in order to fill general package information
     * <p>
     *
     * @param jcrPackageDefinition {@code JcrPackageDefinition} Definition of the package to update
     * @param userSession          Current user {@code Session} as adapted from the acting {@code ResourceResolver}
     * @param packageInfo          {@code PackageInfo} object to store status information in
     * @param paths                {@code List} of {@code PathModel} will be stored in package metadata information and used in future package modifications
     * @param filter               {@code DefaultWorkspaceFilter} instance representing resource selection mechanism for the package
     */
    void setPackageInfo(JcrPackageDefinition jcrPackageDefinition,
                        Session userSession,
                        PackageInfo packageInfo,
                        DefaultWorkspaceFilter filter);

    /**
     * Add information about the exception to {@link PackageInfo}
     *
     * @param packageInfo {@code PackageInfo} object to store status information in
     * @param e           Exception to log
     */
    void addExceptionToLog(PackageInfo packageInfo, Exception e);

    /**
     * Gets a {@link DefaultWorkspaceFilter} instance populated with the specified JCR paths
     *
     * @param paths Collection of JCR paths of resources
     * @return {@code DefaultWorkspaceFilter} object
     */
    DefaultWorkspaceFilter buildWorkspaceFilter(Collection<String> paths);

    /**
     * Called from {@link CreatePackageService#createPackage(ResourceResolver, PackageModel)} to get whether
     * a package with specified own name, group name, and version exists
     *
     * @param packageMgr       Standard {@link JcrPackageManager} object associated with the current user session
     * @param newPackageName   String representing package name to check
     * @param packageGroupName String representing package group name to check
     * @param version          String representing package version to check
     * @return True or false
     * @throws RepositoryException in case {@code JcrPackageManager} could not enumerated existing packages
     *                             or retrieve a packages's info
     */
    boolean isPackageExist(JcrPackageManager packageMgr,
                           String newPackageName,
                           String packageGroupName,
                           String version) throws RepositoryException;

    /**
     * Gets current {@link PackageInfo} objects cache
     *
     * @return {@code Cache<String, PackageInfo>} object
     */
    @SuppressWarnings("UnstableApiUsage")
    // sticking to Guava Cache version bundled in uber-jar; still safe to use
    ConcurrentMap<String, PackageInfo> getPackageCacheAsMap();

    /**
     * @param resourceResolver {@code ResourceResolver} used to collect assets details
     * @param path             Collection of JCR paths of resources
     * @return Data size
     */
    long getAssetSize(ResourceResolver resourceResolver, String path);

    void modifyPackage(Session userSession, String packagePath, PackageInfo packageInfo);
}
