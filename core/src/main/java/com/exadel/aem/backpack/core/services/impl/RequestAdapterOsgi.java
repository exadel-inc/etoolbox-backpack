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

package com.exadel.aem.backpack.core.services.impl;

import com.exadel.aem.request.RequestAdapter;
import com.exadel.aem.request.impl.RequestAdapterImpl;
import com.exadel.aem.request.validator.ValidatorResponse;
import org.osgi.service.component.annotations.Component;

/**
 * Implements {@link RequestAdapter} to adapt user-defined {@code SlingHttpServletRequest} parameters to a data model object
 * which is then used in operations by {@link com.exadel.aem.backpack.core.services.PackageService}
 */
@Component(service = RequestAdapter.class)
public class RequestAdapterOsgi implements RequestAdapter {

    private RequestAdapter requestAdapter = new RequestAdapterImpl();

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T adapt(final Map<String, Object> parameterMap, final Class<T> tClazz) {
        return requestAdapter.adapt(parameterMap, tClazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ValidatorResponse<T> adaptValidate(final Map<String, Object> parameterMap, final Class<T> tClazz) {
        return requestAdapter.adaptValidate(parameterMap, tClazz);
    }
}
