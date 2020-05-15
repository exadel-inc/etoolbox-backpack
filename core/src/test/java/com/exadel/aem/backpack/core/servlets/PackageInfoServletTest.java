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

package com.exadel.aem.backpack.core.servlets;

import com.exadel.aem.backpack.core.dto.response.PackageInfo;
import com.exadel.aem.backpack.core.dto.response.PackageStatus;
import com.exadel.aem.backpack.core.services.PackageService;
import com.exadel.aem.backpack.core.servlets.model.PackageInfoModel;
import com.exadel.aem.backpack.request.RequestAdapter;
import com.exadel.aem.backpack.request.impl.RequestAdapterImpl;
import com.exadel.aem.backpack.request.validator.ValidatorResponse;
import com.google.gson.Gson;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageInfoServletTest {

    private static final String PACKAGE_PATH = "/etc/package/test/package-1";
    private static final String PATH_PARAM = "path";

    @Rule
    public final AemContext context = new AemContext();
    private final PackageService packageServiceMock = mock(PackageService.class);
    private final PackageInfo packageInfo = getPackageInfo();
    private PackageInfoServlet servlet;
    private Gson GSON;

    @Before
    public void beforeTest() {
        when(packageServiceMock.getPackageInfo(any(ResourceResolver.class), any(PackageInfoModel.class))).thenReturn(packageInfo);
        context.registerService(PackageService.class, packageServiceMock);
        context.registerService(RequestAdapter.class, new RequestAdapterImpl());
        servlet = context.registerInjectActivateService(new PackageInfoServlet());
        GSON = new Gson();
    }

    @Test
    public void shouldReturnBadRequestWithNonExistingPathParameter() throws IOException {
        ValidatorResponse validatorResponse = new ValidatorResponse();
        validatorResponse.setLog(Collections.singletonList("Path field is required"));

        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertEquals(GSON.toJson(validatorResponse), context.response().getOutputAsString());
    }

    @Test
    public void shouldReturnOkWithExistingPathParameter() throws IOException {
        context.request().addRequestParameter(PATH_PARAM, PACKAGE_PATH);

        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals(GSON.toJson(packageInfo), context.response().getOutputAsString());
    }

    private PackageInfo getPackageInfo() {
        PackageInfo info = new PackageInfo();
        info.setPackagePath(PACKAGE_PATH);
        info.setPackageStatus(PackageStatus.CREATED);
        info.setDataSize(0L);
        info.setGroupName("test");
        info.setPackageName("package");
        info.setVersion("1");
        return info;
    }
}