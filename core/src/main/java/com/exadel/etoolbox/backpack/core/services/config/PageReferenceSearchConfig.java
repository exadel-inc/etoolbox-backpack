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

package com.exadel.etoolbox.backpack.core.services.config;

import com.exadel.etoolbox.backpack.core.services.impl.PageReferenceSearchServiceImpl;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Represents {@link PageReferenceSearchServiceImpl} OSGi service's configuration
 */
@ObjectClassDefinition(name = "Page Reference Service Search Configuration")
public @interface PageReferenceSearchConfig {
    String DEFAULT_PAGE_ROOT_PATH = "/content/";


    @AttributeDefinition(
            name = "Root Path Of Pages Search",
            description = "Root path which will be used to search for internal links",
            type = AttributeType.STRING)
    String rootPathOfPagesSearch() default DEFAULT_PAGE_ROOT_PATH;

    @AttributeDefinition(
            name = "Ignore Template Paths",
            description = "Templates that will be ignored during page reference search." +
                    "Only 'Ignore Template Paths' or 'Include Template Paths' can be used." +
                    "In the case when both are configured 'Include Template Paths' will be ignored.",
            type = AttributeType.STRING)
    String[] ignoreTemplatePaths();

    @AttributeDefinition(
            name = "Include Template Paths",
            description = "Templates that will be included during page reference search." +
                    "Only 'Ignore Template Paths' or 'Include Template Paths' can be used." +
                    "In the case when both are configured 'Include Template Paths' will be ignored.",
            type = AttributeType.STRING)
    String[] includeTemplatePaths();
}


