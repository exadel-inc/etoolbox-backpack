(function (Granite, $, EBUtils) {
    'use strict';

    const REGISTRY = Granite.UI.Foundation.Registry;
    const FOUNDATION_REGISTRY = $(window).adaptTo('foundation-registry');

    FOUNDATION_REGISTRY.register('foundation.form.response.ui.success', {
        name: 'foundation.prompt.open',
        handler: function (form, config, data) {
            const isWarning = data.status === 'WARNING';
            if (data.status == 'ERROR' || isWarning) {
                const dialog = EBUtils.openLogsDialog(data.logs, 'WARNING', 'Close');
                dialog.on('coral-overlay:close', () => isWarning && window.location.reload());
                return;
            }
            if (data.packagePath) {
                window.location.search = 'packagePath=' + data.packagePath;
            } else {
                window.location.reload();
            }
        }
    });

    FOUNDATION_REGISTRY.register('foundation.form.response.ui.error', {
        name: 'errorResponseCreated',
        handler: function (form, data, xhr) {
            const title = Granite.I18n.get('Error');
            let message = '';
            if (xhr.responseJSON) {
                message = xhr.responseJSON.log;
            } else if (xhr.responseText) {
                const response = JSON.parse(xhr.responseText);
                if (response && response.log) {
                    message = response.log;
                }
            }

            const ui = $(window).adaptTo('foundation-ui');
            ui.alert(title, message, 'error');
        }
    });

    // Avoid collection-related exceptions when using Granite Action API with a non-collection UI element

    REGISTRY.register('foundation.adapters', {
        type: 'foundation-collection',
        selector: '.foundation-collection.stub-collection',
        adapter: function (el) {
            const collection = $(el)

            return {
                append: function(items) {
                    collection.append(items);
                    collection.trigger('foundation-contentloaded');
                },

                clear: () => collection.find('.foundation-collection-item').remove(),

                getPagination: () => { }, // No operation

                reload: function () {
                    collection.trigger('coral-collection:remove')
                    collection.trigger('foundation-collection-reload');
                }
            };
        }
    });

    FOUNDATION_REGISTRY.register('foundation.validation.validator', {
        selector: "[data-validation='text-validation']",
        validate: function(el) {
            if (!el.value || !el.value.trim()) {
                return 'Please enter a value';
            }
        }
    });

    if (window.DOMPurify) {
        window.DOMPurify.addHook('uponSanitizeElement', function (node, hookEvent ) {
            if (hookEvent && hookEvent.tagName === 'meta' && node.classList.contains('backpack-meta')) {
                hookEvent.allowedTags.meta = true;
            }
        });
    }

})(Granite, Granite.$, EBUtils = Granite.EBUtils || {});
