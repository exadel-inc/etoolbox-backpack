package com.exadel.aem.backpack.core.dto.response;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class PackageInfo {

	private String packageName;

	private String groupName;

	private String version;

	private boolean packageCreated;

	private Calendar packageBuilt;

	private String packagePath;

	private Collection<String> paths;

	private Map<String, List<String>> referencedResources;

	private List<String> buildLog;

	private volatile int latestLogIndex;

	private TestBuildInfo testBuildInfo;

	public String getPackageName() {
		return packageName;
	}

	public String getGroupName() {
		return groupName;
	}

	public String getVersion() {
		return version;
	}

	public boolean isPackageCreated() {
		return packageCreated;
	}

	public Collection<String> getPaths() {
		return Collections.unmodifiableCollection(paths);
	}

	public Map<String, List<String>> getReferencedResources() {
		return Collections.unmodifiableMap(referencedResources);
	}

	public TestBuildInfo getTestBuildInfo() {
		return testBuildInfo;
	}

	public void setTestBuildInfo(TestBuildInfo testBuildInfo) {
		this.testBuildInfo = testBuildInfo;
	}

	public List<String> getBuildLog() {
		return Collections.unmodifiableList(buildLog);
	}

	public Calendar getPackageBuilt() {
		return packageBuilt;
	}

	public List<String> getLatestBuildInfo() {
		int currentBuildLogSize = buildLog.size();
		List<String> latestLog = Collections.emptyList();
		if (currentBuildLogSize > 0) {
			latestLog = new ArrayList(buildLog.subList(latestLogIndex, currentBuildLogSize));
			latestLogIndex = currentBuildLogSize - 1;
		}

		return Collections.unmodifiableList(latestLog);
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

	public void setPackageCreated(final boolean packageCreated) {
		this.packageCreated = packageCreated;
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

	public void addLogMessage(final String message) {
		if (buildLog != null && StringUtils.isNotBlank(message)) {
			buildLog.add(message);
		}
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
		private Map<String, List<String>> referencedResources;
		private List<String> buildLog;

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

		public PackageInfo build() {
			PackageInfo packageInfo = new PackageInfo();
			packageInfo.paths = this.paths;
			packageInfo.referencedResources = this.referencedResources;
			packageInfo.groupName = this.groupName;
			packageInfo.packageName = this.packageName;
			packageInfo.version = this.version;
			packageInfo.buildLog = new ArrayList<>();
			return packageInfo;
		}
	}
}
