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

package com.exadel.aembox.backpack.core.model;

import com.google.gson.Gson;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Required;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.inject.Inject;
import javax.inject.Named;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Represents the open prompt props.
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class OpenPromptModel {

    private String name = "backpack.prompt.open";

    @Self
    @Required
    private transient Resource resource;

    @Inject
    @Default(values = EMPTY)
    private String open;

    @Inject
    @Default(values = EMPTY)
    private String redirect;

    @Inject
    @Default(values = EMPTY)
    @Named("jcr:title")
    private String title;

    @Inject
    @Default(values = EMPTY)
    @Named("text")
    private String message;

    /**
     * Gets name of open-promp foundation-registry
     *
     * @return String value
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the URI Template to open the newly created resource
     *
     * @return String value
     */
    public String getOpen() {
        return open;
    }

    /**
     * Gets the URI Template to redirect to
     *
     * @return String value
     */
    public String getRedirect() {
        return redirect;
    }

    /**
     * Gets the title of the prompt
     *
     * @return String value
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the message of the prompt
     *
     * @return String value
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets JSON representation of current object
     *
     * @return String value
     */
    public String getJson() {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(this);

        return jsonStr.replaceAll("\"", "&quot;");
    }
}
