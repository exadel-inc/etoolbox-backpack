package com.exadel.aem.backpack.core.services.impl;

import com.exadel.aem.request.RequestAdapter;
import com.exadel.aem.request.impl.RequestAdapterImpl;
import com.exadel.aem.request.validator.ValidatorResponse;
import org.osgi.service.component.annotations.Component;

import java.util.Map;

@Component(service = RequestAdapter.class)
public class RequestAdapterOsgi implements RequestAdapter {

    private RequestAdapter requestAdapter = new RequestAdapterImpl();

    @Override
    public <T> T adapt(final Map<String, Object> parameterMap, final Class<T> tClazz) {
        return requestAdapter.adapt(parameterMap, tClazz);
    }

    @Override
    public <T> ValidatorResponse<T> adaptValidate(final Map<String, Object> parameterMap, final Class<T> tClazz) {
        return requestAdapter.adaptValidate(parameterMap, tClazz);
    }
}
