package com.exadel.etoolbox.backpack.core.services.pckg;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.servlets.model.InstallPackageModel;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Performs package installation.
 */
public interface InstallPackageService {

    /**
     * Triggers operations needed to install the content package specified in the request, and reports the results
     *
     * @param resourceResolver  {@code ResourceResolver} instance used to install the package
     * @param installPackageModel {@link InstallPackageModel} instance containing user-set options for the package installing
     * @return {@link PackageInfo} instance reporting the current package status
     */
    PackageInfo installPackage(ResourceResolver resourceResolver, InstallPackageModel installPackageModel);
}
