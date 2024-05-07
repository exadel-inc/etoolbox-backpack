package com.exadel.etoolbox.backpack.core.model;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
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

    private String name;
    private String group;
    private String version;
    private String size;
    private String lastBuilt;
    private String lastInstalled;
    private String lastReplicated;
    private String lastModifiedBy;

    @PostConstruct
    private void init() {
        ValidatorResponse<PackageInfoModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), PackageInfoModel.class);
        if (validatorResponse.isValid()) {
            PackageInfo packageInfo = packageInfoService.getPackageInfo(request.getResourceResolver(), validatorResponse.getModel().getPackagePath());
            if (packageInfo == null || packageInfo.getPackagePath() == null) {
                return;
            }
            name = packageInfo.getPackageName();
            group = packageInfo.getGroupName();
            version = packageInfo.getVersion();
            size = convertBytesToMegabytes(packageInfo.getDataSize());
            lastBuilt = packageInfo.getPackageBuilt() == null ? StringUtils.EMPTY : packageInfo.getPackageBuilt().getTime().toString();
            lastInstalled = packageInfo.getPackageInstalled() == null ? StringUtils.EMPTY : packageInfo.getPackageInstalled().getTime().toString();
            lastReplicated = packageInfo.getPackageReplicated() == null ? StringUtils.EMPTY : packageInfo.getPackageReplicated().getTime().toString();
            lastModifiedBy = packageInfo.getLastModifiedBy();
        }
    }

    public String getName() {
        return name;
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

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    private String convertBytesToMegabytes(long bytes) {
        return String.format("%.3f Mb", ((double) bytes) / 1024 / 1024);
    }
}
