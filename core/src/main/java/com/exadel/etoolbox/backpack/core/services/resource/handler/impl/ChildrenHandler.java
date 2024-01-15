package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = BaseHandler.class)
public class ChildrenHandler implements BaseHandler {

    private static final String CHILDREN = "children";
    private static final String JCR_CONTENT_NODE = "/" + JcrConstants.JCR_CONTENT;

    @Override
    public void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {

        Resource resource = resourceResolver.getResource(payload);

        if (resource != null && resource.getChild(JcrConstants.JCR_CONTENT) != null) {
            String path = resource.getPath() + JCR_CONTENT_NODE;
            packageInfo.getPathInfo(resource.getPath()).getChildren().add(path);
        } else {
            packageInfo.setPackageStatus(PackageStatus.ERROR);
            packageInfo.addLogMessage("Resource not found: " + payload);
        }
    }

    @Override
    public String getType() {
        return CHILDREN;
    }
}
