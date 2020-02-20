package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.sling.api.SlingHttpServletRequest;

public abstract class RequestProcessor {
    protected RequestProcessor nextProcessor;
    protected boolean mandatory;

    public RequestProcessor(final RequestProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    public abstract PackageRequestInfo process(SlingHttpServletRequest request,
                                               PackageRequestInfo.PackageRequestInfoBuilder builder);

}
