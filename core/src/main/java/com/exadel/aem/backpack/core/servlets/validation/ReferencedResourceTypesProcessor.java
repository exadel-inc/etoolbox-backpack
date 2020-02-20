package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import java.util.Arrays;

public class ReferencedResourceTypesProcessor extends RequestProcessor {
    public static final String REFERENCED_RESOURCES = "referencedResources";

    public ReferencedResourceTypesProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    public PackageRequestInfo process(final SlingHttpServletRequest request,
                                      final PackageRequestInfo.PackageRequestInfoBuilder builder) {
        String[] parameters = request.getParameterValues(REFERENCED_RESOURCES);

        if (ArrayUtils.isNotEmpty(parameters)) {
            builder.withReferencedResourceTypes(Arrays.asList(parameters));
        } else if (mandatory) {
            builder.withInvalidMessage(REFERENCED_RESOURCES + " is mandatory field!");
            return builder.build();
        }
        if (nextProcessor != null) {
            return nextProcessor.process(request, builder);
        }
        return builder.build();
    }
}
