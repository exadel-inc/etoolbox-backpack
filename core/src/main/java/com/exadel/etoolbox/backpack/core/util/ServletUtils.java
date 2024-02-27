package com.exadel.etoolbox.backpack.core.util;

import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.Map;

public class ServletUtils {

    public static final String PARAMETER_NAME_TYPE = "type";
    public static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";
    public static final String SERVLET_PATH_BASE = "services/backpack/";

    private ServletUtils() {
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
