package com.exadel.etoolbox.backpack.core.util;

import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.Map;

public class RequestUtils {

    private RequestUtils() {
    }

    public static Map<String, String[]> modifyParameterMap(Map<String, String[]> paramMap, String key, String value) {
        Map<String, String[]> map = new HashMap<>(paramMap);
        map.put(key, ArrayUtils.toArray(value));
        return map;
    }
}
