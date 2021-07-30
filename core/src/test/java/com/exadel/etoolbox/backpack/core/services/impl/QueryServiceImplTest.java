package com.exadel.etoolbox.backpack.core.services.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import io.wcm.testing.mock.aem.junit.AemContext;
import junit.framework.TestCase;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.oak.OakMockSlingRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class QueryServiceImplTest {

    private static final String PAGE_1 = "/content/site/pages/page1";
    private static final String PAGE_2 = "/content/site/pages/page2";
    private static final String PAGE_3 = "/content/site/pages/page3";
    private static final String QUERY = "SELECT * FROM [cq:PageContent] AS s WHERE ISDESCENDANTNODE([/content/site/pages]) AND s.[cq:tags] LIKE 'test:topics/test-tag' order by s.[jcr:title] desc";

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);
    private ResourceResolver resourceResolver;
    private QueryServiceImpl queryService;

    @Before
    public void beforeTest() {
        resourceResolver = context.resourceResolver();
        queryService = context.registerInjectActivateService(new QueryServiceImpl());

        context.load().json("/com/exadel/etoolbox/backpack/core/services/impl/page1.json", PAGE_1);
        context.load().json("/com/exadel/etoolbox/backpack/core/services/impl/page2.json", PAGE_2);
        context.load().json("/com/exadel/etoolbox/backpack/core/services/impl/page3.json", PAGE_3);
    }

    @Test
    public void shouldReturnListResourcesPath() {
        PackageInfo packageInfo = new PackageInfo();
        List<String> resourcesPathsFromQuery = queryService.getResourcesPathsFromQuery(resourceResolver, QUERY, packageInfo);

        assertEquals("resources paths size", 2 , resourcesPathsFromQuery.size());
        assertEquals("page3 path", "/content/site/pages/page3/jcr:content", resourcesPathsFromQuery.get(0));
        assertEquals("page1 path","/content/site/pages/page1/jcr:content", resourcesPathsFromQuery.get(1));
    }

    @Test
    public void shouldReturnEmptyListResourcesPath() {
        PackageInfo packageInfo = new PackageInfo();
        List<String> resourcesPathsFromQuery = queryService.getResourcesPathsFromQuery(resourceResolver, QUERY.toUpperCase(), packageInfo);

        assertEquals("empty List with incorrect query", Collections.emptyList(), resourcesPathsFromQuery);
    }
}