(function (Granite, $) {
  'use strict';

  const packagePath = new URL(window.location.href).searchParams.get('packagePath');
  const foundationUi = $(window).adaptTo('foundation-ui');

  class EBUtils {


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
        error: (data) => console.log(data),
        beforeSend: () => foundationUi.wait(),
        complete: () => foundationUi.clearWait()
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
        success: (data) => callback(data),
        beforeSend: () => foundationUi.wait(),
        complete: () => foundationUi.clearWait(),
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

})(Granite, Granite.$);