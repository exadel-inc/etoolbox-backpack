package com.exadel.etoolbox.backpack.core.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Util class using for work with {@code ProgressTrackerListener.class}
 */
public class ProgressTrackerUtil {

    private static final String NOP_STATUS_CODE = "-";
    private static final String AGGREGATION_STATUS_LOG_MESSAGE = "Aggregation status:";

    /**
     * Method check if message contains 'Aggregation status' substring
     *
     * @param message The {@code String} with message of current operation
     * @return {@code boolean} value depends on contain 'Aggregation status' substring
     */
    public static boolean isContainAggregationStatus(final String message) {
        return message.contains(AGGREGATION_STATUS_LOG_MESSAGE);
    }

    /**
     * Build log message that contains status code and operation message
     *
     * @param statusCode The {@code String} with status of current operation
     * @param message The {@code String} with message of current operation
     * @return {@code String} message that contains status code and operation message
     */
    public static String buildLogMessage(final String statusCode, final String message) {
        return (statusCode.equals(NOP_STATUS_CODE) ? StringUtils.EMPTY : statusCode) + " " + message;
    }
}
