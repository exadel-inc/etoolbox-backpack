package com.exadel.aem.backpack.core.services;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.sling.api.resource.ResourceResolver;

public interface PackageService {

    PackageInfo getPackageInfo(ResourceResolver resourceResolver, PackageRequestInfo requestInfo);

    PackageInfo createPackage(ResourceResolver resourceResolver, PackageRequestInfo requestInfo);

    PackageInfo buildPackage(ResourceResolver resourceResolver, PackageRequestInfo requestInfo);

    PackageInfo testBuildPackage(ResourceResolver resourceResolver, PackageRequestInfo requestInfo);

    PackageInfo getLatestPackageBuildInfo(PackageRequestInfo requestInfo);
}
