package com.exadel.aem.request.impl.models;

import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;
import com.exadel.aem.request.annotations.Validate;
import com.exadel.aem.request.validator.impl.RequiredValidator;
import com.exadel.aem.request.validator.impl.WholeNumberValidator;

@RequestMapping
public class ValidateModel {

    @RequestParam
    @Validate(validator = {RequiredValidator.class},
            invalidMessages = {"String field is required"})
    private String requiredString;

    @RequestParam
    @Validate(validator = {WholeNumberValidator.class},
            invalidMessages = {"Field must be whole number!"})
    private int wholeNumber;

    public String getRequiredString() {
        return requiredString;
    }

    public int getWholeNumber() {
        return wholeNumber;
    }
}
