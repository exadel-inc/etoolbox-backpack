(function (Granite, $) {
  'use strict';

  const packagePath = new URL(window.location.href).searchParams.get('packagePath') || ''
  const BACKPACK_PATH = '/tools/etoolbox/backpack.html';
  const FOUNDATION_UI = $(window).adaptTo('foundation-ui');

  class EBUtils {
    static buildRequest(testBuild, callback, referencedResources) {
      const options = {
        type: 'POST',
        url: '/services/backpack/package/build',
        dataType: 'json',
        ContentType : 'application/json',
        data: {
          packagePath,
          testBuild,
          referencedResources: JSON.stringify(referencedResources)
        }
      };
      return this._ajaxPost(options, callback);
    }

    static onProcessChangeRequest(action, data, success) {
      const options = {type: 'POST', url: `/services/backpack/${action}`, data};
      return this._ajaxPost(options, success);
    }

    static async _ajaxPost(options, success) {
      try {
        FOUNDATION_UI.wait();
        const res  = await $.ajax(options);
        res && success(res);
      } catch (e) {
        console.log(e);
      } finally {
        FOUNDATION_UI.clearWait();
      }
    }

    static showSuccessMessage() {
      const popup = new Coral.Alert();
      popup.header.innerHTML = 'INFO';
      popup.content.textContent = 'Package was successfully updated';
      popup.id = 'js-backpack-alert';
      document.body.append(popup);
      setTimeout(() => {
        $(popup).fadeOut();
        window.location.reload();
      }, 2000);
    }

    static async getPackageInfo(packagePath, errorFunction) {
      try {
        await $.ajax({
          url: '/services/backpack/package',
          data: {packagePath}
        });
      } catch {
        errorFunction();
      }
    }

    static replicateRequest(callback) {
      const options = {
        url: '/services/backpack/replicatePackage',
        type: 'POST',
        dataType: 'json',
        ContentType : 'application/json',
        data: {packagePath}
      }

      return this._ajaxPost(options, callback);
    }

    static async updateLog(packageStatus, logIndex, dialog) {
      try {
        if (packageStatus !== 'BUILD_IN_PROGRESS' && packageStatus !== 'INSTALL_IN_PROGRESS') return;
        const result = await $.ajax({
          url: '/services/backpack/package/build',
          data: {packagePath, latestLogIndex: logIndex},
          timeout: 1000,
        });

        if (result.log && result.log.length) {
          $.each(result.log, function (index, value) {
            $(dialog.content).append('<div>' + value + '</div>');
          });
          logIndex = logIndex + result.log.length;
          $(dialog.content).children('div').last()[0].scrollIntoView(false);
        }

        EBUtils.updateLog(result.packageStatus, logIndex, dialog);
      } catch (e) {
        console.log(e);
      }
    }

    static async deleteRequest() {
      const data = {
        _charset_: 'UTF-8',
        cmd: 'deletePage',
        path: packagePath,
        force: true
      };

      try {
        await $.post(Granite.HTTP.externalize('/bin/wcmcommand'), data);
        EBUtils._showAlert('Package deleted', 'Delete', 'warning', () => window.location.replace(BACKPACK_PATH));
      } catch (e) {
        console.log(e);
      }
    }

    static _showAlert(message, title, type, callback) {
      const options = [{
            id: 'ok',
            text: 'OK',
            primary: true
          }];

      FOUNDATION_UI.prompt(title || 'Error', message || 'Unknown Error', type, options, callback);
    }

    static bytesToSize(bytes) {
      const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
      if (typeof bytes !== 'number' || bytes <= 0) return '0 Bytes';
      const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
      return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i];
    }

    static openLogsDialog(init, title, submitText) {
      const dialog = new Coral.Dialog();
      dialog.id = 'jsBackpackLogsDialog';
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