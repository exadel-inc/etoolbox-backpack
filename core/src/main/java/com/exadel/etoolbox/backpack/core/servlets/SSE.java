package com.exadel.etoolbox.backpack.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/services/backpack/sse",
                "sling.servlet.methods=get"
        })
public class SSE extends SlingSafeMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSE.class);
    public static List<String> messages = new ArrayList<>();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");

            if (!messages.isEmpty()) {
                List<String> temp = new ArrayList<>(messages);
                messages = new ArrayList<>();
                String join = String.join("!!!", temp);
                response.getWriter().println("data:" + join + "\n\n");
                response.getWriter().flush();
            }
    }
}
