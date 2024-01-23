package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveCopy;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RangeIterator;
import java.util.ArrayList;
import java.util.List;

@Component(service = BaseHandler.class)
public class LiveCopyHandler implements BaseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveCopyHandler.class);
    private static final String ACTION_LIVE_COPY = "liveCopies";

    @Reference
    private LiveRelationshipManager liveRelationshipManager;

    @Override
    public void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        if (resourceResolver.getResource(payload) != null) {
            List<String> paths = new ArrayList<>(getLiveCopies(resourceResolver, payload, StringUtils.EMPTY));
            packageInfo.getPathInfo(payload).getLiveCopies().addAll(paths);
        } else {
            packageInfo.setPackageStatus(PackageStatus.ERROR);
            packageInfo.addLogMessage("Resource not found: " + payload);
        }
    }

    private List<String> getLiveCopies(ResourceResolver resourceResolver, String path, String sourceSyncPath) {
        List<String> paths = new ArrayList<>();
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            return paths;
        }
        try {
            RangeIterator relationships = liveRelationshipManager.getLiveRelationships(resource, null, null);
            while (relationships.hasNext()) {
                LiveRelationship relationship = (LiveRelationship) relationships.next();
                LiveCopy liveCopy = relationship.getLiveCopy();
                String syncPath = StringUtils.defaultIfEmpty(relationship.getSyncPath(), sourceSyncPath);
                if (liveCopy == null || (StringUtils.isNotBlank(syncPath) && !liveCopy.isDeep())) {
                    continue;
                }
                String liveCopyPath = liveCopy.getPath();
                if (resourceResolver.getResource(liveCopyPath + syncPath) != null) {
                    paths.add(liveCopyPath + syncPath);
                }
                paths.addAll(getLiveCopies(resourceResolver, liveCopyPath, syncPath));
            }
        } catch (WCMException e) {
            LOGGER.error("Can't get relationships of the resource {}", resource.getPath(), e);
        }
        return paths;
    }

    @Override
    public String bindActionType() {
        return ACTION_LIVE_COPY;
    }
}
