package com.exadel.etoolbox.backpack.core.model;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.*;
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

    private Map<String, List<PackageEntry>> referencesByType;
    private int referenceCount;

    private List<PackageEntry> liveCopies;

    @PostConstruct
    private void init() {
        referencesByType = StreamSupport.stream(resource.getChildren().spliterator(), false)
                .map(resource -> resource.adaptTo(PackageEntry.class))
                .filter(Objects::nonNull)
                .filter(packageEntry -> !packageEntry.getType().equals("liveCopy"))
                .collect(Collectors.toMap(PackageEntry::getType, value -> new ArrayList<>(Collections.singletonList(value)), (list1, list2) -> {
                    list1.addAll(list2);
                    return list1;
                }));
        referenceCount = referencesByType.values().stream().mapToInt(List::size).sum();
        liveCopies = StreamSupport.stream(resource.getChildren().spliterator(), false)
                .map(resource -> resource.adaptTo(PackageEntry.class))
                .filter(Objects::nonNull)
                .filter(packageEntry -> packageEntry.getType().equals("liveCopy"))
                .collect(Collectors.toList());
    }

    public boolean hasReferences() {
        return referencesByType != null && !referencesByType.isEmpty();
    }

    public boolean hasLiveCopies() {
        return liveCopies != null && !liveCopies.isEmpty();
    }

    public List<PackageEntry> getLiveCopies() {
        return liveCopies;
    }

    public Map<String, List<PackageEntry>> getReferencesByType() {
        return referencesByType;
    }

    public int getReferenceCount() {
        return referenceCount;
    }
}
