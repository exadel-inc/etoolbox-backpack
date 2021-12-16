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

import com.exadel.etoolbox.backpack.core.services.pckg.PackageSizeService;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.query.Query;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Implements {@link PackageSizeService} to provide base operation with package size
 */
@Component(service = PackageSizeService.class)
public class PackageSizeServiceImpl implements PackageSizeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageSizeServiceImpl.class);

    private static final String USER = "backpack-service";
    private static final Map<String, Long> packageSizes = new HashMap<>();
    private static final Map<String, Object> paramMap = new HashMap<>();
    private static final String query = "SELECT base.* FROM [nt:resource] AS base WHERE ISDESCENDANTNODE(base,[/etc/packages]) AND [jcr:mimeType] = 'application/zip'";
    private static long averageNodeSize = 0;

    @Reference
    private ResourceResolverFactory resolverFactory;

    /**
     * Calculates package size
     *
     * @param resourceResolver Current {@code ResourceResolver} object
     * @param paths List<String> enumerating addresses of package's descendant nodes
     * @return Size of package - long
     */
    @Override
    public long getPackageSize(final ResourceResolver resourceResolver, List<String> paths) {
        long nodeCounts = getNodesCount(resourceResolver, paths);
        if (averageNodeSize != 0) {
            return nodeCounts * averageNodeSize;
        }
        Resource sizeResource = resourceResolver.getResource("/var/etoolbox-backpack");
        if (sizeResource == null) {
            LOGGER.warn("Could not get package size");
            return 0L;
        }
        try {
            Node sizeNode = sizeResource.adaptTo(Node.class);
            Property averageSize = sizeNode.getProperty("averageSize");
            if (averageSize == null) {
                LOGGER.warn("Could not get average size");
                return 0L;
            }
            averageNodeSize = averageSize.getLong();
            return nodeCounts * averageNodeSize;
        } catch (RepositoryException e) {
            LOGGER.error("Could not get package size", e);
        }
        LOGGER.warn("Could not get package size");
        return 0L;
    }

    /**
     * Calculates average size of node in compilated packages
     */
    @Override
    public void calculateAverageSize() {
        paramMap.put(ResourceResolverFactory.SUBSERVICE, USER);
        Session session;
        try (ResourceResolver resourceResolver = resolverFactory.getServiceResourceResolver(paramMap)) {
            session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                LOGGER.warn("Session is null");
                return;
            }
            NodeIterator nodeIterator = session.getWorkspace().getQueryManager().createQuery(query, Query.JCR_SQL2).execute().getNodes();
            while (nodeIterator.hasNext()) {
                Node packageNode = nodeIterator.nextNode();
                NodeIterator localIterator = getChildNodes(packageNode);
                List<String> paths = new ArrayList<>();
                while (localIterator.hasNext()) {
                    paths.add(localIterator.nextNode().getProperty("root").getString());
                }
                long dataSize = Math.abs(packageNode.getProperty("jcr:data").getValue().getBinary().getSize()) / 76;
                long nodesCount = getNodesCount(resourceResolver, paths);
                long size = (dataSize / nodesCount);
                packageSizes.put(packageNode.getParent().getName(), size);
                paths.clear();
            }
            setAverageSizeOfPackages(resourceResolver);
            session.save();
        } catch (Exception e) {
            LOGGER.error("Cannot calculate average size", e);
        }
    }

    /**
     * Called by {@link PackageSizeServiceImpl#calculateAverageSize()}
     * for getting child nodes
     *
     * @param node for whom we get child nodes
     * @return instance of the{@link NodeIterator}
     * @throws RepositoryException in case {@code Node} doesn't have child nodes
     */
    private NodeIterator getChildNodes(Node node) throws RepositoryException {
        NodeIterator nodeIterator = NodeIteratorAdapter.EMPTY;
        Node childNode = node.getNode("vlt:definition");
        if (childNode == null) {
            LOGGER.warn("Child node vlt:definition is null");
            return nodeIterator;
        }
        childNode = childNode.getNode("filter");
        if (childNode == null) {
            LOGGER.warn("Child node filter is null");
            return nodeIterator;
        }
        return childNode.getNodes();
    }

    /**
     * Called by {@link PackageSizeServiceImpl#calculateAverageSize()} in order to save result in JcrRepository
     *
     * @param resourceResolver Current {@code ResourceResolver} object
     * @throws RepositoryException in cases where we cannot add the node or set property for it.
     */
    private void setAverageSizeOfPackages(final ResourceResolver resourceResolver) throws RepositoryException {
        final String packageSize = "etoolbox-backpack";
        Resource resourcePackages = resourceResolver.getResource("/var");
        if (resourcePackages == null) {
            return;
        }
        Resource packageSizesResource = resourcePackages.getChild(packageSize);
        Node packageSizesNode;
        if (packageSizesResource == null) {
            packageSizesNode = resourcePackages.adaptTo(Node.class).addNode(packageSize, JcrConstants.NT_UNSTRUCTURED);
        } else {
            packageSizesNode = packageSizesResource.adaptTo(Node.class);
        }
        OptionalDouble size = LongStream.of(packageSizes.values().stream().mapToLong(x -> x).toArray()).average();
        if (size.isPresent()) {
            averageNodeSize = (long) size.getAsDouble();
            packageSizesNode.setProperty("averageSize", averageNodeSize);
        }
    }

    /**
     * Called by {@link PackageSizeServiceImpl#getPackageSize(ResourceResolver, List)} and {@link PackageSizeServiceImpl#calculateAverageSize()}
     * for getting count of all nodes in package
     *
     * @param resourceResolver Current {@code ResourceResolver} object
     * @param paths List<String> enumerating addresses of package's descendant nodes
     * @return count of nodes
     */
    private long getNodesCount(final ResourceResolver resourceResolver, List<String> paths) {
        ResourceFinder finder = new ResourceFinder();
        List<Resource> resources = paths.stream().map(resourceResolver::getResource).collect(Collectors.toList());
        resources.forEach(finder::accept);
        return finder.getResult();
    }

    /**
     * Implementation of traversing for {@link PackageSizeServiceImpl#getNodesCount(ResourceResolver, List)}
     */
    private static class ResourceFinder extends AbstractResourceVisitor {
        private boolean isFirstCall = true;
        private long result;

        public long getResult() {
            return result;
        }

        @Override
        public void accept(Resource resource) {
            if (resource == null) {
                return;
            }
            visit(resource);
            if (isFirstCall) {
                isFirstCall = false;
                Iterator<Resource> children = resource.listChildren();
                traverseChildren(children);
            } else {
                traverseChildren(resource.listChildren());
            }
        }

        @Override
        protected void visit(Resource resource) {
            result++;
        }
    }
}
