(function(window) {

    var REGEX_ATTR = 'data-regex';
    var VALIDATION_MSG_ATTR = 'data-validation-message';

    $(window).adaptTo("foundation-registry").register("foundation.validation.validator", {
        selector: '[' + REGEX_ATTR + ']',
        validate: function(el) {
            var regex = new RegExp(el.getAttribute(REGEX_ATTR));
            var validationMsg = el.getAttribute(VALIDATION_MSG_ATTR) || 'Invalid field value';
            if(!(el.value || '').match(regex)) {
                return validationMsg;
            }
        }
    });
})(window);