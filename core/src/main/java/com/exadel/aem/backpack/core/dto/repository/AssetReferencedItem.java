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

package com.exadel.aem.backpack.core.dto.repository;

import java.util.Objects;

/**
 * Data model representing a digital asset
 * @see ReferencedItem
 */
public class AssetReferencedItem extends ReferencedItem {

    private final String mimeType;
    private Long size;

    /**
     * Basic constructor
     * @param path String value representing path to a JCR storage item
     * @param mimeType String value representing MIME type of the current JCR storage item
     */
    public AssetReferencedItem(final String path, final String mimeType) {
        super(path);
        this.mimeType = mimeType;
    }

    /**
     * Gets the MIME type of this item
     * @return String value
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Gets the size of this item
     * @return Long value
     */
    @SuppressWarnings("unused")
    public long getSize() {
        return size;
    }

    /**
     * Sets the size of this item
     * @param size Long value
     */
    @SuppressWarnings("unused")
    public void setSize(Long size) {
        this.size = size;
    }

    /**
     * Overrides the standard {@code equals()} routine to implement items comparison by their path amd MIME type requisites
     * @param o Object to test for equality with the current object
     * @return True or false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetReferencedItem)) return false;
        AssetReferencedItem that = (AssetReferencedItem) o;
        return super.equals(that) && Objects.equals(mimeType, that.mimeType);
    }

    /**
     * Overrides the standard {@code hashCode()} routine to accompany {@link AssetReferencedItem#equals(Object)}
     * @return Integer value
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
