/*
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.etoolbox.backpack.core.dto.response;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Data model containing comprehensive data required to manage a package.
 */
public class PackageInfo {

    private String packageName;

    private String packageNodeName;

    private String groupName;

    private String version;

    private Calendar packageBuilt;

    private String packagePath;

    private String thumbnailPath;

    private Map<String, PathInfo> pathInfoMap = new HashMap<>();

    private PackageStatus packageStatus;

    private List<String> log = new ArrayList<>();

    private Long dataSize;

    private Calendar packageInstalled;

    private Calendar packageReplicated;

    private String lastModifiedBy;

    /**
     * Default constructor
     */
    public PackageInfo() {
    }

    /**
     * Cloning constructor
     *
     * @param packageInfo The {@code PackageInfo} object to make a clone
     */
    public PackageInfo(final PackageInfo packageInfo) {
        this.packageName = packageInfo.packageName;
        this.packageNodeName = packageInfo.packageNodeName;
        this.groupName = packageInfo.groupName;
        this.version = packageInfo.version;
        if (packageInfo.packageBuilt != null) {
            this.packageBuilt = Calendar.getInstance();
            this.packageBuilt.setTime(packageInfo.packageBuilt.getTime());
        }
        this.packageStatus = packageInfo.packageStatus;
        this.packagePath = packageInfo.getPackagePath();
        this.thumbnailPath = packageInfo.thumbnailPath;
        this.pathInfoMap = new HashMap<>(packageInfo.getPathInfoMap());
        this.log = packageInfo.log;
        this.dataSize = packageInfo.dataSize;
        if (packageInfo.packageInstalled != null) {
            this.packageInstalled = Calendar.getInstance();
            this.packageInstalled.setTime(packageInfo.packageInstalled.getTime());
        }

        if (packageInfo.packageReplicated != null) {
            this.packageReplicated = Calendar.getInstance();
            this.packageReplicated.setTime(packageInfo.packageReplicated.getTime());
        }
        this.lastModifiedBy = packageInfo.lastModifiedBy;
    }

    /**
     * Gets the name of the current package
     *
     * @return String value
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Gets the group name of the current package
     *
     * @return String value
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Gets the version of the current package
     *
     * @return String value
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the collection of paths representing resources to be included in the current package
     *
     * @return {@code Collection<String>} object, read-only
     */
    public Collection<String> getPaths() {
        return Collections.unmodifiableCollection(pathInfoMap != null ? pathInfoMap.keySet() : Collections.emptyList());
    }

    public Collection<String> getReferences() {
        return Collections.unmodifiableCollection(pathInfoMap != null
                ? pathInfoMap.values().stream()
                .flatMap(pathInfo -> Stream.of(pathInfo.getAssets(), pathInfo.getChildren(), pathInfo.getLiveCopies(), pathInfo.getPages(), pathInfo.getTags()))
                .flatMap(Set::stream)
                .collect(Collectors.toSet())
                : Collections.emptyList());
    }

    /**
     * Gets the collection of log entries for the current package
     *
     * @return {@code List<String>} object, read-only
     */
    public List<String> getLog() {
        return Collections.unmodifiableList(log);
    }

    /**
     * Gets the collection of log entries for the current package
     *
     * @param log {@code List<String>} object
     */
    public void setLog(final List<String> log) {
        this.log = log;
    }

    /**
     * Gets the value representing when the package was built
     *
     * @return {@code Calendar} instance
     */
    public Calendar getPackageBuilt() {
        return packageBuilt;
    }

    /**
     * Gets the collection of log entries for the current package starting from the specified position
     *
     * @param latestLogIndex Position to start log output from
     * @return {@code List<String>} object, read-only
     */
    public List<String> getLatestBuildInfo(int latestLogIndex) {
        int currentBuildLogSize = log.size();

        List<String> latestLog = Collections.emptyList();
        if (currentBuildLogSize > 0) {
            latestLog = new ArrayList<>(log.subList(latestLogIndex, currentBuildLogSize));
        }

        return Collections.unmodifiableList(latestLog);
    }

    /**
     * Clears the log entries for the current package
     */
    public void clearLog() {
        log.clear();
    }

    /**
     * Gets build status of the current package
     *
     * @return {@link PackageStatus} value
     */
    public PackageStatus getPackageStatus() {
        return packageStatus;
    }

    /**
     * Sets build status of the current package
     *
     * @param packageStatus {@link PackageStatus} value
     */
    public void setPackageStatus(final PackageStatus packageStatus) {
        this.packageStatus = packageStatus;
    }

