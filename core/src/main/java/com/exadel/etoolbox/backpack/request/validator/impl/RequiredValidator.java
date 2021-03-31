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

package com.exadel.etoolbox.backpack.request.validator.impl;

import com.exadel.etoolbox.backpack.request.validator.Validator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Implements {@link Validator} to test an HTTP request {@code ParameterMap} value expected to a non-blank {@code String}
 */
public class RequiredValidator implements Validator {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(final Object parameter) {
        if (parameter instanceof String[]) {
            String[] arrayParams = (String[]) parameter;
            return ArrayUtils.isNotEmpty(arrayParams) && StringUtils.isNotBlank(arrayParams[0]);
        } else if (parameter instanceof List) {
            List<?> multifieldObject = (List) parameter;
            return !multifieldObject.isEmpty();
        }
        return false;
    }
}
