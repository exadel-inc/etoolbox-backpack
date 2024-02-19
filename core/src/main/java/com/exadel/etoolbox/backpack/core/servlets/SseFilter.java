package com.exadel.etoolbox.backpack.core.servlets;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import java.util.Dictionary;
import java.util.Hashtable;


@Component(immediate = true)
public class SseFilter extends Filter<ILoggingEvent> {
    private static final String PROPERTY_APPENDERS = "appenders";
    private ServiceRegistration<Filter> filterRegistration;

    @Activate
    @Modified
    private void activate(ComponentContext context) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(PROPERTY_APPENDERS, "*");
        filterRegistration = context.getBundleContext().registerService(
                Filter.class,
                this,
                properties);
    }

    private void unregisterFilter() {
        if (filterRegistration != null) {
            filterRegistration.unregister();
            filterRegistration = null;
        }
    }

    @Deactivate
    protected void deactivate() {
       unregisterFilter();
    }

    @Override
    public FilterReply decide(ILoggingEvent iLoggingEvent) {
        String message = iLoggingEvent.getMessage();
        SSE.messages.add(message);
        final IThrowableProxy throwableProxy = iLoggingEvent.getThrowableProxy();
        if (throwableProxy instanceof ThrowableProxy) {
            return FilterReply.DENY;
        }
        return FilterReply.ACCEPT;
    }
}