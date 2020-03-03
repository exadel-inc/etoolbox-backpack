package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.sling.api.SlingHttpServletRequest;

public class ThumbnailProcessor extends RequestProcessor {
    private static final String THUMBNAIL_PATH = "thumbnailPath";

    public ThumbnailProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    void processRequestParameter(final SlingHttpServletRequest request,
                                 final PackageRequestInfo.PackageRequestInfoBuilder builder,
                                 final String[] parameterValues) {
        builder.withThumbnailPath(parameterValues[0]);
    }

    @Override
    String getParameterName() {
        return THUMBNAIL_PATH;
    }
}
