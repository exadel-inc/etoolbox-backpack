package com.exadel.etoolbox.backpack.core.model;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Model(adaptables = SlingHttpServletRequest.class)
public class PackageEntriesDatasource {

    private static final String[] FOLDERS = new String[] {"regression/resources", "resources/qatest/polaris", "team1/marinas-pages"};
    private static final String[] PAGES = new String[] {"content-fragment", "test-page", "cta_testing", "column-container"};
    private static final String[] LOCALES = new String[] {"us/en", "de/de", "fr/fr"};

    @SlingObject
    private SlingHttpServletRequest request;

    private String resourceType;

    @PostConstruct
    private void initModel() {
        resourceType = Optional.ofNullable(request.getResource().getChild("datasource"))
                .map(Resource::getValueMap)
                .map(vm -> vm.get("itemResourceType", String.class))
                .orElse(JcrConstants.NT_UNSTRUCTURED);

        List<Resource> resources = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            String folder = FOLDERS[RandomUtils.nextInt(0, FOLDERS.length)];
            String page = PAGES[RandomUtils.nextInt(0, PAGES.length)];
            String title = "/content/hpe/base/blueprint/" + folder + "/" + page + "-" + i;

            List<Resource> subsidiaries = new ArrayList<>();

            boolean addLiveCopies = RandomUtils.nextBoolean();
            if (addLiveCopies) {
                for (String locale : LOCALES) {
                    String localeTitle = title.replace("base/blueprint", "country/" + locale);
                    Resource liveCopy = createPackageEntry("livecopy", localeTitle, ImmutableMap.of("upstream", title));
                    subsidiaries.add(liveCopy);
                }
            }

            boolean addAssets = RandomUtils.nextBoolean();
            if (addAssets) {
                for (int j = 0; j < RandomUtils.nextInt(0, 3); j++) {
                    String assetTitle = "/content/dam/hpe/images/public/image" + j + ".png";
                    Resource asset = createPackageEntry("reference", assetTitle, ImmutableMap.of("upstream", title));
                    subsidiaries.add(asset);
                }
            }

            Resource entry = createPackageEntry(
                    "page",
                    title,
                    ImmutableMap.of("hasChildren", true),
                    subsidiaries);
            resources.add(entry);
        }

        request.setAttribute(DataSource.class.getName(), new SimpleDataSource(resources.iterator()));
    }

    private Resource createPackageEntry(String type, String title, Map<String, Object> additionalProperties) {
        return createPackageEntry(type, title, additionalProperties, null);
    }

    private Resource createPackageEntry(String type, String title, Map<String, Object> additionalProperties, List<Resource> children) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(JcrConstants.JCR_TITLE, title);
        properties.put("type", type);
        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }
        ValueMap valueMap = new ValueMapDecorator(properties);
        return new ValueMapResource(
                request.getResourceResolver(),
                "/package-entry/" + title.replace("/", "-"),
                resourceType,
                valueMap,
                children);
    }
}
