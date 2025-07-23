(function (Granite, $, EBUtils, window) {
    'use strict';

    const registry = Granite.UI.Foundation.Registry;
    const $window = $(window);
    const $document = $(document);

    // calls when dom is loaded
    $(() => {
        EBUtils.$pulldown.attr(DISABLED_MARKER, true);
        $('.build-options').toggleClass(DISABLED_MARKER, !EBUtils.$collectionItems.length); // "Build and download" options
        const $jqCollection = $([INSTALL_SEL, REPLICATE_SEL].join(','));
        if (!!EBUtils.$collectionItems.length) $jqCollection.removeAttr(DISABLED_MARKER);
        else $jqCollection.attr(DISABLED_MARKER, true);
    });

    // Make package entries selectable
    $document.on('click', `.${COLLECTION_ITEM_CLASS}.result-row`, function(e) {
        const $this = $(this);
        EBUtils.$pulldown.attr(DISABLED_MARKER, true);
        e.ctrlKey ? EBUtils.onCtrlClick($this) : e.shiftKey ? EBUtils.onShiftClick($this) : EBUtils.onClick($this);
        e.stopPropagation();

        $([LIVE_COPIES_SEL, DELETE_SEL, INCLUDE_CHILDREN_SEL, EXCLUDE_CHILDREN_SEL].join(',')).attr(DISABLED_MARKER, true);
        if (!EBUtils.$selectionItems) return;
        $(DELETE_SEL).removeAttr(DISABLED_MARKER);
        EBUtils.$selectionItems.each((index, item) => {
            if (!$(item).is('.primary')) return;
            $([EXCLUDE_CHILDREN_SEL, LIVE_COPIES_SEL, INCLUDE_CHILDREN_SEL].join(',')).removeAttr(DISABLED_MARKER);
            EBUtils.$pulldown.removeAttr(DISABLED_MARKER);
        });
    });

    // Make top-level package entries collapsible
    $document.on('click', '.toggler', function() {
        const $togglable = $(this).closest(`.${COLLECTION_ITEM_CLASS}`);
        const treeState = $togglable.attr('data-tree-state');
        if (treeState === 'collapsed' || treeState === 'expanded') {
          $togglable.attr('data-tree-state', treeState === 'collapsed' ? 'expanded' : 'collapsed');
        }
    });

    // Actions

    $document.on('click', INCLUDE_CHILDREN_SEL, () => EBUtils.onHandleBtnClick('add/children'));

    $document.on('click', EXCLUDE_CHILDREN_SEL, () => EBUtils.onHandleBtnClick('delete/children'));

    $document.on('click', LIVE_COPIES_SEL, () => EBUtils.onHandleBtnClick('add/liveCopies'));

    $document.on('click', '.add-references-action', function(event) {
        const referenceType = event.target.closest('[data-type]').getAttribute('data-type');
        if (!EBUtils.$selectionItems) return;
        const payload = [];
        EBUtils.$selectionItems.each(function () {
            if ($(this).hasClass('secondary')) return;
            payload.push($(this).attr('data-entry-title'));
        });
        EBUtils.doPost("/services/backpack/add/" + referenceType, {packagePath, payload}, EBUtils.success);
    });

    $document.on('click', DELETE_SEL, function() {
        if (!EBUtils.$selectionItems) return;

        const payload = [];
        EBUtils.$selectionItems.each(function () {
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
        EBUtils.doPost("/services/backpack/delete", {packagePath, payload}, EBUtils.success);
    });

    $document.on('click', '#downloadAction', () => window.location.href = packagePath);

    $document.on('click', '#testBuildAction', function() {
        EBUtils.buildPackage(true, function(data) {
            if (data.log) {
                const dialog = EBUtils.openLogsDialog(data.log, 'Test Build', 'Close');
                const assetText = data.dataSize === 0
                    ? 'There are no assets in the package'
                    : '<h4>Approximate size of the package: ' + EBUtils.bytesToSize(data.dataSize) + '</h4>';
                $(dialog.content).append('<div>' + assetText + '</div>');
                setTimeout(function () {
                    $(dialog.content).children("div").last()[0].scrollIntoView(false);
                })
            }
        });
    });

    $document.on('click', REPLICATE_SEL, function() {
        var fui = $window.adaptTo("foundation-ui");
        fui.prompt("Please confirm", "Replicate this package?", "notice", [{
            text: Granite.I18n.get("Cancel")
        }, {
            text: "Replicate",
            primary: true,
            handler: function () {
                replicatePackage(function(data) {
                    if (data.log) {
                        const dialog = EBUtils.openLogsDialog(data.log, 'Replication', 'Close');
                        const assetText = data.dataSize === 0
                            ? 'There are no assets in the package'
                            : '<h4>Approximate size of the package: ' + EBUtils.bytesToSize(data.dataSize) + '</h4>';
                        $(dialog.content).append('<div>' + assetText + '</div>');
                        setTimeout(function () {
                            $(dialog.content).children("div").last()[0].scrollIntoView(false);
                        })
                    }
                });
            }
        }]);
    });

    $document.on('click', '#mainMenuAction, #cancelButton', () => window.location.replace(BACKPACK_PATH));

    $document.on('click', '#buildAction', () => EBUtils.onBuildAction(false));

    $document.on('click', '#buildAndDownloadAction', () => EBUtils.onBuildAction(true));

    $document.on('click', INSTALL_SEL, function() {
        const dialog = document.querySelector('#installDialog');
        if (dialog) dialog.show();
    });

    $document.on('click', '#deletePackageAction', function() {
        if (!packagePath) return;

        var packageName = packagePath.split('/').pop();

        var ui = $window.adaptTo("foundation-ui");
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
        var fui = $window.adaptTo("foundation-ui"),
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

    $document.on('submit', '#installForm', function (e) {
        e.preventDefault();
        const form = $(this);
        EBUtils.doPost(form.attr('action'), form.serialize(), function(data) {
            const dialog = EBUtils.openLogsDialog(data.log, 'Install', 'Close');
            EBUtils.updateLog(data.packageStatus, data.log.length, dialog);
        });
    })

    $window.adaptTo('foundation-registry').register('foundation.form.response.ui.success', {
        name: 'foundation.prompt.open',
        handler: function (form, config, data) {
            if (data.status == "ERROR" || data.status == "WARNING") {
                const dialog = openLogsDialog(data.logs, 'WARNING', 'Close');
                dialog.on('coral-overlay:close', function() {
                    if (data.status == "WARNING") window.location.reload();
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

    $window.adaptTo("foundation-registry").register("foundation.form.response.ui.error", {
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

            const ui = $window.adaptTo("foundation-ui");
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

                getPagination: function () { }, // No operation

                reload: function () {
                    collection.trigger("coral-collection:remove")
                    collection.trigger("foundation-collection-reload");
                }
            };
        }
    });

    $window.on('load', function() {
        if (packagePath && packagePath.length > 0) {
            EBUtils.getPackageInfo(packagePath, () => EBUtils.openPackageDialog());
        } else EBUtils.openPackageDialog();
    });

    $window.adaptTo("foundation-registry").register("foundation.validation.validator", {
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

})(Granite, Granite.$, EBUtils = Granite.EBUtils || {}, window);
