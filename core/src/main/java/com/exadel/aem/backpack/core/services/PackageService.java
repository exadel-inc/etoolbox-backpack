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

package com.exadel.aem.backpack.core.services;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.servlets.model.BuildPackageModel;
import com.exadel.aem.backpack.core.servlets.model.CreatePackageModel;
import com.exadel.aem.backpack.core.servlets.model.LatestPackageInfoModel;
import com.exadel.aem.backpack.core.servlets.model.PackageInfoModel;
import org.apache.sling.api.resource.ResourceResolver;

public interface PackageService {

    PackageInfo getPackageInfo(ResourceResolver resourceResolver, PackageInfoModel packageInfoModel);

    PackageInfo createPackage(ResourceResolver resourceResolver, CreatePackageModel createPackageModel);

    PackageInfo buildPackage(ResourceResolver resourceResolver, BuildPackageModel buildPackageModel);

    PackageInfo testBuildPackage(ResourceResolver resourceResolver, BuildPackageModel buildPackageModel);

    PackageInfo getLatestPackageBuildInfo(LatestPackageInfoModel latestPackageInfoModel);
}
