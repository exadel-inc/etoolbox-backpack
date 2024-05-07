package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.exadel.etoolbox.backpack.core.services.resource.QuerySearchService;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class QuerySearchServiceImplTest {

    private static final String PAGE_1 = "/content/site/pages/page1";
    private static final String PAGE_2 = "/content/site/pages/page2";
    private static final String PAGE_3 = "/content/site/pages/page3";
    private static final String QUERY = "SELECT * FROM [cq:PageContent] AS s WHERE ISDESCENDANTNODE([/content/site/pages]) AND s.[cq:tags] LIKE 'test:topics/test-tag' order by s.[jcr:title] desc";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);
    private ResourceResolver resourceResolver;
    private QuerySearchService querySearchService;

    @Before
    public void beforeTest() {
        resourceResolver = context.resourceResolver();
        querySearchService = context.registerInjectActivateService(new QuerySearchServiceImpl());

        context.load().json("/com/exadel/etoolbox/backpack/core/services/impl/page1.json", PAGE_1);
        context.load().json("/com/exadel/etoolbox/backpack/core/services/impl/page2.json", PAGE_2);
        context.load().json("/com/exadel/etoolbox/backpack/core/services/impl/page3.json", PAGE_3);
    }

    @Test
    public void shouldReturnListResourcesPath() {
        List<String> resourcesPathsFromQuery = querySearchService.getResourcesPathsFromQuery(resourceResolver, QUERY, new ArrayList<>());

        assertEquals("resources paths size", 2 , resourcesPathsFromQuery.size());
        assertEquals("page3 path", "/content/site/pages/page3/jcr:content", resourcesPathsFromQuery.get(0));
        assertEquals("page1 path","/content/site/pages/page1/jcr:content", resourcesPathsFromQuery.get(1));
    }

    @Test
    public void shouldReturnEmptyListResourcesPath() {
        List<String> resourcesPathsFromQuery = querySearchService.getResourcesPathsFromQuery(resourceResolver, QUERY.toUpperCase(), new ArrayList<>());

        assertEquals("empty List with incorrect query", Collections.emptyList(), resourcesPathsFromQuery);
    }

}