package com.exadel.etoolbox.backpack.core.util;

import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.Map;

public class RequestUtils {

    private static final String PARAMETER_NAME_TYPE = "type";

    private RequestUtils() {
    }

    public static Map<String, String[]> addActionTypeToParameterMap(Map<String, String[]> paramMap, String value) {
        return modifyParameterMap(paramMap, PARAMETER_NAME_TYPE, value);
    }

    public static Map<String, String[]> modifyParameterMap(Map<String, String[]> paramMap, String key, String value) {
        Map<String, String[]> map = new HashMap<>(paramMap);
        map.put(key, ArrayUtils.toArray(value));
        return map;
    }
}
