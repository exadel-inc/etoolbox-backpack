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

import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.request.annotations.RequestMapping;
import com.exadel.aem.backpack.request.annotations.RequestParam;
import com.exadel.aem.backpack.request.annotations.Validate;
import com.exadel.aem.backpack.request.validator.impl.RequiredValidator;
import com.exadel.aem.backpack.request.validator.impl.IntNumberValidator;

/**
 * Represents user-defined options supplied to a request for a rolling package build status information.
 * Upon initialization, passed as a parameter to the {@link PackageService#getLatestPackageBuildInfo(LatestPackageInfoModel)}
 * @see com.exadel.aem.backpack.core.servlets.BuildPackageServlet
 */
@RequestMapping
public class LatestPackageInfoModel extends PackageInfoModel {

    @RequestParam
    @Validate(validator = {RequiredValidator.class, IntNumberValidator.class},
            invalidMessages = {"Latest log index field is required", "Latest log index must be integer number!"})
    private int latestLogIndex;

    /**
     * Gets position in log flow to start output from
     * @return Integer value, non-negative
     */
    public int getLatestLogIndex() {
        return latestLogIndex;
    }

    /**
     * Sets position in log flow to start output from
     * @param latestLogIndex Integer value, non-negative number expected
     */
    public void setLatestLogIndex(final int latestLogIndex) {
        this.latestLogIndex = latestLogIndex;
    }
}
