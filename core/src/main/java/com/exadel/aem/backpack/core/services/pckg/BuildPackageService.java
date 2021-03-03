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
package com.exadel.aem.backpack.core.services.pckg;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.servlets.model.BuildPackageModel;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Represents a service running in an AEM instance responsible for build operation with package
 */
public interface BuildPackageService {

    /**
     * Triggers operations needed to build a content package specified in the request, and reports the results
     *
     * @param resourceResolver  {@code ResourceResolver} instance used to build the package
     * @param buildPackageModel {@link BuildPackageModel} instance containing user-set options for the package building
     * @return {@link PackageInfo} instance reporting the current package status
     */
    PackageInfo buildPackage(ResourceResolver resourceResolver, BuildPackageModel buildPackageModel);

    /**
     * Triggers operations needed to perform a <i>dry-run</i> package build (an imitation without assembling actual
     * storage entity) according to the settings specified in the request, and reports the results
     *
     * @param resourceResolver  {@code ResourceResolver} instance used to build the package
     * @param buildPackageModel {@link BuildPackageModel} instance containing user-set options for the package building
     * @return {@link PackageInfo} instance reporting the current package status
     */
    PackageInfo testBuildPackage(ResourceResolver resourceResolver, BuildPackageModel buildPackageModel);
}
