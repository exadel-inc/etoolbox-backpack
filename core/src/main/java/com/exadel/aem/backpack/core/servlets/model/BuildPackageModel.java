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
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

/**
 * Represents the set of user-defined options for a request to build a package. Upon initialization, passed
 * as a parameter to the {@link com.exadel.aem.backpack.core.services.PackageService#buildPackage(ResourceResolver, BuildPackageModel)}
 * @see com.exadel.aem.backpack.core.servlets.BuildPackageServlet
 */
@RequestMapping
public class BuildPackageModel extends PackageInfoModel {

    @RequestParam
    @SuppressWarnings("UnusedDeclaration") // directly injected by the RequestAdapter routine
    private boolean testBuild;

    @RequestParam
    private List<String> referencedResources;

    /**
     * Gets whether this request is for a test build (a dry-run build without assembling actual package file)
     * @return True or false
     */
    public boolean isTestBuild() {
        return testBuild;
    }

    /**
     * Gets collection of paths to JCR resources that must be included in the current package
     * @return {@code List<String>} object storing collection of paths
     */
    public List<String> getReferencedResources() {
        return referencedResources;
    }

    /**
     * Assigns to this instance a collection of paths to JCR resources that must be included in the current package
     */
    public void setReferencedResources(final List<String> referencedResources) {
        this.referencedResources = referencedResources;
    }
}
