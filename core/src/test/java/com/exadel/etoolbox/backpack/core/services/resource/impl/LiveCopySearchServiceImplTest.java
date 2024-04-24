package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveCopy;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.RangeIterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LiveCopySearchServiceImplTest {

    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);
    private ResourceResolver resourceResolver;
    @Mock
    private LiveRelationshipManager liveRelationshipManager;
    private LiveCopySearchServiceImpl liveCopySearchService;

    @Before
    public void beforeTest() throws WCMException {
        resourceResolver = context.resourceResolver();
        liveRelationshipManager = mock(LiveRelationshipManager.class);
        context.registerService(LiveRelationshipManager.class, liveRelationshipManager);
        liveCopySearchService = context.registerInjectActivateService(new LiveCopySearchServiceImpl());
    }

    @Test
    public void shouldReturnEmptyListWhenResourceIsNull() {
        List<String> result = liveCopySearchService.getLiveCopies(resourceResolver, "path", StringUtils.EMPTY);

        assertEquals(0, result.size());
    }

    @Test
    public void shouldReturnLiveCopiesWhenResourceExists() throws WCMException {
        RangeIterator relationships = mock(RangeIterator.class);
        LiveRelationship relationship = mock(LiveRelationship.class);
        LiveCopy liveCopy = mock(LiveCopy.class);
        context.create().resource("/path");
        context.create().resource("/liveCopyPath");

        when(liveRelationshipManager.getLiveRelationships(any(), any(), any())).thenReturn(relationships);
        when(relationships.hasNext()).thenReturn(true, false);
        when(relationships.next()).thenReturn(relationship);
        when(relationship.getLiveCopy()).thenReturn(liveCopy);
        when(liveCopy.getPath()).thenReturn("/liveCopyPath");

        List<String> result = liveCopySearchService.getLiveCopies(resourceResolver, "/path", StringUtils.EMPTY);

        assertEquals(1, result.size());
        assertEquals("/liveCopyPath", result.get(0));
    }

    @Test
    public void shouldHandleExceptionDuringLiveCopySearch() throws WCMException {
        when(liveRelationshipManager.getLiveRelationships(any(), any(), any())).thenThrow(new WCMException("error message"));

        List<String> result = liveCopySearchService.getLiveCopies(resourceResolver, "path", StringUtils.EMPTY);

        assertEquals(0, result.size());
    }
}