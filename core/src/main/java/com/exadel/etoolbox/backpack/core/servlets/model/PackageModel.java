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

package com.exadel.etoolbox.backpack.core.servlets.model;

import com.exadel.etoolbox.backpack.request.annotations.RequestMapping;
import com.exadel.etoolbox.backpack.request.annotations.RequestParam;
import com.exadel.etoolbox.backpack.request.annotations.Validate;
import com.exadel.etoolbox.backpack.request.validator.impl.RequiredValidator;

import java.util.List;

@RequestMapping
public class PackageModel {

    @RequestParam
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Package name field is required")
    private String packageName;

    @RequestParam
    private String packagePath;

    @RequestParam
    private String group;

    @RequestParam
    private String version;

    @RequestParam
    private String thumbnailPath;

    @RequestParam(name = "initialResource")
    private List<String> initialResources;


    /**
     * Gets the name of the current package
     *
     * @return String value
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Gets path to the image asset serving as the thumbnail image for the current package
     *
     * @return String value
     */
    public String getThumbnailPath() {
        return thumbnailPath;
    }

    /**
     * Gets the name of the current package's group
     *
     * @return String value
     */
    public String getGroup() {
        return group;
    }

    /**
     * Gets the version of the current package
     *
     * @return String value
     */
    public String getVersion() {
        return version;
    }


    /**
     * Assigns package name value to the current instance
     *
     * @param packageName String value, non-blank string expected
     */
    public void setPackageName(final String packageName) {
        this.packageName = packageName;
    }

    /**
     * Assigns thumbnail path value to the current instance
     *
     * @param thumbnailPath String value
     */
    public void setThumbnailPath(final String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    /**
     * Assigns group name value to the current instance
     *
     * @param group String value
     */
    public void setGroup(final String group) {
        this.group = group;
    }

    /**
     * Assigns version value to the current instance
     *
     * @param version String value
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
    }

    public List<String> getInitialResources() {
        return initialResources;
    }
}
