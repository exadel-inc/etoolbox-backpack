(function (Granite, $) {
  'use strict';

  const packagePath = new URL(window.location.href).searchParams.get('packagePath');
  const BACKPACK_PATH = '/tools/etoolbox/backpack.html';
  const FOUNDATION_UI = $(window).adaptTo('foundation-ui');

  class EBUtils {
    // Handles click on 'Build/download' button
    static onBuildAction(isDownload) {
      this.buildPackage(false, (data) => {
        const dialog = this.openLogsDialog.call(this, data.log, 'Build', isDownload ? 'Download' : 'Close');
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
        error: (data) => console.log(data),
        beforeSend: () => FOUNDATION_UI.wait(),
        complete: () => FOUNDATION_UI.clearWait()
      });
    }

    static showSuccessMessage() {
      const popup = new Coral.Alert();
      popup.header.innerHTML = 'INFO';
      popup.content.textContent = 'Package was successfully updated';
      popup.id = 'references-added-alert';
      document.body.append(popup);
      setTimeout(() => {
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
        dataType: 'json',
        data: {
          packagePath,
          testBuild,
          referencedResources: JSON.stringify(referencedResources)
        },
        success: (data) => callback(data),
        beforeSend: () => FOUNDATION_UI.wait(),
        complete: () => FOUNDATION_UI.clearWait()
      });
    }

    static replicatePackage(callback) {
      $.ajax({
        url: '/services/backpack/replicatePackage',
        type: "POST",
        dataType: "json",
        ContentType : 'application/json',
        data: {packagePath: packagePath},
        success: (data)  => callback(data),
        beforeSend: () => FOUNDATION_UI.wait(),
        complete: () => FOUNDATION_UI.clearWait()
      })
    }

    static updateLog(packageStatus, logIndex, dialog) {
      if (packageStatus === 'BUILD_IN_PROGRESS' || packageStatus === 'INSTALL_IN_PROGRESS') {
        setTimeout(() =>{
          $.ajax({
            url: '/services/backpack/package/build',
            data: {packagePath, latestLogIndex: logIndex},
            success: (data) => {
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

    static deleteAction() {
      const data = {
        _charset_: "UTF-8",
        cmd: "deletePage",
        path: packagePath,
        force: true
      };

      $.post(Granite.HTTP.externalize("/bin/wcmcommand"), data).done(() => {
        this.showAlert("Package deleted", "Delete", "warning", () => window.location.replace(BACKPACK_PATH));
      });
    }

    static showAlert(message, title, type, callback) {
      const options = [{
            id: "ok",
            text: "OK",
            primary: true
          }];

      FOUNDATION_UI.prompt(title || "Error", message || "Unknown Error", type, options, callback);
    }

    static bytesToSize(bytes) {
      const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
      if (bytes === 0) return '0 Bytes';
      const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
      return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i];
    }

    static openLogsDialog(init, title, submitText) {
      const dialog = new Coral.Dialog();
      dialog.id = 'LogsDialog';
      dialog.header.innerHTML = `${title} Logs`;
      dialog.footer.innerHTML = `<button is="coral-button" variant="primary" coral-close>${submitText}</button>`;

      if (init && init.length > 0) {
        $.each(init, (index, value) => $(dialog.content).append('<div>' + value + '</div>'));
      }

      dialog.on('coral-overlay:close', (event) => {
        event.preventDefault();
        setTimeout(() => {
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

})(Granite, Granite.$);