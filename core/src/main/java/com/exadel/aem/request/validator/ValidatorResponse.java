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

package com.exadel.aem.request.validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Data object wrapping a model adatped from an HTTP request together with the result of its validation and an optional
 * log message
 * @param <T> Generic type of the underlying data model
 */
public class ValidatorResponse<T> {

    private boolean valid;
    private List<String> log = new ArrayList<>();
    private T model;

    /**
     * Gets whether the underlying data model has passed validation and proven valid
     * @return True or false
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // for semantic contingency reasons
    public boolean isValid() {
        return valid;
    }

    /**
     * Sets whether the underlying data model has passed validation and proven valid
     * @param valid Boolean value
     */
    public void setValid(final boolean valid) {
        this.valid = valid;
    }

    /**
     * Gets the collection of strings representing validation message(-s) for the underlying data model
     * @return {@code List<String>} value
     */
    public List<String> getLog() {
        return log;
    }

    /**
     * Sets the collection of strings representing validation message(-s) for the underlying data model
     * @param log @code List<String>} value
     */
    public void setLog(final List<String> log) {
        this.log = log;
    }

    /**
     * Gets the data model associated with this instance
     * @return {@code <T>}-typed data model object
     */
    public T getModel() {
        return model;
    }

    /**
     * Sets the data model associated with this instance
     * @param model {@code <T>}-typed data model object
     */
    public void setModel(final T model) {
        this.model = model;
    }
}
