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

package com.exadel.etoolbox.backpack.core.model;

import com.exadel.etoolbox.backpack.core.services.pckg.v2.PackageInfoService;
import com.exadel.etoolbox.backpack.core.servlets.model.v2.PackageModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the package modification info.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PackageDataModel {

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private PackageInfoService packageInfoService;

    private PackageModel packageModel;

    private List<String> initialResources;

    /**
     * Instantiation of the model
     */
    @PostConstruct
    public void init() {
        String packagePath = request.getParameter("packagePath");
        if (StringUtils.isNotBlank(packagePath)) {
            packageModel = packageInfoService.getPackageModelByPath(packagePath, request.getResourceResolver());
        }
        if (StringUtils.isNotBlank(request.getParameter("initialResource"))) {
            initialResources = Arrays.stream(request.getParameterValues("initialResource"))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Gets the modified package model
     *
     * @return {@link PackageModel}
     */
    public PackageModel getPackageModel() {
        return packageModel;
    }

    public List<String> getInitialResources() {
        return initialResources;
    }
}
