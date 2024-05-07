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
package com.exadel.etoolbox.backpack.core.services.pckg;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;

import javax.jcr.Session;

/**
 * Represents a service running in an AEM instance responsible for uploading package
 */
public interface UploadPackageService {
    /**
     * Method upload package by byte[] array representation
     *
     * @param session              {@code Session} instance used to adapt to JcrPackageManager object.
     * @param fileUploadBytesArray {@code byte[]} instance used provides the content of the package.
     * @param forceUpdate          {@code boolean} if true existing packages will be replaced
     * @return {@code JcrPackageWrapper} the jcr-package with additional information
     */
    PackageInfo uploadPackage(Session session, byte[] fileUploadBytesArray, boolean forceUpdate);
}