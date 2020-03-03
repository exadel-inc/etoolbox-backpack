package com.exadel.aem.backpack.core.services.impl;


import com.exadel.aem.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.aem.backpack.core.services.ReferenceService;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ReferenceServiceImplTest {

    private static final String PAGE_1 = "/content/site/pages/page1";
    private static final String PAGE_2 = "/content/site/pages/page2";
    private static final String PAGE_NON_EXISTING = "/content/site/pages/pageNonExisting";

    private static final String ASSET_1 = "/content/dam/asset1.png";
    private static final String ASSET_2 = "/content/dam/asset2.pdf";

    private static final String MINE_TYPE_PNG = "image/png";
    private static final String MINE_TYPE_PDF = "application/pdf";

    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
    protected ReferenceService referenceService;
    protected Set<AssetReferencedItem> expectedReferencedItems;
    protected ResourceResolver resourceResolver;

    @Before
    public void beforeTest() {
        // load resources
        context.load().json("/com/exadel/aem/backpack/core/services/impl/page1.json", PAGE_1);
        context.load().json("/com/exadel/aem/backpack/core/services/impl/page2.json", PAGE_2);
        context.create().asset(ASSET_1, "/com/exadel/aem/backpack/core/services/impl/asset.png", MINE_TYPE_PNG);
        context.create().asset(ASSET_2, "/com/exadel/aem/backpack/core/services/impl/asset.pdf", MINE_TYPE_PDF);

        //register services
        referenceService = context.registerInjectActivateService(new ReferenceServiceImpl());

        //setup expected references
        expectedReferencedItems = new HashSet<>();
        expectedReferencedItems.add(new AssetReferencedItem(ASSET_1, MINE_TYPE_PNG));
        expectedReferencedItems.add(new AssetReferencedItem(ASSET_2, MINE_TYPE_PDF));

        resourceResolver = context.resourceResolver();
    }

    @Test
    public void shouldGetAssetReferences() {
        Set<AssetReferencedItem> referencedItems = referenceService.getAssetReferences(resourceResolver, PAGE_1);
        assertEquals(expectedReferencedItems, referencedItems);
    }

    @Test
    public void shouldGetNoReferencesPageWithNoAssets() {
        Set<AssetReferencedItem> referencedItems = referenceService.getAssetReferences(resourceResolver, PAGE_2);
        assertEquals(Collections.EMPTY_SET, referencedItems);
    }

    @Test
    public void shouldGetNoReferencesForNonExistingResource() {
        Set<AssetReferencedItem> referencedItems = referenceService.getAssetReferences(resourceResolver, PAGE_NON_EXISTING);
        assertEquals(Collections.EMPTY_SET, referencedItems);
    }
}