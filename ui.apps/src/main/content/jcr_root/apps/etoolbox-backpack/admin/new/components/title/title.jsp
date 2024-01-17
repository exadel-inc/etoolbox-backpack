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
<%@ include file="/libs/granite/ui/global.jsp" %>
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