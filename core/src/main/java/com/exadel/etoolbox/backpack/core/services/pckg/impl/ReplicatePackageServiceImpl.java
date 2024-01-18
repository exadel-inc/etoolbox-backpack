package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.LoggerService;
import com.exadel.etoolbox.backpack.core.services.SessionService;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.pckg.ReplicatePackageService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageInfoModel;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.time.LocalDateTime;
import java.util.Calendar;

/**
 * Implements {@link ReplicatePackageService} to provide replication operations
 */
@Component(service = ReplicatePackageService.class)
public class ReplicatePackageServiceImpl implements ReplicatePackageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicatePackageServiceImpl.class);
    private static final String START_REPLICATE_MESSAGE = "Replicate Package: ";
    private static final String IN_PROGRESS_REPLICATE_MESSAGE = "Replicating package";
    private static final String FINISH_REPLICATE_MESSAGE = "Package is replicated asynchronously in ";
    private static final String NODE_NOT_ACCESSIBLE_MESSAGE = "Node is not accessible through the current Session";
    private static final String PACKAGE_IS_NOT_BUILT_OR_INSTALL_MESSAGE = "Before replication package must be built or install";

    @Reference
    private PackageInfoService packageInfoService;

    @Reference
    private BasePackageService basePackageService;

    @Reference
    private SessionService sessionService;

    @Reference
    private LoggerService loggerService;

    @Reference
    private Replicator replicator;

    /**
     * {@inheritDoc}
     */
    @Override
    public PackageInfo replicatePackage(ResourceResolver resourceResolver, PackageInfoModel packageInfoModel) {
        PackageInfo packageInfo = packageInfoService.getPackageInfo(resourceResolver, packageInfoModel);
        if (PackageStatus.BUILT.equals(packageInfo.getPackageStatus()) || PackageStatus.INSTALL.equals(packageInfo.getPackageStatus())) {
            packageInfo.clearLog();
            packageInfo.addLogMessage(START_REPLICATE_MESSAGE + packageInfoModel.getPackagePath());
            packageInfo.addLogMessage(LocalDateTime.now().toString());
            basePackageService.getPackageInfos().put(packageInfoModel.getPackagePath(), packageInfo);
            replicatePackage(resourceResolver.getUserID(), packageInfo);
        } else {
            packageInfo.addLogMessage(BasePackageServiceImpl.ERROR + PACKAGE_IS_NOT_BUILT_OR_INSTALL_MESSAGE);
        }
        return packageInfo;
    }

    /**
     * Called from {@link ReplicatePackageServiceImpl#replicatePackage(ResourceResolver, PackageInfoModel)}
     * Encapsulates installing package in a separate execution thread
     *
     * @param userId User ID per the effective {@code ResourceResolver}
     * @param packageInfo {@link PackageInfo} object to store package building status information in
     */
    private void replicatePackageAsync(final String userId, PackageInfo packageInfo) {
        new Thread(() -> replicatePackage(userId, packageInfo)).start();
    }

    /**
     * Called from {@link ReplicatePackageServiceImpl#replicatePackageAsync(String, PackageInfo)}
     * Performs the internal package replication procedure
     *
     * @param userId User ID per the effective {@code ResourceResolver}
     * @param packageInfo {@link PackageInfo} object to store package building status information in
     */
    private void replicatePackage(final String userId, PackageInfo packageInfo) {
        Session userSession = null;
        try {
            userSession = sessionService.getUserImpersonatedSession(userId);
            JcrPackageManager packMgr = basePackageService.getPackageManager(userSession);
            JcrPackage jcrPackage = packMgr.open(userSession.getNode(packageInfo.getPackagePath()));
            if (jcrPackage != null) {
                Node node = jcrPackage.getNode();
                if (node != null) {
                    StopWatch stopWatch = StopWatch.createStarted();
                    packageInfo.addLogMessage(IN_PROGRESS_REPLICATE_MESSAGE);
                    replicator.replicate(node.getSession(), ReplicationActionType.ACTIVATE, node.getPath());
                    packageInfo.addLogMessage(FINISH_REPLICATE_MESSAGE + stopWatch);
                    packageInfo.setPackageReplicated(Calendar.getInstance());
                } else {
                    packageInfo.addLogMessage(BasePackageServiceImpl.ERROR + NODE_NOT_ACCESSIBLE_MESSAGE);
                }
            } else {
                packageInfo.addLogMessage(BasePackageServiceImpl.ERROR + String.format(BasePackageServiceImpl.PACKAGE_DOES_NOT_EXIST_MESSAGE, packageInfo.getPackagePath()));
            }
        } catch (RepositoryException | ReplicationException e) {
            loggerService.addExceptionToLog(packageInfo, e);
            LOGGER.error("Error during package replication", e);
        } finally {
            sessionService.closeSession(userSession);
        }
    }
}
