package com.exadel.etoolbox.backpack.core.services.resource;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.servlets.model.v2.PathModel;
import org.apache.sling.api.resource.ResourceResolver;

public interface BaseResourceService {

    PackageInfo getPackageInfo(ResourceResolver resourceResolver, PathModel pathModel);
}
