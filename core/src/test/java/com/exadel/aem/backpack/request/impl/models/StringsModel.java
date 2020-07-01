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

package com.exadel.aem.backpack.request.impl.models;

import com.exadel.aem.backpack.request.annotations.RequestMapping;
import com.exadel.aem.backpack.request.annotations.RequestParam;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RequestMapping
@SuppressWarnings("UnusedDeclaration") // contains directly injected fields
public class StringsModel {

    @RequestParam
    private String string;

    @RequestParam
    private StringBuilder stringBuilder;

    @RequestParam
    private StringBuffer stringBuffer;

    public String getString() {
        return string;
    }

    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }

    public StringBuffer getStringBuffer() {
        return stringBuffer;
    }

    @PostConstruct
    private void init() throws UnsupportedEncodingException {
        if (StringUtils.isNotBlank(string)) {
            string = URLDecoder.decode(string, StandardCharsets.UTF_8.displayName());
        }
    }
}
