package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.sling.api.SlingHttpServletRequest;

import java.util.Arrays;

public class ReferencedResourceTypesProcessor extends RequestProcessor {
    public static final String REFERENCED_RESOURCES = "referencedResources";

    public ReferencedResourceTypesProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    void processRequestParameter(final SlingHttpServletRequest request,
                                 final PackageRequestInfo.PackageRequestInfoBuilder builder,
                                 final String[] parameterValues) {
        builder.withReferencedResourceTypes(Arrays.asList(parameterValues));
    }

    @Override
    String getParameterName() {
        return REFERENCED_RESOURCES;
    }
}
