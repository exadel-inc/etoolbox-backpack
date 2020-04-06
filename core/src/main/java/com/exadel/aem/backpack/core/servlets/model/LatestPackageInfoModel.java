package com.exadel.aem.backpack.core.servlets.model;

import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;
import com.exadel.aem.request.annotations.Validate;
import com.exadel.aem.request.validator.impl.RequiredValidator;
import com.exadel.aem.request.validator.impl.WholeNumberValidator;

@RequestMapping
public class LatestPackageInfoModel extends PackageInfoModel {

    @RequestParam
    @Validate(validator = {RequiredValidator.class, WholeNumberValidator.class},
            invalidMessages = {"Latest log index field is required", "Latest log index must be whole number!"})
    private int latestLogIndex;

    public int getLatestLogIndex() {
        return latestLogIndex;
    }

    public void setLatestLogIndex(final int latestLogIndex) {
        this.latestLogIndex = latestLogIndex;
    }
}
