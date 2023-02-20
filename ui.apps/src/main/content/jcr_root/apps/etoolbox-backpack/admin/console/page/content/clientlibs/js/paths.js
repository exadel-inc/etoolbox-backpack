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
    "use strict";

    $(document).on("click", '.js-create-package-activator', function(event) {
        var dialog = document.querySelector('#createPackageDialog');
        if (!dialog) {
            var path = $('.js-create-package-activator').data('path');
            $.ajax({
                type: "GET",
                url: '/apps/etoolbox-backpack/admin/console/page/content/createpagedialog.html?paths=' + path,
                success: function(data) {
                    var doc = new DOMParser().parseFromString(data, 'text/html');
                    document.body.appendChild(doc.body.firstChild);
                }
            });
        }
        setTimeout(function() {
            dialog = document.querySelector('#createPackageDialog');
            fillPaths();
            dialog.show();
        }, 300);
    });

    $(document).on("foundation-contentloaded", function() {

        fillPaths();

        var switchField = $('#create-package-switch')[0];
        $(switchField).on('change', function(e){
            $('#create-package-multifield foundation-autocomplete').each(function (){
                var isChecked = switchField.hasAttribute('checked');
                $(this).attr('required',!isChecked);
            });
        })
    });

    function fillPaths() {
        var multifield = $('#create-package-multifield')[0];
        if (multifield && multifield.items.length === 0) {
            var paths = multifield.getAttribute('data-paths');
            paths = paths.split(',');
            paths.forEach(function (part, index) {
                var url = "/apps/etoolbox-backpack/admin/console/page/content/createpagedialog/items/form/items/content/items/column/items/pathContainer/items/paths/field.html?path=" + part;
                $.ajax({
                    type: "GET",
                    url: url,
                    success: function(data) {
                        multifield.items.add({
                            content: {
                                innerHTML: data
                            }
                        });
                    }
                });
            });
        }
    }
});