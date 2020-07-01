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

package com.exadel.aem.backpack.request.impl;

import com.exadel.aem.backpack.request.impl.models.*;
import com.exadel.aem.backpack.request.validator.ValidatorResponse;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;


public class RequestAdapterImplTest {

    private final RequestAdapterImpl requestAdapter = new RequestAdapterImpl();

    @Rule
    public final AemContext aemContext = new AemContext();

    private MockSlingHttpServletRequest request;
    private HashMap<String, Object> parameterMap;

    @Before
    public void before() {
        request = aemContext.request();
        parameterMap = new HashMap<>();
        initPrimitives(parameterMap);
        initStrings(parameterMap);
        initDataStructures(parameterMap);
        request.setParameterMap(parameterMap);
    }


    @Test
    public void shouldAdaptPrimitiveTypes() {
        final PrimitivesModel primitivesModel = requestAdapter.adapt(request.getParameterMap(), PrimitivesModel.class);

        assertTrue(primitivesModel.isBoolean());
        assertEquals(12, primitivesModel.getByte());
        assertEquals(-3612, primitivesModel.getShort());
        assertEquals(23423434, primitivesModel.getInt());
        assertEquals(32444141234454L, primitivesModel.getLong());
        assertEquals(234234.34, primitivesModel.getFloat(), 0.01);
        assertEquals(23423412323.347, primitivesModel.getDouble(), 0.001);
    }

    @Test
    public void shouldAdaptWrapperTypes() {
        final WrappersModel wrappersModel = requestAdapter.adapt(request.getParameterMap(), WrappersModel.class);

        assertTrue(wrappersModel.isBoolean());
        assertEquals(12, wrappersModel.getByte());
        assertEquals(-3612, wrappersModel.getShort());
        assertEquals(23423434, wrappersModel.getInt());
        assertEquals(32444141234454L, wrappersModel.getLong());
        assertEquals(234234.34, wrappersModel.getFloat(), 0.01);
        assertEquals(23423412323.347, wrappersModel.getDouble(), 0.001);
    }

    @Test
    public void shouldAdaptStringsTypes() {
        final StringsModel stringsModel = requestAdapter.adapt(request.getParameterMap(), StringsModel.class);

        assertEquals("test encoded whitespace.zip", stringsModel.getString());
        assertEquals("A String Builder string", stringsModel.getStringBuilder().toString());
        assertEquals("A String Buffer string", stringsModel.getStringBuffer().toString());
    }

    @Test
    public void shouldAdaptDataStructuresTypes() {
        final DataStructureModel dataStructureModel = requestAdapter.adapt(request.getParameterMap(), DataStructureModel.class);

        assertEquals(Arrays.asList("String one", "String two"), dataStructureModel.getList());
        assertArrayEquals(new Integer[]{123, 567457657}, dataStructureModel.getIntegers());
    }

    @Test
    public void shouldReturnInvalidMessages() {
        parameterMap.put("wholeNumber", "-12");
        request.setParameterMap(parameterMap);
        final ValidatorResponse<ValidateModel> response = requestAdapter.adaptValidate(request.getParameterMap(), ValidateModel.class);

        assertFalse(response.isValid());

        final List<String> log = response.getLog();
        assertEquals("String field is required", log.get(0));
        assertEquals("Field must be whole number!", log.get(1));
    }

    @Test
    public void shouldReturnValidModel() {
        parameterMap.put("requiredString", "String");
        parameterMap.put("wholeNumber", "12");
        request.setParameterMap(parameterMap);

        final ValidatorResponse<ValidateModel> response = requestAdapter.adaptValidate(request.getParameterMap(), ValidateModel.class);

        assertTrue(response.isValid());

        assertEquals("String", response.getModel().getRequiredString());
        assertEquals(12, response.getModel().getWholeNumber());
    }

    private void initStrings(final HashMap<String, Object> parameterMap) {
        parameterMap.put("string", "test%20encoded%20whitespace.zip");
        parameterMap.put("stringBuilder", "A String Builder string");
        parameterMap.put("stringBuffer", "A String Buffer string");
    }

    private void initPrimitives(final HashMap<String, Object> parameterMap) {
        parameterMap.put("aBoolean", "true");
        parameterMap.put("aByte", "12");
        parameterMap.put("aShort", "-3612");
        parameterMap.put("anInt", "23423434");
        parameterMap.put("aLong", "32444141234454");
        parameterMap.put("aFloat", "234234.34");
        parameterMap.put("aDouble", "23423412323.347");
    }

    private void initDataStructures(final HashMap<String, Object> parameterMap) {
        parameterMap.put("listOfStrings", new String[]{"String one", "String two"});
        parameterMap.put("arrayOfIntegers", new String[]{"123", "567457657"});
    }
}