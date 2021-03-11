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
package com.exadel.aembox.backpack.core.services.pckg;

import com.exadel.aembox.backpack.core.dto.response.PackageInfo;
import com.exadel.aembox.backpack.core.servlets.model.PackageModel;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Represents a service running in an AEM instance responsible for editing package
 */
public interface EditPackageService {
    /**
     * Triggers package modification. In the case of package name, group or version modification, package also will be moved to the new location
     *
     * @param resourceResolver {@code ResourceResolver} instance used to build the package
     * @param model            {@link PackageModel} with package modification data
     * @return {@link PackageInfo} instance
     */
    PackageInfo editPackage(ResourceResolver resourceResolver, PackageModel model);
}
