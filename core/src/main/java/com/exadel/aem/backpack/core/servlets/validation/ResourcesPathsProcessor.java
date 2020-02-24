package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
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
    void processRequestParameter(final SlingHttpServletRequest request,
                                 final PackageRequestInfo.PackageRequestInfoBuilder builder,
                                 final String[] parameterValues) {
        List<String> paths = Arrays.asList(parameterValues);
        Optional<String> firstInvalid = paths.stream().filter(s -> request.getResourceResolver().getResource(s) == null).findFirst();
        if (firstInvalid.isPresent()) {
            builder.withInvalidMessage("Path: " + firstInvalid.get() + " is invalid!");
            return;
        }
        builder.withPaths(paths);
    }

    @Override
    String getParameterName() {
        return PATHS;
    }
}
