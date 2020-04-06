package com.exadel.aem.request.validator.impl;

import com.exadel.aem.request.validator.Validator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class RequiredValidator implements Validator {

    @Override
    public boolean isValid(final Object parameter) {
        if (parameter instanceof String[]) {
            String[] arrayParams = (String[]) parameter;
            return ArrayUtils.isNotEmpty(arrayParams) && StringUtils.isNotBlank(arrayParams[0]);
        }
        return false;
    }
}
