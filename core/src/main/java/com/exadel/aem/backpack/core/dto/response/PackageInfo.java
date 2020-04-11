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

package com.exadel.aem.backpack.core.dto.response;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import org.apache.commons.lang3.StringUtils;

/**
 * Data model containing comprehensive data required to manage a package
 * via multiple {@link com.exadel.aem.backpack.core.services.PackageService} operations and prepare response to the frontend
 * @see com.exadel.aem.backpack.core.services.PackageService
 * @see com.exadel.aem.backpack.core.servlets.CreatePackageServlet
 * @see com.exadel.aem.backpack.core.servlets.BuildPackageServlet
 */
public class PackageInfo {

    private String packageName;

    private String packageNodeName;

    private String groupName;

    private String version;

    private Calendar packageBuilt;

    private PackageStatus packageStatus;

    private String packagePath;

    private String thumbnailPath;

    private Collection<String> paths;

    private Map<String, List<String>> referencedResources = new TreeMap<>();

    private List<String> log = new ArrayList<>();

    private Long dataSize;

    /**
     * Default constructor
     */
    public PackageInfo() {
    }

    /**
     * Cloning constructor
     * @param packageInfo The {@code PachakeInfo} object to make a clone
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
        this.referencedResources = new HashMap<>(packageInfo.getReferencedResources());
        this.log = packageInfo.log;
        this.dataSize = packageInfo.dataSize;
    }

    /**
     * Gets the name of the current package
     * @return String value
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Gets the group name of the current package
     * @return String value
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Gets the version of the current package
     * @return String value
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the collection of paths representing resources to be included in the current package
     * @return {@code Collection<String>} object, read-only
     */
    public Collection<String> getPaths() {
        return Collections.unmodifiableCollection(paths);
    }

    /**
     * Gets the collection of paths representing assets to be embedded in the current package, grouped by their MIME types
     * @return {@code Collection<String>} object, read-only
     */
    public Map<String, List<String>> getReferencedResources() {
        return Collections.unmodifiableMap(referencedResources);
    }

    /**
     * Gets the collection of log entries for the current package
     * @return {@code List<String>} object, read-only
     */
    public List<String> getLog() {
        return Collections.unmodifiableList(log);
    }

    /**
     * Gets the collection of log entries for the current package
     * @param log {@code List<String>} object
     */
    public void setLog(final List<String> log) {
        this.log = log;
    }

    /**
     * Gets the value representing when the package was built
     * @return {@code Calendar} instance
     */
    public Calendar getPackageBuilt() {
        return packageBuilt;
    }

    /**
     * Gets the collection of log entries for the current package starting from the specified position
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
     * Appends path to a referenced asset to the current {@link PackageInfo}
     * @param item {@link AssetReferencedItem} object containing asset's MIME type and path
     */
    public void addAssetReferencedItem(final AssetReferencedItem item) {
        if (referencedResources == null) {
            referencedResources = new TreeMap<>();
        }
        if (StringUtils.isNotBlank(item.getPath()) && StringUtils.isNotBlank(item.getMimeType())) {
            List<String> assetsPaths = referencedResources.computeIfAbsent(item.getMimeType(), k -> new ArrayList<>());
            assetsPaths.add(item.getPath());
        }
    }

    /**
     * Gets build status of the current package
     * @return {@link PackageStatus} value
     */
    public PackageStatus getPackageStatus() {
        return packageStatus;
    }

    /**
     * Sets build status of the current package
     * @param packageStatus {@link PackageStatus} value
     */
    public void setPackageStatus(final PackageStatus packageStatus) {
        this.packageStatus = packageStatus;
    }

    /**
     * Sets the value representing when the package was built
     * @param packageBuilt {@code Calendar} instance
     */
    public void setPackageBuilt(final Calendar packageBuilt) {
        this.packageBuilt = packageBuilt;
    }

    /**
     * Sets the path of this package as a JCR storage item
     * @param packagePath String value
     */
    public void setPackagePath(final String packagePath) {
        this.packagePath = packagePath;
    }

    /**
     * Gets the path of this package as a JCR storage item
     * @return String value
     */
    public String getPackagePath() {
        return packagePath;
    }

    /**
     * Sets the {@code Node} name of this package as a JCR storage item
     * @param packageNodeName String value
     */
    public void setPackageNodeName(final String packageNodeName) {
        this.packageNodeName = packageNodeName;
    }

    /**
     * Gets the {@code Node} name of this package as a JCR storage item
     * @return String value
     */
    public String getPackageNodeName() {
        return packageNodeName;
    }

    /**
     * Sets the name of the current package
     * @param packageName String value
     */
    public void setPackageName(final String packageName) {
        this.packageName = packageName;
    }

    /**
     * Sets the group name of the current package
     * @param groupName String value
     */
    public void setGroupName(final String groupName) {
        this.groupName = groupName;
    }

    /**
     * Sets the version of the current package
     * @param version String value
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Sets path to the thumbnail of the current package
     * @param thumbnailPath String value
     */
    public void setThumbnailPath(final String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    /**
     * Sets the collection of paths representing resources to be included in the current package
     * @param paths {@code Collection<String>} object
     */
    public void setPaths(final Collection<String> paths) {
        this.paths = paths;
    }

    /**
     * Sets the collection of paths representing assets to be embedded in the current package, grouped by their MIME types
     * @param referencedResources {@code Collection<String>} object
     */
    public void setReferencedResources(final Map<String, List<String>> referencedResources) {
        this.referencedResources = referencedResources;
    }

    /**
     * Gets the path to the thumbnail of the current package
     * @return String value
     */
    public String getThumbnailPath() {
        return thumbnailPath;
    }

    /**
     * Appends a {@code String} message to this package's building log
     * @param message String value, non-blank
     */
    public void addLogMessage(final String message) {
        if (log != null && StringUtils.isNotBlank(message)) {
            log.add(message);
        }
    }

    /**
     * Gets the computed size of the current package
     * @return Long value
     */
    public Long getDataSize() {
        return dataSize;
    }

    /**
     * Sets the computed size of the current package
     * @param dataSize Long value
     */
    public void setDataSize(final Long dataSize) {
        this.dataSize = dataSize;
    }

    /**
     * Overrides the standard {@code equals()} routine to implement packages comparison by their name and group name
     * requisites
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
     * @return Integer value
     */
    @Override
    public int hashCode() {
        return Objects.hash(packageName, groupName);
    }
}
