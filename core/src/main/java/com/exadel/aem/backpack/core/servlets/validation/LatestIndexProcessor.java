package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

public class LatestIndexProcessor extends RequestProcessor {
    private static final String LATEST_LOG_INDEX = "latestLogIndex";

    public LatestIndexProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    public PackageRequestInfo process(final SlingHttpServletRequest request,
                                      final PackageRequestInfo.PackageRequestInfoBuilder builder) {
        String parameter = request.getParameter(LATEST_LOG_INDEX);

        if (StringUtils.isNotBlank(parameter)) {
            int latestLogIndex = Integer.parseInt(parameter);
            if (latestLogIndex < 0) {
                builder.withInvalidMessage(LATEST_LOG_INDEX + " must be positive!");
                return builder.build();
            }
            builder.withLatestLogIndex(latestLogIndex);
        } else if (mandatory) {
            builder.withInvalidMessage(LATEST_LOG_INDEX + " is mandatory field!");
            return builder.build();
        }
        if (nextProcessor != null) {
            return nextProcessor.process(request, builder);
        }

        return builder.build();
    }
}
