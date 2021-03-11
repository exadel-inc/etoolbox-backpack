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

package com.exadel.aembox.backpack.request.validator.impl;

import com.exadel.aembox.backpack.request.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link Validator} to test an HTTP request {@code ParameterMap} value expected to be a number
 */
public class IntNumberValidator implements Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntNumberValidator.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(final Object parameter) {
        if (parameter instanceof String[]) {
            String[] arrayParams = (String[]) parameter;
            if (arrayParams.length == 1) {
                try {
                    final int i = Integer.parseInt(arrayParams[0]);
                    return i >= 0;
                } catch (NumberFormatException e) {
                    LOGGER.error("Parse parameter exception", e);
                }
            }
        }
        return false;
    }
}
