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

package com.exadel.etoolbox.backpack.core.services.impl;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.AssetReferenceSearch;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.exadel.etoolbox.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.PageReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.ReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.TagReferencedItem;
import com.exadel.etoolbox.backpack.core.services.PageReferenceSearchService;
import com.exadel.etoolbox.backpack.core.services.ReferenceService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Node;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implements {@link ReferenceService} to facilitate getting a collection of resources used by resources
 * under specified JCR path
 */
@Component(service = ReferenceService.class)
public class ReferenceServiceImpl implements ReferenceService {
    private static final String DAM_ROOT = "/content/dam";

    @Reference
    private PageReferenceSearchService pageReferenceSearchService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ReferencedItem> getReferences(final ResourceResolver resourceResolver,
                                             final String searchPath) {
        Set<ReferencedItem> tagReferences = getTagReferences(resourceResolver, searchPath);
        Set<ReferencedItem> refPaths = getPageReferences(resourceResolver, searchPath);
        Set<AssetReferencedItem> assetReferencedItems = getAssetReferencedItems(resourceResolver, searchPath);
        tagReferences.addAll(refPaths);
        tagReferences.addAll(assetReferencedItems);


        return tagReferences;
    }

    private Set<ReferencedItem> getPageReferences(final ResourceResolver resourceResolver,
                                                  final String searchPath) {
        return pageReferenceSearchService.findPageReferences(resourceResolver, searchPath)
                .stream()
                .map(page -> new PageReferencedItem(page.getContentResource().getPath()))
                .collect(Collectors.toSet());
    }

    private Set<AssetReferencedItem> getAssetReferencedItems(final ResourceResolver resourceResolver, final String searchPath) {
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


    private Set<ReferencedItem> getTagReferences(final ResourceResolver resourceResolve, final String searchPath) {
        Resource pageResource = resourceResolve.resolve(searchPath);
        TagManager tagManager = resourceResolve.adaptTo(TagManager.class);
        Set<ReferencedItem> tagsReferencedItemSet = new LinkedHashSet<>();

        if (pageResource != null) {
            Tag[] tags = tagManager.getTagsForSubtree(pageResource, false);
            if (tags != null) {
                for (int i = 0; i < tags.length; i++) {
                    Tag tag = tags[i];
                    tagsReferencedItemSet.add(new TagReferencedItem(tag.getPath()));
                }
            }
        }
        return tagsReferencedItemSet;
    }
}
