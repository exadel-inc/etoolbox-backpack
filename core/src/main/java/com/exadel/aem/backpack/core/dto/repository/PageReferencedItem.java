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

import com.day.cq.wcm.api.Page;

/**
 * Data model representing a {@link Page} item accessible by its path
 */
public class PageReferencedItem extends ReferencedItem {
    /**
     * Basic constructor
     *
     * @param path String value representing path to a JCR storage item
     */
    public PageReferencedItem(final String path) {
        super(path, "Pages");
    }
}
