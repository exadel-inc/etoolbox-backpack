package com.exadel.etoolbox.backpack.core.dto.response;

import java.util.Collection;

/**
 * Represents an object with the status of a result of package creation.
 */
public class Status {

    private final String status;
    private final String title;
    private final String message;

    private Status(String status, String title, String message) {
        this.status = status;
        this.title = title;
        this.message = message;
    }

    /**
     * Data object inherited from Status and containing standard warning information and additional data.
     */
    private static final class WarningStatus extends Status {

        private static final String STATUS_WARNING = "warning";
        private static final String TITLE_WARNING = "Warning";
        private static final String MESSAGE_WARNING = "Your package has been created, but some resources are not included in the package:";
        private final Collection<String> brokenPaths;

        public WarningStatus(final Collection<String> brokenPaths) {
            super(STATUS_WARNING, TITLE_WARNING, MESSAGE_WARNING);
            this.brokenPaths = brokenPaths;
        }
    }

    /**
     * Method return Object with warning result status
     *
     * @param brokenPaths Collection<String> of resource paths that were not found
     * @return {@code Status} object
     */
    public static Status warning(final Collection<String> brokenPaths) {
        return new WarningStatus(brokenPaths);
    }
}
