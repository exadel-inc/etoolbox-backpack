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

package com.exadel.aembox.backpack.request.validator;

/**
 * Represents a routine testing an arbitrary object coming from a HTTP request's parameter map for validity
 */
public interface Validator {

    /**
     * Gets whether the supplied object is valid according to some testing logic
     * @param parameter An object coming from an HTTP request's {@code ParameterMap}
     * @return True or false
     */
    boolean isValid(Object parameter);
}
