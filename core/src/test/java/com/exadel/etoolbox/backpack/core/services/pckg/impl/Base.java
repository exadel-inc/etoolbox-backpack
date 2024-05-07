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
package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.resource.ReferencesSearchService;
import com.exadel.etoolbox.backpack.core.services.resource.impl.LiveCopySearchServiceImpl;
import com.exadel.etoolbox.backpack.core.services.resource.impl.QuerySearchServiceImpl;
import com.google.gson.Gson;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Base {
    protected static final String PAGE_1 = "/content/site/pages/page1";
    protected static final String PAGE_2 = "/content/site/pages/page2";

    protected static final String PICTURE_1 = "/content/dam/picture1.jpg";
    protected static final String PICTURE_2 = "/content/dam/picture2.png";
    protected static final String PICTURE_3 = "/content/dam/picture3.jpg";
    protected static final String PACKAGE_PATH = "/etc/packages/testGroup/testPackage-1.zip";
    protected static final String THUMBNAIL = "/content/dam/thumbnail.png";
    protected static final String IMAGE_JPEG = "image/jpeg";
    protected static final String IMAGE_PNG = "image/png";
    protected static final String BACKPACK = "EToolbox_BackPack";
    protected static final String MY_PACKAGES_GROUP = "my_packages";
    protected static final String TEST_PACKAGE = "testPackage";
    protected static final String PACKAGE_VERSION = "1";
    protected static final String PACKAGE_VERSION_2 = "2";
    protected static final String PACKAGE_SIZE_NODE = "/var/etoolbox-backpack";

    protected static final String TEST_GROUP = "testGroup";

    protected static final String REFERENCED_RESOURCES = "referencedResources";
    protected static final String GENERAL_RESOURCES = "generalResources";

    protected static final Gson GSON = new Gson();
    protected static final String PACKAGE_2_2_ZIP = "testPackage2-2.zip";
    protected Gson gson = new Gson();

    protected static final String INITIAL_FILTERS = "initialFilters";
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_OAK);
    protected PackageInfoService packageInfoService;
    protected ResourceResolver resourceResolver;
    protected ResourceResolver resourceResolverMock;
    protected JcrPackageManager packMgr;
    protected Session session;
    protected ReferencesSearchService referencesSearchService = mock(ReferencesSearchService.class);
    protected BasePackageService basePackageService;

    @Mock
    private LiveRelationshipManager liveRelationshipManager;

    @Before
    public void beforeTest() throws IOException, RepositoryException, WCMException {

        context.registerService(ReferencesSearchService.class, referencesSearchService);

        Map<String, Object> properties = new HashMap<>();
        properties.put("buildInfoTTL", 1);
        context.registerInjectActivateService(new QuerySearchServiceImpl());
        liveRelationshipManager = mock(LiveRelationshipManager.class);
        context.registerService(LiveRelationshipManager.class, liveRelationshipManager);
        RangeIterator relationships = mock(RangeIterator.class);
        when(relationships.hasNext()).thenReturn(false);
        when(liveRelationshipManager.getLiveRelationships(any(Resource.class), any(), any())).thenReturn(relationships);
        context.registerInjectActivateService(new LiveCopySearchServiceImpl());
        basePackageService = context.registerInjectActivateService(new BasePackageServiceImpl(), properties);
        packageInfoService = context.registerInjectActivateService(new PackageInfoServiceImpl());

        context.create().page(PAGE_1);
        context.create().page(PAGE_2);
        context.create().asset(PICTURE_1, 100, 100, IMAGE_JPEG);
        context.create().asset(PICTURE_2, 100, 100, IMAGE_PNG);
        context.create().resource(PACKAGE_SIZE_NODE, Collections.singletonMap("averageSize", 4000));
        resourceResolver = context.resourceResolver();
        session = resourceResolver.adaptTo(Session.class);
        packMgr = PackagingService.getPackageManager(session);

        resourceResolverMock = mock(ResourceResolver.class);
        when(resourceResolverMock.adaptTo(Session.class)).thenReturn(null);
    }


    protected PackageInfo getDefaultPackageInfo() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setGroupName(BACKPACK);
        packageInfo.setPackageName(TEST_PACKAGE);
        packageInfo.setVersion(PACKAGE_VERSION);
        packageInfo.setPaths(Collections.singletonList(PAGE_1));
        return packageInfo;
    }

    protected JcrPackage createPackage(final PackageInfo packageInfo,
                                       final DefaultWorkspaceFilter filter) throws RepositoryException, IOException {
        JcrPackage jcrPackage = null;
        try {
            jcrPackage = packMgr.create(packageInfo.getGroupName(), packageInfo.getPackageName(), packageInfo.getVersion());
            JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();
            if (jcrPackageDefinition != null) {
                jcrPackageDefinition.set(GENERAL_RESOURCES, GSON.toJson(packageInfo.getPaths()), true);
                jcrPackageDefinition.setFilter(filter, true);

                Node packageNode = jcrPackage.getNode();
                if (packageNode != null) {
                    packageInfo.setPackageNodeName(packageNode.getName());
                }
            }
        } finally {
            if (jcrPackage != null) {
                jcrPackage.close();
            }
        }
        return jcrPackage;
    }
}