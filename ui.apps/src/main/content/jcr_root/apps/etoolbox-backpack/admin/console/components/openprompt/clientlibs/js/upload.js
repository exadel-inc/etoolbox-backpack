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

$(function () {
    $('#indexTypeSelect').change(function () {
        $(this).closest('form').submit();
    })
});

(function (window, $, URITemplate) {
    'use strict';

    $(window).adaptTo('foundation-registry').register('foundation.form.response.ui.success', {
        name: 'backpack.prompt.open',
        handler: function (form, config, data, textStatus, xhr) {
            function successPopup(ui, dataJson) {
                ui.prompt(config.title, config.message, 'success', [{
                    text: Granite.I18n.get('Done'),
                    handler: function () {
                        window.location.href = URITemplate.expand(config.redirect, dataJson);
                    }
                }, {
                    text: Granite.I18n.get('Open'),
                    primary: true,
                    handler: function () {
                        open(URITemplate.expand(config.open, dataJson), true);
                        window.location.href = URITemplate.expand(config.redirect, dataJson);
                    }
                }]);
            }

            var ui = $(window).adaptTo('foundation-ui'),
                dataJson = JSON.parse(data);

            successPopup(ui, dataJson);

        }
    });
})(window, Granite.$, Granite.URITemplate);