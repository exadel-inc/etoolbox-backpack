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

package com.exadel.aem.backpack.core.servlets.model;

import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;
import com.exadel.aem.request.annotations.Validate;
import com.exadel.aem.request.validator.impl.RequiredValidator;

import java.util.List;

@RequestMapping
public class CreatePackageModel {

    @RequestParam
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Package name field is required")
    private String packageName;

    @RequestParam
    private String thumbnailPath;

    @RequestParam
    private String group;

    @RequestParam
    private String version;

    @RequestParam
    private boolean excludeChildren;

    @RequestParam
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Resource filter(s) is required")
    private List<String> paths;

    public String getPackageName() {
        return packageName;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public boolean isExcludeChildren() {
        return excludeChildren;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPackageName(final String packageName) {
        this.packageName = packageName;
    }

    public void setThumbnailPath(final String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setExcludeChildren(final boolean excludeChildren) {
        this.excludeChildren = excludeChildren;
    }

    public void setPaths(final List<String> paths) {
        this.paths = paths;
    }
}
