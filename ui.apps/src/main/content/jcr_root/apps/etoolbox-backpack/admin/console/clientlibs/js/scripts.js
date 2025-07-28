(function (Granite, $, EBUtils) {
    'use strict';

    const DELETE_TITLE = Granite.I18n.get("Delete");
    const CANCEL_TITLE = Granite.I18n.get("Cancel");

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
    const LIVE_COPIES_SEL = '#liveCopiesAction'; // "Add live copies" button
    const INCLUDE_CHILDREN_SEL = '#includeChildrenAction'; // "Include children" button
    const EXCLUDE_CHILDREN_SEL = '#excludeChildrenAction'; // "Exclude children" button
    const DELETE_SEL = '#deleteAction'; // "Delete button
    const INSTALL_SEL = '#installAction'; // "Install" button
    const REPLICATE_SEL = '#replicateAction'; // "Replicate" button

    // calls when dom is loaded
    $(() => {
        const backpack = new EBackpack();
        backpack.$pulldown.attr(DISABLED_MARKER, true);
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
        get $pulldown() {
            return $('.selection-pulldown');
        }

        bindEvents() {
            $document.off('backpack');
            $document.on('click.backpack', `.${COLLECTION_ITEM_CLASS}.result-row`, this.onPackageEntryClick.bind(this));
            $document.on('click.backpack', '.toggler', this.onTogglerClick);
            $document.on('click.backpack', INCLUDE_CHILDREN_SEL, this.onChangePackageEntries.bind(this, 'add/children'));
            $document.on('click.backpack', EXCLUDE_CHILDREN_SEL, this.onChangePackageEntries.bind(this, 'delete/children'));
            $document.on('click.backpack', LIVE_COPIES_SEL, this.onChangePackageEntries.bind(this, 'add/liveCopies'));
            $document.on('click.backpack', '.add-references-action', this.onChangePackageEntries.bind(this, 'add'));
            $document.on('click.backpack', DELETE_SEL, this.onChangePackageEntries.bind(this, 'delete'));
            $document.on('click.backpack', '#downloadAction', () => window.location.href = packagePath);
            $document.on('click.backpack', '#testBuildAction', this.onTestBuild.bind(this));
            $document.on('click.backpack', REPLICATE_SEL, this.onReplicate.bind(this));
            $document.on('click.backpack', '#mainMenuAction, #cancelButton', () => window.location.replace(BACKPACK_PATH));
            $document.on('click.backpack', '#buildAction', () => EBUtils.onBuildAction(false));
            $document.on('click.backpack', '#buildAndDownloadAction', () => EBUtils.onBuildAction(true));
            $document.on('click.backpack', INSTALL_SEL, this.onInstallPackage);
            $document.on('click.backpack', '#deletePackageAction', this.onDeletePackage.bind(this));
            $document.on('submit.backpack', '#installForm', this.onHandleInstallForm);
            $window.on('load.backpack', this.onLoad);
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
                target.hasClass('primary') && this.$pulldown.removeAttr(DISABLED_MARKER);
            }
        }

        // Make package entries selectable
        onPackageEntryClick(e) {
            const target = $(e.target.closest(`.${COLLECTION_ITEM_CLASS}`));
            this.$pulldown.attr(DISABLED_MARKER, true);
            (e.ctrlKey ? this.packageEntriesCtrlClick : e.shiftKey ? this.packageEntriesShiftClick : this.packageEntriesClick).call(this, target);
            e.stopPropagation();

            $([LIVE_COPIES_SEL, DELETE_SEL, INCLUDE_CHILDREN_SEL, EXCLUDE_CHILDREN_SEL].join(',')).attr(DISABLED_MARKER, true);
            if (!this.$selectionItems) return;
            $(DELETE_SEL).removeAttr(DISABLED_MARKER);
            this.$selectionItems.each((index, item) => {
                if (!$(item).is('.primary')) return;
                $([EXCLUDE_CHILDREN_SEL, LIVE_COPIES_SEL, INCLUDE_CHILDREN_SEL].join(',')).removeAttr(DISABLED_MARKER);
                this.$pulldown.removeAttr(DISABLED_MARKER);
            });
        };

        // Make top-level package entries collapsible
        onTogglerClick() {
            const $togglable = $(this).closest(`.${COLLECTION_ITEM_CLASS}`);
            const treeState = $togglable.attr('data-tree-state');
            if (treeState === 'collapsed' || treeState === 'expanded') {
                $togglable.attr('data-tree-state', treeState === 'collapsed' ? 'expanded' : 'collapsed');
            }
        };

        // 'Add/Delete children', 'Add live copies', 'Add references', 'Delete item'
         onChangePackageEntries(action, e) {
            if (!this.$selectionItems) return;
            const payload = [];
            this.$selectionItems.each((i, item) => {
                const $item = $(item);
                if (!$item.hasClass('secondary')) payload.push($item.attr(TITLE_ATTR));
                if (action === 'delete') this.onDeleteItem.call(this, $item, payload);
            });
            const referenceType = action === 'add' ? e.target.closest('[data-type]').getAttribute('data-type') : '';
            EBUtils.doPost(`/services/backpack/${action}` + referenceType, {packagePath, payload}, EBUtils.showSuccessMessage);
         }

        onDeleteItem($item, payload) {
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
            setTimeout(() => $(dialog.content).children("div").last()[0].scrollIntoView(false));
        }

        onTestBuild() {
            const callback = (data) => data.log && this.onHandleData(data, 'Test Build');
            EBUtils.buildPackage(true, callback);
        }

        onReplicate() {
            const replicateBtn = {
                text: "Replicate",
                primary: true,
                handler: () => EBUtils.replicatePackage((data) => data.log && this.onHandleData(data, 'Replication'))
            }
            FOUNDATION_UI.prompt("Please confirm", "Replicate this package?", "notice", [{text: CANCEL_TITLE}, replicateBtn]);
        }

        onInstallPackage() {
            const dialog = document.querySelector('#installDialog');
            if (dialog) dialog.show();
        };

        onDeletePackage() {
            if (!packagePath) return;
            const packageName = packagePath.split('/').pop();
            const message = this.createEl("div");
            this.createEl("p").text(Granite.I18n.get("You are going to delete the following package:")).appendTo(message);
            this.createEl("p").html(this.createEl("b")).text(packageName).appendTo(message);
            const deleteBtn = {
                text: DELETE_TITLE,
                warning: true,
                handler: () => EBUtils.deleteAction()
            }
            FOUNDATION_UI.prompt(DELETE_TITLE, message.html(), "notice", [{text: CANCEL_TITLE}, deleteBtn]);
        };

        createEl(name) {
            return $(document.createElement(name));
        }

        onHandleInstallForm(e) {
            e.preventDefault();
            const form = $(this);
            EBUtils.doPost(form.attr('action'), form.serialize(), function(data) {
                const dialog = EBUtils.openLogsDialog(data.log, 'Install', 'Close');
                EBUtils.updateLog(data.packageStatus, data.log.length, dialog);
            });
        }

        onLoad() {
            if (packagePath && packagePath.length > 0) EBUtils.getPackageInfo(packagePath, () => EBUtils.openPackageDialog());
            else EBUtils.openPackageDialog();
        };
    }

})(Granite, Granite.$, EBUtils = Granite.EBUtils || {});
