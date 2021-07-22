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

(function(document, $) {
    "use strict";

    /**
     * Component initialization
     */
    $(document).on("foundation-contentloaded", function() {
        var switchStatus = $(".cq-dialog-switch-toggle").attr("checked") !== undefined;
        toggleFilterElements(switchStatus);
    });

    /**
     * Listen to switcher changes
     */
    $(document).on("change", ".cq-dialog-switch-toggle", function() {
        toggleFilterElements($(this).prop("checked"));
    });

    /**
     * Toggles visibility of elements
     * @param switchStatus Boolean value represents switcher status
     */
    function toggleFilterElements(switchStatus) {
        var pathElement = $(".path-option-element-target");
        var sqlElement = $(".sql2-option-element-target");

        if (switchStatus) {
            // Show SQL2 element and hide Filter Path element
            pathElement.hide();
            sqlElement.show();
            changeValidationState(sqlElement, "enable");
            changeValidationState(pathElement, "disable");
            $(".cq-dialog-path-filter-option").attr("aria-required", "false");
        } else {
            // Show Filter Path element and hide SQL2 element
            pathElement.show();
            sqlElement.hide();
            changeValidationState(pathElement, "enable");
            changeValidationState(sqlElement, "disable");
            $(".cq-dialog-path-filter-option").attr("aria-required", "true");
        }
    }

    /**
     * Changes validation rules for elements
     * @param element html element
     * @param state string that can be "enable" or "disable"
     */
    function changeValidationState(element, state) {
        element.find("input, textarea").each(function() {
            $(this).attr("aria-required", state === "enable" ? "true" : "false");
            if (state === "enable") {
                $(this).removeAttr('disabled');
            } else {
                $(this).attr('disabled', 'disabled');
            }
        });
    }

})(document,Granite.$);