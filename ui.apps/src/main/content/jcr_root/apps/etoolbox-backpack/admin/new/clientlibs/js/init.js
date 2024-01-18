(function (Granite, $) {
       'use strict';

       const packagePath = new URL(window.location.href).searchParams.get('packagePath');

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
               data: {'packagePath': packagePath},
               success: updateFunction,
               error: errorFunction
           });
       }

       $(window).on('load', function() {
          if (packagePath && packagePath.length > 0) {
              getPackageInfo(packagePath, function (data) {
                  updatePackageData(data);
              }, function (data) {
                  openPackageDialog()
              });
          } else {
               openPackageDialog()
          }
       });

})(Granite, Granite.$);
