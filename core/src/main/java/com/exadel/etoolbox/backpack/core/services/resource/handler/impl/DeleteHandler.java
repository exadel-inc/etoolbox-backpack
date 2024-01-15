package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import com.google.gson.Gson;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = BaseHandler.class)
public class DeleteHandler implements BaseHandler {

    private static final String DELETE = "delete";

    private static final Gson GSON = new Gson();

    @Override
    public void process(ResourceResolver resourceResolver, String payload, PackageInfo packageInfo) {
        PayloadDto payloadDto = GSON.fromJson(payload, PayloadDto.class);
        switch (payloadDto.getType()) {
            case "page":
                packageInfo.deletePath(payloadDto.getEntry());
                break;
            case "reference":
                packageInfo.getPathInfo(payloadDto.getEntry())
                        .getReferences()
                        .get(payloadDto.getType())
                        .remove(payloadDto.getSubsidiary());
                break;
            case "livecopy":
                packageInfo.getPathInfo(payloadDto.getEntry())
                        .getLiveCopies()
                        .remove(payloadDto.getSubsidiary());
                break;
            //todo children all or one check
            case "child":
                packageInfo.getPathInfo(payloadDto.getEntry())
                        .getChildren()
                        .remove(payloadDto.getSubsidiary());
                break;
            case "children":
                packageInfo.getPathInfo(payloadDto.getEntry())
                        .getChildren()
                        .clear();
                break;
            default:
                break;
        }
    }

    @Override
    public String getType() {
        return DELETE;
    }


    private static final class PayloadDto {

        private String entry;
        private String type;
        private String subsidiary;

        public String getEntry() {
            return entry;
        }

        public String getType() {
            return type;
        }

        public String getSubsidiary() {
            return subsidiary;
        }
    }
}
