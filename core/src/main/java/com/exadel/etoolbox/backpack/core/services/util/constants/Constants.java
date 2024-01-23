package com.exadel.etoolbox.backpack.core.services.util.constants;

import com.day.cq.commons.jcr.JcrConstants;

public class Constants {

    private Constants() {}

    public static final String ERROR = "ERROR: ";

    public static final String PACKAGE_METADATA_NODE = "metadata";
    public static final String DEFAULT_PACKAGE_GROUP = "EToolbox_BackPack";

    public static final String JCR_CONTENT_NODE = "/" + JcrConstants.JCR_CONTENT;
    public static final String REFERENCED_RESOURCES = "referencedResources";
    public static final String GENERAL_RESOURCES = "generalResources";
    public static final String PACKAGE_DOES_NOT_EXIST_MESSAGE = "Package by this path %s doesn't exist in the repository.";
    public static final String THUMBNAIL_FILE = "thumbnail.png";
    public static final String DEFAULT_THUMBNAILS_LOCATION = "/apps/etoolbox-backpack/assets/";
    public static final String INITIAL_FILTERS = "initialFilters";
    public static final String THUMBNAIL_PATH_TEMPLATE = DEFAULT_THUMBNAILS_LOCATION + "backpack_%s.png";
    public static final String PACKAGES_ROOT_PATH = "/etc/packages";
    public static final String QUERY_PARAMETER = "queryPackage";
    public static final String SWITCH_PARAMETER = "toggle";
    public static final String THUMBNAIL_PATH_PARAMETER = "thumbnailPath";

}
