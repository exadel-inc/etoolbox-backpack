package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

public class ExcludeChildrenProcessor extends RequestProcessor {
    private static final String EXCLUDE_CHILDREN = "excludeChildren";

    public ExcludeChildrenProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    public PackageRequestInfo process(final SlingHttpServletRequest request,
                                      final PackageRequestInfo.PackageRequestInfoBuilder builder) {
        String parameter = request.getParameter(EXCLUDE_CHILDREN);

        if (StringUtils.isNotBlank(parameter)) {
            builder.withExcludeChildren(Boolean.parseBoolean(parameter));
        } else if (mandatory) {
            builder.withInvalidMessage(EXCLUDE_CHILDREN + " is mandatory field!");
            return builder.build();
        }
        if (nextProcessor != null) {
            return nextProcessor.process(request, builder);
        }

        return builder.build();
    }
}
