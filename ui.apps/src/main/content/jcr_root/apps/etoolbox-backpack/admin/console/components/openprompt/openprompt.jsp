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

<%@ include file="/libs/granite/ui/global.jsp" %><%
%><%@ page session="false"
           import="org.apache.sling.commons.json.io.JSONStringer,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.ExpressionHelper,
                  com.adobe.granite.ui.components.Tag" %>
<%

    Config cfg = cmp.getConfig();
    ExpressionHelper ex = cmp.getExpressionHelper();

    Tag tag = cmp.consumeTag();

    AttrBuilder attrs = tag.getAttrs();
    cmp.populateCommonAttrs(attrs);

    JSONStringer json = new JSONStringer();
    json.object();

    json.key("name").value("backpack.prompt.open");
    json.key("open").value(handleURITemplate(cfg, "open", ex, request));
    json.key("redirect").value(handleURITemplate(cfg, "redirect", ex, request));
    json.key("title").value(i18n.getVar(cfg.get("jcr:title", String.class)));
    json.key("message").value(i18n.getVar(cfg.get("text", String.class)));

    json.endObject();

    attrs.addClass("foundation-form-response-ui-success");
    attrs.add("data-foundation-form-response-ui-success", json.toString());

%><meta <%= attrs %>><%!

    private String handleURITemplate(Config cfg, String name, ExpressionHelper ex, HttpServletRequest request) {
        String value = ex.getString(cfg.get(name, String.class));

        if (value != null) {
            if (value.startsWith("/")) {
                return request.getContextPath() + value;
            } else {
                return value;
            }
        }

        value = ex.getString(cfg.get(name + ".abs", String.class));

        if (value != null) {
            return request.getContextPath() + value;
        } else {
            return value;
        }
    }
%>
