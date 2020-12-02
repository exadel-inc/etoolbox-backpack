package com.exadel.aem.backpack.core.servlets.model;

import com.exadel.aem.backpack.request.annotations.RequestMapping;
import com.exadel.aem.backpack.request.annotations.RequestParam;
import com.exadel.aem.backpack.request.annotations.Validate;
import com.exadel.aem.backpack.request.validator.impl.RequiredValidator;

/**
 * Represents the set of user-defined options for a path filter creation.
 * Upon initialization, used as part of {@link PackageModel}
 */
@RequestMapping
public class PathModel {

    @RequestParam
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Path is required")
    private String path;

    @RequestParam
    private boolean excludeChildren;

    public PathModel() {
    }

    public PathModel(final String path, final boolean excludeChildren) {
        this.path = path;
        this.excludeChildren = excludeChildren;
    }

    /**
     * Gets the filter path.
     * @return String value
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the whether or not child pages excluded from a package build
     * @return String value
     */
    public boolean isExcludeChildren() {
        return excludeChildren;
    }
}
