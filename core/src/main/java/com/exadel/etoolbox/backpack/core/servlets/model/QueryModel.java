package com.exadel.etoolbox.backpack.core.servlets.model;

import com.exadel.etoolbox.backpack.request.annotations.RequestMapping;
import com.exadel.etoolbox.backpack.request.annotations.RequestParam;
import com.exadel.etoolbox.backpack.request.annotations.Validate;
import com.exadel.etoolbox.backpack.request.validator.impl.RequiredValidator;

@RequestMapping
public class QueryModel {
    @RequestParam
    private String query;

    @RequestParam
    private boolean excludeChildren;

    public QueryModel(String query, boolean excludeChildren) {
        this.query = query;
        this.excludeChildren = excludeChildren;
    }

    public String getQuery() {
        return query;
    }

    public boolean isExcludeChildren() {
        return excludeChildren;
    }
}
