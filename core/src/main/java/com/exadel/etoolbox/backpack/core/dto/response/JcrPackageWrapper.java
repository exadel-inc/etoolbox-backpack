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

package com.exadel.etoolbox.backpack.core.dto.response;

import com.google.gson.Gson;

/**
 * Represents the package modification info.
 */
public class JcrPackageWrapper {

    private transient PackageInfo packageInfo;
    private String message;
    private int statusCode;

    /**
     * Sets PackageInfo object
     *
     * @param packageInfo {@link PackageInfo} value
     */
    public void setPackageInfo(final PackageInfo packageInfo) {
        this.packageInfo = packageInfo;
    }

    /**
     * Sets error msg value
     *
     * @param message {@link String} value
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * Sets http(s) status code
     *
     * @param statusCode {@link int} value
     */
    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets PackageInfo object
     *
     * @return PackageInfo object
     */
    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    /**
     * Gets error msg
     *
     * @return String value
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets error status code
     *
     * @return int value
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Checks package-info object is exist
     *
     * @return true or false value
     */
    public boolean isExist() {
        return packageInfo != null;
    }

    /**
     * Gets JSON representation of current object
     *
     * @return String value
     */
    public String getJson() {
        Gson gson = new Gson();

        return gson.toJson(this);
    }
}
