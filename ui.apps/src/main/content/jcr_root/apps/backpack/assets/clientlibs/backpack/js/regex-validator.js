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

(function(window) {

    var REGEX_ATTR = 'data-regex';
    var VALIDATION_MSG_ATTR = 'data-validation-message';

    $(window).adaptTo("foundation-registry").register("foundation.validation.validator", {
        selector: '[' + REGEX_ATTR + ']',
        validate: function(el) {
            if (!el.value) {
                return;
            }
            var regex = new RegExp(el.getAttribute(REGEX_ATTR));
            var validationMsg = el.getAttribute(VALIDATION_MSG_ATTR) || 'Invalid field value';

            if(!el.value.match(regex)) {
                return validationMsg;
            }
        }
    });
})(window);