(function (Granite, $) {
       'use strict';

       const path = new URL(window.location.href).searchParams.get('path');

       function openPackageDialog(success, error) {
            //todo looking for another way for opening
            $('#editDialogButton').trigger('click');
       }

       function updatePackageData(data) {
            //todo set package info to editDialog
            console.log(data);
       }

       function getPackageInfo(packagePath, updateFunction, errorFunction) {
           $.ajax({
               url: '/services/backpack/package',
               data: {path: packagePath},
               success: updateFunction,
               error: errorFunction
           });
       }

       $(window).on('load', function() {
          if (path && path.length > 0) {
              getPackageInfo(path, function (data) {
                  updatePackageData(data);
              }, function (data) {
                  openPackageDialog()
              });
          } else {
               openPackageDialog()
          }
       });

})(Granite, Granite.$);
