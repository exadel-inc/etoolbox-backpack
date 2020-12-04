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

package com.exadel.aem.backpack.core.model;

import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.servlets.model.PackageModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;

import static com.exadel.aem.backpack.core.services.impl.PackageServiceImpl.PACKAGES_ROOT_PATH;

/**
 * Represents the package modification info.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class EditPackageModel {

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private PackageService packageService;

    private String packagePath;
    private PackageModel packageModel;

    /**
     * Instantiation of the model
     */
    @PostConstruct
    public void init() {
        packagePath = request.getParameter("packagePath");
        if (StringUtils.isNotBlank(packagePath)) {
            packageModel = packageService.getPackageModelByPath(packagePath, request.getResourceResolver());
        }
    }

    /**
     * Gets the modified package path
     * @return String value
     */
    public String getPackagePath() {
        return packagePath;
    }

    /**
     * Gets the modified package group path
     * @return String value
     */
    public String getPackageGroupPath() {
        return PACKAGES_ROOT_PATH + "/" + packageModel.getGroup();
    }

    /**
     * Gets the modified package model
     * @return {@link PackageModel}
     */
    public PackageModel getPackageModel() {
        return packageModel;
    }
}
