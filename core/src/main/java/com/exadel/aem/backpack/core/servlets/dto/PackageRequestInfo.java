package com.exadel.aem.backpack.core.servlets.dto;

import java.util.List;

public class PackageRequestInfo {
    private List<String> paths;
    private String packageName;
    private String packageGroup;
    private String version;
    private boolean excludeChildren;
    private String packagePath;
    private List<String> referencedResourceTypes;
    private boolean invalid;
    private boolean testBuild;
    private String log;
    private int latestLogIndex;

    public List<String> getPaths() {
        return paths;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getPackageGroup() {
        return packageGroup;
    }

    public String getVersion() {
        return version;
    }

    public boolean isExcludeChildren() {
        return excludeChildren;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public List<String> getReferencedResourceTypes() {
        return referencedResourceTypes;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public String getLog() {
        return log;
    }

    public boolean isTestBuild() {
        return testBuild;
    }

    public int getLatestLogIndex() {
        return latestLogIndex;
    }

    public static final class PackageRequestInfoBuilder {
        private List<String> paths;
        private String packageName;
        private String packageGroup;
        private String version;
        private boolean excludeChildren;
        private String packagePath;
        private List<String> referencedResourceTypes;
        private boolean invalid;
        private boolean testBuild;
        private String log;
        private int latestLogIndex;

        private PackageRequestInfoBuilder() {
        }

        public static PackageRequestInfoBuilder aPackageRequestInfo() {
            return new PackageRequestInfoBuilder();
        }

        public PackageRequestInfoBuilder withPaths(final List<String> paths) {
            this.paths = paths;
            return this;
        }

        public PackageRequestInfoBuilder withPackageName(final String packageName) {
            this.packageName = packageName;
            return this;
        }

        public PackageRequestInfoBuilder withPackageGroup(final String packageGroup) {
            this.packageGroup = packageGroup;
            return this;
        }

        public PackageRequestInfoBuilder withVersion(final String version) {
            this.version = version;
            return this;
        }

        public PackageRequestInfoBuilder withExcludeChildren(final boolean excludeChildren) {
            this.excludeChildren = excludeChildren;
            return this;
        }

        public PackageRequestInfoBuilder withPackagePath(final String packagePath) {
            this.packagePath = packagePath;
            return this;
        }

        public PackageRequestInfoBuilder withReferencedResourceTypes(final List<String> referencedResourceTypes) {
            this.referencedResourceTypes = referencedResourceTypes;
            return this;
        }

        public PackageRequestInfoBuilder withInvalidMessage(final String log) {
            this.log = log;
            this.invalid = true;
            return this;
        }

        public PackageRequestInfoBuilder withTestBuild(final boolean testBuild) {
            this.testBuild = testBuild;
            return this;
        }

        public PackageRequestInfoBuilder withLatestLogIndex(final int latestLogIndex) {
            this.latestLogIndex = latestLogIndex;
            return this;
        }

        public PackageRequestInfo build() {
            PackageRequestInfo packageRequestInfo = new PackageRequestInfo();
            packageRequestInfo.excludeChildren = this.excludeChildren;
            packageRequestInfo.invalid = this.invalid;
            packageRequestInfo.referencedResourceTypes = this.referencedResourceTypes;
            packageRequestInfo.log = this.log;
            packageRequestInfo.version = this.version;
            packageRequestInfo.paths = this.paths;
            packageRequestInfo.packageGroup = this.packageGroup;
            packageRequestInfo.packageName = this.packageName;
            packageRequestInfo.packagePath = this.packagePath;
            packageRequestInfo.testBuild = this.testBuild;
            packageRequestInfo.latestLogIndex = this.latestLogIndex;
            return packageRequestInfo;
        }
    }
}
