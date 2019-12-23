package com.exadel.aem.backpack.core.dto.response;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;

import java.util.Set;

public class TestBuildInfo {

    private Set<AssetReferencedItem> assetReferences;
    private Long totalSize;

    public Set<AssetReferencedItem> getAssetReferences() {
        return assetReferences;
    }

    public void setAssetReferences(Set<AssetReferencedItem> assetReferences) {
        this.assetReferences = assetReferences;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }
}
