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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.query.QueryResult;
import java.util.*;
import java.util.stream.LongStream;

/**
 * Implements {@link PackageSizeService} to provide base operation with package
 */
@Component(service = PackageSizeService.class)
public class PackageSizeServiceImpl implements PackageSizeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageSizeServiceImpl.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    final String USER = "backpack-service";
    Map<String, Object> paramMap;

    @Override
    public void calculateAverageSize() {
        Map<String, Long> packageSizes = new HashMap<>();
        paramMap = new HashMap<>();
        paramMap.put(ResourceResolverFactory.SUBSERVICE, USER);
        List<String> paths = new ArrayList<>();
        Node packageNode;
        NodeIterator localIterator;
        Node localNode;
        long size;
        Session session;
        String query = "SELECT base.* FROM [nt:resource] AS base WHERE ISDESCENDANTNODE(base,[/etc/packages]) " +
                "AND [jcr:mimeType] = 'application/zip'";
        try (ResourceResolver resourceResolver = resolverFactory.getServiceResourceResolver(paramMap)) {
            session = resourceResolver.adaptTo(Session.class);
            if (session != null) {
                NodeIterator nodeIterator = session.getWorkspace().getQueryManager().createQuery(query, "JCR-SQL2").execute().getNodes();
                while (nodeIterator.hasNext()) {
                    packageNode = nodeIterator.nextNode();
                    localNode = packageNode.getNode("vlt:definition");
                    if (localNode != null) {
                        localNode = localNode.getNode("filter");
                        if (localNode != null) {
                            localIterator = localNode.getNodes();
                            while (localIterator.hasNext()) {
                                paths.add(localIterator.nextNode().getProperty("root").getString());
                            }
                            size = Math.abs(getAverageSizeOfPackage(paths, session, packageNode.getProperty("jcr:data").getValue().getBinary().getSize()));
                            packageSizes.put(packageNode.getParent().getName(), size);
                            paths.clear();
                        }
                    }
                }
                setAverageSizeOfPackages(resourceResolver, packageSizes);
                session.save();
            }
        } catch (Exception e) {
            LOGGER.error("Cannot calculate average size", e);
        }
    }

    private long getAverageSizeOfPackage(List<String> paths, Session session, long dataSize) {
        StringBuilder query;
        NodeIterator nodeIterator;
        long nodesCount = 1L;
        if (paths.size() > 1) {
            query = new StringBuilder("SELECT * FROM [nt:base] AS a WHERE (");
            for (int i = 0; i < paths.size() - 1; i++) {
                query.append("ISDESCENDANTNODE([").append(paths.get(i)).append("]) OR ");
            }
            query.append("ISDESCENDANTNODE([").append(paths.get(paths.size() - 1)).append("]))");
        } else if (paths.size() == 0) {
            return nodesCount;
        } else {
            query = new StringBuilder("SELECT * FROM [nt:base] AS node WHERE ISDESCENDANTNODE([" + paths.get(0) + "])");
        }
        try {
            nodeIterator = session.getWorkspace().getQueryManager().createQuery(query.toString(), "JCR-SQL2").execute().getNodes();
            while (nodeIterator.hasNext()) {
                nodesCount++;
                nodeIterator.nextNode();
            }
            return (dataSize / nodesCount) / 50;
        } catch (Exception e) {
            LOGGER.error("This package contain too much nodes", e);
        }
        return nodesCount;
    }

    private void setAverageSizeOfPackages(final ResourceResolver resourceResolver, Map<String, Long> packageSizes) throws RepositoryException {
        boolean isNull = false;
        Resource resourcePackages = resourceResolver.getResource("/etc/packages");
        Node packageSizesNode = null;
        if (resourcePackages != null) {
            try {
                packageSizesNode = resourcePackages.getChild("packageSize").adaptTo(Node.class);
            } catch (NullPointerException e) {
                LOGGER.error("Package Sizes Node is null", e);
                isNull = true;
            }
            if (isNull) {
                packageSizesNode = resourcePackages.adaptTo(Node.class).addNode("packageSize", "nt:unstructured");
            }
            if (packageSizesNode != null) {
                OptionalDouble size = LongStream.of(packageSizes.values().stream().mapToLong(l -> l).toArray()).average();
                if (size.isPresent()) {
                    packageSizesNode.setProperty("AverageSize", (long) size.getAsDouble());
                    packageSizesNode.setProperty("Packages", Arrays.toString(packageSizes.keySet().toArray()));
                }

            }
        }
    }

    private long getAverageSizeOfPackages(final ResourceResolver resourceResolver) throws RepositoryException {
        try {
            Node size = resourceResolver.getResource("/etc/packages/packageSize").adaptTo(Node.class);
            return size.getProperty("AverageSize").getLong();
        } catch (NullPointerException e) {
            LOGGER.error("Node doesn't exist", e);
        }
        return 1L;
    }

    /**
     * Calculated package size
     *
     * @param paths List<String> representing where is located package descendant nodes.
     * @return Size of package - long.
     */
    public long getPackageSize(final ResourceResolver resourceResolver, List<String> paths) {
        Session session;
        long size = 0L;
        StringBuilder query;
        if (paths.size() > 1) {
            query = new StringBuilder("SELECT * FROM [nt:base] AS a WHERE (");
            for (int i = 0; i < paths.size() - 1; i++) {
                query.append("ISDESCENDANTNODE([").append(paths.get(i)).append("]) OR ");
            }
            query.append("ISDESCENDANTNODE([").append(paths.get(paths.size() - 1)).append("]))");
        } else if (paths.size() == 0) {
            return size;
        } else {
            query = new StringBuilder("SELECT * FROM [nt:base] AS node WHERE ISDESCENDANTNODE([" + paths.get(0) + "])");
        }
        try {
            session = resourceResolver.adaptTo(Session.class);
            if (session != null) {
                QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(query.toString(), "JCR-SQL2").execute();
                NodeIterator nodeIterator = queryResult.getNodes();
                long nodesCount = 0L;
                while (nodeIterator.hasNext()) {
                    nodeIterator.nextNode();
                    nodesCount++;
                }
                session.save();
                size = nodesCount * getAverageSizeOfPackages(resourceResolver);
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return size;
    }
}
