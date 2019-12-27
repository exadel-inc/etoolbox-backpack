package com.exadel.aem.backpack.core.dto.repository;

public class AssetReferencedItem extends ReferencedItem {

	private String mimeType;

	public AssetReferencedItem(final String path, final String mimeType) {
		super(path);
		this.mimeType = mimeType;
	}

	public String getMimeType() {
		return mimeType;
	}


}
