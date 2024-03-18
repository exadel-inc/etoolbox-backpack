(function (Granite, $) {
    'use strict';

    const registry = Granite.UI.Foundation.Registry;
    const packagePath = new URL(window.location.href).searchParams.get('packagePath');

    const BUILD_IN_PROGRESS = 'BUILD_IN_PROGRESS';
    const INSTALL_IN_PROGRESS = 'INSTALL_IN_PROGRESS';

    // Make package entries selectable

    $(document).on('click', '.foundation-collection-item.result-row', function(e) {
        const $this = $(this);
        const $pulldown = $('.selection-pulldown');
        $pulldown.addClass('foundation-collection-action-hidden');
        if ($this.hasClass('foundation-selections-item')) {
            $this.removeClass('foundation-selections-item');
        } else {
            $this.addClass('foundation-selections-item');
        }
        if ($('.primary.foundation-selections-item').length > 0) {
            $pulldown.removeClass('foundation-collection-action-hidden');
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
        const payload = selection.attr('data-entry-title');
        if (selection) {
            doPost("/services/backpack/add/" + referenceType, {'packagePath': packagePath, 'payload': payload}, success(payload));
        }
    });

    $(document).on('click', '#deleteAction', function(event) {
        const selection = $('.foundation-selections-item');

        if (!selection) {
            return;
        }

        const payload = [];
        selection.each(function () {
            if ($(this).hasClass('secondary')) {
                // payload.push($(this).attr('data-subsidiary-title'));
                payload.push('[' + $(this).attr('data-entry-title') + ',' + $(this).attr('data-subsidiary-title') + ']');
            } else {
                payload.push($(this).attr('data-entry-title'));
                var children = $(this).find('.secondary');
                children.each(function () {
                    // payload.push($(this).attr('data-subsidiary-title'));
                    payload.push('[' + $(this).attr('data-entry-title') + ',' + $(this).attr('data-subsidiary-title') + ']');
                });
            }
        });
        doPost("/services/backpack/delete", {'packagePath': packagePath, 'payload': payload}, success);

        // if (selection.hasClass('secondary')) {
        //     const payload = [selection.attr('data-entry-title'), selection.attr('data-subsidiary-title')];
        //     doPost("/services/backpack/delete/"  + selection.attr('data-type'), {'packagePath': packagePath, 'payload': JSON.stringify(payload)}, success);
        // } else {
        //     const payload = [];
        //     selection.each(function () {
        //         payload.push($(this).attr('data-entry-title'));
        //     });
        //     doPost("/services/backpack/delete", {'packagePath': packagePath, 'payload': payload}, success);
        // }
    });

    $(document).on('click', '#downloadAction', function() {
        window.location.href = packagePath;
    });

    $(document).on('click', '#testBuildAction', function() {
        buildPackage(true, function(data) {
            if (data.log) {
                const dialog = openLogsDialog(data.log);
                const assetText = data.dataSize === 0
                    ? 'There are no assets in the package'
                    : '<h4>Approximate size of the assets in the package: ' + bytesToSize(data.dataSize) + '</h4>';
                $(dialog.content).append('<div>' + assetText + '</div>');
                setTimeout(function () {
                    $(dialog.content).children("div").last()[0].scrollIntoView(false);
                })
            }
        });
    });

    $(document).on('click', '#buildAction', function() {
        buildPackage(false, function(data) {
            const dialog = openLogsDialog(data.log);
            updateLog(data.packageStatus, data.log.length, dialog);
        });
    });

    $(document).on('click', '#buildAndDownloadAction', function() {
        buildPackage(false, function(data) {
            const dialog = openLogsDialog(data.log);
            dialog.on('coral-overlay:beforeclose', function(event) {
                window.location.href = packagePath;
            });
            updateLog(data.packageStatus, data.log.length, dialog);
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
        doPost(form.attr('action'), form.serialize(), function(data) {
            const dialog = openLogsDialog(data.log);
            updateLog(data.packageStatus, data.log.length, dialog);
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

    function success(path) {
        openSuccessDialog(path);
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
                const dialog = openLogsDialog(data.logs);
                dialog.on('coral-overlay:close', function(event) {
                    if (data.status == "WARNING") {
                        window.location.reload();
                    }
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

    $(window).adaptTo("foundation-registry").register("foundation.form.response.ui.error", {
        name: "errorResponseCreated",
        handler: function (form, data, xhr) {
            const title = Granite.I18n.get("Error");
            let message = "";
            if (xhr.responseJSON) {
                message = xhr.responseJSON.log;
            } else if (xhr.responseText) {
                const response = JSON.parse(xhr.responseText);
                if (response && response.log) {
                    message = response.log;
                }
            }

            const ui = $(window).adaptTo("foundation-ui");
            ui.alert(title, message, "error");
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

    function updateLog(packageStatus, logIndex, dialog) {
        if (packageStatus === BUILD_IN_PROGRESS || packageStatus === INSTALL_IN_PROGRESS) {
            setTimeout(function () {
                $.ajax({
                    url: '/services/backpack/package/build',
                    data: {packagePath: packagePath, latestLogIndex: logIndex},
                    success: function (data) {
                        if (data.log && data.log.length) {
                            $.each(data.log, function (index, value) {
                                $(dialog.content).append('<div>' + value + '</div>');
                            });
                            logIndex = logIndex + data.log.length;
                            $(dialog.content).children("div").last()[0].scrollIntoView(false);
                        }
                        updateLog(data.packageStatus, logIndex, dialog);
                    }
                });
            }, 1000);
        }
    }

    function bytesToSize(bytes) {
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        if (bytes === 0) return '0 Bytes';
        const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
        return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i];
    }

    function openLogsDialog(init) {

        const dialog = new Coral.Dialog().set({
            id: 'LogsDialog',
            header: {
                innerHTML: 'Logs'
            },
            footer: {
                innerHTML: '<button is="coral-button" variant="primary" coral-close>Ok</button>'
            }
        });

        if (init && init.length > 0) {
            $.each(init, function (index, value) {
                $(dialog.content).append('<div>' + value + '</div>');
            });
        }

        dialog.on('coral-overlay:close', function(event) {
            event.preventDefault();
            setTimeout(function () {
                dialog.remove();
                window.location.reload();
            });
        });

        document.body.appendChild(dialog);
        dialog.show();

        return dialog;
    }

    function openSuccessDialog(path) {

        const dialog = new Coral.Dialog().set({
            id: 'SuccessDialog',
            header: {
                innerHTML: 'Success'
            },
            content: {
                innerHTML: `<div>References for ${path} were successfully added</div>`
            },
            footer: {
                innerHTML: '<button is="coral-button" variant="primary" coral-close>Ok</button>'
            }
        });

        dialog.on('coral-overlay:close', function(event) {
            event.preventDefault();
            setTimeout(function () {
                dialog.remove();
                window.location.reload();
            });
        });

        document.body.appendChild(dialog);
        dialog.show();

        return dialog;
    }

    function openPackageDialog(success, error) {
        $('#editDialogButton').trigger('click');
    }

    function getPackageInfo(packagePath, errorFunction) {
        $.ajax({
            url: '/services/backpack/package',
            data: {'packagePath': packagePath},
            error: errorFunction
        });
    }

    $(window).on('load', function() {
        if (packagePath && packagePath.length > 0) {
            getPackageInfo(packagePath, function (data) {
                openPackageDialog()
            });
        } else {
            openPackageDialog()
        }
    });

})(Granite, Granite.$);
