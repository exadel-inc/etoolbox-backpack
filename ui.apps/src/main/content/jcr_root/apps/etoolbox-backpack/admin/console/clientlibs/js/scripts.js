(function (Granite, $) {
    'use strict';

    const registry = Granite.UI.Foundation.Registry;
    const packagePath = new URL(window.location.href).searchParams.get('packagePath');
    const BACKPACK_PATH = '/tools/etoolbox/backpack.html';

    const BUILD_IN_PROGRESS = 'BUILD_IN_PROGRESS';
    const INSTALL_IN_PROGRESS = 'INSTALL_IN_PROGRESS';

    const DISABLED_MARKER = 'disabled';
    const LAST_SELECTED_CLASS = 'last-selected';
    const SELECTIONS_ITEM_CLASS = 'foundation-selections-item';
    const COLLECTION_ITEM_CLASS = 'foundation-collection-item';

    const LIVE_COPIES_SEL = '#liveCopiesAction'; // "Add live copies" button
    const INCLUDE_CHILDREN_SEL = '#includeChildrenAction'; // "Include children" button
    const EXCLUDE_CHILDREN_SEL = '#excludeChildrenAction'; // "Exclude children" button
    const DELETE_SEL = '#deleteAction'; // "Delete button
    const INSTALL_SEL = '#installAction'; // "Install" button
    const REPLICATE_SEL = '#replicateAction'; // "Replicate" button

    const $pulldown = $('.selection-pulldown'); // "Add References" button
    const $collectionItems = $(`.${COLLECTION_ITEM_CLASS}`);

    // calls when dom is loaded
    $(() => {
        $pulldown.attr(DISABLED_MARKER, true);
        $('.build-options').toggleClass(DISABLED_MARKER, !$collectionItems.length); // "Build and download" options
        const $jqCollection = $([INSTALL_SEL, REPLICATE_SEL].join(','));
        if (!!$collectionItems.length) $jqCollection.removeAttr(DISABLED_MARKER);
        else $jqCollection.attr(DISABLED_MARKER, true);
    });

    function ctrlKeyClick() {
        const $this = $(this);
        $collectionItems.removeClass(LAST_SELECTED_CLASS);
        $this.toggleClass(SELECTIONS_ITEM_CLASS);
        $this.hasClass(SELECTIONS_ITEM_CLASS) && $this.addClass(LAST_SELECTED_CLASS);
    }

    function shiftKeyClick() {
        const $this = $(this);
        const $lastSelected = $('.last-selected');
        $collectionItems.removeClass(`${SELECTIONS_ITEM_CLASS} ${LAST_SELECTED_CLASS}`);
        $this.addClass(SELECTIONS_ITEM_CLASS);
        if ($this.index() > $lastSelected.index()) {
            $lastSelected.nextUntil($this).addClass(SELECTIONS_ITEM_CLASS);
            $lastSelected.addClass(LAST_SELECTED_CLASS);
        } else {
            $lastSelected.prevUntil($this).addClass(SELECTIONS_ITEM_CLASS);
            $this.addClass(LAST_SELECTED_CLASS);
        }
        $lastSelected.addClass(SELECTIONS_ITEM_CLASS);
    }

    function otherKeyClick() {
    const $this = $(this);
        const mustSelect = !$this.hasClass(SELECTIONS_ITEM_CLASS);
        $collectionItems.removeClass(`${SELECTIONS_ITEM_CLASS} ${LAST_SELECTED_CLASS}`);
        if (mustSelect) {
            $this.addClass(`${SELECTIONS_ITEM_CLASS} ${LAST_SELECTED_CLASS}`);
            $this.hasClass('primary') && $pulldown.removeAttr(DISABLED_MARKER);
        }
    }

    // Make package entries selectable

    $(document).on('click', `.${COLLECTION_ITEM_CLASS}.result-row`, function(e) {
        $pulldown.attr(DISABLED_MARKER, true);

        if (e.ctrlKey) ctrlKeyClick();
        if (e.shiftKey) shiftKeyClick();
        if (!e.ctrlKey && !e.shiftKey) otherKeyClick();
        e.stopPropagation();

        const selection = $(`.${SELECTIONS_ITEM_CLASS}`);
        $([LIVE_COPIES_SEL, DELETE_SEL, INCLUDE_CHILDREN_SEL, EXCLUDE_CHILDREN_SEL].join(',')).attr(DISABLED_MARKER, true);
        if (selection && selection.length > 0) {
            $(DELETE_SEL).removeAttr(DISABLED_MARKER);
            selection.each((index, item) => {
                if (!$(item).is('.primary')) return;
                $([EXCLUDE_CHILDREN_SEL, LIVE_COPIES_SEL, INCLUDE_CHILDREN_SEL].join(',')).removeAttr(DISABLED_MARKER);
                $pulldown.removeAttr(DISABLED_MARKER);
            });
        }
    });

    const foundationUi = $(window).adaptTo('foundation-ui');

    // Make top-level package entries collapsible
    $(document).on('click', '.toggler', function() {
        const $togglable = $(this).closest(`.${COLLECTION_ITEM_CLASS}`);
        const treeState = $togglable.attr('data-tree-state');
        if (treeState === 'collapsed' || treeState === 'expanded') {
          $togglable.attr('data-tree-state', treeState === 'collapsed' ? 'expanded' : 'collapsed');
        }
    });

    // Actions

    $(document).on('click', INCLUDE_CHILDREN_SEL, function() {
        const selection = $(`.${SELECTIONS_ITEM_CLASS}`);
        const payload = [];
        selection.each(function () {
            if (!$(this).hasClass('secondary')) {
                payload.push($(this).attr('data-entry-title'));
            }
        });
        if (selection) {
            doPost("/services/backpack/add/children", {packagePath, payload}, success);
        }
    });

    $(document).on('click', EXCLUDE_CHILDREN_SEL, function() {
        const selection = $(`.${SELECTIONS_ITEM_CLASS}`);
        if (!selection) return;
        const payload = [];
        selection.each(function () {
            if (!$(this).hasClass('secondary')) {
                payload.push($(this).attr('data-entry-title'));
            }
        });
        if (selection) {
            doPost("/services/backpack/delete/children", {packagePath, payload}, success);
        }
    });

    $(document).on('click', LIVE_COPIES_SEL, function() {
        const selection = $('.foundation-selections-item');
        const payload = [];
        selection.each(function () {
            if (!$(this).hasClass('secondary')) {
                payload.push($(this).attr('data-entry-title'));
            }
        });
        if (selection) {
            doPost("/services/backpack/add/liveCopies", {packagePath, payload}, success);
        }
    });

    $(document).on('click', '.add-references-action', function(event) {
        const selection = $('.foundation-selections-item');
        if (!selection) return;
        const referenceType = event.target.closest('[data-type]').getAttribute('data-type');
        const payload = [];
        selection.each(function () {
            if ($(this).hasClass('secondary')) return;
            payload.push($(this).attr('data-entry-title'));
        });
        doPost("/services/backpack/add/" + referenceType, {packagePath, payload}, success);
    });

    $(document).on('click', DELETE_SEL, function(event) {
        const selection = $(`.${SELECTIONS_ITEM_CLASS}`);
        if (!selection) return;

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
        doPost("/services/backpack/delete", {packagePath, payload}, success);
    });

    $(document).on('click', '#downloadAction', () => window.location.href = packagePath);

    $(document).on('click', '#testBuildAction', function() {
        buildPackage(true, function(data) {
            if (data.log) {
                const dialog = openLogsDialog(data.log, 'Test Build', 'Close');
                const assetText = data.dataSize === 0
                    ? 'There are no assets in the package'
                    : '<h4>Approximate size of the package: ' + bytesToSize(data.dataSize) + '</h4>';
                $(dialog.content).append('<div>' + assetText + '</div>');
                setTimeout(function () {
                    $(dialog.content).children("div").last()[0].scrollIntoView(false);
                })
            }
        });
    });

    $(document).on('click', REPLICATE_SEL, function() {
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
                            : '<h4>Approximate size of the package: ' + bytesToSize(data.dataSize) + '</h4>';
                        $(dialog.content).append('<div>' + assetText + '</div>');
                        setTimeout(function () {
                            $(dialog.content).children("div").last()[0].scrollIntoView(false);
                        })
                    }
                });
            }
        }]);
    });

    $(document).on('click', '#mainMenuAction, #cancelButton', function() {
        window.location.replace(BACKPACK_PATH);
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

    $(document).on('click', INSTALL_SEL, function() {
        const dialog = document.querySelector('#installDialog');
        if (dialog) dialog.show();
    });

    $(document).on('click', '#deletePackageAction', function() {
        if (!packagePath) return;

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
                window.location.replace(BACKPACK_PATH);
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
            },
            beforeSend: function () {
                foundationUi.wait();
            },
            complete: function () {
                foundationUi.clearWait();
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
            },
            beforeSend: function () {
                foundationUi.wait();
            },
            complete: function () {
                foundationUi.clearWait();
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
                packagePath,
                testBuild,
                referencedResources: JSON.stringify(referencedResources)
            },
            success: function (data) {
                callback(data);
            },
            beforeSend: function () {
                foundationUi.wait();
            },
            complete: function () {
                foundationUi.clearWait();
            },
            dataType: 'json'
        });
    }

    function updateLog(packageStatus, logIndex, dialog) {
        if (packageStatus === BUILD_IN_PROGRESS || packageStatus === INSTALL_IN_PROGRESS) {
            setTimeout(function () {
                $.ajax({
                    url: '/services/backpack/package/build',
                    data: {packagePath, latestLogIndex: logIndex},
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
            data: {packagePath},
            error: errorFunction
        });
    }

    $(window).on('load', function() {
        if (packagePath && packagePath.length > 0) {
            getPackageInfo(packagePath, () => openPackageDialog());
        } else openPackageDialog();
    });

    $(window).adaptTo("foundation-registry").register("foundation.validation.validator", {
        selector: "[data-validation='text-validation']",
        validate: function(el) {
            if (!el.value || !el.value.trim()) {
                return "Please enter a value";
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

})(Granite, Granite.$);
