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

(function () {
    $(window).adaptTo('foundation-registry').register('foundation.collection.action.action', {
        name: 'backpack.delete',
        handler: function (name, el, config) {
            const ui = $(window).adaptTo('foundation-ui');
            const message = $('<div>');

            const title = $('<p>').text('You are going to delete the following package:');
            const packageInfo = $('<p>').append($('<b>').text(config.data.packagePath));
            message.append(title, packageInfo);

            const deleteBtn = {
                text: 'Delete',
                warning: true,
                handler: () => deleteAction(config.data.packagePath)
            };

            ui.prompt('Delete', message.html(), 'notice', [{ text: 'Cancel' }, deleteBtn]);

            function deleteAction(path) {
                $.ajax({
                    url: '/services/backpack/package?path=' + encodeURIComponent(path),
                    type: 'DELETE',
                    success: () => window.location.reload()
                });
            }
        }
    });
})();
