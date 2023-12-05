package com.exadel.etoolbox.backpack.core.util;

import org.apache.commons.lang3.StringUtils;

public class ProgressTrackerMessageUtil {

    private static final String NOP_STATUS_CODE = "-";
    private static final String AGGREGATION_STATUS_LOG_MESSAGE = "Aggregation status:";

    public static boolean isContainAggregationStatus(final String path) {
        return path.contains(AGGREGATION_STATUS_LOG_MESSAGE);
    }

    public static String buildLogMessage(final String statusCode, final String path) {
        return (statusCode.equals(NOP_STATUS_CODE) ? StringUtils.EMPTY : statusCode) + " " + path;
    }
}
