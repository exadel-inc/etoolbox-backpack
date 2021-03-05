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

package com.exadel.aembox.backpack.request.annotations;

import com.exadel.aembox.backpack.request.validator.Validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field within a data model class, the value of which needs to be validated by a specific routine upon
 * adaptation from a {@link org.apache.sling.api.SlingHttpServletRequest} object
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Validate {
    /**
     * Gets zero or more validators assigned to this field
     * @return An array of classes extending {@link Validator}, or null
     */
    Class<? extends Validator>[] validator() default {};

    /**
     * Gets zero or more messages informing that validation has failed
     * @return An array of strings, or null
     */
    String[] invalidMessages() default {};
}
