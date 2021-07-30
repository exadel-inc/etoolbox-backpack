package com.exadel.etoolbox.backpack.core.model;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;

/**
 * Represents the package installation info.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class InstallPackageModel {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    @Default(intValues = 1024)
    private int threshold;

    @ValueMapValue
    @Default(values = "required")
    private String dependencyHandling;

    private String packagePath;

    /**
     * Instantiation of the model
     */
    @PostConstruct
    public void init() {
        packagePath = request.getParameter("path");
    }

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

    /**
     * Gets the installed package path
     * @return String value
     */
    public String getPackagePath() {
        return packagePath;
    }
}
