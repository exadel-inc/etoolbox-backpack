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
"use strict"
$(function () {
    var path = new URL(window.location.href).searchParams.get('path')
        || (window.location.href.indexOf('.html/') ? window.location.href.split('.html').pop() : ''),
        packageGroupPath,
        packageName,
        goBackLink;
    var BUILT = 'BUILT',
        BUILD_IN_PROGRESS = 'BUILD_IN_PROGRESS',
        COMMAND_URL = Granite.HTTP.externalize("/bin/wcmcommand"),
        DIALOG_MODAL_URL = '/mnt/overlay/etoolbox-backpack/admin/console/page/content/editpackagedialog.html?packagePath=',
        BUILD_PAGE_URL = '/tools/etoolbox/backpack/buildPackage.html?path=',
        REPLICATE_URL = '/services/backpack/replicatePackage',
        INSTALL = 'INSTALL',
        INSTALL_IN_PROGRESS = 'INSTALL_IN_PROGRESS';
    var $packageName = $('#packageName'),
        $name = $('#name'),
        $version = $('#version'),
        $lastBuilt = $('#lastBuilt-time'),
        $filters = $('#filters'),
        $packageSize = $('#packageSize'),
        $buildButton = $('#buildButton'),
        $referencedResourcesList = $('#referencedResourcesList'),
        $testBuildButton = $('#testBuildButton'),
        $downloadBtn = $('#downloadBtn'),
        $deleteButton = $("#deleteButton"),
        $buildLog = $('#buildLog'),
        $buildLogWrapper = $('#build-log-wrapper'),
        $containerInner = $('.content-container-inner'),
        $errorContainer = $('.content-error-container'),
        $closeLink = $('#shell-propertiespage-closeactivator'),
        $goBackSection = $('#goBackLink'),
        $query = $('#query'),
        $lastInstalled = $('#lastInstalled-time'),
        $lastReplicated = $('#lastReplicated-time'),
        $installButton = $('#installButton'),
        $replicateButton = $('#replicateBtn');
    if (path) {
        var lastIndex = path.lastIndexOf('/');
        packageName = path.substring(lastIndex + 1);
        packageGroupPath = path.substring(0, lastIndex);
        if (packageGroupPath) {
            goBackLink = "/tools/etoolbox/backpack.html/?group=" + packageGroupPath;
        } else {
            goBackLink = "/tools/etoolbox/backpack.html/?group=/etc/packages/EToolbox_BackPack";
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
            } else if (data.packageStatus === INSTALL) {
                packageBuilt();
                packageInstall();
            } else if (data.packageStatus === INSTALL_IN_PROGRESS) {
                packageBuilt();
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

                    var accordion = new Coral.Accordion();

                    $.each(data.referencedResources, function (key, referencedValue) {
                        var checkbox = '<coral-checkbox coral-interactive="true" name="referencedResources" value="' + key + '">' +
                            key + ' (' + referencedValue.length + ')' +
                            '</coral-checkbox>';

                        var listItem = '';
                        $.each(referencedValue, function (index, value) {
                            var itemCheckbox = '<coral-checkbox name="referencedResourcesItem" value="' + value + '">' + value + '</coral-checkbox>'
                            listItem += '<div>' + itemCheckbox + '</div>'
                        });
                        accordion.items.add({
                            label: {
                                innerHTML: checkbox
                            },
                            content: {
                                innerHTML: listItem
                            },
                            disabled: false
                        });
                    });
                    $referencedResourcesList.append(accordion.outerHTML);

                    $(document).on("change", '[name ="referencedResources"]', function (e) {
                        if (this.checked) {
                            $(this).closest('coral-accordion-item').find('[name ="referencedResourcesItem"]').each(function (index, innerInput) {
                                innerInput.checked = true;
                            });
                        } else {
                            $(this).closest('coral-accordion-item').find('[name ="referencedResourcesItem"]').each(function (index, innerInput) {
                                innerInput.checked = false;
                            });
                        }
                    });

                    $(document).on("change", '[name ="referencedResourcesItem"]', function (e) {
                        var currentCheckboxVal = this.checked;
                        var indeterminate = false;
                        $(this).closest('coral-accordion-item').find('[name ="referencedResourcesItem"]').each(function (index, innerInput) {
                            if (innerInput.checked !== currentCheckboxVal) {
                                indeterminate = true;
                                return false;
                            }
                        });
                        var referencedResourcesTypeCheckbox = $(this).closest('coral-accordion-item').find('coral-checkbox[name ="referencedResources"]');
                        if (referencedResourcesTypeCheckbox.length === 1) {
                            referencedResourcesTypeCheckbox[0].checked = indeterminate ? true : currentCheckboxVal;
                            referencedResourcesTypeCheckbox[0].indeterminate = indeterminate;
                        }
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

    $installButton.click(function () {
        var dialog = document.querySelector('#installDialog');
        dialog.show();
        $('#installSubmitBtn').click(function() {
            $("#installForm").submit(function(e) {
                e.preventDefault();
                var form = $(this);
                var url = form.attr('action');
                $.ajax({
                    type: "POST",
                    url: url,
                    data: form.serialize(),
                    success: function(data) {
                        $buildLog.empty();
                        updateLog(0);
                    }
                });
            });
        });
    });

    /**
     * Invokes replication confirmation window
     */
    $replicateButton.click(function () {
        var fui = $(window).adaptTo("foundation-ui");
        fui.prompt("Please confirm", "Replicate this package?", "notice", [{
            text: Granite.I18n.get("Cancel")
        }, {
            text: "Replicate",
            primary: true,
            handler: function () {
                replicatePackage();
            }
        }]);
    });

    /**
     * Replication helper function: sends 'post' request with path information to server
     * and updates log information
     */
    function replicatePackage() {
        $.ajax({
            url: REPLICATE_URL,
            type: "POST",
            dataType: "json",
            ContentType : 'application/json',
            data: {path: path},
            success: function (data) {
                $buildLog.empty();
                if (data.log) {
                    updateLog(0);
                    scrollLog();
                }
            }
        })
    }

    function downloadPackage() {
        window.location.href = path;
    }

    function buildPackage(testBuild) {
        var referencedResources = {};
        $('input[name="referencedResources"]:checked').each(function () {
            var resources = [];
            var currentResourceType = this.value;
            $(this).closest('coral-accordion-item').find('input[name="referencedResourcesItem"]:checked').each(function () {
                resources.push(this.value);
            });
            referencedResources[currentResourceType] = resources;
        });
        $.ajax({
            type: 'POST',
            url: '/services/backpack/buildPackage',
            data: {
                path: path,
                referencedResources: JSON.stringify(referencedResources),
                testBuild: testBuild
            }, success: function (data) {
                $buildLog.empty();
                if (testBuild) {
                    if (data.log) {
                        $.each(data.log, function (index, value) {
                            $buildLog.append('<div>' + value + '</div>');
                        });
                        $buildLog.append('<h4>Approximate size of the assets in the package: ' + bytesToSize(data.dataSize) + '</h4>');
                        scrollLog();
                    } else {
                        $buildLog.append(<h4>There are no assets in the package</h4>);
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
        $installButton.prop('disabled' ,true);
        $replicateButton.prop('disabled', true);
    }

    function packageBuilt() {
        $buildButton.text('Rebuild');
        $downloadBtn.prop('disabled', false);
        $testBuildButton.prop('disabled', false);
        $buildButton.prop('disabled', false);
        $installButton.prop('disabled', false);
        $replicateButton.prop('disabled', false);
    }

    function packageCreated() {
        $testBuildButton.prop('disabled', false);
        $buildButton.prop('disabled', false);
    }

    function packageInstall() {
        $installButton.text('Reinstall');
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
                if (data.packageStatus === BUILD_IN_PROGRESS || data.packageStatus === INSTALL_IN_PROGRESS) {
                    setTimeout(function () {
                        updateLog(logIndex);
                    }, 1000);

                } else if (data.packageStatus === BUILT) {
                    packageBuilt();
                    updatePackageDisplayInfo(data);
                } else if (data.packageStatus === INSTALL) {
                    packageInstall();
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
        if (data.query) {
            $query.text('SQL2 Query: ' + data.query);
        } else {
            $query.hide();
        }
        if (data.dataSize) {
            $packageSize.text('Package size: ' + bytesToSize(data.dataSize));
        }
        $lastInstalled.val(getLastBuiltDate(data.packageInstalled));
        $lastReplicated.val(getLastBuiltDate(data.packageReplicated));
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
            showAlert("Package deleted", "Delete", "warning", function () {
                window.location.href = goBackLink;
            });
        });
    }

    function showAlert(message, title, type, callback) {
        var fui = $(window).adaptTo("foundation-ui"),
            options = [{
                id: "ok",
                text: "OK",
                primary: true
            }];

        message = message || "Unknown Error";
        title = title || "Error";

        fui.prompt(title, message, type, options, callback);
    }

    function createEl(name) {
        return $(document.createElement(name));
    }

    /**
     * Registers new behaviour for Edit Dialog. Sets src according to the current Build page's Path variable.
     * Handler returns false that indicates that current handler has been done and system can be proceed
     * with next handler.
     */
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "foundation.dialog",
        handler: function(name, el, config, collection, selections) {
            if (path) {
                config.data.src = DIALOG_MODAL_URL + path;
            }
            return false;
        }
    });

    /**
     * Registers new behaviour for the Edit Dialog's Success Dialog. Instead of default Success Dialog
     * handler show custom dialog window specified especially for the Build page. Handler returns true
     * that indicate that current handler will be last one.
     */
    $(window).adaptTo("foundation-registry").register("foundation.form.response.ui.success", {
        name: "foundation.prompt.open",
        handler: function(name, el, config, collection, selections) {
            showAlert("Your package has been updated.", "Success", "success",function () {
                window.location.href = BUILD_PAGE_URL + path;
            });
            return true;
        }
    });

});