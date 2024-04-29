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
import com.exadel.etoolbox.backpack.core.servlets.model.LatestPackageInfoModel;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageInfoModel;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageModel;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

/**
 * Represents a service running in an AEM instance responsible for providing information about the package
 */
public interface PackageInfoService {

    /**
     * Gets structured information about a package specified in a user request. This information can be subsequently serialized
     * to a JSON-coded HTTP response
     *
     * @param resourceResolver {@code ResourceResolver} instance used to collect data for a package as a JCR repository item
     * @param packagePath {@link String} instance containing requisites of the required package
     * @return {@link PackageInfo} instance
     */
    PackageInfo getPackageInfo(final ResourceResolver resourceResolver, String packagePath);

    /**
     * Gets structured information about a package specified in a jcr-package object. This information can be subsequently serialized
     * to a JSON-coded HTTP response
     *
     * @param jcrPackage {@code JcrPackage} instance containing requisites of the required package
     * @return {@link PackageInfo} instance
     */
    PackageInfo getPackageInfo(JcrPackage jcrPackage);

    /**
     * Gets a chunk of rolling package building process update according to options specified in the HTTP request
     *
     * @param latestPackageInfoModel {@link LatestPackageInfoModel} containing requisites of required log information chunk.
     * @return {@link PackageInfo} instance
     */
    PackageInfo getLatestPackageBuildInfo(LatestPackageInfoModel latestPackageInfoModel);

    /**
     * Gets information about current state of package node.
     *
     * @param resourceResolver {@code ResourceResolver} instance used to build the package
     * @param packageInfoModel {@link PackageInfoModel} instance containing requisites of the required package
     * @return {@code boolean} reporting package existence
     */
    boolean packageExists(ResourceResolver resourceResolver, PackageInfoModel packageInfoModel);

    /**
     * Gets the basic information about the existing package by its path
     *
     * @param packagePath      {@code String} path of existing package
     * @param resourceResolver {@code ResourceResolver} instance used to build the package
     * @return {@link PackageModel} instance
     */
    PackageModel getPackageModelByPath(String packagePath, ResourceResolver resourceResolver);

    /**
     * The method to collect all folders under <i>/etc/packages</i> node
     *
     * @param resourceResolver {@code ResourceResolver} instance used to find folders
     * @return {@code List} of folder resources
     */
    List<Resource> getPackageFolders(ResourceResolver resourceResolver);
}