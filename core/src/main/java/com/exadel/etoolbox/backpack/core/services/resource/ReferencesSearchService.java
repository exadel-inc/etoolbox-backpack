package com.exadel.etoolbox.backpack.core.services.resource;

import com.exadel.etoolbox.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.PageReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.TagReferencedItem;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Set;

public interface ReferencesSearchService {

    Set<PageReferencedItem> getPageReferences(final ResourceResolver resourceResolver, final String searchPath);

    Set<AssetReferencedItem> getAssetReferences(final ResourceResolver resourceResolver, final String searchPath);

    Set<TagReferencedItem> getTagReferences(final ResourceResolver resourceResolve, final String searchPath);
}
