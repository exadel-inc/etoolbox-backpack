package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.AssetReferenceSearch;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.exadel.etoolbox.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.PageReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.TagReferencedItem;
import com.exadel.etoolbox.backpack.core.services.resource.PageReferenceSearchService;
import com.exadel.etoolbox.backpack.core.services.resource.ReferencesSearchService;
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

@Component(service = ReferencesSearchService.class)
public class ReferencesSearchServiceImpl implements ReferencesSearchService {

    private static final String DAM_ROOT = "/content/dam";

    @Reference
    private PageReferenceSearchService pageReferenceSearchService;

    @Override
    public Set<PageReferencedItem> getPageReferences(final ResourceResolver resourceResolver,
                                                     final String searchPath) {
        return pageReferenceSearchService.findPageReferences(resourceResolver, searchPath)
                .stream()
                .map(page -> new PageReferencedItem(page.getContentResource().getPath()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<AssetReferencedItem> getAssetReferences(final ResourceResolver resourceResolver, final String searchPath) {
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


    @Override
    public Set<TagReferencedItem> getTagReferences(final ResourceResolver resourceResolve, final String searchPath) {
        Resource pageResource = resourceResolve.resolve(searchPath);
        TagManager tagManager = resourceResolve.adaptTo(TagManager.class);
        Set<TagReferencedItem> tagsReferencedItemSet = new LinkedHashSet<>();

        if (tagManager != null) {
            Tag[] tags = tagManager.getTagsForSubtree(pageResource, false);
            if (tags != null) {
                for (Tag tag : tags) {
                    tagsReferencedItemSet.add(new TagReferencedItem(tag.getPath()));
                }
            }
        }
        return tagsReferencedItemSet;
    }
}
