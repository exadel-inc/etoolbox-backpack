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
        if (selection) {
            doPost("/services/backpack/delete/children", {'packagePath': packagePath, 'payload': selection.attr('data-entry-title')}, success);
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
        const referenceType = event.target.closest('[data-type]').getAttribute('data-type');
        if (selection) {
            doPost("/services/backpack/add/" + referenceType, {'packagePath': packagePath, 'payload': selection.attr('data-entry-title')}, success);
        }
    });

    $(document).on('click', '#deleteAction', function(event) {
        const selection = $('.foundation-selections-item');

        if (!selection) {
            return;
        }

        if (selection.hasClass('secondary')) {
           const payload = [selection.attr('data-entry-title'), selection.attr('data-subsidiary-title')];
           doPost("/services/backpack/delete/"  + selection.attr('data-type'), {'packagePath': packagePath, 'payload': JSON.stringify(payload)}, success);
        } else {
           doPost("/services/backpack/delete", {'packagePath': packagePath, 'payload': selection.attr('data-entry-title')}, success);
        }
    });

    $(document).on('click', '#downloadAction', function() {
        window.location.href = packagePath;
    });

    $(document).on('click', '#testBuildAction', function() {
        buildPackage(true, function(data) {
            const container = openLogsDialog();
            if (data.log) {
                $.each(data.log, function (index, value) {
                    $(container).append(value + '<br>');
                });
                const assetText = data.dataSize === 0
                ? 'There are no assets in the package'
                : '<h4>Approximate size of the assets in the package: ' + bytesToSize(data.dataSize) + '</h4>';
                container.append(assetText);
                container.scrollIntoView(false)
            }
        });
    });

    $(document).on('click', '#buildAction', function() {
        buildPackage(false, function(data) {
            updateLog(0);
        });
    });

    $(document).on('click', '#buildAndDownloadAction', function() {
        buildPackage(false, function(data) {
            updateLog(0, function() {
                window.location.href = packagePath;
            });
        });
    });

    $(document).on('click', '#installAction', function() {
        const dialog = document.querySelector('#installDialog');
        if (dialog) {
            dialog.show();
        }
    });

    $(document).on('submit', '#installForm', function (e) {
        e.preventDefault();
        const form = $(this);
        doPost(form.attr('action'), form.serialize(), function() {
              updateLog(0);
        });
    })

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
            if (data.status == "ERROR" || data.status == "WARNING") {
                const container = openLogsDialog(function() {
                   if (data.status == "WARNING") {
                       window.location.reload();
                   }
                });
                $.each(data.logs, function (index, value) {
                    $(container).append(value + '<br>');
                });
                return;
            }
            if (data.packagePath) {
                window.location.search = 'packagePath=' + data.packagePath;
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

    function buildPackage(testBuild, callback) {
        const referencedResources = [];
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
                callback(data);
            },
            dataType: 'json'
        });
    }

    function updateLog(logIndex, callback) {
        const container = openLogsDialog();
        $.ajax({
            url: '/services/backpack/package/build',
            data: {packagePath: packagePath, latestLogIndex: logIndex},
            success: function (data) {
                if (data.log && data.log.length) {
                    $.each(data.log, function (index, value) {
                        $(container).append(value + '<br>');
                    });
                    logIndex = logIndex + data.log.length;
                    container.scrollIntoView(false);
                }
                if (data.packageStatus === BUILD_IN_PROGRESS || data.packageStatus === INSTALL_IN_PROGRESS) {
                    setTimeout(function () {
                        updateLog(logIndex, callback);
                    }, 1000);
                } else if (callback){
                    callback();
                }
            }
        })
    }

    function bytesToSize(bytes) {
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        if (bytes === 0) return '0 Bytes';
        const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
        return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i];
    }

    function openLogsDialog(onClose) {
        let dialog = $('#LogsDialog')[0];
        if (dialog) {
            dialog.show();
            return $('#LogsContainer')[0];
        } else {
            dialog = new Coral.Dialog().set({
                id: 'LogsDialog',
                header: {
                    innerHTML: 'Logs'
                },
                content: {
                    innerHTML: '<div id="LogsContainer"></div>'
                },
                footer: {
                    innerHTML: '<button onclick="window.location.reload();" is="coral-button" variant="primary" coral-close>Ok</button>'
                }
            });
            dialog.on('coral-overlay:close', function(event) {
              event.preventDefault();
              $(dialog.content).find('#LogsContainer').innerHTML = '';
                if (onClose) {
                   onClose();
                }
            });
            document.body.appendChild(dialog);
            dialog.show();
            return $(dialog.content).find('#LogsContainer');
        }
    }

    function openPackageDialog(success, error) {
         //todo looking for another way for opening
         $('#editDialogButton').trigger('click');
    }

    function updatePackageData(data) {
         //todo set package info to editDialog or delete method
         console.log(data);
    }

    function getPackageInfo(packagePath, updateFunction, errorFunction) {
        $.ajax({
            url: '/services/backpack/package',
            data: {'packagePath': packagePath},
            success: updateFunction,
            error: errorFunction
        });
    }

    $(window).on('load', function() {
       if (packagePath && packagePath.length > 0) {
           getPackageInfo(packagePath, function (data) {
               updatePackageData(data);
           }, function (data) {
               openPackageDialog()
           });
       } else {
            openPackageDialog()
       }
    });

})(Granite, Granite.$);
