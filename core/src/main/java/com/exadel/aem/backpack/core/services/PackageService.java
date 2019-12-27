package com.exadel.aem.backpack.core.services;

import com.exadel.aem.backpack.core.dto.response.BuildPackageInfo;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Collection;
import java.util.List;

public interface PackageService {

	BuildPackageInfo testBuild(ResourceResolver resourceResolver, Collection<String> paths);
	BuildPackageInfo buildPackage(ResourceResolver resourceResolver, String pkgName, String packageGroup, String version);
	BuildPackageInfo createPackage(ResourceResolver resourceResolver, Collection<String> paths, String pkgName, String packageGroup, String version);
	List<String> getLatestPackageBuildInfo(String pkgName, String pkgGroupName, String version);
}
