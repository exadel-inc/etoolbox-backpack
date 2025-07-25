(function (Granite, $, EBUtils) {
    'use strict';

    const registry = Granite.UI.Foundation.Registry;
    const packagePath = new URL(window.location.href).searchParams.get('packagePath');

    const LIVE_COPIES_SEL = '#liveCopiesAction'; // "Add live copies" button
    const INCLUDE_CHILDREN_SEL = '#includeChildrenAction'; // "Include children" button
    const EXCLUDE_CHILDREN_SEL = '#excludeChildrenAction'; // "Exclude children" button

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

    // Click on 'add/delete children' and 'add live copies' buttons
    function onHandleBtnClick (action) {
        if ($('.foundation-selections-item')) return;
        const payload = [];
        $('.foundation-selections-item').each(function () {
            if ($(this).hasClass('secondary')) return;
            payload.push($(this).attr('data-entry-title'));
        });
        EBUtils.doPost(`/services/backpack/${action}`, {packagePath, payload}, EBUtils.success);
    }

    // Actions

    $(document).on('click', INCLUDE_CHILDREN_SEL, () => onHandleBtnClick('add/children'));

    $(document).on('click', EXCLUDE_CHILDREN_SEL, () => onHandleBtnClick('delete/children'));

    $(document).on('click', LIVE_COPIES_SEL, () => onHandleBtnClick('add/liveCopies'));

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
            EBUtils.doPost("/services/backpack/add/" + referenceType, {'packagePath': packagePath, 'payload': payload}, EBUtils.success);
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
        EBUtils.doPost("/services/backpack/delete", {'packagePath': packagePath, 'payload': payload}, EBUtils.success);
    });

    $(document).on('click', '#downloadAction', function() {
        window.location.href = packagePath;
    });

    $(document).on('click', '#testBuildAction', function() {
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

    $(document).on('click', '#mainMenuAction', function() {
        window.location.replace("/tools/etoolbox/backpack.html");
    });

    $(document).on('click', '#cancelButton', function() {
        window.location.replace("/tools/etoolbox/backpack.html");
    });

    $(document).on('click', '#buildAction', () => EBUtils.onBuildAction(false));

    $(document).on('click', '#buildAndDownloadAction', () => EBUtils.onBuildAction(true));

    $(document).on('click', '#installAction', function() {
        const dialog = document.querySelector('#installDialog');
        if (dialog) dialog.show();
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
        EBUtils.doPost(form.attr('action'), form.serialize(), function(data) {
            const dialog = EBUtils.openLogsDialog(data.log, 'Install', 'Close');
            EBUtils.updateLog(data.packageStatus, data.log.length, dialog);
        });
    })

    $(window).adaptTo('foundation-registry').register('foundation.form.response.ui.success', {
        name: 'foundation.prompt.open',
        handler: function (form, config, data, textStatus, xhr) {
            if (data.status == "ERROR" || data.status == "WARNING") {
                const dialog = EBUtils.openLogsDialog(data.logs, 'WARNING', 'Close');
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

    $(window).on('load', function() {
        if (packagePath && packagePath.length > 0) {
            EBUtils.getPackageInfo(packagePath, function (data) {
                EBUtils. openPackageDialog()
            });
        } else {
            EBUtils.openPackageDialog()
        }
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

})(Granite, Granite.$, EBUtils = Granite.EBUtils || {});
