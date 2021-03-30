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

package com.exadel.etoolbox.backpack.core.services.impl;

import com.exadel.etoolbox.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.PageReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.ReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.TagReferencedItem;
import com.exadel.etoolbox.backpack.core.services.ReferenceService;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class ReferenceSearchServiceImplTest {

    private static final String PAGE_1 = "/content/site/pages/page1";
    private static final String PAGE_2 = "/content/site/pages/page2";
    private static final String PAGE_NON_EXISTING = "/content/site/pages/pageNonExisting";

    private static final String ASSET_1 = "/content/dam/asset1.png";
    private static final String ASSET_2 = "/content/dam/asset2.pdf";

    private static final String MINE_TYPE_PNG = "image/png";
    private static final String MINE_TYPE_PDF = "application/pdf";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
    private ReferenceService referenceService;
    private Set<ReferencedItem> expectedReferencedItems;
    private ResourceResolver resourceResolver;

    @Before
    public void beforeTest() {
        // load resources
        context.load().json("/com/exadel/etoolbox/backpack/core/services/impl/page1.json", PAGE_1);
        context.load().json("/com/exadel/etoolbox/backpack/core/services/impl/page2.json", PAGE_2);
        context.create().tag("test:topics/test-tag");
        context.create().asset(ASSET_1, "/com/exadel/etoolbox/backpack/core/services/impl/asset.png", MINE_TYPE_PNG);
        context.create().asset(ASSET_2, "/com/exadel/etoolbox/backpack/core/services/impl/asset.pdf", MINE_TYPE_PDF);

        //register services
        context.registerInjectActivateService(new PageReferenceSearchServiceImpl());
        referenceService = context.registerInjectActivateService(new ReferenceServiceImpl());

        //setup expected references
        expectedReferencedItems = new LinkedHashSet<>();
        expectedReferencedItems.add(new TagReferencedItem("/etc/tags/test/topics/test-tag"));
        expectedReferencedItems.add(new PageReferencedItem(PAGE_2 + "/jcr:content"));
        expectedReferencedItems.add(new AssetReferencedItem(ASSET_1, MINE_TYPE_PNG));
        expectedReferencedItems.add(new AssetReferencedItem(ASSET_2, MINE_TYPE_PDF));

        resourceResolver = context.resourceResolver();
    }

    @Test
    public void shouldGetAssetReferences() {
        Set<ReferencedItem> referencedItems = referenceService.getReferences(resourceResolver, PAGE_1);

        assertEquals(expectedReferencedItems.size(), referencedItems.size());
        assertTrue(expectedReferencedItems.containsAll(referencedItems));
    }

    @Test
    public void shouldGetNoReferencesPageWithNoAssets() {
        Set<ReferencedItem> referencedItems = referenceService.getReferences(resourceResolver, PAGE_2);
        assertEquals(Collections.EMPTY_SET, referencedItems);
    }

    @Test
    public void shouldGetNoReferencesForNonExistingResource() {
        Set<ReferencedItem> referencedItems = referenceService.getReferences(resourceResolver, PAGE_NON_EXISTING);
        assertEquals(Collections.EMPTY_SET, referencedItems);
    }
}