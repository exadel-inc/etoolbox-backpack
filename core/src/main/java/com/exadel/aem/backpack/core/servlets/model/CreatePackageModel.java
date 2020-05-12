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

import com.exadel.aem.backpack.request.annotations.RequestMapping;
import com.exadel.aem.backpack.request.annotations.RequestParam;
import com.exadel.aem.backpack.request.annotations.Validate;
import com.exadel.aem.backpack.request.validator.impl.RequiredValidator;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

/**
 * Represents the set of user-defined options for a request to build a package. Upon initialization, passed
 * as a parameter to the {@link com.exadel.aem.backpack.core.services.PackageService#createPackage(ResourceResolver, CreatePackageModel)}
 * @see com.exadel.aem.backpack.core.servlets.CreatePackageServlet
 */
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

    /**
     * Gets the name of the current package
     * @return String value
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Gets path to the image asset serving as the thumbnail image for the current package
     * @return String value
     */
    public String getThumbnailPath() {
        return thumbnailPath;
    }

    /**
     * Gets the name of the current package's group
     * @return String value
     */
    public String getGroup() {
        return group;
    }

    /**
     * Gets the version of the current package
     * @return String value
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets whether child resource(-s) should be excluded from the current package build-up
     * @return True or false
     */
    public boolean isExcludeChildren() {
        return excludeChildren;
    }

    /**
     * Gets the collection of JCR paths indicating separate resources (resource trees) to be included in this package
     * @return True or false
     */
    public List<String> getPaths() {
        return paths;
    }

    /**
     * Assigns package name value to the current instance
     * @param packageName String value, non-blank string expected
     */
    public void setPackageName(final String packageName) {
        this.packageName = packageName;
    }

    /**
     * Assigns thumbnail path value to the current instance
     * @param thumbnailPath String value
     */
    public void setThumbnailPath(final String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    /**
     * Assigns group name value to the current instance
     * @param group String value
     */
    public void setGroup(final String group) {
        this.group = group;
    }

    /**
     * Assigns version value to the current instance
     * @param version String value
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Sets a flag indicating whether child resource(-s) should be excluded from the current package build-up
     * @param excludeChildren Boolean value
     */
    public void setExcludeChildren(final boolean excludeChildren) {
        this.excludeChildren = excludeChildren;
    }

    /**
     * Assigns to the current instance the collection of JCR paths indicating separate resources (resource trees)
     * to be included in the package
     * @param paths {@code List<String>} object, non-empty list expected
     */
    public void setPaths(final List<String> paths) {
        this.paths = paths;
    }
}
