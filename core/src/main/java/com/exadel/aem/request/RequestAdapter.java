package com.exadel.aem.request;

import com.exadel.aem.request.validator.ValidatorResponse;

import java.util.Map;

public interface RequestAdapter {
    <T> T adapt(Map<String, Object> parameterMap, Class<T> tClazz);

    <T> ValidatorResponse<T> adaptValidate(Map<String, Object> parameterMap, Class<T> tClazz);
}
