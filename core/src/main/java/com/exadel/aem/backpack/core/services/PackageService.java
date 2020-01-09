package com.exadel.aem.backpack.core.services;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Collection;

public interface PackageService {

	PackageInfo testBuildPackage(ResourceResolver resourceResolver, String packagePath, Collection<String> referencedResources);

	PackageInfo buildPackage(ResourceResolver resourceResolver, String pkgName, String packageGroup, String version);

	PackageInfo buildPackage(ResourceResolver resourceResolver, String packagePath, Collection<String> referencedResources);

	PackageInfo createPackage(ResourceResolver resourceResolver, Collection<String> paths, String pkgName, String packageGroup, String version);

	PackageInfo getPackageInfo(ResourceResolver resourceResolver, String pathToPackage);

	PackageInfo getLatestPackageBuildInfo(String packagePath);
}
