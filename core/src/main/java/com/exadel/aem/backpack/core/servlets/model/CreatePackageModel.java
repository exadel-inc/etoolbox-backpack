package com.exadel.aem.backpack.core.servlets.model;

import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;
import com.exadel.aem.request.annotations.Validate;
import com.exadel.aem.request.validator.impl.RequiredValidator;

import java.util.List;

@RequestMapping
public class CreatePackageModel {

    @RequestParam
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Package name field is required")
    private String packageName;

    @RequestParam
    private String thumbnailPath;

    @RequestParam
    private String group;

    @RequestParam
    private String version;

    @RequestParam
    private boolean excludeChildren;

    @RequestParam
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Resource filter(s) is required")
    private List<String> paths;

    public String getPackageName() {
        return packageName;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public boolean isExcludeChildren() {
        return excludeChildren;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPackageName(final String packageName) {
        this.packageName = packageName;
    }

    public void setThumbnailPath(final String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setExcludeChildren(final boolean excludeChildren) {
        this.excludeChildren = excludeChildren;
    }

    public void setPaths(final List<String> paths) {
        this.paths = paths;
    }
}
