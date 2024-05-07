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

package com.exadel.etoolbox.backpack.core.services.resource.impl;

import com.day.cq.commons.PathInfo;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Template;
import com.exadel.etoolbox.backpack.core.services.config.PageReferenceSearchConfig;
import com.exadel.etoolbox.backpack.core.services.resource.PageReferenceSearchService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.exadel.etoolbox.backpack.core.services.config.PageReferenceSearchConfig.DEFAULT_PAGE_ROOT_PATH;

/**
 * Implements {@link PageReferenceSearchService} to facilitate getting a collection of pages used by resources
 * under specified JCR path
 */
@Component(name = "Page Reference Search", service = PageReferenceSearchService.class, immediate = true)
@Designate(ocd = PageReferenceSearchConfig.class)
public class PageReferenceSearchServiceImpl implements PageReferenceSearchService {

    private Pattern resourcePathPattern = Pattern.compile("([\"']|^)("
            + Pattern.quote(DEFAULT_PAGE_ROOT_PATH) + "[\\S]*?)([\"']|$)");

    private List<String> ignoreTemplates;
    private List<String> includeTemplates;

    @Activate
    @Modified
    protected void activate(PageReferenceSearchConfig config) {
        resourcePathPattern = Pattern.compile("([\"']|^)(" + Pattern.quote(config.rootPathOfPagesSearch()) + "[\\S]*?)([\"']|$)");
        ignoreTemplates = getListConfig(config.ignoreTemplatePaths());
        includeTemplates = getListConfig(config.includeTemplatePaths());
    }

    private List<String> getListConfig(final String[] config) {
        if (config != null) {
            return Arrays.asList(config).stream().filter(s -> StringUtils.isNotBlank(s)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Page> findPageReferences(final ResourceResolver resourceResolver, final String searchPath) {
        Set<Page> references = new LinkedHashSet<>();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        Resource resource = resourceResolver.getResource(searchPath);
        if (resource == null) {
            return references;
        }
        search(resource, references, pageManager);
        return filterPages(references, resource);
    }

    /**
     * Search referenced pages under specific resource path recursively
     *
     * @param resource    under which to search
     * @param pages       Set of found referenced pages
     * @param pageManager instance of the{@link PageManager}
     */
    private void search(final Resource resource, Set<Page> pages, PageManager pageManager) {
        findReferences(resource, pages, pageManager);
        Iterator<Resource> iter = resource.listChildren();
        while (iter.hasNext()) {
            search(iter.next(), pages, pageManager);
        }
    }

    /**
     * Find referenced pages for specific resource
     *
     * @param resource    under which to search
     * @param pages       Set of found referenced pages
     * @param pageManager instance of the{@link PageManager}
     */
    private void findReferences(Resource resource,
                                Set<Page> pages,
                                PageManager pageManager) {
        ValueMap map = resource.getValueMap();
        for (Object value : map.values()) {
            if (value instanceof String) {
                String strValue = (String) value;
                addPagesFromPropertyValue(strValue, pages, pageManager, resource.getResourceResolver());
            } else if (value instanceof String[]) {
                for (String strValue : (String[]) value) {
                    addPagesFromPropertyValue(strValue, pages, pageManager, resource.getResourceResolver());
                }
            }
        }
    }

    /**
     * Retrieve page references from single property
     *
     * @param strValue         property value
     * @param pages            Set of found referenced pages
     * @param pageManager      instance of the{@link PageManager}
     * @param resourceResolver instance of the{@link ResourceResolver}
     */
    private void addPagesFromPropertyValue(final String strValue,
                                           final Set<Page> pages,
                                           final PageManager pageManager,
                                           final ResourceResolver resourceResolver) {
        Matcher matcher = resourcePathPattern.matcher(strValue);
        while (matcher.find()) {
            Resource resource = resourceResolver.resolve(decode(matcher.group(2)));
            Page page = pageManager.getContainingPage(resource);
            if (page != null) {
                pages.add(page);
            }
        }
    }

    /**
     * Filter pages by templates and exclude from found pages current resource if any found
     *
     * @param pages           Set of pages to filter
     * @param initialResource An initial resource under which search occurred
     * @return Set of filtered pages
     */
    private Set<Page> filterPages(final Set<Page> pages, final Resource initialResource) {
        Set<Page> filteredPages = new LinkedHashSet<>();
        for (Page page : pages) {
            Resource contentResource = page.getContentResource();
            if (contentResource != null
                    && !page.getPath().equals(initialResource.getPath())
                    && !page.getContentResource().getPath().equals(initialResource.getPath())) {
                if (!ignoreTemplates.isEmpty() || !includeTemplates.isEmpty()) {
                    filterByTemplate(filteredPages, page);
                } else {
                    filteredPages.add(page);
                }
            }
        }
        return filteredPages;
    }

    /**
     * Filter Set of pages by templates.
     * With ignoreTemplates configuration: all pages with any template will be included
     * except for pages with templates specified in ignoreTemplates configuration.
     * With includeTemplates: all pages with any template specified in includeTemplates configuration will be included
     * pages with any other template or without a template will be ignored.
     * In the case when both ignoreTemplates and includeTemplates are present ignoreTemplates will have priority and includeTemplates will be ignored.
     *
     * @param pages Set of filtered pages.
     * @param page  Page to check.
     */
    private void filterByTemplate(final Set<Page> pages, final Page page) {
        Template template = page.getTemplate();
        if (template != null) {
            String templatePath = template.getPath();
            if (!ignoreTemplates.isEmpty()) {
                if (!ignoreTemplates.contains(templatePath)) {
                    pages.add(page);
                }
            } else if (!includeTemplates.isEmpty()) {
                if (includeTemplates.contains(templatePath)) {
                    pages.add(page);
                }
            }
        }
    }

    /**
     * Decode URL and get Page path.
     *
     * @param url URL to decode.
     * @return Decoded resource path.
     */
    private String decode(String url) {
        return new PathInfo(url).getResourcePath();
    }
}

