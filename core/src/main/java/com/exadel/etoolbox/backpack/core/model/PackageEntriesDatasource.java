package com.exadel.etoolbox.backpack.core.model;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.pckg.v2.BasePackageService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageInfoModel;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.validator.ValidatorResponse;
import com.google.common.collect.ImmutableMap;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Model(adaptables = SlingHttpServletRequest.class)
public class PackageEntriesDatasource {

    @SlingObject
    private SlingHttpServletRequest request;

    @OSGiService
    private PackageInfoService packageInfoService;

    @OSGiService
    private BasePackageService basePackageService;

    @OSGiService
    private RequestAdapter requestAdapter;

    private String resourceType;

    @PostConstruct
    private void initModel() {

        List<Resource> resources = new ArrayList<>();

        ValidatorResponse<PackageInfoModel> validatorResponse = requestAdapter.adaptValidate(request.getParameterMap(), PackageInfoModel.class);

        if (validatorResponse.isValid() && packageInfoService.packageExists(request.getResourceResolver(), validatorResponse.getModel())) {
            PackageInfo packageInfo = packageInfoService.getPackageInfo(request.getResourceResolver(), validatorResponse.getModel());

            resourceType = Optional.ofNullable(request.getResource().getChild("datasource"))
                    .map(Resource::getValueMap)
                    .map(vm -> vm.get("itemResourceType", String.class))
                    .orElse(JcrConstants.NT_UNSTRUCTURED);

            if (packageInfo.getPaths() != null) {
                packageInfo.getPaths().forEach(path -> {
                    List<String> references = packageInfo.getReferencedResources().get(path);
                    if (references != null) {
                        List<Resource> assets = references.stream().map(title -> createPackageEntry("reference", title, ImmutableMap.of("upstream", title))).collect(Collectors.toList());
                        resources.add(createPackageEntry("page", path, ImmutableMap.of("noChildren", true), assets));
                    }
                    resources.add(createPackageEntry("page", path, ImmutableMap.of("noChildren", true)));
                });
            }

            basePackageService.getPackageInfos().asMap().put(packageInfo.getPackagePath(), packageInfo);
        }

        request.setAttribute(DataSource.class.getName(), new SimpleDataSource(resources.iterator()));


//        for (int i = 0; i < 400; i++) {
//            String folder = FOLDERS[RandomUtils.nextInt(0, FOLDERS.length)];
//            String page = PAGES[RandomUtils.nextInt(0, PAGES.length)];
//            String title = "/content/hpe/base/blueprint/" + folder + "/" + page + "-" + i;
//
//            List<Resource> subsidiaries = new ArrayList<>();
//
//            boolean addLiveCopies = RandomUtils.nextBoolean();
//            if (addLiveCopies) {
//                for (String locale : LOCALES) {
//                    String localeTitle = title.replace("base/blueprint", "country/" + locale);
//                    Resource liveCopy = createPackageEntry("livecopy", localeTitle, ImmutableMap.of("upstream", title));
//                    subsidiaries.add(liveCopy);
//                }
//            }
//
//            boolean addAssets = RandomUtils.nextBoolean();
//            if (addAssets) {
//                for (int j = 0; j < RandomUtils.nextInt(0, 3); j++) {
//                    String assetTitle = "/content/dam/hpe/images/public/image" + j + ".png";
//                    Resource asset = createPackageEntry("reference", assetTitle, ImmutableMap.of("upstream", title));
//                    subsidiaries.add(asset);
//                }
//            }
//
//            Resource entry = createPackageEntry(
//                    "page",
//                    title,
//                    ImmutableMap.of("hasChildren", true),
//                    subsidiaries);
//        }


    }

    private Resource createPackageEntry(String type, String title, Map<String, Object> additionalProperties) {
        return createPackageEntry(type, title, additionalProperties, null);
    }

    private Resource createPackageEntry(String type, String title, Map<String, Object> additionalProperties, List<Resource> children) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(JcrConstants.JCR_TITLE, title);
        properties.put("type", type);
        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }
        ValueMap valueMap = new ValueMapDecorator(properties);
        return new ValueMapResource(
                request.getResourceResolver(),
                "/package-entry/" + title.replace("/", "-"),
                resourceType,
                valueMap,
                children);
    }
}
