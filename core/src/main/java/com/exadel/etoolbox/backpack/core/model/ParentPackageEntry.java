package com.exadel.etoolbox.backpack.core.model;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ParentPackageEntry extends PackageEntry {

    @SlingObject
    private Resource resource;

    @ValueMapValue(name = JcrConstants.JCR_TITLE)
    private String title;

    @ValueMapValue
    private String type;

    @ValueMapValue
    private boolean hasChildren;

    private List<PackageEntry> subsidiaries;

    @PostConstruct
    private void init() {
        subsidiaries = StreamSupport.stream(resource.getChildren().spliterator(), false)
                .map(resource -> resource.adaptTo(PackageEntry.class))
                .collect(Collectors.toList());
    }

    public boolean hasSubsidiaries() {
        return subsidiaries != null && !subsidiaries.isEmpty();
    }

    public List<PackageEntry> getSubsidiaries() {
        return subsidiaries;
    }
}
