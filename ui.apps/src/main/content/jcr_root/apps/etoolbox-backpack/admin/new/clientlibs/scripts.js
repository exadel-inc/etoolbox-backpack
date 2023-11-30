(function (Granite, $) {
    'use strict';

    const registry = Granite.UI.Foundation.Registry;

    // Make package entries selectable

    $(document).on('click', '.foundation-collection-item', function(e) {
        const $this = $(this);
        const $pulldown = $('.selection-pulldown');
        const mustSelect = !$this.hasClass('foundation-selections-item');
        $('.foundation-collection-item').removeClass('foundation-selections-item');
        $pulldown.addClass('foundation-collection-action-hidden');
        if (mustSelect) {
            $this.addClass('foundation-selections-item');
            $this.hasClass('primary') &&  $pulldown.removeClass('foundation-collection-action-hidden');
        }
        $this.closest('.foundation-collection').trigger("foundation-selections-change");
        e.stopPropagation();
    });

    // Make top-level package entries collapsible

    $(document).on('click', '.toggler', function() {
        const $this = $(this);
        const $togglable = $this.closest('.foundation-collection-item');
        const treeState = $togglable.attr('data-tree-state');
        if (treeState === 'collapsed') {
            $togglable.attr('data-tree-state', 'expanded');
        } else if (treeState === 'expanded') {
            $togglable.attr('data-tree-state', 'collapsed');
        }
    });

    // Register visibility conditions for package entries' actions

    $(window).adaptTo('foundation-registry').register('foundation.collection.action.activecondition', {
        name: 'backpack.package.selection',
        handler: function(name, el, config, collection, selections) {
            return selections && selections.length > 0;
        }
    });
    $(window).adaptTo('foundation-registry').register('foundation.collection.action.activecondition', {
        name: 'backpack.package.selection.isPrimary',
        handler: function(name, el, config, collection, selections) {
            return selections
                && selections.length > 0
                && selections.every(item => $(item).is('.primary'));
        }
    });
    $(window).adaptTo('foundation-registry').register('foundation.collection.action.activecondition', {
        name: 'backpack.package.selection.hasChildren',
        handler: function(name, el, config, collection, selections) {
            return selections
                && selections.length > 0
                && selections.every(item => {
                    const $item = $(item);
                    return $item.is('.primary') && $item.is('[data-has-children]');
                });
        }
    });
    $(window).adaptTo('foundation-registry').register('foundation.collection.action.activecondition', {
        name: 'backpack.package.selection.noChildren',
        handler: function(name, el, config, collection, selections) {
            return selections
                && selections.length > 0
                && selections.every(item => {
                    const $item = $(item);
                    return $item.is('.primary') && !$item.is('[data-has-children]');
                });
        }
    });

    // Avoid collection-related exceptions when using Granite Action API with a non-collection UI element

    registry.register('foundation.adapters', {
        type: 'foundation-collection',
        selector: '.foundation-collection.stub-collection',
        adapter: function () {
            return {
                append: function (items) {
                    // No operation
                },
                clear: function () {
                    // No operation
                },
                getPagination: function () {
                    // No operation
                }
            };
        }
    });

})(Granite, Granite.$);
