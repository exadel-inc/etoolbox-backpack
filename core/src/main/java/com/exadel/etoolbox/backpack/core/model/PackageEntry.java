package com.exadel.etoolbox.backpack.core.model;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PackageEntry {

    @ValueMapValue(name = JcrConstants.JCR_TITLE)
    private String title;

    @ValueMapValue
    private String type;

    public String getIcon() {
        if ("livecopy".equals(type)) {
            return "multiple";
        } else if ("asset".equals(type)) {
            return "gears";
        } else if ("tag".equals(type)) {
            return "tag";
        }
        return "page";
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }
}
