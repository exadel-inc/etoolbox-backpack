(function (Granite, $) {
    'use strict';
    $(window).adaptTo('foundation-registry').register('foundation.form.response.ui.error', {
    name: 'backpack.error.response',
    handler: function (form, data, xhr) {
        if (!$(form).hasClass('js-backpack-package-form')) return false;
        let message = '';
        if (xhr.responseJSON) {
            message = xhr.responseJSON.log;
        } else if (xhr.responseText) {
            try {
                const response = JSON.parse(xhr.responseText);
                if (response && response.log) {
                    message = [].concat(response.log).join('\n');
                }
            } catch (e) {
                console.error('Failed to parse responseText:', e);
            }
        }

        $(window).adaptTo('foundation-ui').alert('Error', message, 'error');
    }
});

})(Granite, Granite.$);