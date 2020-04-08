package com.exadel.aem.backpack.core.servlets.model;

import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;
import com.exadel.aem.request.annotations.Validate;
import com.exadel.aem.request.validator.impl.RequiredValidator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RequestMapping
public class PackageInfoModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageInfoModel.class);


    @RequestParam(name = "path")
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Path field is required")
    private String packagePath;

    public String getPackagePath() {
        return packagePath;
    }

    public void setPackagePath(final String packagePath) {
        this.packagePath = packagePath;
    }

    @PostConstruct
    public void init() {
        if (StringUtils.isNotBlank(packagePath)) {
            try {
                packagePath = URLDecoder.decode(packagePath, StandardCharsets.UTF_8.displayName());
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("Path decode exception", e);
            }
        }
    }
}
