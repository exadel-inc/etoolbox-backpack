package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.sling.api.SlingHttpServletRequest;

public class NameProcessor extends RequestProcessor {
    private static final String PACKAGE_NAME = "packageName";

    public NameProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    void processRequestParameter(final SlingHttpServletRequest request,
                                 final PackageRequestInfo.PackageRequestInfoBuilder builder,
                                 final String[] parameterValues) {
        builder.withPackageName(parameterValues[0]);
    }

    @Override
    String getParameterName() {
        return PACKAGE_NAME;
    }
}
