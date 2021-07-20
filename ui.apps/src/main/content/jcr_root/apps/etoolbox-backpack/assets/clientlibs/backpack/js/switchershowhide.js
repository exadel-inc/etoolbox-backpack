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
    $(document).on("foundation-contentloaded", function(e) {
        $(".cq-dialog-checkbox-showhide").each( function() {
            showHide($(this));
        });
    });

    /**
     * Listen to switcher changes
     */
    $(document).on("change", ".cq-dialog-checkbox-showhide", function(e) {
        showHide($(this));
    });

    /**
     * Show/Hide XPath or SQL2 elements, handle validation logic
     * @param el represents switch element
     */
    function showHide(el){
        let checked = el.prop('checked');
        console.log(checked);

        let pathElement = $('.path-option-enable-showhide-target');
        let sqlElement = $('.sql2-option-enable-showhide-target');

        if (checked) {
            // Show XPath element and hide SQL2 element
            pathElement.show();
            sqlElement.hide();
            changeValidation(pathElement, sqlElement);
            $('.cq-dialog-xpath-option').attr("aria-required", "true");
        } else {
            // Show SQL2 element and hide XPath element
            pathElement.hide();
            sqlElement.show();
            changeValidation(sqlElement, pathElement);
            $('.cq-dialog-xpath-option').attr("aria-required", "false");
        }
    }

    /**
     * Changes validation rules for elements
     * @param enableValidationElement element that will be validated
     * @param disableValidationElement element that won't be validated
     */
    function changeValidation(enableValidationElement, disableValidationElement) {

        enableValidationElement.find('input, textarea').each(function() {
            $(this).attr('aria-required', 'true');
            $(this).removeAttr('disabled');
        });

        disableValidationElement.find('input, textarea').each(function() {
            $(this).attr('aria-required', 'false');
            $(this).attr('disabled', 'disabled');
        });
    }

})(document,Granite.$);