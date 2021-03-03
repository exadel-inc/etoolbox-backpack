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

import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Set;

/**
 * Represents a service running in an AEM instance responsible for gathering pages referenced by resources
 * of the current package (under specified JCR path). This is mainly invoked from the {@link ReferenceService}.
 */
public interface PageReferenceSearchService {

    /**
     * Gets a collection of unique {@link Page} instances representing pages referenced by resources
     * under specified JCR path.
     *
     * @param resourceResolver {@code ResourceResolver} used to access JCR resources.
     * @param searchPath       String value representing root path containing resources to gather references for.
     * @return {@code Set<Page>} object, or an empty {@code Set}.
     */
    Set<Page> findPageReferences(ResourceResolver resourceResolver, String searchPath);
}
