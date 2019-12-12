package com.exadel.aem.backpack.core.services.impl;

import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.aem.backpack.core.dto.response.BuildPackageInfo;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.services.ReferenceService;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component(service = PackageService.class)
public class PackageServiceImpl implements PackageService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PackageServiceImpl.class);


	private static final String DEFAULT_PACKAGE_GROUP = "my_packages";
	private static final String ERROR = "ERROR: ";

	@Reference
	private ReferenceService referenceService;

	@Override
	public BuildPackageInfo createPackage(final ResourceResolver resourceResolver,
										  final Collection<String> paths, String pkgName,
										  final String packageGroup) {
		final Session session = resourceResolver.adaptTo(Session.class);
		JcrPackageManager packMgr = PackagingService.getPackageManager(session);
		BuildPackageInfo.BuildPackageInfoBuilder builder = BuildPackageInfo.BuildPackageInfoBuilder.aBuildPackageInfo();
		builder.withPackageName(pkgName);
		builder.withPaths(paths);
		List<String> buildLog = new ArrayList<>();
		builder.withBuildLog(buildLog);


		String pkgGroupName = DEFAULT_PACKAGE_GROUP;

		if (StringUtils.isNotBlank(packageGroup)) {
			pkgGroupName = packageGroup;
			builder.withGroupName(pkgGroupName);
		}

		try {
			if (isPkgExists(pkgName, packMgr, pkgGroupName)) {
				String packageExistMsg = "Package with such name already exist in the " + pkgGroupName + " group.";
				buildLog.add(ERROR + packageExistMsg);
				LOGGER.error(packageExistMsg);
				return builder.build();
			}
		} catch (RepositoryException e) {
			buildLog.add(ERROR + e.getMessage());
			LOGGER.error("Error during existing packages check", e);
			return builder.build();
		}

		DefaultWorkspaceFilter filter = getWorkspaceFilter(paths);
		try {
			if (!filter.getFilterSets().isEmpty()) {
				JcrPackage jcrPackage = packMgr.create(pkgGroupName, pkgName);
				JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
				jcrPackageDefinition.setFilter(filter, false);

				packMgr.assemble(jcrPackage, new ProgressTrackerListener() {
					@Override
					public void onMessage(final Mode mode, final String s, final String s1) {
						buildLog.add(s + " " + s1);
					}

					@Override
					public void onError(final Mode mode, final String s, final Exception e) {
						buildLog.add(s + " " + e.getMessage());
					}
				});
			}
			builder.withPackageCreated(true);

		} catch (Exception e) {
			buildLog.add(ERROR + e.getMessage());
			LOGGER.error("Error during package generation", e);
		}

		return builder.build();
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



