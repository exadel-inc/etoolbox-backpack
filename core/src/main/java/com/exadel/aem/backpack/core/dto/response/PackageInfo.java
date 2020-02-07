package com.exadel.aem.backpack.core.dto.response;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

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

	private Map<String, List<String>> referencedResources;

	private List<String> buildLog;

	private Long dataSize;

	public PackageInfo() {
	}

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
		this.buildLog = packageInfo.buildLog;
		this.dataSize = packageInfo.dataSize;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getGroupName() {
		return groupName;
	}

	public String getVersion() {
		return version;
	}

	public Collection<String> getPaths() {
		return Collections.unmodifiableCollection(paths);
	}

	public Map<String, List<String>> getReferencedResources() {
		return Collections.unmodifiableMap(referencedResources);
	}

	public List<String> getBuildLog() {
		return Collections.unmodifiableList(buildLog);
	}

	public void setBuildLog(final List<String> buildLog) {
		this.buildLog = buildLog;
	}

	public Calendar getPackageBuilt() {
		return packageBuilt;
	}


	public List<String> getLatestBuildInfo(int latestLogIndex) {
		int currentBuildLogSize = buildLog.size();

		List<String> latestLog = Collections.emptyList();
		if (currentBuildLogSize > 0) {
			latestLog = new ArrayList(buildLog.subList(latestLogIndex, currentBuildLogSize));
		}

		return Collections.unmodifiableList(latestLog);
	}

	public void clearLog() {
		buildLog.clear();
	}

	public void addAssetReferencedItem(final AssetReferencedItem item) {
		if (referencedResources == null) {
			referencedResources = new TreeMap<>();
		}
		if (StringUtils.isNotBlank(item.getPath()) && StringUtils.isNotBlank(item.getMimeType())) {
			List<String> assetsPaths = referencedResources.get(item.getMimeType());
			if (assetsPaths == null) {
				assetsPaths = new ArrayList<>();
				referencedResources.put(item.getMimeType(), assetsPaths);
			}
			assetsPaths.add(item.getPath());
		}
	}

	public PackageStatus getPackageStatus() {
		return packageStatus;
	}

	public void setPackageStatus(final PackageStatus packageStatus) {
		this.packageStatus = packageStatus;
	}

	public void setPackageBuilt(final Calendar packageBuilt) {
		this.packageBuilt = packageBuilt;
	}

	public void setPackagePath(final String packagePath) {
		this.packagePath = packagePath;
	}

	public String getPackagePath() {
		return packagePath;
	}

	public void setPackageNodeName(final String packageNodeName) {
		this.packageNodeName = packageNodeName;
	}

	public String getThumbnailPath() {
		return thumbnailPath;
	}

	public void addLogMessage(final String message) {
		if (buildLog != null && StringUtils.isNotBlank(message)) {
			buildLog.add(message);
		}
	}

	public Long getDataSize() {
		return dataSize;
	}

	public void setDataSize(final Long dataSize) {
		this.dataSize = dataSize;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PackageInfo that = (PackageInfo) o;
		return Objects.equals(packageName, that.packageName) &&
				Objects.equals(groupName, that.groupName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(packageName, groupName);
	}


	public static final class BuildPackageInfoBuilder {
		private String packageName;
		private String groupName;
		private String version;
		private Collection<String> paths;
		private Map<String, List<String>> referencedResources = new TreeMap<>();
		private String thumbnailPath;

		private BuildPackageInfoBuilder() {
		}

		public static BuildPackageInfoBuilder aBuildPackageInfo() {
			return new BuildPackageInfoBuilder();
		}

		public BuildPackageInfoBuilder withPackageName(String packageName) {
			this.packageName = packageName;
			return this;
		}

		public BuildPackageInfoBuilder withGroupName(String groupName) {
			this.groupName = groupName;
			return this;
		}

		public BuildPackageInfoBuilder withVersion(String version) {
			this.version = version;
			return this;
		}


		public BuildPackageInfoBuilder withPaths(Collection<String> paths) {
			this.paths = paths;
			return this;
		}

		public BuildPackageInfoBuilder withReferencedResources(Map<String, List<String>> referencedResources) {
			this.referencedResources = referencedResources;
			return this;
		}

		public BuildPackageInfoBuilder withThumbnailPath(String thumbnailPath) {
			this.thumbnailPath = thumbnailPath;
			return this;
		}

		public PackageInfo build() {
			PackageInfo packageInfo = new PackageInfo();
			packageInfo.paths = this.paths;
			packageInfo.referencedResources = this.referencedResources;
			packageInfo.groupName = this.groupName;
			packageInfo.packageName = this.packageName;
			packageInfo.version = this.version;
			packageInfo.buildLog = new ArrayList<>();
			packageInfo.thumbnailPath = this.thumbnailPath;
			return packageInfo;
		}
	}
}
