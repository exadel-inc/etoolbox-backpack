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

package com.exadel.aembox.backpack.core.servlets.model;

import com.exadel.aembox.backpack.request.annotations.RequestMapping;
import com.exadel.aembox.backpack.request.annotations.RequestParam;
import com.exadel.aembox.backpack.request.annotations.Validate;
import com.exadel.aembox.backpack.request.validator.impl.RequiredValidator;

/**
 * Represents the set of user-defined options for a path filter creation.
 * Upon initialization, used as part of {@link PackageModel}
 */
@RequestMapping
public class PathModel {

    @RequestParam
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Path is required")
    private String path;

    @RequestParam
    private boolean excludeChildren;

    public PathModel() {
    }

    public PathModel(final String path, final boolean excludeChildren) {
        this.path = path;
        this.excludeChildren = excludeChildren;
    }

    /**
     * Gets the filter path.
     * @return String value
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the whether or not child pages excluded from a package build
     * @return String value
     */
    public boolean isExcludeChildren() {
        return excludeChildren;
    }
}
