$(function () {
    var path = window.location.href.split('.html')[1];
    var $packageName = $('#packageName'),
        $version = $('version'),
        $lastBuilt = $('lastBuilt'),
        $filters = $('#filters'),
        $buildButton = $('#buildButton'),
        $testBuildButton = $('#testBuildButton'),
        $buildLog = $('#buildLog');
    if (path) {
        getPackageInfo(path, function (data) {
            $packageName.text('Package name: ' + data.packageName);
            $version.text('Package version: '+ data.version);
            if (data.packageBuilt) {
                $lastBuilt.text('Last built: ' + data.packageBuilt);
            }
            var filters = '';
            if (data.paths) {
                $.each(data.paths, function (index, value) {
                    filters = filters + '<div>' + value + '</div>'
                });
                $filters.append(filters);
            }
        });
    }

    $buildButton.click(function () {
        $.ajax({
            type: 'POST',
            url: '/services/backpack/buildPackage',
            data: {path: path},
            success: function () {
                setTimeout(updateLog, 1000);
            },
            dataType: 'json'
        });
    });

    function updateLog() {
        $.ajax({
            url: '/services/backpack/buildPackage',
            data: {path: path},
            success: function (data) {
                if (data.buildLog) {
                    $.each(data.buildLog, function (index, value) {
                        $buildLog.append('<div>' + value + '</div>');
                    });
                }
                if (!data.packageBuilt) {
                    setTimeout(updateLog, 1000);
                }
            }
        })
    }

    function getPackageInfo(path, updateFunction) {
        $.ajax({
            url: '/services/backpack/packageInfo',
            data: {path: path},
            success: updateFunction,
        });
    }
});