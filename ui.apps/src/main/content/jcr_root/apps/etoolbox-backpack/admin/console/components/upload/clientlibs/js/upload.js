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
    });
});

(function (window, $, URITemplate) {
    'use strict';

    const foundationUiAPI = $(window).adaptTo('foundation-ui');

    $(document).on('change', '.js-backpack-fileupload', function (event) {
        const files = event.target.querySelector('input').files;
        if (!files || !files[0]) return;
        const fileName = files[0].name;
        const input =event.target.closest('form').querySelector('.js-backpack-filename')
        input.value = fileName;
    });

    $(window).adaptTo('foundation-registry').register('foundation.form.response.ui.success', {
        name: 'foundation.prompt.open',
        handler: function (form, config, data) {
            const dataJson = JSON.parse(data);
            if (dataJson && !!dataJson.statusCode) errorPopup();
            else successPopup();

            function errorPopup() {
                const cancelHandler = () => open(URITemplate.expand(config.redirect, {}), '_self');
                foundationUiAPI.prompt('Error', dataJson.message, 'error', [{text: 'Cancel', handler: cancelHandler}]);
            }

            function successPopup() {
                const redirectHandler = () => open(URITemplate.expand(config.redirect, dataJson), '_self');
                const openHandler = () => open(URITemplate.expand(config.open, dataJson), '_self');
                foundationUiAPI.prompt(config.title, config.message, 'success', [{
                    text: 'Done',
                    handler: redirectHandler
                }, {
                    text: 'Open',
                    primary: true,
                    handler: openHandler
                }]);
            }
        }
    });

    $(window).adaptTo('foundation-registry').register('foundation.form.response.ui.error', {
        name: 'errorResponseCreated',
        handler: function (form, data, xhr) {
            let message = 'An error occurred while uploading the package.';
            try {
                const response = JSON.parse(xhr.responseText);
                if (response && response.log) {
                    message = response.log;
                }
            } catch (e) {
                console.error('Failed to parse responseText:', e);
            }

            foundationUiAPI.alert('Error', message, 'error');
        }
    });
})(window, Granite.$, Granite.URITemplate);
