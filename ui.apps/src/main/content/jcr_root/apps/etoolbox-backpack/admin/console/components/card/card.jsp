<%--
  ~ Licensed under the Apache License, Version 2.0 (the "License").
  ~ You may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%
%><%@include file="/libs/granite/ui/global.jsp"%><%
%><%@page session="false"%><%
%><%@page import="com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Tag,
                  org.apache.commons.lang.StringUtils,
                  org.apache.jackrabbit.util.Text,
                  javax.jcr.RepositoryException,
                  javax.jcr.Session,
                  javax.jcr.security.AccessControlManager,
                  javax.jcr.security.Privilege,
                  java.util.ArrayList,
                  java.util.List" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Optional" %>
<%

    AccessControlManager acm = null;
    try {
        acm = resourceResolver.adaptTo(Session.class).getAccessControlManager();
    } catch (RepositoryException e) {
        log.warn("Unable to get access manager", e);
    }

    String title = resource.getName();
    String path = resource.getPath();
    String actionRels = StringUtils.join(getActionRels(resource, acm), " ");
    double size = Optional.ofNullable(resource.getChild("jcr:content").getValueMap().get("jcr:data", Double.class)).orElse(0.0);

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();

    String thumbnailUrl = getThumbnailUrl(resource);
    if (thumbnailUrl.startsWith("/")) {
        thumbnailUrl = request.getContextPath() + thumbnailUrl;
    }

    attrs.addClass("foundation-collection-navigator");

    attrs.add("data-timeline", true);

%><coral-card class="foundation-collection-navigator backpack-card" data-foundation-collection-navigator-href="#" <%= attrs %>>

    <coral-card-asset class="coral-card-asset">
        <img src="<%= xssAPI.getValidHref(thumbnailUrl) %>" alt="<%= xssAPI.encodeForHTMLAttr(title) %>">
    </coral-card-asset>

    <coral-card-content><%
    %><coral-card-context><%= xssAPI.encodeForHTML("Package") %></coral-card-context><%
    %><coral-card-title class="foundation-collection-item-title"><%= xssAPI.encodeForHTML(title) %></coral-card-title><%

        Calendar created = resource.getValueMap().get("jcr:created", Calendar.class);
        Calendar modified = resource.getChild("jcr:content").getValueMap().get("jcr:lastModified", Calendar.class);

    %><coral-card-propertylist><%
        if (created != null) {
    %><coral-card-property icon="add" title="<%= xssAPI.encodeForHTMLAttr(i18n.get("Created")) %>">
        <foundation-time value="<%= xssAPI.encodeForHTMLAttr(created.toInstant().toString()) %>"></foundation-time>
    </coral-card-property><%
        }
        if (modified != null) {
    %><coral-card-property icon="edit" title="<%= xssAPI.encodeForHTMLAttr(i18n.get("Modified")) %>">
        <foundation-time value="<%= xssAPI.encodeForHTMLAttr(modified.toInstant().toString()) %>"></foundation-time>
    </coral-card-property>
        <%
            }

        %></coral-card-propertylist>
    </coral-card-content>
    <meta class="foundation-collection-quickactions" data-foundation-collection-quickactions-rel="<%= xssAPI.encodeForHTMLAttr(actionRels) %>">
    <link rel="properties" href="<%= xssAPI.getValidHref(request.getContextPath()) %>"/>
</coral-card>

<coral-quickactions target="_prev" alignmy="left top" alignat="left top">
    <coral-quickactions-item icon="dataRefresh" class="foundation-collection-action" data-foundation-collection-action='{"action": "cq.wcm.open", "data": {"cookiePath":"<%= request.getContextPath() %>/","href":"/tools/etoolbox/backpack/package.html?path=<%= xssAPI.getValidHref(path) %>"}}'><%= xssAPI.encodeForHTML(i18n.get("Package")) %></coral-quickactions-item>
    <coral-quickactions-item icon="dataRefresh" class="foundation-collection-action" data-foundation-collection-action='{"action": "cq.wcm.open", "data": {"cookiePath":"<%= request.getContextPath() %>/","href":"/apps/etoolbox-backpack/admin/new.html?packagePath=<%= xssAPI.getValidHref(path) %>"}}'><%= xssAPI.encodeForHTML(i18n.get("PackageV2")) %></coral-quickactions-item>

    <%

    if (resource != null && hasPermission(acm, resource, Privilege.JCR_READ)) {
%>
    <coral-quickactions-item icon="edit" class="foundation-collection-action" data-foundation-collection-action='{"action":"foundation.dialog","data":{"nesting":"hide","src":"/mnt/overlay/etoolbox-backpack/admin/console/page/content/editpackagedialog.html?packagePath=<%= xssAPI.getValidHref(path) %>"}}'
    ><%= xssAPI.encodeForHTML(i18n.get("Edit")) %></coral-quickactions-item><%
    }

    if (hasPermission(acm, resource, Privilege.JCR_REMOVE_NODE)) {
%>
    <coral-quickactions-item icon="delete" class="foundation-collection-action" data-foundation-collection-action='{"action": "cq.wcm.delete"}'
    ><%= xssAPI.encodeForHTML(i18n.get("Delete")) %></coral-quickactions-item>
    <%
        }
        if (resource != null && size > 0 && hasPermission(acm, resource, Privilege.JCR_READ)) {
    %>

    <coral-quickactions-item icon="download" class="foundation-collection-action"
                             data-foundation-collection-action='{"action": "backpack.download", "data": {"href":"/crx/packmgr/download.jsp?_charset_=utf-8&path=<%= xssAPI.getValidHref(path) %>"}}'
    ><%= xssAPI.encodeForHTML(i18n.get("Download")) %></coral-quickactions-item><%
    }
%></coral-quickactions>

<%!
    private String getThumbnailUrl(Resource resource) {
        return "/crx/packmgr/thumbnail.jsp?_charset_=utf-8&path=" + resource.getPath() + "&ck=" + Long.toString(System.currentTimeMillis());
    }

    private List<String> getActionRels(Resource resource, AccessControlManager acm) {
        List<String> actionRels = new ArrayList<>();

        actionRels.add("cq-siteadmin-admin-actions-edit-activator");
        actionRels.add("cq-siteadmin-admin-actions-properties-activator");

        if (hasPermission(acm, resource, Privilege.JCR_REMOVE_NODE)) {
            actionRels.add("cq-siteadmin-admin-actions-delete-activator");
        }

        if (hasPermission(acm, resource, Privilege.JCR_ADD_CHILD_NODES)) {
            actionRels.add("cq-siteadmin-admin-createlivecopy");
        }

        return actionRels;
    }

    private boolean hasPermission(AccessControlManager acm, String path, String privilege) {
        if (acm != null) {
            try {
                Privilege p = acm.privilegeFromName(privilege);
                return acm.hasPrivileges(path, new Privilege[]{p});
            } catch (RepositoryException ignore) {
            }
        }
        return false;
    }

    private boolean hasPermission(AccessControlManager acm, Resource resource, String privilege) {
        return hasPermission(acm, resource.getPath(), privilege);
    }
%>
