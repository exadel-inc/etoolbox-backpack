package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.DeletePackageService;
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

/**
 * Implements {@link DeletePackageService} to provide delete package operations
 */
@Component(service = DeletePackageService.class)
public class DeletePackageServiceImpl implements DeletePackageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeletePackageServiceImpl.class);

    @Reference
    private BasePackageService basePackageService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(ResourceResolver resourceResolver, String path) {
        final Session session = resourceResolver.adaptTo(Session.class);

        if (session == null) {
            LOGGER.error("Error during package delete: session is null.");
            return;
        }

        JcrPackage jcrPackage = null;

        try {
            JcrPackageManager packMgr = basePackageService.getPackageManager(session);
            Node packageNode = session.getNode(path);
            jcrPackage = packMgr.open(packageNode);
            if (jcrPackage != null) {
                packMgr.remove(jcrPackage);
            }
            clearCache(path);
        } catch (RepositoryException e) {
            LOGGER.error("Error during package delete", e);
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
    }

    private void clearCache(String path) {
        basePackageService.getPackageInfos().asMap().remove(path);
    }
}
