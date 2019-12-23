package com.exadel.aem.backpack.core.services.impl;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.aem.backpack.core.dto.repository.ReferencedItem;
import com.exadel.aem.backpack.core.dto.response.BuildPackageInfo;
import com.exadel.aem.backpack.core.dto.response.TestBuildInfo;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.services.ReferenceService;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
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
import java.util.stream.Collectors;

@Component(service = PackageService.class)
public class PackageServiceImpl implements PackageService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PackageServiceImpl.class);


	private static final String DEFAULT_PACKAGE_GROUP = "my_packages";
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
            totalSize += getAssetSize(resourceResolver, referencedItem.getPath());
		}

        BuildPackageInfo packageInfo = new BuildPackageInfo();
        TestBuildInfo testBuildInfo = new TestBuildInfo();
        packageInfo.setTestBuildInfo(testBuildInfo);

        testBuildInfo.setAssetReferences(assetLinks);
        testBuildInfo.setTotalSize(totalSize);

		return packageInfo;
	}

	private Long getAssetSize(ResourceResolver resourceResolver, String path) {
        Long totalSize = 0l;
        String queryString = "SELECT * FROM [nt:file] AS a WHERE ISDESCENDANTNODE(a,'"+ path +"')";
        Iterator<Resource> searchResult = resourceResolver.findResources(queryString, Query.JCR_SQL2);
        while (searchResult.hasNext()) {
            Resource resource = searchResult.next();
            Resource childResource = resource.getChild("jcr:content/jcr:data");
            if (childResource != null && childResource.getResourceMetadata().containsKey("sling.contentLength")) {
                totalSize += (Long)childResource.getResourceMetadata().get("sling.contentLength");
            }
        }
		return totalSize;
	}

	@Override
	public BuildPackageInfo buildPackage(final ResourceResolver resourceResolver,
										 final Collection<String> initialPaths, String pkgName,
										 final String packageGroup) {
		final Session session = resourceResolver.adaptTo(Session.class);

		JcrPackageManager packMgr = PackagingService.getPackageManager(session);
		BuildPackageInfo.BuildPackageInfoBuilder builder = BuildPackageInfo.BuildPackageInfoBuilder.aBuildPackageInfo();
		builder.withPackageName(pkgName);
		builder.withPaths(initialPaths);


		String pkgGroupName = DEFAULT_PACKAGE_GROUP;

		if (StringUtils.isNotBlank(packageGroup)) {
			pkgGroupName = packageGroup;
			builder.withGroupName(pkgGroupName);
		}
		BuildPackageInfo buildInfo = builder.build();
		try {
			if (isPkgExists(pkgName, packMgr, pkgGroupName)) {
				String packageExistMsg = "Package with such name already exist in the " + pkgGroupName + " group.";

				buildInfo.addLogMessage(ERROR + packageExistMsg);
				LOGGER.error(packageExistMsg);
				return buildInfo;
			}
		} catch (RepositoryException e) {
			buildInfo.addLogMessage(ERROR + e.getMessage());
			LOGGER.error("Error during existing packages check", e);
			return buildInfo;
		}

		Collection<String> resultingPaths = includeReferencedAssetPaths(resourceResolver, initialPaths);
		DefaultWorkspaceFilter filter = getWorkspaceFilter(resultingPaths);
		packagesInfos.put(pkgGroupName + ":" + pkgName, buildInfo);
		buildPackage(resourceResolver.getUserID(), buildInfo, filter);

		return buildInfo;
	}

	@Override
	public List<String> getLatestPackageBuildInfo(final String pkgName, final String pkgGroupName) {
		BuildPackageInfo buildPackageInfo = packagesInfos.asMap().get(pkgGroupName + ":" + pkgName);
		if (buildPackageInfo != null) {
			return buildPackageInfo.getLatestBuildInfo();
		}
		return Collections.EMPTY_LIST;
	}

	private void buildPackage(final String userId, final BuildPackageInfo buildInfo, final DefaultWorkspaceFilter filter) {
		new Thread(() -> {
			Session userSession = null;
			try {
				userSession = slingRepository.loginAdministrative(null);
				userSession.impersonate(new SimpleCredentials(userId, new char[0]));
				JcrPackageManager packMgr = PackagingService.getPackageManager(userSession);
				if (!filter.getFilterSets().isEmpty()) {
					JcrPackage jcrPackage = packMgr.create(buildInfo.getGroupName(), buildInfo.getPackageName());
					JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
					jcrPackageDefinition.setFilter(filter, false);

					packMgr.assemble(jcrPackage, new ProgressTrackerListener() {
						@Override
						public void onMessage(final Mode mode, final String statusCode, final String path) {
							buildInfo.addLogMessage(statusCode + " " + path);
						}

						@Override
						public void onError(final Mode mode, final String s, final Exception e) {
							buildInfo.addLogMessage(s + " " + e.getMessage());
						}
					});
				}
				buildInfo.setPackageCreated(true);

			} catch (Exception e) {
				buildInfo.addLogMessage(ERROR + e.getMessage());
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

	private Collection<String> includeReferencedAssetPaths(final ResourceResolver resourceResolver, final Collection<String> paths) {
		Set<AssetReferencedItem> assetLinks = new HashSet();
		paths.forEach(path -> {
			Set<AssetReferencedItem> assetReferences = referenceService.getAssetReferences(resourceResolver, path);
			assetLinks.addAll(assetReferences);
		});
		Collection<String> resultingPaths = new ArrayList();
		resultingPaths.addAll(paths);
		resultingPaths.addAll(assetLinks.stream().map(ReferencedItem::getPath).collect(Collectors.toList()));
		return resultingPaths;
	}

	private DefaultWorkspaceFilter getWorkspaceFilter(final Collection<String> paths) {
		DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
		paths.forEach(path -> {
			PathFilterSet pathFilterSet = new PathFilterSet(path);
			filter.add(pathFilterSet);
		});

		return filter;
	}

	private boolean isPkgExists(final String newPkgName, final JcrPackageManager pkgMgr,
								final String pkgGroupName) throws RepositoryException {
		List<JcrPackage> packages = pkgMgr.listPackages(pkgGroupName, false);
		for (JcrPackage jcrpackage : packages) {
			String packageName = jcrpackage.getDefinition().getId().toString();
			if (packageName.equalsIgnoreCase(pkgGroupName + ":" + newPkgName)) {
				return true;
			}
		}
		return false;
	}


}



