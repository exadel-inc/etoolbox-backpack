package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class PageReferenceSearchServiceImplTest {

    private static final String PAGE_1 = "/content/site/pages/page1";
    private static final String PAGE_2 = "/content/site/pages/page2";
    private static final String PAGE_3 = "/content/site/pages/page3";
    private static final String PAGE_4 = "/content/site/pages/page4";
    private static final String PAGE_5 = "/content/site/pages/page5";

    private static final String PAGE_NON_EXISTING = "/content/site/pages/pageNonExisting";
    private static final String TEMPLATE_PATH = "/conf/test/template";
    private static final String TEMPLATE_PATH_2 = "/conf/test/template2";
    private static final String TEMPLATE_PATH_3 = "/conf/test/template3";




    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
    private Set<Page> expectedPages;
    private ResourceResolver resourceResolver;
    private PageReferenceSearchServiceImpl pageReferenceSearchService;
    private Page page2Template3;
    private Page page4Template;
    private Page page5Template2;

    @Before
    public void beforeTest() {

        // load resources
        Map<String, Object> templateProperties = new HashMap<>();
        templateProperties.put("jcr:primaryType", "cq:Template");
        context.create().resource(TEMPLATE_PATH, templateProperties);
        context.create().resource(TEMPLATE_PATH_2, templateProperties);
        context.create().resource(TEMPLATE_PATH_3, templateProperties);


        context.load().json("/com/exadel/etoolbox/backpack/core/services/impl/page3.json", PAGE_3);
        page2Template3 = context.load().json("/com/exadel/etoolbox/backpack/core/services/impl/page2.json", PAGE_2).adaptTo(Page.class);
        page4Template = context.create().page(PAGE_4, TEMPLATE_PATH);
        page5Template2 = context.create().page(PAGE_5, TEMPLATE_PATH_2);


        //register services
        pageReferenceSearchService = context.registerInjectActivateService(new PageReferenceSearchServiceImpl());

        //setup expected references
        expectedPages = new LinkedHashSet<>();
        resourceResolver = context.resourceResolver();
    }

    @Test
    public void shouldGetPageReferences() {
        expectedPages.add(page2Template3);
        expectedPages.add(page4Template);
        expectedPages.add(page5Template2);
        Set<Page> referencedItems = pageReferenceSearchService.findPageReferences(resourceResolver, PAGE_3);

        assertEquals(expectedPages.size(), referencedItems.size());
        assertTrue(expectedPages.containsAll(referencedItems));
    }

    @Test
    public void shouldSkipPagesWithIgnoreTemplate() {
        expectedPages.add(page2Template3);
        expectedPages.add(page5Template2);
        Map<String, Object> properties = new HashMap<>();
        properties.put("ignoreTemplatePaths", new String[]{TEMPLATE_PATH});
        pageReferenceSearchService = context.registerInjectActivateService(new PageReferenceSearchServiceImpl(), properties);

        Set<Page> referencedItems = pageReferenceSearchService.findPageReferences(resourceResolver, PAGE_3);

        assertEquals(expectedPages.size(), referencedItems.size());
        assertTrue(expectedPages.containsAll(referencedItems));
    }

    @Test
    public void shouldFindPagesOnlyWithIncludeTemplate() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("includeTemplatePaths", new String[]{TEMPLATE_PATH});
        expectedPages.add(page4Template);
        pageReferenceSearchService = context.registerInjectActivateService(new PageReferenceSearchServiceImpl(), properties);

        Set<Page> referencedItems = pageReferenceSearchService.findPageReferences(resourceResolver, PAGE_3);

        assertEquals(expectedPages.size(), referencedItems.size());
        assertTrue(expectedPages.containsAll(referencedItems));
    }

    @Test
    public void shouldUseIgnoreTemplatesWhenBothIgnoreAndIncludeAreConfigured() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("ignoreTemplatePaths", new String[]{TEMPLATE_PATH_2});
        properties.put("includeTemplatePaths", new String[]{TEMPLATE_PATH});
        expectedPages.add(page2Template3);
        expectedPages.add(page4Template);
        pageReferenceSearchService = context.registerInjectActivateService(new PageReferenceSearchServiceImpl(), properties);

        Set<Page> referencedItems = pageReferenceSearchService.findPageReferences(resourceResolver, PAGE_3);

        assertEquals(expectedPages.size(), referencedItems.size());
        assertTrue(expectedPages.containsAll(referencedItems));
    }

    @Test
    public void shouldGetNoReferencesForPageWithoutAnyReferences() {
        Set<Page> referencedItems = pageReferenceSearchService.findPageReferences(resourceResolver, PAGE_2);
        assertEquals(Collections.EMPTY_SET, referencedItems);
    }

    @Test
    public void shouldGetNoReferencesForNonExistingResource() {
        Set<Page> referencedItems = pageReferenceSearchService.findPageReferences(resourceResolver, PAGE_NON_EXISTING);
        assertEquals(Collections.EMPTY_SET, referencedItems);
    }

}