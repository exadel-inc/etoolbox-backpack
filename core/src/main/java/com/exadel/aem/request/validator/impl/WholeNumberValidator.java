package com.exadel.aem.request.validator.impl;

import com.exadel.aem.request.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WholeNumberValidator implements Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(WholeNumberValidator.class);

    @Override
    public boolean isValid(final Object parameter) {
        if (parameter instanceof String[]) {
            String[] arrayParams = (String[]) parameter;
            if (arrayParams.length == 1) {
                try {
                    final int i = Integer.parseInt(arrayParams[0]);
                    return i >= 0;
                } catch (NumberFormatException e) {
                    LOGGER.error("Parse parameter exception", e);
                }
            }
        }
        return false;
    }
}
