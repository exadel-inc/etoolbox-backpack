(function (Granite, $) {
    'use strict';

    const registry = Granite.UI.Foundation.Registry;
    const packagePath = new URL(window.location.href).searchParams.get('packagePath');

    const BUILD_IN_PROGRESS = 'BUILD_IN_PROGRESS';
    const INSTALL_IN_PROGRESS = 'INSTALL_IN_PROGRESS';

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

    // Actions

    $(document).on('click', '#includeChildrenAction', function() {
        const selection = $('.foundation-selections-item');
        if (selection) {
            doPost("/services/backpack/add/children", {'packagePath': packagePath, 'payload': selection.attr('data-entry-title')}, success);
        }
    });

    $(document).on('click', '#excludeChildrenAction', function() {
        const selection = $('.foundation-selections-item');
        const dataObject = {'entry':selection.attr('data-entry-title'),'type':'children'};
        if (selection) {
            doPost("/services/backpack/delete", {'packagePath': packagePath, 'payload': JSON.stringify(dataObject)}, success);
        }
    });

    $(document).on('click', '#liveCopiesAction', function() {
        const selection = $('.foundation-selections-item');
        if (selection) {
            doPost("/services/backpack/add/liveCopies", {'packagePath': packagePath, 'payload': selection.attr('data-entry-title')}, success);
        }
    });

    $(document).on('click', '.add-references-action', function(event) {
        const selection = $('.foundation-selections-item');
        const dataObject = {'entry':selection.attr('data-entry-title'),'type':event.target.closest('[data-type]').getAttribute('data-type')}
        if (selection) {
            doPost("/services/backpack/add/references", {'packagePath': packagePath, 'payload': JSON.stringify(dataObject)}, success);
        }
    });

    $(document).on('click', '#deleteAction', function(event) {
        const selection = $('.foundation-selections-item');
        const dataObject = {'entry':selection.attr('data-entry-title'),'type':selection.attr('data-type')};

        if (selection.hasClass('secondary')) {
           dataObject['subsidiary'] = selection.attr('data-subsidiary-title');
        }

        if (selection) {
            doPost("/services/backpack/delete", {'packagePath': packagePath, 'payload': JSON.stringify(dataObject)}, success);
        }
    });

    $(document).on('click', '#buildAndDownloadAction', function() {
        console.log("buildAndDownloadAction")
    });

    $(document).on('click', '#downloadAction', function() {
        window.location.href = packagePath;
    });

    $(document).on('click', '#testBuildAction', function() {
        buildPackage(true);
    });

    $(document).on('click', '#buildAction', function() {
        buildPackage(false);
    });

    function doPost(url, data, success) {
       $.ajax({
         type: "POST",
         url: url,
         data: data,
         success: success,
         error: function(data) {
            console.log(data);
         }
       });
    }

    function success() {
        // todo change loading on success
        window.location.reload();
    }

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

    $(window).adaptTo('foundation-registry').register('foundation.form.response.ui.success', {
        name: 'foundation.prompt.open',
        handler: function (form, config, data, textStatus, xhr) {
            //todo change loading on success
            if (data && data.packagePath) {
                const paramPath = 'packagePath=' + data.packagePath;
                window.location.search = paramPath;
            } else {
                window.location.reload();
            }
        }
    });

    // Avoid collection-related exceptions when using Granite Action API with a non-collection UI element

    registry.register('foundation.adapters', {
        type: 'foundation-collection',
        selector: '.foundation-collection.stub-collection',
        adapter: function (el) {
            const collection = $(el)

            return {
                append: function(items) {
                    collection.append(items);
                    collection.trigger("foundation-contentloaded");
                },

                clear: function() {
                    collection.find(".foundation-collection-item").remove();
                },

                getPagination: function () {
                    // No operation
                },

                reload: function () {
                   collection.trigger("coral-collection:remove")
                   collection.trigger("foundation-collection-reload");
                }
            };
        }
    });


    function buildPackage(testBuild) {
        var container = $('#buildLogsContainer');
        var referencedResources = [];
        $('.reference').each(function () {
            referencedResources.push(this.innerText);
        });
        $.ajax({
            type: 'POST',
            url: '/services/backpack/package/build',
            data: {
                packagePath: packagePath,
                referencedResources: JSON.stringify(referencedResources),
                testBuild: testBuild
            }, success: function (data) {
                if (testBuild) {
                    if (data.log) {
                        $.each(data.log, function (index, value) {
                            container.append('<div>' + value + '</div>');
                        });
                        const assetText = data.dataSize === 0 ? 'There are no assets in the package' : '<h4>Approximate size of the assets in the package: ' + bytesToSize(data.dataSize) + '</h4>';
                        container.append(assetText);
                        scrollLog();
                    }
                } else {
                    updateLog(0);
                }
            },
            dataType: 'json'
        });
    }

    function updateLog(logIndex) {
        var container = $('#buildLogsContainer');
        $.ajax({
            url: '/services/backpack/package/build',
            data: {packagePath: packagePath, latestLogIndex: logIndex},
            success: function (data) {
                if (data.log && data.log.length) {
                    $.each(data.log, function (index, value) {
                        container.append('<div>' + value + '</div>');
                    });
                    logIndex = logIndex + data.log.length;
                    scrollLog();
                }
                if (data.packageStatus === BUILD_IN_PROGRESS || data.packageStatus === INSTALL_IN_PROGRESS) {
                    setTimeout(function () {
                        updateLog(logIndex);
                    }, 1000);

                }
            }
        })
    }

    function scrollLog() {
        $('#buildLogsContainer')[0].scrollIntoView(false)
    }

})(Granite, Granite.$);
