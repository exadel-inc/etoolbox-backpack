(function (Granite, $, EBUtils) {
    'use strict';

    const DELETE_TITLE = Granite.I18n.get('Delete');
    const CANCEL_TITLE = Granite.I18n.get('Cancel');

    const $window = $(window);
    const $document = $(document);
    const FOUNDATION_UI = $window.adaptTo('foundation-ui');
    const packagePath = new URL(window.location.href).searchParams.get('packagePath');
    const BACKPACK_PATH = '/tools/etoolbox/backpack.html';
    const TITLE_ATTR = 'data-entry-title';

    const SELECTIONS_ITEM_CLASS = 'foundation-selections-item';
    const COLLECTION_ITEM_CLASS = 'foundation-collection-item';
    const DISABLED_MARKER = 'disabled';
    const LAST_SELECTED_CLASS = 'last-selected';
    const LIVE_COPIES_SEL = '.js-backpack-live-copies'; // "Add live copies" button
    const INCLUDE_CHILDREN_SEL = '.js-backpack-include-children'; // "Include children" button
    const EXCLUDE_CHILDREN_SEL = '.js-backpack-exclude-children'; // "Exclude children" button
    const DELETE_SEL = '.js-backpack-delete'; // "Delete button
    const INSTALL_SEL = '.js-backpack-install'; // "Install" button
    const REPLICATE_SEL = '.js-backpack-replicate'; // "Replicate" button

    // calls when dom is loaded
    $(() => {
        const backpack = new EBackpack();
        backpack.$addReferences.attr(DISABLED_MARKER, true);
        $('.build-options').toggleClass(DISABLED_MARKER, backpack.$collectionItems.length); // "Build and download" options
        $([INSTALL_SEL, REPLICATE_SEL].join(',')).attr(DISABLED_MARKER, backpack.$collectionItems.length ? null : true);
    });

    class EBackpack {
        constructor() {
            this.bindEvents();
        }

        get $collectionItems() {
            return $(`.${COLLECTION_ITEM_CLASS}`);
        }

        get $selectionItems() {
            return $(`.${SELECTIONS_ITEM_CLASS}`);
        }

        // "Add References" button
        get $addReferences() {
            return $('.js-backpack-add-references');
        }

        get referencedResources () {
            const referencedResources = [];
            $('.reference').each(function () {
                referencedResources.push(this.innerText);
            });
            return referencedResources;
        }

        bindEvents() {
            $document.off('backpack');
            $document.on('click.backpack', `.${COLLECTION_ITEM_CLASS}.result-row`, this.onPackageEntryClick.bind(this));
            $document.on('click.backpack', '.toggler', this.onTogglerClick);
            $document.on('click.backpack', `${INCLUDE_CHILDREN_SEL}, ${EXCLUDE_CHILDREN_SEL}, ${LIVE_COPIES_SEL}, ${DELETE_SEL}, .js-backpack-add-references-item`, this.onChangePackageEntries.bind(this));
            $document.on('click.backpack', '.js-backpack-download', () => window.location.href = packagePath);
            $document.on('click.backpack', '.js-backpack-test-build', this.onBuildPackage.bind(this, true, false));
            $document.on('click.backpack', REPLICATE_SEL, this.onReplicatePackage.bind(this));
            $document.on('click.backpack', '.js-backpack-main-menu, .jsBackpackCancelButton', () => window.location.replace(BACKPACK_PATH));
            $document.on('click.backpack', '.js-backpack-build', this.onBuildPackage.bind(this, false, false));
            $document.on('click.backpack', '.js-backpack-build-download', this.onBuildPackage.bind(this, false, true));
            $document.on('click.backpack', INSTALL_SEL, this.onInstallPackage);
            $document.on('click.backpack', '.js-backpack-delete-package', this.onDeletePackage.bind(this));
            $document.on('submit.backpack', '#jsBackpackInstallForm', this.onHandleInstallPackageForm);
            $window.on('load.backpack', this.onLoad.bind(this));
        }

        packageEntriesCtrlClick(target) {
            this.$collectionItems.removeClass(LAST_SELECTED_CLASS);
            target.toggleClass(SELECTIONS_ITEM_CLASS);
            target.hasClass(SELECTIONS_ITEM_CLASS) && target.addClass(LAST_SELECTED_CLASS);
        }

        packageEntriesShiftClick(target) {
            this.$collectionItems.removeClass(`${SELECTIONS_ITEM_CLASS}`);
            target.addClass(SELECTIONS_ITEM_CLASS);
            const $lastSelected = $('.last-selected');
            this.$collectionItems.removeClass(`${LAST_SELECTED_CLASS}`);
            if (target.index() > $lastSelected.index()) {
                $lastSelected.nextUntil(target).addClass(SELECTIONS_ITEM_CLASS);
                $lastSelected.addClass(LAST_SELECTED_CLASS);
            } else {
                $lastSelected.prevUntil(target).addClass(SELECTIONS_ITEM_CLASS);
                target.addClass(LAST_SELECTED_CLASS);
            }
            $lastSelected.addClass(SELECTIONS_ITEM_CLASS);
        }

        packageEntriesClick(target) {
            const mustSelect = !target.hasClass(SELECTIONS_ITEM_CLASS);
            this.$collectionItems.removeClass(`${SELECTIONS_ITEM_CLASS} ${LAST_SELECTED_CLASS}`);
            if (mustSelect) {
                target.addClass(`${SELECTIONS_ITEM_CLASS} ${LAST_SELECTED_CLASS}`);
                target.hasClass('primary') && this.$addReferences.removeAttr(DISABLED_MARKER);
            }
        }

        // Make package entries selectable
        onPackageEntryClick(e) {
            const target = $(e.target.closest(`.${COLLECTION_ITEM_CLASS}`));
            this.$addReferences.attr(DISABLED_MARKER, true);
            (e.ctrlKey ? this.packageEntriesCtrlClick : e.shiftKey ? this.packageEntriesShiftClick : this.packageEntriesClick).call(this, target);
            e.stopPropagation();

            $([LIVE_COPIES_SEL, DELETE_SEL, INCLUDE_CHILDREN_SEL, EXCLUDE_CHILDREN_SEL].join(',')).attr(DISABLED_MARKER, true);
            if (!this.$selectionItems.length) return;
            $(DELETE_SEL).removeAttr(DISABLED_MARKER);
            this.$selectionItems.each((index, item) => {
                if (!$(item).is('.primary')) return;
                $([EXCLUDE_CHILDREN_SEL, LIVE_COPIES_SEL, INCLUDE_CHILDREN_SEL].join(',')).removeAttr(DISABLED_MARKER);
                this.$addReferences.removeAttr(DISABLED_MARKER);
            });
        };

        // Make top-level package entries collapsible
        onTogglerClick() {
            const $togglable = $(this).closest(`.${COLLECTION_ITEM_CLASS}`);
            const treeState = $togglable.attr('data-tree-state');
            $togglable.attr('data-tree-state', treeState === 'collapsed' ? 'expanded' : 'collapsed');
        };

        // 'Add/Delete children', 'Add live copies', 'Add references', 'Delete item'
         onChangePackageEntries(e) {
            if (!this.$selectionItems.length) return;
            const action = e.target.closest('[data-path]').dataset.path;
            const payload = [];
            this.$selectionItems.each((i, item) => {
                const $item = $(item);
                if (!$item.hasClass('secondary')) payload.push($item.attr(TITLE_ATTR));
                if (action === 'delete') this.onDeleteEntry.call(this, $item, payload);
            });
            const referenceType = action === 'add' ? e.target.closest('[data-type]').getAttribute('data-type') || '' : '';
            EBUtils.onProcessChangeRequest(action + (referenceType ? `/${referenceType}` : ''), {packagePath, payload}, EBUtils.showSuccessMessage);
         }

        onDeleteEntry($item, payload) {
             if (!$item.hasClass('secondary')) {
                 $item.find('.secondary').each((i, item) => payload.push('[' + $(item).attr(TITLE_ATTR) + ',' + $(item).attr('data-subsidiary-title') + ']'));
             } else payload.push('[' + $item.attr(TITLE_ATTR) + ',' + $item.attr('data-subsidiary-title') + ']');
        };

        onHandleData(data, text) {
            const dialog = EBUtils.openLogsDialog(data.log, text, 'Close');
            const assetText = data.dataSize === 0
                ? 'There are no assets in the package'
                : '<h4>Approximate size of the package: ' + EBUtils.bytesToSize(data.dataSize) + '</h4>';
            $(dialog.content).append('<div>' + assetText + '</div>');
            setTimeout(() => $(dialog.content).children('div').last()[0].scrollIntoView(false));
        }

        onBuildPackage(isTest, isDownload) {
            const callback = (data) => {
                if (!(data && data.log)) return;
                if (!isTest) {
                    const dialog = EBUtils.openLogsDialog(data.log, 'Build', isDownload ? 'Download' : 'Close');
                    isDownload && dialog.on('coral-overlay:beforeclose', () => window.location.href = packagePath);
                    EBUtils.updateLog(data.packageStatus, data.log.length, dialog);
                } else {
                    this.onHandleData(data, 'Test Build');
                }
            }
            EBUtils.buildRequest(isTest, callback, this.referencedResources)
        }

        onReplicatePackage() {
            const replicateBtn = {
                text: 'Replicate',
                primary: true,
                handler: () => EBUtils.replicateRequest((data) => data.log && this.onHandleData(data, 'Replication'))
            }
            FOUNDATION_UI.prompt('Please confirm', 'Replicate this package?', 'notice', [{text: CANCEL_TITLE}, replicateBtn]);
        }

        onInstallPackage() {
            const dialog = document.querySelector('#installDialog');
            if (dialog) dialog.show();
        };

        onDeletePackage() {
            if (!packagePath) return;
            const packageName = packagePath.split('/').pop();
            const message = $(document.createElement('div'));
            $(document.createElement('p')).text(Granite.I18n.get('You are going to delete the following package:')).appendTo(message);
            $(document.createElement('p')).html($(document.createElement('b'))).text(packageName).appendTo(message);
            const deleteBtn = {
                text: DELETE_TITLE,
                warning: true,
                handler: () => EBUtils.deleteRequest()
            }
            FOUNDATION_UI.prompt(DELETE_TITLE, message.html(), 'notice', [{text: CANCEL_TITLE}, deleteBtn]);
        };

        onHandleInstallPackageForm(e) {
            e.preventDefault();
            const callback = (data) => {
                if (!data || !data.log) return;
                const dialog = EBUtils.openLogsDialog(data.log, 'Install', 'Close');
                EBUtils.updateLog(data.packageStatus, data.log.length, dialog);
            }
            EBUtils.onProcessChangeRequest('/package/install', $(this).closest('form').serialize(), callback);
        }

        openPackageDialog() {
            $('#editDialogButton').trigger('click');
        }

        onLoad() {
            if (packagePath && packagePath.length > 0) EBUtils.getPackageInfo(packagePath, () => this.openPackageDialog());
            else this.openPackageDialog();
        };
    }

})(Granite, Granite.$, EBUtils = Granite.EBUtils || {});
