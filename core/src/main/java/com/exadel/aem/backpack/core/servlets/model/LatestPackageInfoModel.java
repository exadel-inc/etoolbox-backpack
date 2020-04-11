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

package com.exadel.aem.backpack.core.servlets.model;

import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;
import com.exadel.aem.request.annotations.Validate;
import com.exadel.aem.request.validator.impl.RequiredValidator;
import com.exadel.aem.request.validator.impl.WholeNumberValidator;

@RequestMapping
public class LatestPackageInfoModel extends PackageInfoModel {

    @RequestParam
    @Validate(validator = {RequiredValidator.class, WholeNumberValidator.class},
            invalidMessages = {"Latest log index field is required", "Latest log index must be whole number!"})
    private int latestLogIndex;

    public int getLatestLogIndex() {
        return latestLogIndex;
    }

    public void setLatestLogIndex(final int latestLogIndex) {
        this.latestLogIndex = latestLogIndex;
    }
}
