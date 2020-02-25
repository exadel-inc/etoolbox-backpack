package com.exadel.aem.backpack.core.services;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Set;

public interface ReferenceService {


    Set<AssetReferencedItem> getAssetReferences(ResourceResolver resourceResolver, String searchPath);

}
