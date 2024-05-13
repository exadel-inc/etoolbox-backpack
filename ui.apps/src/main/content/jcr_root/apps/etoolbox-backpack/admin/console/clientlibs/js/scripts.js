(function (Granite, $) {
    'use strict';

    const registry = Granite.UI.Foundation.Registry;
    const packagePath = new URL(window.location.href).searchParams.get('packagePath');

    const BUILD_IN_PROGRESS = 'BUILD_IN_PROGRESS';
    const INSTALL_IN_PROGRESS = 'INSTALL_IN_PROGRESS';

    $(() => {
        const $pulldown = $('.selection-pulldown');
        $pulldown.attr('disabled', 'disabled');
    });

    $(() => {
        const items = $('.foundation-collection-item');
        if (items && items.length > 0) {
            $('.build-options').removeClass('disabled');
            $('#installAction').removeAttr('disabled');
            $('#replicateAction').removeAttr('disabled');
        } else {
            $('.build-options').addClass('disabled');
            $('#installAction').attr('disabled', 'disabled');
            $('#replicateAction').attr('disabled', 'disabled');
        }
    });

    // Make package entries selectable

    $(document).on('click', '.foundation-collection-item.result-row', function(e) {
        const $this = $(this);
        const $pulldown = $('.selection-pulldown');
        $pulldown.attr('disabled', 'disabled');

        if (e.ctrlKey) {
            $('.foundation-collection-item').removeClass('last-selected');
            if ($this.hasClass('foundation-selections-item')) {
                $this.removeClass('foundation-selections-item');
            } else {
                $this.addClass('foundation-selections-item');
                $this.addClass('last-selected');
            }
        }
        if (e.shiftKey) {
            $('.foundation-collection-item').removeClass('foundation-selections-item');
            $this.addClass('foundation-selections-item');
            var $lastSelected = $('.last-selected');
            $('.foundation-collection-item').removeClass('last-selected');
            if ($this.index() > $lastSelected.index()) {
                $lastSelected.nextUntil($this).addClass('foundation-selections-item');
                $lastSelected.addClass('last-selected');
            } else {
                $lastSelected.prevUntil($this).addClass('foundation-selections-item');
                $this.addClass('last-selected');
            }
            $lastSelected.addClass('foundation-selections-item');


        }

        if (!e.ctrlKey && !e.shiftKey) {
            const mustSelect = !$this.hasClass('foundation-selections-item');
            $('.foundation-collection-item').removeClass('foundation-selections-item last-selected');
            if (mustSelect) {
                $this.addClass('foundation-selections-item');
                $this.addClass('last-selected');
                $this.hasClass('primary') &&  $pulldown.removeAttr('disabled');
            }
        }
        e.stopPropagation();

        const selection = $('.foundation-selections-item');
        $('#liveCopiesAction').attr('disabled', 'disabled');
        $('#deleteAction').attr('disabled', 'disabled');
        $('#includeChildrenAction').attr('disabled', 'disabled');
        $('#excludeChildrenAction').attr('disabled', 'disabled');
        if (selection && selection.length > 0) {
            $('#deleteAction').removeAttr('disabled');
            selection.each((index, item) => {
                const $item = $(item);
                if ($item.is('.primary')) {
                    $('#excludeChildrenAction').removeAttr('disabled');
                    $('#liveCopiesAction').removeAttr('disabled');
                    $('#includeChildrenAction').removeAttr('disabled');
                    $pulldown.removeAttr('disabled');
                }
            });
        }
    });

    const foundationUi = $(window).adaptTo('foundation-ui');

    $(document)
        .ajaxStart(function () {
            foundationUi.wait();
        })
        .ajaxStop(function () {
            foundationUi.clearWait();
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
        const payload = [];
        selection.each(function () {
            if (!$(this).hasClass('secondary')) {
                payload.push($(this).attr('data-entry-title'));
            }
        });
        if (selection) {
            doPost("/services/backpack/add/children", {'packagePath': packagePath, 'payload': payload}, success);
        }
    });

    $(document).on('click', '#excludeChildrenAction', function() {
        const selection = $('.foundation-selections-item');
        if (!selection) {
            return;
        }
        const payload = [];
        selection.each(function () {
            if (!$(this).hasClass('secondary')) {
                payload.push($(this).attr('data-entry-title'));
            }
        });
        if (selection) {
            doPost("/services/backpack/delete/children", {'packagePath': packagePath, 'payload': payload}, success);
        }
    });

    $(document).on('click', '#liveCopiesAction', function() {
        const selection = $('.foundation-selections-item');
        const payload = [];
        selection.each(function () {
            if (!$(this).hasClass('secondary')) {
                payload.push($(this).attr('data-entry-title'));
            }
        });
        if (selection) {
            doPost("/services/backpack/add/liveCopies", {'packagePath': packagePath, 'payload': payload}, success);
        }
    });

    $(document).on('click', '.add-references-action', function(event) {
        const selection = $('.foundation-selections-item');
        const referenceType = event.target.closest('[data-type]').getAttribute('data-type');
        const payload = [];
        selection.each(function () {
            if (!$(this).hasClass('secondary')) {
                payload.push($(this).attr('data-entry-title'));
            }
        });
        if (selection) {
            doPost("/services/backpack/add/" + referenceType, {'packagePath': packagePath, 'payload': payload}, success);
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
                payload.push('[' + $(this).attr('data-entry-title') + ',' + $(this).attr('data-subsidiary-title') + ']');
            } else {
                payload.push($(this).attr('data-entry-title'));
                var children = $(this).find('.secondary');
                children.each(function () {
                    payload.push('[' + $(this).attr('data-entry-title') + ',' + $(this).attr('data-subsidiary-title') + ']');
                });
            }
        });
        doPost("/services/backpack/delete", {'packagePath': packagePath, 'payload': payload}, success);
    });

    $(document).on('click', '#downloadAction', function() {
        window.location.href = packagePath;
    });

    $(document).on('click', '#testBuildAction', function() {
        buildPackage(true, function(data) {
            if (data.log) {
                const dialog = openLogsDialog(data.log, 'Test Build', 'Close');
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

    $(document).on('click', '#replicateAction', function() {
        var fui = $(window).adaptTo("foundation-ui");
        fui.prompt("Please confirm", "Replicate this package?", "notice", [{
            text: Granite.I18n.get("Cancel")
        }, {
            text: "Replicate",
            primary: true,
            handler: function () {
                replicatePackage(function(data) {
                    if (data.log) {
                        const dialog = openLogsDialog(data.log, 'Replication', 'Close');
                        const assetText = data.dataSize === 0
                            ? 'There are no assets in the package'
                            : '<h4>Approximate size of the assets in the package: ' + bytesToSize(data.dataSize) + '</h4>';
                        $(dialog.content).append('<div>' + assetText + '</div>');
                        setTimeout(function () {
                            $(dialog.content).children("div").last()[0].scrollIntoView(false);
                        })
                    }
                });
            }
        }]);
    });

    $(document).on('click', '#mainMenuAction', function() {
        window.location.replace("/tools/etoolbox/backpack.html");
    });

    $(document).on('click', '#cancelButton', function() {
        window.location.replace("/tools/etoolbox/backpack.html");
    });

    $(document).on('click', '#buildAction', function() {
        buildPackage(false, function(data) {
            const dialog = openLogsDialog(data.log, 'Build', 'Close');
            updateLog(data.packageStatus, data.log.length, dialog);
        });
    });

    $(document).on('click', '#buildAndDownloadAction', function() {
        buildPackage(false, function(data) {
            const dialog = openLogsDialog(data.log, 'Build', 'Download');
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

    $(document).on('click', '#deletePackageAction', function() {
        if (!packagePath) {
            return;
        }
        var packageName = packagePath.split('/').pop();

        var ui = $(window).adaptTo("foundation-ui");
        var message = createEl("div");
        var intro = createEl("p").appendTo(message);

        intro.text(Granite.I18n.get("You are going to delete the following package:"));
        createEl("p").html(createEl("b")).text(packageName).appendTo(message);
        ui.prompt(Granite.I18n.get("Delete"), message.html(), "notice", [{
            text: Granite.I18n.get("Cancel")
        }, {
            text: Granite.I18n.get("Delete"),
            warning: true,
            handler: function () {
                deleteAction();
            }
        }]);
    });

    function deleteAction() {

        var data = {
            _charset_: "UTF-8",
            cmd: "deletePage",
            path: packagePath,
            force: true
        };

        $.post(Granite.HTTP.externalize("/bin/wcmcommand"), data).done(function () {
            showAlert("Package deleted", "Delete", "warning", function () {
                window.location.replace("/tools/etoolbox/backpack.html");
            });
        });
    }

    function showAlert(message, title, type, callback) {
        var fui = $(window).adaptTo("foundation-ui"),
            options = [{
                id: "ok",
                text: "OK",
                primary: true
            }];

        message = message || "Unknown Error";
        title = title || "Error";

        fui.prompt(title, message, type, options, callback);
    }

    function createEl(name) {
        return $(document.createElement(name));
    }

    function replicatePackage(callback) {
        $.ajax({
            url: '/services/backpack/replicatePackage',
            type: "POST",
            dataType: "json",
            ContentType : 'application/json',
            data: {packagePath: packagePath},
            success: function (data) {
                callback(data);
            }
        })
    }

    $(document).on('submit', '#installForm', function (e) {
        e.preventDefault();
        const form = $(this);
        doPost(form.attr('action'), form.serialize(), function(data) {
            const dialog = openLogsDialog(data.log, 'Install', 'Close');
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

    function success() {
        showReferencedAlert();
    }

    $(window).adaptTo('foundation-registry').register('foundation.form.response.ui.success', {
        name: 'foundation.prompt.open',
        handler: function (form, config, data, textStatus, xhr) {
            if (data.status == "ERROR" || data.status == "WARNING") {
                const dialog = openLogsDialog(data.logs, 'WARNING', 'Close');
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

    function openLogsDialog(init, title, submitText) {

        const dialog = new Coral.Dialog().set({
            id: 'LogsDialog',
            header: {
                innerHTML: `${title} Logs`
            },
            footer: {
                innerHTML: `<button is="coral-button" variant="primary" coral-close>${submitText}</button>`
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

    function showReferencedAlert() {

        var popup = new Coral.Alert().set({
            variant: 'info',
            header: {
                innerHTML: 'INFO'
            },
            content: {
                textContent: `Package was successfully updated`
            },
            id: 'references-added-alert'
        });
        document.body.append(popup);
        setTimeout(function () {
            $(popup).fadeOut();
            window.location.reload();
        }, 2000);
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
