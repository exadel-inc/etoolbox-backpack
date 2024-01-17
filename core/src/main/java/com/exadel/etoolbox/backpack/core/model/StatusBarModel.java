package com.exadel.etoolbox.backpack.core.model;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.PackageInfoService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageInfoModel;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.validator.ValidatorResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;

@Model(adaptables = SlingHttpServletRequest.class)
public class StatusBarModel {

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private PackageInfoService packageInfoService;

    @OSGiService
    private RequestAdapter requestAdapter;

    private String group;
    private String version;
    private String size;
    private String lastBuilt;
    private String lastInstalled;
    private String lastReplicated;

    @PostConstruct
    private void init() {
        ValidatorResponse<PackageInfoModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), PackageInfoModel.class);
        if (validatorResponse.isValid()) {
            PackageInfo packageInfo = packageInfoService.getPackageInfo(request.getResourceResolver(), validatorResponse.getModel());
            if (packageInfo == null) {
                return;
            }
            group = packageInfo.getGroupName();
            version = packageInfo.getVersion();
            size = getStringOrEmpty(packageInfo.getDataSize());
            lastBuilt = getStringOrEmpty(packageInfo.getPackageBuilt());
            lastInstalled = getStringOrEmpty(packageInfo.getPackageInstalled());
            lastReplicated = getStringOrEmpty(packageInfo.getPackageReplicated());
        }
    }

    private String getStringOrEmpty(Object object) {
        return object == null ? StringUtils.EMPTY : object.toString();
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public String getSize() {
        return size;
    }

    public String getLastBuilt() {
        return lastBuilt;
    }

    public String getLastInstalled() {
        return lastInstalled;
    }

    public String getLastReplicated() {
        return lastReplicated;
    }
}
