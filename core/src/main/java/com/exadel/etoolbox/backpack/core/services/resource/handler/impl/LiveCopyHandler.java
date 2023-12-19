package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveCopy;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = BaseHandler.class)
public class LiveCopyHandler implements BaseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveCopyHandler.class);
    private static final String LIVE_COPY = "liveCopies";

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
    public void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        List<String> paths = new ArrayList<>(Collections.singletonList(payload));
        paths.addAll(getLiveCopies(resourceResolver, payload, StringUtils.EMPTY));
        packageInfo.setPaths(Stream.of(paths, packageInfo.getPaths()).flatMap(Collection::stream).collect(Collectors.toSet()));
    }

    @Override
    public String getType() {
        return LIVE_COPY;
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
}
