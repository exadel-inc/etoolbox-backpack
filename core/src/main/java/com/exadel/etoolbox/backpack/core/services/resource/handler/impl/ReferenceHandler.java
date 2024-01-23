package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.AssetReferenceSearch;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.exadel.etoolbox.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.PageReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.ReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.TagReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.dto.response.PathInfo;
import com.exadel.etoolbox.backpack.core.services.resource.PageReferenceSearchService;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import com.exadel.etoolbox.backpack.core.services.resource.handler.dto.PayloadDto;
import com.google.gson.Gson;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Node;
import java.util.*;
import java.util.stream.Collectors;

@Component(service = BaseHandler.class)
public class ReferenceHandler implements BaseHandler {

    private static final String DAM_ROOT = "/content/dam";
    private static final Gson GSON = new Gson();

    @Reference
    private PageReferenceSearchService pageReferenceSearchService;

    @Override
    public void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        PayloadDto referencesDto = GSON.fromJson(payload, PayloadDto.class);
        PathInfo pathInfo = packageInfo.getPathInfo(referencesDto.getEntry());
        List<ReferencedItem> referencedItems = new ArrayList<>();

        if (resourceResolver.getResource(referencesDto.getEntry()) != null) {
            switch (referencesDto.getType()) {
                case "pages":
                    referencedItems.addAll(getPageReferences(resourceResolver, referencesDto.getEntry()));
                    break;
                case "assets":
                    referencedItems.addAll(getAssetReferences(resourceResolver, referencesDto.getEntry()));
                    break;
                case "tags":
                    referencedItems.addAll(getTagReferences(resourceResolver, referencesDto.getEntry()));
                    break;
                default:
                    packageInfo.addLogMessage("References not implemented yet");
                    break;
            }

            pathInfo.getReferences().addAll(referencedItems.stream().map(ReferencedItem::getPath).collect(Collectors.toSet()));

        } else {
            packageInfo.setPackageStatus(PackageStatus.ERROR);
            packageInfo.addLogMessage("Resource not found: " + payload);
        }
    }

    @Override
    public String bindActionType() {
        return "references";
    }

    private Set<ReferencedItem> getPageReferences(final ResourceResolver resourceResolver,
                                                  final String searchPath) {
        return pageReferenceSearchService.findPageReferences(resourceResolver, searchPath)
                .stream()
                .map(page -> new PageReferencedItem(page.getContentResource().getPath()))
                .collect(Collectors.toSet());
    }

    private Set<AssetReferencedItem> getAssetReferences(final ResourceResolver resourceResolver, final String searchPath) {
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
