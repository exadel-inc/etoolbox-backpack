package com.exadel.etoolbox.backpack.core.services.impl;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveCopy;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.exadel.etoolbox.backpack.core.services.LiveCopyService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RangeIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component(service = LiveCopyService.class)
public class LiveCopyServiceImpl implements LiveCopyService {

    @Reference
    private LiveRelationshipManager liveRelationshipManager;

    @Override
    public List<String> getResourcePathWithLiveCopiesPaths(ResourceResolver resourceResolver, String path, boolean includeLiveCopies) {
        List<String> paths = new ArrayList<>(Arrays.asList(path));
        if (!includeLiveCopies) {
            return paths;
        }
        Resource resource = resourceResolver.getResource(path);
        try {
            RangeIterator relationships = liveRelationshipManager.getLiveRelationships(resource, null, null);
            while (relationships.hasNext()) {
                LiveRelationship relationship = (LiveRelationship) relationships.next();
                LiveCopy liveCopy = relationship.getLiveCopy();
                if (liveCopy == null) {
                    continue;
                }
                String liveCopyPath = liveCopy.getPath() + relationship.getSyncPath();
                if (resourceResolver.getResource(liveCopyPath) != null) {
                    paths.add(liveCopyPath);
                }
            }
        } catch (WCMException e) {
            throw new RuntimeException(e);
        }
        return paths;
    }
}
