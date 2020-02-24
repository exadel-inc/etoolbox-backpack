package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.sling.api.SlingHttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PathProcessor extends RequestProcessor {
    public static final String PATH = "path";

    public PathProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    void processRequestParameter(final SlingHttpServletRequest request,
                                 final PackageRequestInfo.PackageRequestInfoBuilder builder,
                                 final String[] parameterValues) {
        try {
            String packagePath = URLDecoder.decode(parameterValues[0], StandardCharsets.UTF_8.displayName());
            builder.withPackagePath(packagePath);
        } catch (UnsupportedEncodingException e) {
            builder.withInvalidMessage(e.getMessage());
        }
    }

    @Override
    String getParameterName() {
        return PATH;
    }
}
