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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RequestMapping
public class PackageInfoModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageInfoModel.class);


    @RequestParam(name = "path")
    @Validate(validator = RequiredValidator.class,
            invalidMessages = "Path field is required")
    private String packagePath;

    public String getPackagePath() {
        return packagePath;
    }

    public void setPackagePath(final String packagePath) {
        this.packagePath = packagePath;
    }

    @PostConstruct
    public void init() {
        if (StringUtils.isNotBlank(packagePath)) {
            try {
                packagePath = URLDecoder.decode(packagePath, StandardCharsets.UTF_8.displayName());
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("Path decode exception", e);
            }
        }
    }
}
