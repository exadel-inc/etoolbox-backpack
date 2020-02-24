package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.sling.api.SlingHttpServletRequest;

public class ExcludeChildrenProcessor extends RequestProcessor {
    private static final String EXCLUDE_CHILDREN = "excludeChildren";

    public ExcludeChildrenProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    void processRequestParameter(final SlingHttpServletRequest request,
                                 final PackageRequestInfo.PackageRequestInfoBuilder builder,
                                 final String[] parameterValues) {
        builder.withExcludeChildren(Boolean.parseBoolean(parameterValues[0]));
    }

    @Override
    String getParameterName() {
        return EXCLUDE_CHILDREN;
    }
}
