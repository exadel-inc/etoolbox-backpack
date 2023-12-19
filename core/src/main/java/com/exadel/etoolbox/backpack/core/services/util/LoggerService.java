package com.exadel.etoolbox.backpack.core.services.util;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;

public interface LoggerService {

    /**
     * Add information about the exception to {@link PackageInfo}
     *
     * @param packageInfo {@code PackageInfo} object to store status information in
     * @param e Exception to log
     */
    void addExceptionToLog(final PackageInfo packageInfo, final Exception e);
}
