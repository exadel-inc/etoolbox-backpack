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

package com.exadel.aembox.backpack.request;

import com.exadel.aembox.backpack.request.validator.ValidatorResponse;

import java.util.Map;

/**
 * Represents a service used to adapt user-defined {@code SlingHttpServletRequest} parameters to a data model object.
 * The data model can be subsequently validated by a specific routine as required.
 */
@SuppressWarnings("unused") // exposes methods as part of public API
public interface RequestAdapter {

    /**
     * Adapts parameters of a {@code SlingHttpServletRequest} to a specified data model object
     *
     * @param parameterMap {@code Map} representing parameters of a request
     * @param tClazz       Class object representing the data model
     * @param <T>          Generic parameter representing the type of data model
     * @return {@code <T>}-typed data object
     */
    <T> T adapt(Map<?, ?> parameterMap, Class<T> tClazz);

    /**
     * Adapts parameters of a {@code SlingHttpServletRequest} to a specified data model object and then validates
     * the resulting object
     *
     * @param parameterMap {@code Map} representing parameters of a request
     * @param tClazz       Class object representing the data model
     * @param <T>          Generic parameter representing the type of data model
     * @return {@code ValidatorResponse} containing the data model together with its validation report
     */
    <T> ValidatorResponse<T> adaptValidate(Map<?, ?> parameterMap, Class<T> tClazz);
}
