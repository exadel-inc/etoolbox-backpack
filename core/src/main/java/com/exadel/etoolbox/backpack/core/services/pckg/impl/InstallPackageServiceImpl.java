package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.InstallPackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.util.LoggerService;
import com.exadel.etoolbox.backpack.core.services.util.SessionService;
import com.exadel.etoolbox.backpack.core.services.util.constants.BackpackConstants;
import com.exadel.etoolbox.backpack.core.servlets.model.InstallPackageModel;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.DependencyHandling;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Calendar;

/**
 * Implements {@link InstallPackageService} to provide install package operations
 */
@Component(service = InstallPackageService.class)
public class InstallPackageServiceImpl implements InstallPackageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallPackageServiceImpl.class);
    private static final String START_INSTALL_MESSAGE = "Install Package: ";

    @Reference
    private PackageInfoService packageInfoService;

    @Reference
    private BasePackageService basePackageService;

    @Reference
    private SessionService sessionService;

    @Reference
    private LoggerService loggerService;

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo installPackage(ResourceResolver resourceResolver, InstallPackageModel installPackageModel) {
        PackageInfo packageInfo = packageInfoService.getPackageInfo(resourceResolver, installPackageModel.getPackagePath());
        if (!PackageStatus.INSTALL_IN_PROGRESS.equals(packageInfo.getPackageStatus()) && !PackageStatus.BUILD_IN_PROGRESS.equals(packageInfo.getPackageStatus())) {
            packageInfo.setPackageStatus(PackageStatus.INSTALL_IN_PROGRESS);
            packageInfo.clearLog();
            packageInfo.addLogMessage(START_INSTALL_MESSAGE + packageInfo.getPackagePath());
            packageInfo.addLogMessage(LocalDateTime.now().toString());
            basePackageService.getPackageCacheAsMap().put(installPackageModel.getPackagePath(), packageInfo);
            installPackageAsync(resourceResolver.getUserID(), installPackageModel, packageInfo);
        }
        return packageInfo;
    }

    /**
     * Called from {@link InstallPackageServiceImpl#installPackage(ResourceResolver, InstallPackageModel)}.
     * Encapsulates installing package in a separate execution thread
     *
     * @param userId              User ID per the effective {@code ResourceResolver}
     * @param installPackageModel {@link InstallPackageModel} object containing user-set options for the package installing
     * @param packageInfo         {@link PackageInfo} object to store package installation status information in
     */
    private void installPackageAsync(String userId, InstallPackageModel installPackageModel, PackageInfo packageInfo) {
        new Thread(() -> installPackage(userId, installPackageModel, packageInfo)).start();
    }

    /**
     * Performs the internal package installing procedure and stores status information
     *
     * @param userId              User ID per the effective {@code ResourceResolver}
     * @param installPackageModel {@link InstallPackageModel} object containing user-set options for the package installing
     * @param packageInfo         {@link PackageInfo} object to store package installation status information in
     */
    private void installPackage(String userId, InstallPackageModel installPackageModel, PackageInfo packageInfo) {
        Session session = null;
        try {
            session = sessionService.getUserImpersonatedSession(userId);
            JcrPackageManager packMgr = basePackageService.getPackageManager(session);
            JcrPackage jcrPackage = packMgr.open(session.getNode(packageInfo.getPackagePath()));
            if (jcrPackage == null) {
                packageInfo.setPackageStatus(PackageStatus.ERROR);
                packageInfo.addLogMessage(BackpackConstants.ERROR + String.format(BackpackConstants.PACKAGE_DOES_NOT_EXIST_MESSAGE, packageInfo.getPackagePath()));
                return;
            }
            StopWatch stopWatch = StopWatch.createStarted();
            ImportOptions importOptions = getImportOptions(installPackageModel, packageInfo);
            packageInfo.setPackageStatus(PackageStatus.INSTALL_IN_PROGRESS);
            jcrPackage.install(importOptions);
            packageInfo.setPackageInstalled(Calendar.getInstance());
            packageInfo.setPackageStatus(PackageStatus.INSTALL);
            packageInfo.addLogMessage("Package installed in " + stopWatch);
        } catch (RepositoryException | PackageException | IOException e) {
            packageInfo.setPackageStatus(PackageStatus.ERROR);
            loggerService.addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during package installation", e);
        } finally {
            sessionService.closeSession(session);
        }
    }

    /**
     * Called from {@link InstallPackageServiceImpl#installPackage(String, InstallPackageModel, PackageInfo)} to get the {@code ImportOptions}
     * instance with options that control the package import
     *
     * @param installPackageModel {@link InstallPackageModel} object containing user-set options for the package installing
     * @param packageInfo         {@link PackageInfo} object to store package installation status information in
     * @return {@code ImportOptions} object
     */
    private ImportOptions getImportOptions(InstallPackageModel installPackageModel, PackageInfo packageInfo) {
        ImportOptions importOptions = new ImportOptions();
        importOptions.setAutoSaveThreshold(installPackageModel.getThreshold());
        importOptions.setDependencyHandling(DependencyHandling.valueOf(installPackageModel.getDependencyHandling().toUpperCase()));
        importOptions.setListener(new ProgressTrackerListener() {
            @Override
            public void onMessage(Mode mode, String s, String s1) {
                packageInfo.addLogMessage(s + " " + s1);
            }

            @Override
            public void onError(Mode mode, String s, Exception e) {
                packageInfo.addLogMessage(s + " " + e.getMessage());
            }
        });
        return importOptions;
    }
}
