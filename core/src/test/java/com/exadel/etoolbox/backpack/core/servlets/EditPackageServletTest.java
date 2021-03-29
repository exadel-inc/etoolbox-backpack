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

package com.exadel.etoolbox.backpack.core.servlets;

import com.exadel.etoolbox.backpack.core.dto.response.PackageInfo;
import com.exadel.etoolbox.backpack.core.dto.response.PackageStatus;
import com.exadel.etoolbox.backpack.core.services.pckg.EditPackageService;
import com.exadel.etoolbox.backpack.core.servlets.model.PackageModel;
import com.exadel.etoolbox.backpack.request.RequestAdapter;
import com.exadel.etoolbox.backpack.request.impl.RequestAdapterImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EditPackageServletTest {

    private static final String APPLICATION_JSON = "application/json";
    private static final String PAGE_1 = "/content/site/pages/page1";
    private static final String PACKAGE_NAME = "test-package";
    private static final String PACKAGE_NAME_PARAM = "packageName";
    private static final String PACKAGE_PATHS_MULTIFIELD_PARAM = "paths/item0/path";

    @Rule
    public final AemContext context = new AemContext();
    private final EditPackageService editPackageServiceMock = mock(EditPackageService.class);
    private EditPackageServlet servlet;
    private PackageInfo packageInfoWithModifiedStatus;
    private PackageInfo packageInfoWithErrorStatus;

    @Before
    public void beforeTest() {
        packageInfoWithModifiedStatus = getPackageInfoWithModifiedStatus();
        packageInfoWithErrorStatus = getPackageInfoWithErrorStatus();
        context.registerService(EditPackageService.class, editPackageServiceMock);
        context.registerService(RequestAdapter.class, new RequestAdapterImpl());
        servlet = context.registerInjectActivateService(new EditPackageServlet());

        context.create().page(PAGE_1);
    }

    @Test
    public void shouldReturnBadRequestWhenRequestIsEmpty() throws IOException {
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertEquals(APPLICATION_JSON, context.response().getContentType());
    }

    @Test
    public void shouldReturnOkWhenRequestIsValid() throws IOException {
        createBaseRequest();
        when(editPackageServiceMock.editPackage(any(ResourceResolver.class), any(PackageModel.class))).thenReturn(packageInfoWithModifiedStatus);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals(APPLICATION_JSON, context.response().getContentType());
    }

    @Test
    public void shouldReturnConflictWhenPackageAlreadyExist() throws IOException {
        createBaseRequest();
        when(editPackageServiceMock.editPackage(any(ResourceResolver.class), any(PackageModel.class))).thenReturn(packageInfoWithErrorStatus);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_CONFLICT, context.response().getStatus());
        assertEquals(APPLICATION_JSON, context.response().getContentType());
    }

    private void createBaseRequest() {
        context.request().addRequestParameter(PACKAGE_NAME_PARAM, PACKAGE_NAME);
        context.request().addRequestParameter(PACKAGE_PATHS_MULTIFIELD_PARAM, PAGE_1);
    }

    private PackageInfo getPackageInfoWithErrorStatus() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageStatus(PackageStatus.ERROR);
        return packageInfo;
    }

    private PackageInfo getPackageInfoWithModifiedStatus() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageStatus(PackageStatus.MODIFIED);
        return packageInfo;
    }
}