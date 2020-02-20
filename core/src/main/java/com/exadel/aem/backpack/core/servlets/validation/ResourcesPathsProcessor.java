package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ResourcesPathsProcessor extends RequestProcessor {
    public static final String PATHS = "paths";

    public ResourcesPathsProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    public PackageRequestInfo process(final SlingHttpServletRequest request,
                                      final PackageRequestInfo.PackageRequestInfoBuilder builder) {
        String[] parameters = request.getParameterValues(PATHS);

        if (ArrayUtils.isNotEmpty(parameters)) {
            List<String> paths = Arrays.asList(parameters);
            Optional<String> firstInvalid = paths.stream().filter(s -> request.getResourceResolver().getResource(s) == null).findFirst();
            if(firstInvalid.isPresent()){
                builder.withInvalidMessage("Path: " + firstInvalid.get() + " is invalid!");
                return builder.build();
            }
            builder.withPaths(paths);
        } else if (mandatory) {
            builder.withInvalidMessage(PATHS + " is mandatory field!");
            return builder.build();
        }
        if (nextProcessor != null) {
            return nextProcessor.process(request, builder);
        }
        return builder.build();
    }
}
