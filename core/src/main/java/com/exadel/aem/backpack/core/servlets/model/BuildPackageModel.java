package com.exadel.aem.backpack.core.servlets.model;

import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;

import java.util.List;

@RequestMapping
public class BuildPackageModel extends PackageInfoModel {


    @RequestParam
    private boolean testBuild;

    @RequestParam
    private List<String> referencedResources;

    public boolean isTestBuild() {
        return testBuild;
    }

    public List<String> getReferencedResources() {
        return referencedResources;
    }

    public void setTestBuild(final boolean testBuild) {
        this.testBuild = testBuild;
    }

    public void setReferencedResources(final List<String> referencedResources) {
        this.referencedResources = referencedResources;
    }
}
