package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

public class TestBuildProcessor extends RequestProcessor {
    private static final String TEST_BUILD = "testBuild";

    public TestBuildProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    public PackageRequestInfo process(final SlingHttpServletRequest request,
                                      final PackageRequestInfo.PackageRequestInfoBuilder builder) {
        String parameter = request.getParameter(TEST_BUILD);

        if (StringUtils.isNotBlank(parameter)) {
            builder.withTestBuild(Boolean.parseBoolean(parameter));
        } else if (mandatory) {
            builder.withInvalidMessage(TEST_BUILD + " is mandatory field!");
            return builder.build();
        }
        if (nextProcessor != null) {
            return nextProcessor.process(request, builder);
        }

        return builder.build();
    }
}
