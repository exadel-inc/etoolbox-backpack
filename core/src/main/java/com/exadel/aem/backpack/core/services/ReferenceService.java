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

import java.util.Set;

import org.apache.sling.api.resource.ResourceResolver;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Represents a service running in an AEM instance responsible for gathering information on assets referenced by resources
 * of the current package (under specified JCR path). This is mainly invoked from the {@link PackageService}
 */
public interface ReferenceService {

    /**
     * Gets a collection of unique {@link AssetReferencedItem} instances representing assets referenced by resources
     * under specified JCR path
     * @param resourceResolver {@code ResourceResolver} used to access JCR resources
     * @param searchPath String value representing root path containg resources to gather references for
     * @return {@code Set<AssetReferencedItem>} object, or an empty {@code Set}
     */
    Set<AssetReferencedItem> getAssetReferences(ResourceResolver resourceResolver, String searchPath);

}
