package com.exadel.aem.backpack.core.dto;

import java.util.*;

public class BuildPackageInfo {

	private String packageName;

	private String groupName;

	private boolean packageCreated;

	private Collection<String> paths;

	private Map<String, List<String>> referencedResources;

	private List<String> buildLog;

	public String getPackageName() {
		return packageName;
	}

	public String getGroupName() {
		return groupName;
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

	public List<String> getBuildLog() {
		return Collections.unmodifiableList(buildLog);
	}

	public List<String> getLatestBuildInfo() {
		//todo implement
		return Collections.EMPTY_LIST;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BuildPackageInfo that = (BuildPackageInfo) o;
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
		private boolean packageCreated;
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

		public BuildPackageInfoBuilder withPackageCreated(boolean packageCreated) {
			this.packageCreated = packageCreated;
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

		public BuildPackageInfoBuilder withBuildLog(List<String> buildLog) {
			this.buildLog = buildLog;
			return this;
		}

		public BuildPackageInfo build() {
			BuildPackageInfo buildPackageInfo = new BuildPackageInfo();
			buildPackageInfo.paths = this.paths;
			buildPackageInfo.referencedResources = this.referencedResources;
			buildPackageInfo.groupName = this.groupName;
			buildPackageInfo.packageCreated = this.packageCreated;
			buildPackageInfo.packageName = this.packageName;
			buildPackageInfo.buildLog = this.buildLog;
			return buildPackageInfo;
		}
	}
}
