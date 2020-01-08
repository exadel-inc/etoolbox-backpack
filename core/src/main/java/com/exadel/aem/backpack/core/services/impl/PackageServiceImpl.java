package com.exadel.aem.backpack.core.services.impl;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.aem.backpack.core.dto.response.BuildPackageInfo;
import com.exadel.aem.backpack.core.dto.response.TestBuildInfo;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.services.ReferenceService;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.*;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component(service = PackageService.class)
public class PackageServiceImpl implements PackageService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PackageServiceImpl.class);


	private static final String DEFAULT_PACKAGE_GROUP = "backpack";
	private static final String ERROR = "ERROR: ";

	@Reference
	private ReferenceService referenceService;

	@Reference
	private SlingRepository slingRepository;

	private Cache<String, BuildPackageInfo> packagesInfos = CacheBuilder.newBuilder()
			.maximumSize(100)
			.expireAfterWrite(1, TimeUnit.DAYS)
			.build();

	@Override
	public BuildPackageInfo testBuild(ResourceResolver resourceResolver, Collection<String> paths) {

		Set<AssetReferencedItem> assetLinks = new HashSet();
		paths.forEach(path -> {
			Set<AssetReferencedItem> assetReferences = referenceService.getAssetReferences(resourceResolver, path);
			assetLinks.addAll(assetReferences);
		});

		Long totalSize = 0l;
		for (AssetReferencedItem referencedItem : assetLinks) {
			referencedItem.setSize(getAssetSize(resourceResolver, referencedItem.getPath()));
            totalSize += referencedItem.getSize();
		}

        BuildPackageInfo packageInfo = new BuildPackageInfo();
        TestBuildInfo testBuildInfo = new TestBuildInfo();
        packageInfo.setTestBuildInfo(testBuildInfo);

        testBuildInfo.setAssetReferences(assetLinks);
        testBuildInfo.setTotalSize(totalSize);

		return packageInfo;
	}

	private Long getAssetSize(ResourceResolver resourceResolver, String path) {
		Resource rootResource = resourceResolver.getResource(path);
		Long totalSize = getAssetSize(rootResource);
		return totalSize;
	}

	private Long getAssetSize(Resource resource) {
		Long totalSize = 0l;
		if (resource == null) {
			return totalSize;
		}
		for (Resource child : resource.getChildren()) {
			totalSize += getAssetSize(child);
		}
		Resource childResource = resource.getChild("jcr:content/jcr:data");
		if (childResource != null && childResource.getResourceMetadata().containsKey("sling.contentLength")) {
			totalSize += (Long)childResource.getResourceMetadata().get("sling.contentLength");
		}
		return totalSize;
	}

	@Override
	public BuildPackageInfo buildPackage(final ResourceResolver resourceResolver,
										 final String pkgName,
										 final String packageGroup,
										 final String version) {
		final Session session = resourceResolver.adaptTo(Session.class);

		JcrPackageManager packMgr = PackagingService.getPackageManager(session);
		BuildPackageInfo.BuildPackageInfoBuilder builder = BuildPackageInfo.BuildPackageInfoBuilder.aBuildPackageInfo();
		builder.withPackageName(pkgName);
		builder.withVersion(version);

		String pkgGroupName = DEFAULT_PACKAGE_GROUP;

		if (StringUtils.isNotBlank(packageGroup)) {
			pkgGroupName = packageGroup;
			builder.withGroupName(pkgGroupName);
		}
		BuildPackageInfo buildInfo = builder.build();
		try {
			if (!isPkgExists(packMgr, pkgName, pkgGroupName, version)) {
				String packageExistMsg = "Package with such name don't exist in the" + pkgGroupName + " group.";

				buildInfo.addLogMessage(ERROR + packageExistMsg);
				LOGGER.error(packageExistMsg);
				return buildInfo;
			}
		} catch (RepositoryException e) {
			buildInfo.addLogMessage(ERROR + e.getMessage());
			LOGGER.error("Error during existing packages check", e);
			return buildInfo;
		}

		packagesInfos.put(getPackageId(pkgGroupName, pkgName, version), buildInfo);
		buildPackage(resourceResolver.getUserID(), buildInfo);

		return buildInfo;
	}

	@Override
	public BuildPackageInfo createPackage(final ResourceResolver resourceResolver, final Collection<String> initialPaths, final String pkgName, final String packageGroup, final String version) {
		final Session session = resourceResolver.adaptTo(Session.class);

		JcrPackageManager packMgr = PackagingService.getPackageManager(session);
		BuildPackageInfo.BuildPackageInfoBuilder builder = BuildPackageInfo.BuildPackageInfoBuilder.aBuildPackageInfo();
		builder.withPackageName(pkgName);
		builder.withPaths(initialPaths);
		builder.withVersion(version);

		String pkgGroupName = DEFAULT_PACKAGE_GROUP;

		if (StringUtils.isNotBlank(packageGroup)) {
			pkgGroupName = packageGroup;
		}
		builder.withGroupName(pkgGroupName);
		BuildPackageInfo packageInfo = builder.build();
		try {
			if (isPkgExists(packMgr, pkgName, pkgGroupName, version)) {
				String packageExistMsg = "Package with such name already exist in the " + pkgGroupName + " group.";

				packageInfo.addLogMessage(ERROR + packageExistMsg);
				LOGGER.error(packageExistMsg);
				return packageInfo;
			}
		} catch (RepositoryException e) {
			packageInfo.addLogMessage(ERROR + e.getMessage());
			packageInfo.addLogMessage(ExceptionUtils.getStackTrace(e));
			LOGGER.error("Error during existing packages check", e);
			return packageInfo;
		}

		Set<AssetReferencedItem> referencedAssets = getReferencedAssets(resourceResolver, initialPaths);
		Collection<String> resultingPaths = initAssets(initialPaths, referencedAssets, packageInfo);
		DefaultWorkspaceFilter filter = getWorkspaceFilter(resultingPaths);
		createPackage(resourceResolver.getUserID(), packageInfo, filter);

		return packageInfo;
	}

	private Collection<String> initAssets(final Collection<String> initialPaths,
										  final Set<AssetReferencedItem> referencedAssets,
										  final BuildPackageInfo buildPackageInfo) {
		Collection<String> resultingPaths = new ArrayList();
		resultingPaths.addAll(initialPaths);
		referencedAssets.forEach(assetReferencedItem -> {
			resultingPaths.add(assetReferencedItem.getPath());
			buildPackageInfo.addAssetReferencedItem(assetReferencedItem);

		});
		return resultingPaths;
	}

	@Override
	public List<String> getLatestPackageBuildInfo(final String pkgName, final String pkgGroupName, final String version) {
		BuildPackageInfo buildPackageInfo = packagesInfos.asMap().get(getPackageId(pkgGroupName, pkgName, version));
		if (buildPackageInfo != null) {
			return buildPackageInfo.getLatestBuildInfo();
		}
		return Collections.emptyList();
	}

	private JcrPackage createPackage(final String userId, final BuildPackageInfo packageBuildInfo, final DefaultWorkspaceFilter filter) {

		Session userSession = null;
		JcrPackage jcrPackage = null;
		try {
			userSession = slingRepository.loginAdministrative(null);
			userSession.impersonate(new SimpleCredentials(userId, new char[0]));
			JcrPackageManager packMgr = PackagingService.getPackageManager(userSession);
			if (!filter.getFilterSets().isEmpty()) {
				jcrPackage = packMgr.create(packageBuildInfo.getGroupName(), packageBuildInfo.getPackageName(), packageBuildInfo.getVersion());
				JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
				jcrPackageDefinition.setFilter(filter, true);
				jcrPackage.close();
			}
			packageBuildInfo.setPackageCreated(true);
		} catch (Exception e) {
			packageBuildInfo.addLogMessage(ERROR + e.getMessage());
			packageBuildInfo.addLogMessage(ExceptionUtils.getStackTrace(e));
			LOGGER.error("Error during package creation", e);
		} finally {
			closeSession(userSession);
		}
		return jcrPackage;
	}

	private void buildPackage(final String userId, final BuildPackageInfo packageBuildInfo) {
		new Thread(() -> {
			Session userSession = null;
			try {
				userSession = slingRepository.loginAdministrative(null);
				userSession.impersonate(new SimpleCredentials(userId, new char[0]));
				JcrPackageManager packMgr = PackagingService.getPackageManager(userSession);
				PackageId packageId = new PackageId(packageBuildInfo.getGroupName(), packageBuildInfo.getPackageName(), packageBuildInfo.getVersion());
				JcrPackage jcrPackage = packMgr.open(packageId);

				packMgr.assemble(jcrPackage, new ProgressTrackerListener() {
					@Override
					public void onMessage(final Mode mode, final String statusCode, final String path) {
						packageBuildInfo.addLogMessage(statusCode + " " + path);
					}

					@Override
					public void onError(final Mode mode, final String s, final Exception e) {
						packageBuildInfo.addLogMessage(s + " " + e.getMessage());
					}
				});

				packageBuildInfo.setPackageBuilt(true);

			} catch (Exception e) {
				packageBuildInfo.addLogMessage(ERROR + e.getMessage());
				packageBuildInfo.addLogMessage(ExceptionUtils.getStackTrace(e));
				LOGGER.error("Error during package generation", e);
			} finally {
				closeSession(userSession);
			}
		}).start();
	}

	private void closeSession(final Session userSession) {
		if (userSession != null && userSession.isLive()) {
			userSession.logout();
		}
	}

	private Set<AssetReferencedItem> getReferencedAssets(final ResourceResolver resourceResolver, final Collection<String> paths) {
		Set<AssetReferencedItem> assetLinks = new HashSet();
		paths.forEach(path -> {
			Set<AssetReferencedItem> assetReferences = referenceService.getAssetReferences(resourceResolver, path);
			assetLinks.addAll(assetReferences);
		});
		return assetLinks;
	}

	private DefaultWorkspaceFilter getWorkspaceFilter(final Collection<String> paths) {
		DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
		paths.forEach(path -> {
			PathFilterSet pathFilterSet = new PathFilterSet(path);
			filter.add(pathFilterSet);
		});

		return filter;
	}

	private boolean isPkgExists(final JcrPackageManager pkgMgr,
								final String newPkgName,
								final String pkgGroupName,
								final String version) throws RepositoryException {
		List<JcrPackage> packages = pkgMgr.listPackages(pkgGroupName, false);
		for (JcrPackage jcrpackage : packages) {
			String packageName = jcrpackage.getDefinition().getId().toString();
			if (packageName.equalsIgnoreCase(getPackageId(pkgGroupName, newPkgName, version))) {
				return true;
			}
		}
		return false;
	}

	private String getPackageId(final String pkgGroupName, final String packageName, final String version) {
		return pkgGroupName + ":" + packageName + (StringUtils.isNotBlank(version) ? ":" + version : StringUtils.EMPTY);
	}

}



