package com.exadel.aem.backpack.core.services;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.servlets.model.BuildPackageModel;
import com.exadel.aem.backpack.core.servlets.model.CreatePackageModel;
import com.exadel.aem.backpack.core.servlets.model.LatestPackageInfoModel;
import com.exadel.aem.backpack.core.servlets.model.PackageInfoModel;
import org.apache.sling.api.resource.ResourceResolver;

public interface PackageService {

    PackageInfo getPackageInfo(ResourceResolver resourceResolver, PackageInfoModel packageInfoModel);

    PackageInfo createPackage(ResourceResolver resourceResolver, CreatePackageModel createPackageModel);

    PackageInfo buildPackage(ResourceResolver resourceResolver, BuildPackageModel buildPackageModel);

    PackageInfo testBuildPackage(ResourceResolver resourceResolver, BuildPackageModel buildPackageModel);

    PackageInfo getLatestPackageBuildInfo(LatestPackageInfoModel latestPackageInfoModel);
}
