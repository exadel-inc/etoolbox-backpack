package com.exadel.etoolbox.backpack.core.services.pckg;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;

/**
 * Narrow cache-facing API for storing and retrieving package build information.
 */
public interface PackageInfoCacheAccess {

    PackageInfo get(String packagePath);

    PackageInfo put(String packagePath, PackageInfo packageInfo);

    PackageInfo remove(String packagePath);

    void clear();
}

