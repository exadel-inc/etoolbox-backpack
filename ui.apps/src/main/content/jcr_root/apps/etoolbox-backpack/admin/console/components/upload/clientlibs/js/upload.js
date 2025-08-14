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

(function (window, $) {
    'use strict';

    $(document).on('change', '.js-backpack-fileupload', function (event) {
        const files = event.target.querySelector('input').files;
        if (!files || !files[0]) return;
        const fileName = files[0].name;
        const input =event.target.closest('form').querySelector('.js-backpack-filename')
        input.value = fileName;
    });

    $(window).adaptTo('foundation-registry').register('foundation.form.response.parser', {
        name: 'foundation.json',
        contentType: /application\/json/,
        selector: '.js-backpack-package-form',
        handler: function(form, xhr) {
            return JSON.parse(xhr.responseText);
        }
    });
})(window, Granite.$);
