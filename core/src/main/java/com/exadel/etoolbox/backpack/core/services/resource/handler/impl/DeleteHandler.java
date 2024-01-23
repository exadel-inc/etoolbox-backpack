package com.exadel.etoolbox.backpack.core.services.resource.handler.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.resource.handler.BaseHandler;
import com.exadel.etoolbox.backpack.core.services.resource.handler.dto.PayloadDto;
import com.google.gson.Gson;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = BaseHandler.class)
public class DeleteHandler implements BaseHandler {

    private static final String ACTION_DELETE = "delete";

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
                        .remove(payloadDto.getSubsidiary());
                break;
            case "livecopy":
                packageInfo.getPathInfo(payloadDto.getEntry())
                        .getLiveCopies()
                        .remove(payloadDto.getSubsidiary());
                break;
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
    public String bindActionType() {
        return ACTION_DELETE;
    }
}
