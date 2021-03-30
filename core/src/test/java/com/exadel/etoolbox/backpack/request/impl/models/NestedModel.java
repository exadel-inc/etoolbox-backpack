package com.exadel.etoolbox.backpack.request.impl.models;

import com.exadel.etoolbox.backpack.request.annotations.RequestMapping;
import com.exadel.etoolbox.backpack.request.annotations.RequestParam;
import com.exadel.etoolbox.backpack.request.annotations.Validate;
import com.exadel.etoolbox.backpack.request.validator.impl.RequiredValidator;

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
