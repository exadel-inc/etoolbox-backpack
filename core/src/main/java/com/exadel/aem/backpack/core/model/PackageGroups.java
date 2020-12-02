package com.exadel.aem.backpack.core.model;

import com.day.cq.commons.jcr.JcrConstants;
import com.exadel.aem.backpack.core.services.PackageService;
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
    private PackageService packageService;

    private List<Option> options;

    /**
     * Instantiation of the model.
     */
    @PostConstruct
    public void init() {
        ResourceResolver resolver = request.getResourceResolver();
        List<Resource> results = packageService.getPackageFolders(resolver);
        String groupParam = request.getParameter("group");

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
        boolean selected = resource.getPath().equals(groupFromRequest);
        return new Option(optionText, resource.getPath(), selected);
    }


    /**
     * Called from {@link PackageGroups#createDataOption(Resource, String)} for option text retrieve
     * Option text is create from <i>jcr:title</i> property by default. In the case when property is missed option text
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
            title = StringUtils.substringAfter(resource.getPath(), ROOT_KEY + "/");
        }
        return title;
    }

    /**
     * Gets the package group options
     *
     * @return list of options
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
