(function (Granite, $, window) {
    'use strict';

    window.packagePath = new URL(window.location.href).searchParams.get('packagePath');
    window.BACKPACK_PATH = '/tools/etoolbox/backpack.html';

    window.SELECTIONS_ITEM_CLASS = 'foundation-selections-item';
    window.COLLECTION_ITEM_CLASS = 'foundation-collection-item';
    window.DISABLED_MARKER = 'disabled';
    window.LAST_SELECTED_CLASS = 'last-selected';
    window.LIVE_COPIES_SEL = '#liveCopiesAction'; // "Add live copies" button
    window.INCLUDE_CHILDREN_SEL = '#includeChildrenAction'; // "Include children" button
    window.EXCLUDE_CHILDREN_SEL = '#excludeChildrenAction'; // "Exclude children" button
    window.DELETE_SEL = '#deleteAction'; // "Delete button
    window.INSTALL_SEL = '#installAction'; // "Install" button
    window.REPLICATE_SEL = '#replicateAction'; // "Replicate" button

    window.foundationUi = $(window).adaptTo('foundation-ui');

    class EBUtils {
        static get $collectionItems() { return $(`.${COLLECTION_ITEM_CLASS}`); }
        static get $selectionItems() { return $(`.${SELECTIONS_ITEM_CLASS}`); }
        static get $pulldown() { return $('.selection-pulldown'); } // "Add References" button

        // Package entries click + Ctrl
        static onCtrlClick($this) {
            this.$collectionItems.removeClass(LAST_SELECTED_CLASS);
            $this.toggleClass(SELECTIONS_ITEM_CLASS);
            $this.hasClass(SELECTIONS_ITEM_CLASS) && $this.addClass(LAST_SELECTED_CLASS);
        }

        // Package entries click + Shift
        static onShiftClick($this) {
            this.$collectionItems.removeClass(`${SELECTIONS_ITEM_CLASS}`);
            $this.addClass(SELECTIONS_ITEM_CLASS);
            const $lastSelected = $('.last-selected');
            this.$collectionItems.removeClass(`${LAST_SELECTED_CLASS}`);
            if ($this.index() > $lastSelected.index()) {
                $lastSelected.nextUntil($this).addClass(SELECTIONS_ITEM_CLASS);
                $lastSelected.addClass(LAST_SELECTED_CLASS);
            } else {
                $lastSelected.prevUntil($this).addClass(SELECTIONS_ITEM_CLASS);
                $this.addClass(LAST_SELECTED_CLASS);
            }
            $lastSelected.addClass(SELECTIONS_ITEM_CLASS);
        }

        // Package entries click
        static onClick($this) {
            const mustSelect = !$this.hasClass(SELECTIONS_ITEM_CLASS);
            this.$collectionItems.removeClass(`${SELECTIONS_ITEM_CLASS} ${LAST_SELECTED_CLASS}`);
            if (mustSelect) {
                $this.addClass(`${SELECTIONS_ITEM_CLASS} ${LAST_SELECTED_CLASS}`);
                $this.hasClass('primary') && this.$pulldown.removeAttr(DISABLED_MARKER);
            }
        }

        // Click on 'add/delete children' and 'add live copies' buttons
        static onHandleBtnClick (action) {
            if (!this.$selectionItems) return;
            const payload = [];
            this.$selectionItems.each(function () {
                if ($(this).hasClass('secondary')) return;
                payload.push($(this).attr('data-entry-title'));
            });
            this.doPost(`/services/backpack/${action}`, {packagePath, payload}, this.success);
        }

        // Handles click on 'Build/download' button
        static onBuildAction(isDownload) {
            this.buildPackage(false, function(data) {
                const dialog = this.openLogsDialog(data.log, 'Build', isDownload ? 'Download' : 'Close');
                isDownload && dialog.on('coral-overlay:beforeclose', () => window.location.href = packagePath);
                this.updateLog(data.packageStatus, data.log.length, dialog);
            });
        }

        static doPost(url, data, success) {
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

        static success() {
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


        static openPackageDialog() {
            $('#editDialogButton').trigger('click');
        }

        static getPackageInfo(packagePath, errorFunction) {
            $.ajax({
                url: '/services/backpack/package',
                data: {packagePath},
                error: errorFunction
            });
        }

        static buildPackage(testBuild, callback) {
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

        static updateLog(packageStatus, logIndex, dialog) {
            if (packageStatus === 'BUILD_IN_PROGRESS' || packageStatus === 'INSTALL_IN_PROGRESS') {
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
                            this.updateLog(data.packageStatus, logIndex, dialog);
                        }
                    });
                }, 1000);
            }
        }

        static bytesToSize(bytes) {
            const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
            if (bytes === 0) return '0 Bytes';
            const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
            return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i];
        }

        static openLogsDialog(init, title, submitText) {

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
    }

    Granite.EBUtils = EBUtils;

})(Granite, Granite.$, window);