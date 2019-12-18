<%
%>
<%@include file="/libs/granite/ui/global.jsp" %>
<%
%>
<%@include file="/libs/cq/gui/components/siteadmin/admin/createpagewizard/FilteringResourceWrapper.jsp"
%>
<%@page session="false"
        import="java.util.Iterator,
                com.adobe.granite.ui.components.AttrBuilder,
                com.adobe.granite.ui.components.Config,
                com.adobe.granite.ui.components.Tag,
                com.day.cq.wcm.api.components.Component" %>
<%@ page import="org.apache.sling.api.resource.Resource" %>
<%

    Config cfg = cmp.getConfig();
    String componentPath = cmp.getExpressionHelper().getString(cfg.get("component", String.class));
    Resource dialogContent = getDialogContent(componentPath, resourceResolver);

    if (dialogContent == null) {
        return;
    }

    Tag tag = cmp.consumeTag();

    AttrBuilder attrs = tag.getAttrs();
    cmp.populateCommonAttrs(attrs);

%>
<div <%= attrs %>><%
    for (Iterator<Resource> items = cmp.getItemDataSource().iterator(); items.hasNext(); ) {
%><sling:include resource="<%= items.next() %>"/><%
    }
%><sling:include resource="<%= new FilteringResourceWrapper(dialogContent) %>"/>
</div>
<%!

    private Resource getDialogContent(String componentPath, ResourceResolver resourceResolver) {
        Resource componentResource = resourceResolver.getResource(componentPath);
        if (componentResource == null) {
            return null;
        }

        Component component = componentResource.adaptTo(Component.class);
        if (component == null) {
            return null;
        }

        Resource dialog = resourceResolver.getResource(component.getPath() + "/cq:dialog");
        if (dialog == null) {
            return null;
        }

        return resourceResolver.getResource(dialog.getPath() + "/content");
    }
%>
