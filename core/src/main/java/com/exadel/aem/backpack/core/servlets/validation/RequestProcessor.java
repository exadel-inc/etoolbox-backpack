package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;

public abstract class RequestProcessor {
    protected RequestProcessor nextProcessor;
    protected boolean mandatory;

    public RequestProcessor(final RequestProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    public PackageRequestInfo processRequest(SlingHttpServletRequest request,
                                             PackageRequestInfo.PackageRequestInfoBuilder builder) {
        String parameterName = getParameterName();
        String[] parameterValues = request.getParameterValues(parameterName);

        if (ArrayUtils.isNotEmpty(parameterValues)) {
            processRequestParameter(request, builder, parameterValues);
        } else if (mandatory) {
            builder.withInvalidMessage(parameterName + " is mandatory field!");
            return builder.build();
        }
        if (nextProcessor != null) {
            return nextProcessor.processRequest(request, builder);
        }

        return builder.build();
    }

    abstract void processRequestParameter(final SlingHttpServletRequest request,
                                          final PackageRequestInfo.PackageRequestInfoBuilder builder,
                                          final String[] parameterValues);

    abstract String getParameterName();

}
