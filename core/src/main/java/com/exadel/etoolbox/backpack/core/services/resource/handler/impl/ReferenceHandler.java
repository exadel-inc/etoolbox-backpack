package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.exadel.etoolbox.backpack.core.dto.repository.ReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.resource.ReferenceService;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import com.google.gson.Gson;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component(service = BaseHandler.class)
public class ReferenceHandler implements BaseHandler {

    private static final String REFERENCES = "references";
    private static final Gson GSON = new Gson();

    @Reference
    private ReferenceService referenceService;

    @Override
    public void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        ReferencesDto referencesDto = GSON.fromJson(payload, ReferencesDto.class);

        if (referencesDto != null && resourceResolver.getResource(referencesDto.getPath()) != null) {
            Set<ReferencedItem> referencedItemSet = referenceService.getReferences(resourceResolver, referencesDto.getPath());
            Map<String, List<String>> referencedItemMap = referencedItemSet.stream()
                    .collect(Collectors.groupingBy(ReferencedItem::getType, Collectors.mapping(ReferencedItem::getPath, Collectors.toList())));
            packageInfo.getPathInfo(referencesDto.getPath()).getReferences().putAll(referencedItemMap);

        } else {
            packageInfo.setPackageStatus(PackageStatus.ERROR);
            packageInfo.addLogMessage("Resource not found: " + payload);
        }
    }

    @Override
    public String getType() {
        return REFERENCES;
    }

    private static final class ReferencesDto {

        private String path;
        private String type;

        public String getPath() {
            return path;
        }

        public String getType() {
            return type;
        }
    }

}
