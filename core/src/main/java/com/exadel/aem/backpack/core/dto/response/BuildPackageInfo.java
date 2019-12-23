package com.exadel.aem.backpack.core.dto.response;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class BuildPackageInfo {

	private String packageName;

	private String groupName;

	private boolean packageCreated;

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

	public List<String> getLatestBuildInfo() {
		int currentBuildLogSize = buildLog.size();
		List<String> latestLog = Collections.emptyList();
		if (currentBuildLogSize > 0) {
			latestLog= new ArrayList(buildLog.subList(latestLogIndex, currentBuildLogSize));
			latestLogIndex = currentBuildLogSize - 1;
		}

		return Collections.unmodifiableList(latestLog);
	}

	public void setPackageCreated(final boolean packageCreated) {
		this.packageCreated = packageCreated;
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


		public BuildPackageInfoBuilder withPaths(Collection<String> paths) {
			this.paths = paths;
			return this;
		}

		public BuildPackageInfoBuilder withReferencedResources(Map<String, List<String>> referencedResources) {
			this.referencedResources = referencedResources;
			return this;
		}

		public BuildPackageInfo build() {
			BuildPackageInfo buildPackageInfo = new BuildPackageInfo();
			buildPackageInfo.paths = this.paths;
			buildPackageInfo.referencedResources = this.referencedResources;
			buildPackageInfo.groupName = this.groupName;
			buildPackageInfo.packageName = this.packageName;
			buildPackageInfo.buildLog = new ArrayList<>();
			return buildPackageInfo;
		}
	}
}
