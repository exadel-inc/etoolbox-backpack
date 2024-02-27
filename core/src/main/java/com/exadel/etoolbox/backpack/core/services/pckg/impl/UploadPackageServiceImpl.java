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
package com.exadel.etoolbox.backpack.core.services.pckg.impl;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.services.pckg.BasePackageService;
import com.exadel.etoolbox.backpack.core.services.pckg.PackageInfoService;
import com.exadel.etoolbox.backpack.core.services.pckg.UploadPackageService;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.exadel.etoolbox.backpack.core.dto.response.PackageStatus.ERROR;

/**
 * Implements {@link UploadPackageService} to provide upload package operation
 */
@Component(service = UploadPackageService.class)
public class UploadPackageServiceImpl implements UploadPackageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadPackageServiceImpl.class);

    @Reference
    private PackageInfoService packageInfoService;

    @Reference
    private BasePackageService basePackageService;

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public PackageInfo uploadPackage(final Session session,
                                     final byte[] fileUploadBytesArray,
                                     final boolean forceUpdate) {
        File fileUpload = null;
        JcrPackage uploadedPackage = null;

        PackageInfo packageInfo = new PackageInfo();

        if (session != null && fileUploadBytesArray != null) {
            JcrPackageManager packageManager = basePackageService.getPackageManager(session);
            try {
                fileUpload = getFile(fileUploadBytesArray);
                String nameHint = "uploaded_package_name";
                final boolean isTempFile = true;
                final boolean strict = true;

                uploadedPackage = packageManager.upload(fileUpload, isTempFile, forceUpdate, nameHint, strict);

                return packageInfoService.getPackageInfo(uploadedPackage);
            } catch (Exception e) {
                LOGGER.error("Cannot upload package: {}", e.getMessage(), e);
                packageInfo.addLogMessage(e.getMessage());
                packageInfo.setPackageStatus(ERROR);
            } finally {
                if (fileUpload != null) {
                    fileUpload.delete();
                }

                if (uploadedPackage != null) {
                    uploadedPackage.close();
                }
            }
        } else {
            packageInfo.addLogMessage("An incorrect value of parameter(s)");
            packageInfo.setPackageStatus(ERROR);
        }

        return packageInfo;
    }

    private File getFile(final byte[] fileUpload) throws IOException {
        File tmpFile = null;
        tmpFile = allocateTmpFile();
        try (FileOutputStream out = new FileOutputStream(tmpFile)) {
            out.write(fileUpload);
        } catch (IOException e) {
            LOGGER.error("Cannot create temp archive.", e);
        }

        return tmpFile;
    }

    private File allocateTmpFile() throws IOException {
        return File.createTempFile("crx_backpack__", ".zip");
    }

}
