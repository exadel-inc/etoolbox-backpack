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

import java.util.List;

@RequestMapping
public class BuildPackageModel extends PackageInfoModel {


    @RequestParam
    private boolean testBuild;

    @RequestParam
    private List<String> referencedResources;

    public boolean isTestBuild() {
        return testBuild;
    }

    public List<String> getReferencedResources() {
        return referencedResources;
    }

    public void setTestBuild(final boolean testBuild) {
        this.testBuild = testBuild;
    }

    public void setReferencedResources(final List<String> referencedResources) {
        this.referencedResources = referencedResources;
    }
}
