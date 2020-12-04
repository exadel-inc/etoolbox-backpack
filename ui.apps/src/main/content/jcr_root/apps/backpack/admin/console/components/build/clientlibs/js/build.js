/*
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

$(function () {
    var path = new URL(window.location.href).searchParams.get('path')
        || (window.location.href.indexOf('.html/') ? window.location.href.split('.html').pop() : ''),
        packageGroupPath,
        packageName,
        goBackLink;
    var BUILT = 'BUILT',
        BUILD_IN_PROGRESS = 'BUILD_IN_PROGRESS',
        COMMAND_URL = Granite.HTTP.externalize("/bin/wcmcommand");
    $packageName = $('#packageName'),
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
        $deleteButton = $("#deleteButton"),
        $buildLog = $('#buildLog'),
        $buildLogWrapper = $('#build-log-wrapper'),
        $containerInner = $('.content-container-inner'),
        $errorContainer = $('.content-error-container'),
        $closeLink = $('#shell-propertiespage-closeactivator'),
        $goBackSection = $('#goBackLink');
    if (path) {
        var lastIndex = path.lastIndexOf('/');
        packageName = path.substring(lastIndex + 1);
        packageGroupPath = path.substring(0, lastIndex);
        if (packageGroupPath) {
            goBackLink = "/backpack.html/?group=" + packageGroupPath;
        } else {
            goBackLink = "/backpack.html/?group=/etc/packages/backpack";
        }
        $closeLink.attr('href', goBackLink);
        $goBackSection.find('a').attr('href', goBackLink);
    }
    if (path && $packageName.length !== 0) {
        disableAllActions();
        getPackageInfo(path, function (data) {
            if (data.packageStatus === BUILT) {
                packageBuilt();
            } else if (data.packageStatus === BUILD_IN_PROGRESS) {
                updateLog(0);
            } else {
                packageCreated();
            }

            updatePackageDisplayInfo(data);

            function initFilters() {
                if (data.paths) {
                    var filters = '';
                    $.each(data.paths, function (index, value) {
                        filters = filters + '<div>' + value + '</div>'
                    });
                    $filters.append(filters);
                }
            }

            function initReferencedResources() {
                if (data.referencedResources) {
                    $.each(data.referencedResources, function (key, referencedValue) {
                        var checkbox = new Coral.Checkbox().set({
                            label: {
                                innerHTML: key
                            },
                            value: key,
                            name: 'referencedResources'
                        });
                        $referencedResources.append(checkbox);

                        var listItem = '<li><h4>' + key + '</h4>';
                        $.each(referencedValue, function (index, value) {
                            listItem += '<div>' + value + '</div>'
                        });
                        listItem += '</li>';
                        $referencedResourcesList.append(listItem)
                    });
                }

            }

            initFilters();
            initReferencedResources();
        }, function (data) {
            $errorContainer.removeAttr('hidden');
            $containerInner.attr('hidden', true);
            $('#error').find('h3').text('Package at path ' + path + ' does not exist');
        });
    }

    $testBuildButton.click(function () {
        buildPackage(true);
    });


    $buildButton.click(function () {
        disableAllActions();
        buildPackage(false);
    });

    $downloadBtn.click(function () {
        downloadPackage();
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
                    if (data.log) {
                        $.each(data.log, function (index, value) {
                            $buildLog.append('<div>' + value + '</div>');
                        });
                        $buildLog.append('<h4>Approximate referenced resources size: ' + bytesToSize(data.dataSize) + '</h4>');
                        scrollLog();
                    }
                } else {
                    updateLog(0);
                }
            },
            dataType: 'json'
        });
    }

    function disableAllActions() {
        $downloadBtn.prop('disabled', true);
        $testBuildButton.prop('disabled', true);
        $buildButton.prop('disabled', true);
    }

    function packageBuilt() {
        $buildButton.text('Rebuild');
        $downloadBtn.prop('disabled', false);
        $testBuildButton.prop('disabled', false);
        $buildButton.prop('disabled', false);
    }

    function packageCreated() {
        $testBuildButton.prop('disabled', false);
        $buildButton.prop('disabled', false);
    }

    function getLastBuiltDate(packageBuiltDate) {
        if (packageBuiltDate && typeof packageBuiltDate === 'object') {
            return new Date(packageBuiltDate.year,
                packageBuiltDate.month,
                packageBuiltDate.dayOfMonth,
                packageBuiltDate.hourOfDay,
                packageBuiltDate.minute,
                packageBuiltDate.second).toISOString();
        } else if (packageBuiltDate && typeof packageBuiltDate === 'string') {
            return new Date(packageBuiltDate).toISOString();
        }
        return 'never';
    }

    function bytesToSize(bytes) {
        var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        if (bytes === 0) return '0 Bytes';
        var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
        return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i];
    }

    function updateLog(logIndex) {
        $.ajax({
            url: '/services/backpack/buildPackage',
            data: {path: path, latestLogIndex: logIndex},
            success: function (data) {
                if (data.log && data.log.length) {
                    $.each(data.log, function (index, value) {
                        $buildLog.append('<div>' + value + '</div>');
                    });
                    logIndex = logIndex + data.log.length;

                    scrollLog();
                }
                if (data.packageStatus === BUILD_IN_PROGRESS) {
                    setTimeout(function () {
                        updateLog(logIndex);
                    }, 1000);

                } else if (data.packageStatus === BUILT) {
                    packageBuilt();
                    updatePackageDisplayInfo(data);
                }
            }
        })
    }

    function getPackageInfo(packagePath, updateFunction, errorFunction) {
        $.ajax({
            url: '/services/backpack/packageInfo',
            data: {path: packagePath},
            success: updateFunction,
            statusCode: {404: errorFunction}
        });
    }

    function scrollLog() {
        $buildLogWrapper.stop().animate({
            scrollTop: $buildLogWrapper[0].scrollHeight
        }, 800);
        $buildLogWrapper[0].scrollIntoView();
    }

    function updatePackageDisplayInfo(data) {
        $packageName.html('Package name: ' + data.packageName);
        $name.text(data.packageNodeName);
        $version.text('Package version: ' + data.version);
        $lastBuilt.val(getLastBuiltDate(data.packageBuilt));
        if (data.dataSize) {
            $packageSize.text('Package size: ' + bytesToSize(data.dataSize));
        }
    }


    $deleteButton.click(function () {
        if (!path) {
            return;
        }

        var ui = $(window).adaptTo("foundation-ui");
        var message = createEl("div");
        var intro = createEl("p").appendTo(message);

        intro.text(Granite.I18n.get("You are going to delete the following package:"));
        createEl("p").html(createEl("b")).text(packageName).appendTo(message);
        ui.prompt(Granite.I18n.get("Delete"), message.html(), "notice", [{
            text: Granite.I18n.get("Cancel")
        }, {
            text: Granite.I18n.get("Delete"),
            warning: true,
            handler: function () {
                deleteAction();
            }
        }]);

    });


    function deleteAction() {

        var data = {
            _charset_: "UTF-8",
            cmd: "deletePage",
            path: path,
            force: true
        };

        $.post(COMMAND_URL, data).done(function () {
            showAlert("Package deleted", "Delete", function () {
                window.location.href = goBackLink;
            });
        });
    }

    function showAlert(message, title, callback) {
        var fui = $(window).adaptTo("foundation-ui"),
            options = [{
                id: "ok",
                text: "OK",
                primary: true
            }];

        message = message || "Unknown Error";
        title = title || "Error";

        fui.prompt(title, message, "warning", options, callback);
    }

    function createEl(name) {
        return $(document.createElement(name));
    }

});