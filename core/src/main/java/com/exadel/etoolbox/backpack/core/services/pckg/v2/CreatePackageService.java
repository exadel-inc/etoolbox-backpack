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
package com.exadel.etoolbox.backpack.core.services.pckg.v2;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.servlets.model.v2.PackageModel;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Represents a service running in an AEM instance responsible for creating package
 */
public interface CreatePackageService {

    /**
     * Triggers operations needed to create a content package in JCR according to options specified, and reports the results
     *
     * @param resourceResolver {@code ResourceResolver} instance used to create the package
     * @param packageModel     {@link PackageModel} instance containing user-set options for the package creation
     * @return {@link PackageInfo} instance reporting the current package status
     */
    PackageInfo createPackage(ResourceResolver resourceResolver, PackageModel packageModel);
}
