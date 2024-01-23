package com.exadel.etoolbox.backpack.core.services.pckg.v2;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageInfoModel;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Represents a service running in an AEM instance responsible for replication operation with package
 */
public interface ReplicatePackageService {

    /**
     * Triggers operations needed to replicate the content package specified in the request, and report the result
     *
     * @param resourceResolver {@code ResourceResolver} instance used to replicate the package
     * @param packageInfoModel {@code PackageInfoModel} instance contains package path for replication
     * @return {@link PackageInfo} instance reporting the current package status
     */
    PackageInfo replicatePackage(ResourceResolver resourceResolver, PackageInfoModel packageInfoModel);
}
