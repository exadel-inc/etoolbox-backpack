package com.exadel.etoolbox.backpack.core.services.pckg;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * Represents a service running in an AEM instance responsible for deleting package
 */
public interface DeletePackageService {

    /**
     * Triggers operations needed to delete a content package in JCR
     *
     * @param resourceResolver {@code ResourceResolver} instance used to delete the package
     * @param path            {@link String} with path to the package for delete
     */
    void delete(ResourceResolver resourceResolver, String path);
}
