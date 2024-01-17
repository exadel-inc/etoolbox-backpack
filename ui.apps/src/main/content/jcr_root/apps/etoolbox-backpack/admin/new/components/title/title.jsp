<%@ include file="/libs/granite/ui/global.jsp" %><%
%>
<%@ page import="javax.jcr.Session" %>
<%@ page import="javax.jcr.RepositoryException" %>
<%@ page import="org.apache.sling.api.resource.ResourceResolver" %>
<%@ page import="org.apache.jackrabbit.vault.packaging.PackagingService" %>
<%@ page import="org.apache.jackrabbit.vault.packaging.JcrPackageManager" %>
<%@ page import="org.apache.jackrabbit.vault.packaging.JcrPackage" %>
<%@ page import="org.apache.jackrabbit.vault.packaging.JcrPackageDefinition" %>
<%@ page import="org.apache.jackrabbit.vault.packaging.PackageId" %>
<%

    final String path = slingRequest.getParameter("path");

    if (path == null) {
        out.print(xssAPI.encodeForHTML(""));
        return;
    }

    final Session session = slingRequest.getResourceResolver().adaptTo(Session.class);
    final JcrPackageManager packageManager = PackagingService.getPackageManager(session);
    JcrPackage jcrPackage = null;

    try {
        jcrPackage = packageManager.open(session.getNode(path), false);
        out.print(xssAPI.encodeForHTML(jcrPackage.getDefinition().getId().getName()));
    } catch (RepositoryException ex) {
        out.print(xssAPI.encodeForHTML(""));
    } finally {
        if (jcrPackage != null) {
            jcrPackage.close();
        }
    }
%>