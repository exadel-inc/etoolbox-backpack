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

import com.day.cq.commons.jcr.JcrConstants;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.PackageInfoService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Represents the package groups options.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PackageGroups {

    private static final String ROOT_KEY = "/etc/packages";

    private static final String ROOT_TEXT = "All packages";

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private PackageInfoService packageInfoService;

    private List<Option> options;

    /**
     * Instantiation of the model.
     */
    @PostConstruct
    public void init() {
        ResourceResolver resolver = request.getResourceResolver();
        List<Resource> results = packageInfoService.getPackageFolders(resolver);
        String groupParam = request.getParameter("group")==null ? ROOT_KEY : request.getParameter("group");
        options = results.stream()
                .map(resource -> createDataOption(resource, groupParam))
                .collect(Collectors.toList());
        Option rootOption = createDataOption(resolver.getResource(ROOT_KEY), groupParam);
        options.add(0, rootOption);
    }


    /**
     * Called from {@link PackageGroups#init()} for {@link Option} creation
     *
     * @param resource         {@code Resource} to create option from
     * @param groupFromRequest {@code String} group that came from request
     * @return {@link Option} instance
     */
    private Option createDataOption(Resource resource, String groupFromRequest) {
        String optionText = getOptionText(resource);
        String packageId = getPackageId(resource);
        boolean selected = resource.getPath().equals(StringUtils.removeEnd(groupFromRequest, "/"));
        return new Option(optionText, packageId, selected);
    }


    /**
     * Called from {@link PackageGroups#createDataOption(Resource, String)} for option text retrieval
     * Option text is created from <i>jcr:title</i> property by default. In the case when property is missing option text
     * will be created from node name
     *
     * @param resource {@code Resource} to get option text from
     * @return Option text
     */
    private String getOptionText(Resource resource) {
        ValueMap valueMap = resource.getValueMap();
        if (resource.getPath().equals(ROOT_KEY)) {
            return ROOT_TEXT;
        }
        String title = valueMap.get(JcrConstants.JCR_TITLE, String.class);
        if (StringUtils.isBlank(title)) {
            title = getPackageId(resource);
        }
        return title;
    }

    private String getPackageId(final Resource resource) {
        return StringUtils.substringAfter(resource.getPath(), ROOT_KEY + "/");
    }

    /**
     * Gets the package group options
     *
     * @return List of options
     */
    public List<Option> getOptions() {
        return options;
    }

    /**
     * Represents the single option used for select or autocomplete component generation
     */
    public class Option {
        private String text;
        private String value;
        private boolean selected;

        public Option(final String text, final String value, final boolean selected) {
            this.text = text;
            this.value = value;
            this.selected = selected;
        }

        public String getText() {
            return text;
        }

        public String getValue() {
            return value;
        }

        public boolean isSelected() {
            return selected;
        }
    }
}
