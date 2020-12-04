package com.exadel.aem.backpack.request.impl.models;

import com.exadel.aem.backpack.request.annotations.RequestMapping;
import com.exadel.aem.backpack.request.annotations.RequestParam;
import com.exadel.aem.backpack.request.annotations.Validate;
import com.exadel.aem.backpack.request.validator.impl.RequiredValidator;

@RequestMapping
public class NestedModel {

    @RequestParam
    @Validate(validator = {RequiredValidator.class},
            invalidMessages = {"String field is required"})
    private String requiredString;

    @RequestParam
    private String notRequiredString;

    public String getRequiredString() {
        return requiredString;
    }

    public String getNotRequiredString() {
        return notRequiredString;
    }
}
