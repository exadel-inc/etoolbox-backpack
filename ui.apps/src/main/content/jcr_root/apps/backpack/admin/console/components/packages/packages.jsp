<%
%><%@include file="/libs/granite/ui/global.jsp"%><%
%><%@page session="false"
          import="com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.ExpressionHelper,
                  com.adobe.granite.ui.components.PagingIterator,
                  com.adobe.granite.ui.components.ds.AbstractDataSource,
                  com.adobe.granite.ui.components.ds.DataSource,
                  com.adobe.granite.ui.components.ds.EmptyDataSource,
                  org.apache.commons.collections.IteratorUtils,
                  org.apache.commons.collections.Predicate,
                  org.apache.commons.collections.Transformer,
                  org.apache.commons.collections.iterators.FilterIterator,
                  org.apache.commons.collections.iterators.TransformIterator,
                  org.apache.sling.api.resource.Resource,
                  org.apache.sling.api.resource.ResourceWrapper,
                  org.apache.commons.lang3.StringUtils,
                  java.util.Collections,
                  java.util.Comparator,
                  java.util.Iterator,
                  java.util.List"%>
<%@ page import="org.apache.jackrabbit.vault.packaging.JcrPackage" %>
<%@ page import="javax.jcr.Session" %>
<%@ page import="org.apache.jackrabbit.vault.packaging.JcrPackageManager" %>
<%@ page import="org.apache.jackrabbit.vault.packaging.PackagingService" %>
<%@ page import="javax.jcr.RepositoryException" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.apache.sling.api.resource.ResourceResolver" %>
<%
    final String ETC_PACKAGES = "/etc/packages/";

    ExpressionHelper ex = cmp.getExpressionHelper();
    Config dsCfg = new Config(resource.getChild(Config.DATASOURCE));

    String parentPath = ex.getString(dsCfg.get("path", String.class));
    if (StringUtils.isEmpty(parentPath) || parentPath.equals("/")) {
        parentPath = ETC_PACKAGES + "backpack";
    }

    if (StringUtils.isNotBlank(request.getParameter("group"))) {
        parentPath = request.getParameter("group");
    }

    final Integer offset = ex.get(dsCfg.get("offset", String.class), Integer.class);
    final Integer limit = ex.get(dsCfg.get("limit", String.class), Integer.class);

    final String sortName = StringUtils.defaultIfEmpty(slingRequest.getParameter("sortName"), "modified");
    final String sortDir = StringUtils.defaultIfEmpty(slingRequest.getParameter("sortDir"), sortName.equals("modified") ? "desc" : StringUtils.EMPTY);

    final Session session = resourceResolver.adaptTo(Session.class);
    final JcrPackageManager packageManager = PackagingService.getPackageManager(session);

    List<JcrPackage> packages = packageManager.listPackages(StringUtils.substringAfter(parentPath, ETC_PACKAGES), false);
    final Iterator<Resource> sortedChildrenIterator = getIterator(resourceResolver, packages, sortName, sortDir);
    final String itemRT = dsCfg.get("itemResourceType", String.class);

    @SuppressWarnings("unchecked")
    DataSource datasource = new AbstractDataSource() {
        public Iterator<Resource> iterator() {
            Iterator<Resource> it = new PagingIterator<Resource>(sortedChildrenIterator, offset, limit);

            return new TransformIterator(it, new Transformer() {
                public Object transform(Object o) {
                    Resource r = ((Resource) o);

                    return new ResourceWrapper(r) {
                        public String getResourceType() {
                            return itemRT;
                        }
                    };
                }
            });
        }
    };

    request.setAttribute(DataSource.class.getName(), datasource);
%>
<%!

    Iterator<Resource> getIterator(ResourceResolver resourceResolver, List<JcrPackage> packages, String sortName, String sortDir) {
        List<Resource> childrenList = new ArrayList<>();
        try {
            for (JcrPackage pkg : packages) {
                Resource r = resourceResolver.getResource(pkg.getNode().getPath());
                if (r != null) {
                    childrenList.add(r);
                }
            }
        } catch (RepositoryException ex) {
            //todo: handle ex
        }

        Comparator comparator = getComparator(sortName, "desc".equalsIgnoreCase(sortDir));
        Collections.sort(childrenList, comparator);
        return childrenList.iterator();
    }

    private Comparator<Resource> getComparator(final String sortName, final boolean reverse) {
        Comparator comparator = new Comparator<Resource>() {
            public int compare(Resource r1, Resource r2) {
                switch (sortName) {
                    case "name":
                        return compareProperty(r1, r2, "vlt:definition/name");
                    case "modified":
                        return compareProperty(r1, r2, "jcr:lastModified");
                    default:
                        return 0;
                }
            }
        };
        return reverse ? Collections.reverseOrder(comparator) : comparator;
    }

    private int compareProperty(Resource r1, Resource r2, String prop) {
        Resource cr1 = r1.getChild("jcr:content");
        if (cr1 == null) {
            cr1 = r1;
        }
        Resource cr2 = r2.getChild("jcr:content");
        if (cr2 == null) {
            cr2 = r2;
        }
        ValueMap vm1 = cr1.getValueMap();
        ValueMap vm2 = cr2.getValueMap();
        return vm1.get(prop, "").compareToIgnoreCase(vm2.get(prop, ""));
    }
%>
