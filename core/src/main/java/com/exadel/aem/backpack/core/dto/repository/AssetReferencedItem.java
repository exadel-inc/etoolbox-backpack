package com.exadel.aem.backpack.core.dto.repository;

public class AssetReferencedItem extends ReferencedItem {

    private final String mimeType;
    private Long size;

    public AssetReferencedItem(final String path, final String mimeType) {
        super(path);
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
