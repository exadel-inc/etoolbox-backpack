package com.exadel.aem.backpack.core.servlets.validation;

import com.exadel.aem.backpack.core.servlets.dto.PackageRequestInfo;
import org.apache.sling.api.SlingHttpServletRequest;

public class TestBuildProcessor extends RequestProcessor {
    private static final String TEST_BUILD = "testBuild";

    public TestBuildProcessor(RequestProcessor nextProcessor, boolean mandatory) {
        super(nextProcessor);
        this.mandatory = mandatory;
    }

    @Override
    void processRequestParameter(final SlingHttpServletRequest request,
                                 final PackageRequestInfo.PackageRequestInfoBuilder builder,
                                 final String[] parameterValues) {
        builder.withTestBuild(Boolean.parseBoolean(parameterValues[0]));
    }

    @Override
    String getParameterName() {
        return TEST_BUILD;
    }
}
