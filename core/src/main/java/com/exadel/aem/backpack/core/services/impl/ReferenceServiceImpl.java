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

package com.exadel.aem.backpack.core.services.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.jcr.Node;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.AssetReferenceSearch;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.aem.backpack.core.services.ReferenceService;

/**
 * Implements {@link ReferenceService} to facilitate getting a collection of asses referenced by resources
 * under specified JCR path
 */
@Component(service = ReferenceService.class)
public class ReferenceServiceImpl implements ReferenceService {
    private static final String DAM_ROOT = "/content/dam";

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AssetReferencedItem> getAssetReferences(final ResourceResolver resourceResolver,
                                                       final String searchPath) {
        Resource resource = resourceResolver.getResource(searchPath);

        if (resource == null) {
            return Collections.emptySet();
        }

        Node node = resource.adaptTo(Node.class);

        AssetReferenceSearch assetReferenceSearch = new AssetReferenceSearch(node, DAM_ROOT,
                resourceResolver);

        Map<String, Asset> result = assetReferenceSearch.search();
        Set<AssetReferencedItem> assetReferencedItemSet = new LinkedHashSet<>();

        for (Map.Entry<String, Asset> entry : result.entrySet()) {
            Asset asset = entry.getValue();
            AssetReferencedItem assetDetails = new AssetReferencedItem(asset.getPath(), asset.getMimeType());
            assetReferencedItemSet.add(assetDetails);
        }

        return assetReferencedItemSet;
    }
}
