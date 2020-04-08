package com.exadel.aem.backpack.core.services.impl;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.AssetReferenceSearch;
import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.aem.backpack.core.services.ReferenceService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import javax.jcr.Node;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component(service = ReferenceService.class)
public class ReferenceServiceImpl implements ReferenceService {
    private static final String DAM_ROOT = "/content/dam";

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
