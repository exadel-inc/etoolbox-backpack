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

package com.exadel.etoolbox.backpack.core.dto.repository;

import java.util.Objects;

/**
 * Data model representing a JCR storage item accessible by its path
 */
public class ReferencedItem {
    private final String path;
    private final String type;
    /**
     * Basic constructor
     * @param path String value representing path to a JCR storage item
     */
    public ReferencedItem(String path, String type) {
        this.path = path;
        this.type = type;
    }

    /**
     * Gets the JCR path of the current item
     * @return String value
     */
    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    /**
     * Overrides the standard {@code equals()} routine to implement items comparison by their path requisites
     * @param o Object to test for equality with the current object
     * @return True or false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReferencedItem that = (ReferencedItem) o;
        return Objects.equals(path, that.path);
    }

    /**
     * Overrides the standard {@code hashCode()} routine to accompany {@link ReferencedItem#equals(Object)}
     * @return Integer value
     */
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
