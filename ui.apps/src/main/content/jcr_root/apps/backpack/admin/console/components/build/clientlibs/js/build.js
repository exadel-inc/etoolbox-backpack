$(function () {
    var path = window.location.href.split('.html')[1];
    var $packageName = $('#packageName'),
        $name = $('#name'),
        $version = $('#version'),
        $lastBuilt = $('#lastBuilt-time'),
        $filters = $('#filters'),
        $packageSize = $('#packageSize'),
        $buildButton = $('#buildButton'),
        $referencedResources = $('#referencedResources').find('div'),
        $referencedResourcesList = $('#referencedResourcesList'),
        $testBuildButton = $('#testBuildButton'),
        $downloadBtn = $('#downloadBtn'),
        $buildLog = $('#buildLog');
        $downloadBtn.hide();
    if (path && $packageName.length != 0) {
        getPackageInfo(path, function (data) {
            if (data.packageBuilt) {
                $downloadBtn.show();
            }
            $packageName.html('Package name: ' + data.packageName);
            $name.text(data.packageNodeName);
            $version.text('Package version: ' + data.version);
            $lastBuilt.val(getLastBuiltDate(data.packageBuilt));
            if(data.packageBuilt) {
              $buildButton.text('Rebuild');
            }
            var filters = '';
            if (data.paths) {
                $.each(data.paths, function (index, value) {
                    filters = filters + '<div>' + value + '</div>'
                });

                $filters.append(filters);

                if (data.referencedResources) {
                    $.each(data.referencedResources, function (key, value) {
                        var checkbox = new Coral.Checkbox().set({
                            label: {
                                innerHTML: key
                            },
                            value: key,
                            name: 'referencedResources'
                        });
                        $referencedResources.append(checkbox);

                        var listItem = '<li><h4>' + key + '</h4>';
                        $.each(value, function (index, value) {
                            listItem += '<div>' + value + '</div>'
                        });
                        listItem += '</li>';
                        $referencedResourcesList.append(listItem)
                    });
                }
            }
            if (data.dataSize) {
                $packageSize.text('Package size: ' + bytesToSize(data.dataSize));
            }
        });
    }

    $testBuildButton.click(function () {
        buildPackage(true);
    });


    $buildButton.click(function () {
        buildPackage(false);
    });

    $downloadBtn.click(function () {
        downloadPackage(false);
    });

    function downloadPackage() {
        window.location.href = path;
    }

    function buildPackage(testBuild) {
        var referencedResources = [];
        $('input[name="referencedResources"]:checked').each(function () {
            referencedResources.push(this.value);
        });
        $.ajax({
            type: 'POST',
            url: '/services/backpack/buildPackage',
            data: {
                path: path,
                referencedResources: referencedResources,
                testBuild: testBuild
            }, success: function (data) {
                $buildLog.empty();
                if (testBuild) {
                    if (data.buildLog) {
                        $.each(data.buildLog, function (index, value) {
                            $buildLog.append('<div>' + value + '</div>');
                        });
                        $buildLog.append('<h4>Approximate referenced resources size: ' + bytesToSize(data.dataSize) + '</h4>');
                    }
                } else {
                    setTimeout(updateLog, 1000);
                    $buildButton.text('Rebuild');
                    $downloadBtn.show();
                }
            },
            dataType: 'json'
        });
    }

    function getLastBuiltDate(packageBuiltDate) {
        if(packageBuiltDate) {
            return new Date(packageBuiltDate.year,
                packageBuiltDate.month,
                packageBuiltDate.dayOfMonth,
                packageBuiltDate.hourOfDay,
                packageBuiltDate.minute,
                packageBuiltDate.second).toISOString();
        }
        return 'never';

    }

    function bytesToSize(bytes) {
        var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        if (bytes == 0) return '0 Byte';
        var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
        return (bytes / Math.pow(1024, i)).toFixed( 1) + ' ' + sizes[i];
    }

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