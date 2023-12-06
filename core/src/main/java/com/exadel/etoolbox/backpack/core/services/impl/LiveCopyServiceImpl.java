package com.exadel.etoolbox.backpack.core.services.impl;

import com.day.cq.wcm.msm.api.LiveCopy;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.exadel.etoolbox.backpack.core.dto.response.ResourceRelationships;
import com.exadel.etoolbox.backpack.core.services.LiveCopyService;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RangeIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component(service = LiveCopyService.class)
public class LiveCopyServiceImpl implements LiveCopyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveCopyServiceImpl.class);
    private static final String ITEM_NOT_FOUND_EXP_MESSAGE = "javax.jcr.ItemNotFoundException: No content resource for resource at path ";

    @Reference
    private LiveRelationshipManager liveRelationshipManager;

    /**
     * Called by {@link BasePackageService#getPackageInfo(ResourceResolver, PackageModel)} to adjust paths to resources
     * intended for the package
     *
     * @param path             Resource path
     * @param includeLiveCopies  Flag indicating if this resource's live copies must be included
     * @param resourceResolver Current {@code ResourceResolver} object
     * @return List of paths
     */
    @Override
    public ResourceRelationships getResourceRelationships(ResourceResolver resourceResolver, String path, boolean includeLiveCopies) {
        ResourceRelationships resourceRelationships = new ResourceRelationships(new ArrayList<>(Collections.singletonList(path)), new ArrayList<>());
        if (!includeLiveCopies) {
            return resourceRelationships;
        }
        return resourceRelationships.concat(getLiveCopies(resourceResolver, path, StringUtils.EMPTY));
    }

    private ResourceRelationships getLiveCopies(ResourceResolver resourceResolver, String path, String sourceSyncPath) {
        List<String> validPaths = new ArrayList<>();
        List<String> brokenPaths = new ArrayList<>();
        Resource resource = resourceResolver.getResource(path);
        if (resource == null) {
            brokenPaths.add(path);
            return new ResourceRelationships(validPaths, brokenPaths);
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
                    validPaths.add(liveCopyPath + syncPath);
                }
                ResourceRelationships resourceRelationships = getLiveCopies(resourceResolver, liveCopyPath, syncPath);
                validPaths.addAll(resourceRelationships.getValidPaths());
                brokenPaths.addAll(resourceRelationships.getBrokenPaths());
            }
        } catch (Exception e) {
            if (e.getMessage().contains(ITEM_NOT_FOUND_EXP_MESSAGE)) {
                brokenPaths.add(StringUtils.substringAfter(e.getMessage(), ITEM_NOT_FOUND_EXP_MESSAGE));
            }
            LOGGER.error("Can't get relationships of the resource {}", resource.getPath(), e);
        }
        return new ResourceRelationships(validPaths, brokenPaths);
    }
}
