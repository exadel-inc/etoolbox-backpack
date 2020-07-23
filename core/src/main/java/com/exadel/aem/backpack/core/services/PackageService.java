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

package com.exadel.aem.backpack.core.services;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.services.impl.PackageServiceImpl;
import com.exadel.aem.backpack.core.servlets.model.BuildPackageModel;
import com.exadel.aem.backpack.core.servlets.model.CreatePackageModel;
import com.exadel.aem.backpack.core.servlets.model.LatestPackageInfoModel;
import com.exadel.aem.backpack.core.servlets.model.PackageInfoModel;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;

/**
 * Represents a service running in an AEM instance responsible for managing packages and reporting packages's status info
 * This service's methods are mainly called from {@link com.exadel.aem.backpack.core.servlets.CreatePackageServlet},
 * {@link com.exadel.aem.backpack.core.servlets.BuildPackageServlet}, and {@link com.exadel.aem.backpack.core.servlets.PackageInfoServlet}
 * to serve corresponding HTTP requests and produce responses
 */
public interface PackageService {

    /**
     * Gets structured information about a package specified in a user request. This information can be subsequently serialized
     * to a JSON-coded HTTP response
     * @param resourceResolver {@code ResourceResolver} instance used to collect data for a package as a JCR repository item
     * @param packageInfoModel {@link PackageInfoModel} instance containing requisites of the required package
     * @return {@link PackageInfo} instance
     */
    PackageInfo getPackageInfo(ResourceResolver resourceResolver, PackageInfoModel packageInfoModel);

    /**
     * Triggers operations needed to create a content package in JCR according to options specified, and reports the results
     * @param resourceResolver {@code ResourceResolver} instance used to create the package
     * @param createPackageModel {@link CreatePackageModel} instance containing user-set options for the package creation
     * @return {@link PackageInfo} instance reporting the current package status
     */
    PackageInfo createPackage(ResourceResolver resourceResolver, CreatePackageModel createPackageModel);

    /**
     * Triggers operations needed to build a content package specified in the request, and reports the results
     * @param resourceResolver {@code ResourceResolver} instance used to build the package
     * @param buildPackageModel {@link BuildPackageModel} instance containing user-set options for the package building
     * @return {@link PackageInfo} instance reporting the current package status
     */
    PackageInfo buildPackage(ResourceResolver resourceResolver, BuildPackageModel buildPackageModel);

    /**
     * Triggers operations needed to perform a <i>dry-run</i> package build (an imitation without assembling actual
     * storage entity) according to the settings specified in the request, and reports the results
     * @param resourceResolver {@code ResourceResolver} instance used to build the package
     * @param buildPackageModel {@link BuildPackageModel} instance containing user-set options for the package building
     * @return {@link PackageInfo} instance reporting the current package status
     */
    PackageInfo testBuildPackage(ResourceResolver resourceResolver, BuildPackageModel buildPackageModel);

    /**
     * Gets a chunk of rolling package building process update according to options specified in the HTTP request
     * @param latestPackageInfoModel {@link LatestPackageInfoModel} containing requisites of of required log information
     *                                                             chunk
     * @return {@link PackageInfo} instance
     */
    PackageInfo getLatestPackageBuildInfo(LatestPackageInfoModel latestPackageInfoModel);

    /**
     * Gets information about current state of package node.
     * @param resourceResolver {@code ResourceResolver} instance used to build the package
     * @param packageInfoModel {@link PackageInfoModel} instance containing requisites of the required package
     * @return {@code boolean} reporting package existence
     */
    boolean packageExists(ResourceResolver resourceResolver, PackageInfoModel packageInfoModel);
}
