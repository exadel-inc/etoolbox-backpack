package com.exadel.etoolbox.backpack.core.services.util.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.util.LoggerService;
import com.exadel.etoolbox.backpack.core.services.util.constants.BackpackConstants;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = LoggerService.class)
public class LoggerServiceImpl implements LoggerService {

    @Reference
    private BasePackageService basePackageService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void addExceptionToLog(PackageInfo packageInfo, Exception e) {
        packageInfo.addLogMessage(BackpackConstants.ERROR + e.getMessage());
        if (basePackageService.isEnableStackTrace()) {
            packageInfo.addLogMessage(ExceptionUtils.getStackTrace(e));
        }
    }
}
