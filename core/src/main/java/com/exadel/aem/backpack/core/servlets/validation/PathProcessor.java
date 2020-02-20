package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.commons.lang3.StringUtils;
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
    public PackageRequestInfo process(final SlingHttpServletRequest request,
                                      final PackageRequestInfo.PackageRequestInfoBuilder builder) {
        String parameter = request.getParameter(PATH);

        if (StringUtils.isNotBlank(parameter)) {
            try {
                String packagePath = URLDecoder.decode(parameter, StandardCharsets.UTF_8.displayName());
                builder.withPackagePath(packagePath);
            } catch (UnsupportedEncodingException e) {
                builder.withInvalidMessage(e.getMessage());
                return builder.build();
            }
        } else if (mandatory) {
            builder.withInvalidMessage(PATH + " is mandatory field!");
            return builder.build();
        }
        if (nextProcessor != null) {
            return nextProcessor.process(request, builder);
        }
        return builder.build();
    }
}
