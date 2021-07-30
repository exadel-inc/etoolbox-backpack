package com.exadel.etoolbox.backpack.core.servlets.model;

import com.exadel.etoolbox.backpack.core.services.pckg.InstallPackageService;
import com.exadel.etoolbox.backpack.core.servlets.InstallPackageServlet;
import com.exadel.etoolbox.backpack.request.annotations.RequestMapping;
import com.exadel.etoolbox.backpack.request.annotations.RequestParam;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Represents the set of user-defined options for a request to install a package. Upon initialization, passed
 * as a parameter to the {@link InstallPackageService#installPackage(ResourceResolver, InstallPackageModel)}
 *
 * @see InstallPackageServlet
 */
@RequestMapping
public class InstallPackageModel extends PackageInfoModel{

    @RequestParam
    private int threshold;

    @RequestParam
    private String dependencyHandling;

    /**
     * Gets the installed package auto-save threshold
     * @return int value
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * Gets the installed package the dependency handling
     * @return String value
     */
    public String getDependencyHandling() {
        return dependencyHandling;
    }
}
