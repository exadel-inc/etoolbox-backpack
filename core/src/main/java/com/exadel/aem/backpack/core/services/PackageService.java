package com.exadel.aem.backpack.core.services;

import com.exadel.aem.backpack.core.dto.BuildPackageInfo;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Collection;

public interface PackageService {

	BuildPackageInfo createPackage(ResourceResolver resourceResolver, Collection<String> paths, String pkgName, String packageGroup);
}
