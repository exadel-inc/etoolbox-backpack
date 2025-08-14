(function (Granite, $, EBUtils) {
    'use strict';

    const $window = $(window);
    const $document = $(document);
    const foundationRegistryAPI = $window.adaptTo('foundation-ui');
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
        backpack.$addReferencesBtn.attr(DISABLED_MARKER, true);
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
        get $addReferencesBtn() {
            return $('.js-backpack-add-references');
        }

        get referencedResources() {
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
            $document.on('click.backpack', `${INCLUDE_CHILDREN_SEL}, ${EXCLUDE_CHILDREN_SEL}, ${LIVE_COPIES_SEL}, ${DELETE_SEL}, .js-backpack-add-references-item`, this.onPreparePackageEntriesChanges.bind(this));
            $document.on('click.backpack', REPLICATE_SEL, this.onHandleReplicatePackage.bind(this));
            $document.on('click.backpack', '.js-backpack-main-menu, #js-backpack-cancel-button', this.onMainMenuNav);
            $document.on('click.backpack', '.js-backpack-test-build', () => this.wrapUiAsyncRequest(this.buildPackage, this, true, false));
            $document.on('click.backpack', '.js-backpack-build', () => this.wrapUiAsyncRequest(this.buildPackage, this, false, false));
            $document.on('click.backpack', '.js-backpack-build-download', () => this.wrapUiAsyncRequest(this.buildPackage, this, false, true));
            $document.on('click.backpack', '.js-backpack-download', this.onDownloadPackage);
            $document.on('click.backpack', INSTALL_SEL, this.onInstallPackage);
            $document.on('click.backpack', '.js-backpack-delete-package', this.onDeletePackage.bind(this));
            $document.on('submit.backpack', '#js-backpack-install-form', this.onHandleInstallPackageForm.bind(this));
            $window.on('load.backpack', this.onLoad.bind(this));
        }

        wrapUiAsyncRequest(callback, context, ...args) {
            foundationRegistryAPI.wait();
            return callback.call(context, ...args)
                .catch((error) => console.log(error))
                .finally(() => foundationRegistryAPI.clearWait());
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
                target.hasClass('primary') && this.$addReferencesBtn.removeAttr(DISABLED_MARKER);
            }
        }

        // Make package entries selectable
        onPackageEntryClick(e) {
            const target = $(e.target.closest(`.${COLLECTION_ITEM_CLASS}`));
            this.$addReferencesBtn.attr(DISABLED_MARKER, true);
            (e.ctrlKey ? this.packageEntriesCtrlClick : e.shiftKey ? this.packageEntriesShiftClick : this.packageEntriesClick).call(this, target);
            e.stopPropagation();

            $([LIVE_COPIES_SEL, DELETE_SEL, INCLUDE_CHILDREN_SEL, EXCLUDE_CHILDREN_SEL].join(',')).attr(DISABLED_MARKER, true);
            if (!this.$selectionItems.length) return;
            $(DELETE_SEL).removeAttr(DISABLED_MARKER);
            if (this.$selectionItems.is('.primary')) {
                $([EXCLUDE_CHILDREN_SEL, LIVE_COPIES_SEL, INCLUDE_CHILDREN_SEL].join(',')).removeAttr(DISABLED_MARKER);
                this.$addReferencesBtn.removeAttr(DISABLED_MARKER);
            }
        };

        // Make top-level package entries collapsible
        onTogglerClick() {
            const $togglable = $(this).closest(`.${COLLECTION_ITEM_CLASS}`);
            const treeState = $togglable.attr('data-tree-state');
            $togglable.attr('data-tree-state', treeState === 'collapsed' ? 'expanded' : 'collapsed');
        };

        // 'Add/Delete children', 'Add live copies', 'Add references', 'Delete item'
        onPreparePackageEntriesChanges(e) {
            if (!this.$selectionItems.length) return;
            const action = e.target.closest('[data-path]').dataset.path;
            const payload = [];
            this.$selectionItems.each((i, item) => {
                const $item = $(item);
                if (!$item.hasClass('secondary')) payload.push($item.attr(TITLE_ATTR));
                if (action === 'delete') this.onDeleteEntry.call(this, $item, payload);
            });
            const referenceType = action === 'add' ? e.target.closest('[data-type]').getAttribute('data-type') || '' : '';
            this.onChangePackageEntries(action, referenceType, payload, EBUtils.packagePath);
        }

        onChangePackageEntries(action, referenceType, payload, packagePath) {
            this.wrapUiAsyncRequest(EBUtils.onProcessChangeRequest, EBUtils, action + (referenceType ? `/${referenceType}` : ''), { packagePath, payload })
                .then(() => EBUtils.showSuccessMessage());
        }

        onDeleteEntry($item, payload) {
            if (!$item.hasClass('secondary')) {
                $item.find('.secondary').each((i, item) => payload.push('[' + $(item).attr(TITLE_ATTR) + ',' + $(item).attr('data-subsidiary-title') + ']'));
            } else payload.push('[' + $item.attr(TITLE_ATTR) + ',' + $item.attr('data-subsidiary-title') + ']');
        };

        onHandleData(data, text) {
            const dialog = EBUtils.openLogsDialog(data.log, text);
            const assetText = data.dataSize === 0 ?
                'There are no assets in the package' :
                '<h4>Approximate size of the package: ' + EBUtils.bytesToSize(data.dataSize) + '</h4>';
            $(dialog.content).append('<div>' + assetText + '</div>');
            setTimeout(() => $(dialog.content).children('div').last()[0].scrollIntoView(false));
        }

        onHandleReplicatePackage() {
            const replicateBtn = {
                text: 'Replicate',
                primary: true,
                handler: this.onReplicatePackage.bind(this)
            };
            foundationRegistryAPI.prompt('Please confirm', 'Replicate this package?', 'notice', [{ text: 'Cancel' }, replicateBtn]);
        }

        onReplicatePackage() {
            this.wrapUiAsyncRequest(EBUtils.replicateRequest, EBUtils)
                .then((data) => data && this.onHandleData(data, 'Replication'));
        }

        onInstallPackage() {
            const dialog = document.querySelector('#installDialog');
            if (dialog) dialog.show();
        };

        onDeletePackage() {
            const { packagePath } = EBUtils;
            if (!packagePath) return;
            const packageName = packagePath.split('/').pop();
            const message = $(document.createElement('div'));
            $(document.createElement('p')).text('You are going to delete the following package:').appendTo(message);
            $(document.createElement('p')).html($(document.createElement('b'))).text(packageName).appendTo(message);
            const deleteBtn = {
                text: 'Delete',
                warning: true,
                handler: () => EBUtils.deleteRequest()
            };
            foundationRegistryAPI.prompt('Delete', message.html(), 'notice', [{ text: 'Cancel' }, deleteBtn]);
        };

        onHandleInstallPackageForm(e) {
            e.preventDefault();
            const $form = $(e.target.closest('form'));
            this.wrapUiAsyncRequest(this.submitInstallPackageForm, this, $form);
        }

        onLoad() {
            const { packagePath } = EBUtils;
            if (packagePath && packagePath.length > 0) {
                EBUtils.getPackageInfo(packagePath).catch(() => $('#editDialogButton').trigger('click'));
            } else {
                $('#editDialogButton').trigger('click');
            }
        }

        onDownloadPackage() {
            window.location.href = EBUtils.packagePath;
        }

        onMainMenuNav() {
            window.location.replace(BACKPACK_PATH);
        }

        async buildPackage(isTest, isDownload) {
            const data = await EBUtils.buildRequest(isTest, this.referencedResources);
            if (!data.log) throw new Error('No log data received during package build');
            if (!isTest) {
                const dialog = EBUtils.openLogsDialog(data.log, 'Build');
                await EBUtils.updateLog(data.packageStatus, data.log.length, dialog);
                isDownload && this.onDownloadPackage();
            } else {
                this.onHandleData(data, 'Test Build');
            }
        }

        async submitInstallPackageForm($form) {
            const data = await EBUtils.onProcessChangeRequest('package/install', $form.serialize());
            if (!data.log) throw new Error('No log data received during package installation');
            const dialog = EBUtils.openLogsDialog(data.log, 'Install');
            await EBUtils.updateLog(data.packageStatus, data.log.length, dialog);
        }
    }
})(Granite, Granite.$, Granite.EBUtils = Granite.EBUtils || {});
