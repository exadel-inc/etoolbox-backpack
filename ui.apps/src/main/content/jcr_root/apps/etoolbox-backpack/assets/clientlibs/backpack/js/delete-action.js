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
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "backpack.delete",
        handler: function(name, el, config) {

            let ui = $(window).adaptTo("foundation-ui");
            let message = $("<div>");

            $("<p>").text(Granite.I18n.get("You are going to delete the following package:")).appendTo(message);
            $("<p>").append($("<b>").text(config.data.packagePath)).appendTo(message);

            ui.prompt(Granite.I18n.get("Delete"), message.html(), "notice", [{
                text: Granite.I18n.get("Cancel")
            }, {
                text: Granite.I18n.get("Delete"),
                warning: true,
                handler: function () {
                    deleteAction(config.data.packagePath);
                }
            }]);

            function deleteAction(path) {
                $.ajax({
                    url: '/services/backpack/package?path=' + path,
                    type: 'DELETE',
                    success: function(result) {
                        window.location.reload()
                    }
                });
            }
        }
    });
})(window);