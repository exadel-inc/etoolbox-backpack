package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.sling.api.SlingHttpServletRequest;

public class LatestIndexProcessor extends RequestProcessor {
    private static final String LATEST_LOG_INDEX = "latestLogIndex";

    public LatestIndexProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    void processRequestParameter(final SlingHttpServletRequest request,
                                 final PackageRequestInfo.PackageRequestInfoBuilder builder,
                                 final String[] parameterValues) {
        int latestLogIndex = Integer.parseInt(parameterValues[0]);
        if (latestLogIndex < 0) {
            builder.withInvalidMessage(LATEST_LOG_INDEX + " must be positive!");
            return;
        }
        builder.withLatestLogIndex(latestLogIndex);
    }

    @Override
    String getParameterName() {
        return LATEST_LOG_INDEX;
    }
}
