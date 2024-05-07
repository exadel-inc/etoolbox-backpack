package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.exadel.etoolbox.backpack.core.dto.repository.AssetReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.PageReferencedItem;
import com.exadel.etoolbox.backpack.core.dto.repository.TagReferencedItem;
import com.exadel.etoolbox.backpack.core.services.resource.ReferencesSearchService;
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

public class ReferencesSearchServiceImplTest {

    private static final String PAGE_1 = "/content/site/pages/page1";
    private static final String PAGE_2 = "/content/site/pages/page2";
    private static final String PAGE_NON_EXISTING = "/content/site/pages/pageNonExisting";

    private static final String ASSET_1 = "/content/dam/asset1.png";
    private static final String ASSET_2 = "/content/dam/asset2.pdf";

    private static final String MINE_TYPE_PNG = "image/png";
    private static final String MINE_TYPE_PDF = "application/pdf";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
    private ReferencesSearchService referenceService;
    private final Set<AssetReferencedItem> expectedAssetReferences = new LinkedHashSet<>();
    private final Set<PageReferencedItem> expectedPageReferences = new LinkedHashSet<>();
    private final Set<TagReferencedItem> expectedTagReferences = new LinkedHashSet<>();
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
        referenceService = context.registerInjectActivateService(new ReferencesSearchServiceImpl());

        //setup expected references
        expectedAssetReferences.add(new AssetReferencedItem(ASSET_1, MINE_TYPE_PNG));
        expectedAssetReferences.add(new AssetReferencedItem(ASSET_2, MINE_TYPE_PDF));
        expectedPageReferences.add(new PageReferencedItem(PAGE_2 + "/jcr:content"));
        expectedTagReferences.add(new TagReferencedItem("/etc/tags/test/topics/test-tag"));
        resourceResolver = context.resourceResolver();
    }

    @Test
    public void shouldGetAssetReferences() {
        Set<AssetReferencedItem> assetReferences = referenceService.getAssetReferences(resourceResolver, PAGE_1);
        Set<TagReferencedItem> tagReferences = referenceService.getTagReferences(resourceResolver, PAGE_1);
        Set<PageReferencedItem> pageReferences = referenceService.getPageReferences(resourceResolver, PAGE_1);

        assertEquals(assetReferences.size(), expectedAssetReferences.size());
        assertTrue(assetReferences.containsAll(expectedAssetReferences));
        assertEquals(tagReferences.size(), expectedTagReferences.size());
        assertTrue(tagReferences.containsAll(expectedTagReferences));
        assertEquals(pageReferences.size(), expectedPageReferences.size());
        assertTrue(pageReferences.containsAll(expectedPageReferences));
    }

    @Test
    public void shouldGetNoReferencesPageWithNoAssets() {
        Set<AssetReferencedItem> assetReferences = referenceService.getAssetReferences(resourceResolver, PAGE_2);
        Set<TagReferencedItem> tagReferences = referenceService.getTagReferences(resourceResolver, PAGE_2);
        Set<PageReferencedItem> pageReferences = referenceService.getPageReferences(resourceResolver, PAGE_2);
        assertEquals(Collections.EMPTY_SET, assetReferences);
        assertEquals(Collections.EMPTY_SET, tagReferences);
        assertEquals(Collections.EMPTY_SET, pageReferences);
    }

    @Test
    public void shouldGetNoReferencesForNonExistingResource() {
        Set<AssetReferencedItem> assetReferences = referenceService.getAssetReferences(resourceResolver, PAGE_NON_EXISTING);
        Set<TagReferencedItem> tagReferences = referenceService.getTagReferences(resourceResolver, PAGE_NON_EXISTING);
        Set<PageReferencedItem> pageReferences = referenceService.getPageReferences(resourceResolver, PAGE_NON_EXISTING);

        assertEquals(Collections.EMPTY_SET, assetReferences);
        assertEquals(Collections.EMPTY_SET, tagReferences);
        assertEquals(Collections.EMPTY_SET, pageReferences);
    }
}