    /**
     * Sets the value representing when the package was built
     *
     * @param packageBuilt {@code Calendar} instance
     */
    public void setPackageBuilt(final Calendar packageBuilt) {
        this.packageBuilt = packageBuilt;
    }

    /**
     * Sets the path of this package as a JCR storage item
     *
     * @param packagePath String value
     */
    public void setPackagePath(final String packagePath) {
        this.packagePath = packagePath;
    }

    /**
     * Gets the path of this package as a JCR storage item
     *
     * @return String value
     */
    public String getPackagePath() {
        return packagePath;
    }

    /**
     * Sets the {@code Node} name of this package as a JCR storage item
     *
     * @param packageNodeName String value
     */
    public void setPackageNodeName(final String packageNodeName) {
        this.packageNodeName = packageNodeName;
    }

    /**
     * Gets the {@code Node} name of this package as a JCR storage item
     *
     * @return String value
     */
    public String getPackageNodeName() {
        return packageNodeName;
    }

    /**
     * Sets the name of the current package
     *
     * @param packageName String value
     */
    public void setPackageName(final String packageName) {
        this.packageName = packageName;
    }

    /**
     * Sets the group name of the current package
     *
     * @param groupName String value
     */
    public void setGroupName(final String groupName) {
        this.groupName = groupName;
    }

    /**
     * Sets the version of the current package
     *
     * @param version String value
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Sets path to the thumbnail of the current package
     *
     * @param thumbnailPath String value
     */
    public void setThumbnailPath(final String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    /**
     * Sets the collection of paths representing resources to be included in the current package
     *
     * @param paths {@code Collection<String>} object
     */
    public void setPaths(final Collection<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        for (String path : paths) {
            if (this.pathInfoMap.containsKey(path)) {
                continue;
            }
            this.pathInfoMap.put(path, new PathInfo());
        }
    }

    /**
     * Gets the path to the thumbnail of the current package
     *
     * @return String value
     */
    public String getThumbnailPath() {
        return thumbnailPath;
    }

    /**
     * Appends a {@code String} message to this package's building log
     *
     * @param message String value, non-blank
     */
    public void addLogMessage(final String message) {
        if (log != null) {
            log.add(message);
        }
    }

    /**
     * Gets the computed size of the current package
     *
     * @return Long value
     */
    public Long getDataSize() {
        return dataSize;
    }

    /**
     * Sets the computed size of the current package
     *
     * @param dataSize Long value
     */
    public void setDataSize(final Long dataSize) {
        this.dataSize = dataSize;
    }

    /**
     * Gets the value representing when the package was installed
     *
     * @return {@code Calendar} instance
     */
    public Calendar getPackageInstalled() {
        return packageInstalled;
    }

    /**
     * Sets the value representing when the package was installed
     *
     * @param packageInstalled {@code Calendar} instance
     */
    public void setPackageInstalled(Calendar packageInstalled) {
        this.packageInstalled = packageInstalled;
    }

    /**
     * Gets the value representing when the package was replicated
     *
     * @return {@code Calendar} instance
     */
    public Calendar getPackageReplicated() {
        return packageReplicated;
    }

    /**
     * Sets the value representing when the package was replicated
     *
     * @param packageReplicated {@code Calendar} instance
     */
    public void setPackageReplicated(Calendar packageReplicated) {
        this.packageReplicated = packageReplicated;
    }

    /**
     * Overrides the standard {@code equals()} routine to implement packages comparison by their name and group name
     * requisites
     *
     * @param o Object to test for equality with the current object
     * @return True or false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackageInfo that = (PackageInfo) o;
        return Objects.equals(packageName, that.packageName) &&
                Objects.equals(groupName, that.groupName);
    }

    /**
     * Overrides the standard {@code hashCode()} routine to accompany {@link PackageInfo#equals(Object)}
     *
     * @return Integer value
     */
    @Override
    public int hashCode() {
        return Objects.hash(packageName, groupName);
    }

    public PathInfo getPathInfo(String path) {
        return pathInfoMap.computeIfAbsent(path, k -> new PathInfo());
    }

    public void deletePath(String path) {
        pathInfoMap.remove(path);
    }

    public Map<String, PathInfo> getPathInfoMap() {
        return pathInfoMap;
    }

    public void setPathInfoMap(Map<String, PathInfo> pathInfoMap) {
        this.pathInfoMap = pathInfoMap;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
}
