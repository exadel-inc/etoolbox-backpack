package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

public class GroupProcessor extends RequestProcessor {
    private static final String PACKAGE_GROUP = "packageGroup";

    public GroupProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    public PackageRequestInfo process(final SlingHttpServletRequest request,
                                      final PackageRequestInfo.PackageRequestInfoBuilder builder) {
        String parameter = request.getParameter(PACKAGE_GROUP);

        if (StringUtils.isNotBlank(parameter)) {
            builder.withPackageName(parameter);
        } else if(mandatory){
            builder.withInvalidMessage(PACKAGE_GROUP + " is mandatory field!");
            return builder.build();
        }
        if (nextProcessor != null) {
            return nextProcessor.process(request, builder);
        }
        return builder.build();
    }
}
