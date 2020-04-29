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

package com.exadel.aem.request.impl;

import com.exadel.aem.request.RequestAdapter;
import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;
import com.exadel.aem.request.annotations.Validate;
import com.exadel.aem.request.validator.Validator;
import com.exadel.aem.request.validator.ValidatorResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

/**
 * Implements {@link RequestAdapter} to adapt user-defined {@code SlingHttpServletRequest} parameters to a data model object
 * which is then used in operations by {@link com.exadel.aem.backpack.core.services.PackageService}
 */
public class RequestAdapterImpl implements RequestAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestAdapterImpl.class);

    private static final Map<Class, Function<String, Object>> SUPPORTED_TYPES;

    static {
        SUPPORTED_TYPES = new HashMap<>();
        SUPPORTED_TYPES.put(Boolean.class, Boolean::valueOf);
        SUPPORTED_TYPES.put(boolean.class, Boolean::parseBoolean);
        SUPPORTED_TYPES.put(Byte.class, Byte::valueOf);
        SUPPORTED_TYPES.put(byte.class, Byte::parseByte);
        SUPPORTED_TYPES.put(Short.class, Short::valueOf);
        SUPPORTED_TYPES.put(short.class, Short::parseShort);
        SUPPORTED_TYPES.put(Integer.class, Integer::valueOf);
        SUPPORTED_TYPES.put(int.class, Integer::parseInt);
        SUPPORTED_TYPES.put(Long.class, Long::valueOf);
        SUPPORTED_TYPES.put(long.class, Long::parseLong);
        SUPPORTED_TYPES.put(Float.class, Float::valueOf);
        SUPPORTED_TYPES.put(float.class, Float::parseFloat);
        SUPPORTED_TYPES.put(Double.class, Double::valueOf);
        SUPPORTED_TYPES.put(double.class, Double::parseDouble);
        SUPPORTED_TYPES.put(Character.class, s -> s.charAt(0));
        SUPPORTED_TYPES.put(char.class, s -> s.charAt(0));
        SUPPORTED_TYPES.put(String.class, s -> s);
        SUPPORTED_TYPES.put(StringBuilder.class, StringBuilder::new);
        SUPPORTED_TYPES.put(StringBuffer.class, StringBuffer::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T adapt(Map parameterMap, Class<T> tClazz) {
        T newObject = null;
        if (tClazz.isAnnotationPresent(RequestMapping.class)) {
            newObject = createDefaultObject(tClazz);
            if (newObject != null) {
                Field[] fields = tClazz.getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(RequestParam.class)) {
                        RequestParam annotation = field.getAnnotation(RequestParam.class);
                        String parameterName = annotation.name();
                        if (StringUtils.isBlank(parameterName)) {
                            parameterName = field.getName();
                        }
                        initField(newObject, parameterMap.get(parameterName), field);
                    }
                }
            }
        }
        return newObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ValidatorResponse<T> adaptValidate(Map parameterMap, Class<T> tClazz) {
        ValidatorResponse<T> response = new ValidatorResponse<>();
        if (tClazz.isAnnotationPresent(RequestMapping.class)) {
            T newObject = createDefaultObject(tClazz);
            boolean objectValid = true;
            if (newObject != null) {
                List<String> validationMessages = new ArrayList<>();
                response.setLog(validationMessages);
                List<Field> allFields;
                allFields = getAllFields(new ArrayList<>(), tClazz);
                objectValid = initValidateFields(parameterMap, newObject, validationMessages, allFields);
            }
            if (objectValid) {
                response.setModel(newObject);
                response.setValid(true);
            }
        }
        return response;
    }

    /**
     * Called by {@link RequestAdapterImpl#adaptValidate(Map, Class)}. Attempts to store values
     * from a {@code SlingHttpRequest} parameter map to the fields of a {@code <T>}-typed adaptation object. Values
     * extracted from the parameter map are validated as needed. Returns Boolean value indicating whether all the stored
     * values have been valid
     *
     * @param parameterMap       Parameters taken from a {@code SlingHttpRequest}
     * @param newObject          Reference to the {@code <T>}-typed adaptation object instance
     * @param validationMessages Reference to the collection of warnings produced by validation routines
     * @param allFields          Collection of {@code Field} instances than need to be populated with data
     * @param <T>                Type of the adaptation object instance
     * @return True is the adaptation object has been successfully populated with validated data; otherwise, false
     */
    private <T> boolean initValidateFields(final Map parameterMap,
                                           final T newObject,
                                           final List<String> validationMessages,
                                           final List<Field> allFields) {
        boolean objectValid = true;
        for (Field field : allFields) {
            if (field.isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParamAnnotation = field.getAnnotation(RequestParam.class);
                String parameterName = requestParamAnnotation.name();
                if (StringUtils.isBlank(parameterName)) {
                    parameterName = field.getName();
                }
                boolean fieldValid = isParameterValid(parameterMap.get(parameterName), validationMessages, field);
                if (fieldValid) {
                    initField(newObject, parameterMap.get(parameterName), field);
                }
                objectValid &= fieldValid;
            }
        }
        return objectValid;
    }

    /**
     * Called by {@link RequestAdapterImpl#adaptValidate(Map, Class)} to get a collection of declared fields
     * of the specified class and its superclasses that will then be populated with data coming in a
     * {@code SlingHttpServletRequest}
     *
     * @param fields List of pre-collected fields that will be prepended to fields of the current class
     * @param type   Class to analyze
     * @return {@code List<Field>} object
     */
    private List<Field> getAllFields(List<Field> fields, Class<?> type) {

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        return fields;
    }

    /**
     * Gets whether the given parameter is valid per an [optional] validator associated with the current field
     *
     * @param parameter          And arbitrary parameter value
     * @param validationMessages Reference to the collection of validation messages to add an emerging warning to
     * @param field              {@code Field} instance used to get an [optional] validator
     * @return True if no validator assigned to the current {@code Field} or else the provided object is valid
     * for the field; otherwise, false
     */
    private boolean isParameterValid(Object parameter,
                                     final List<String> validationMessages,
                                     final Field field) {
        Validate validateAnnotation = field.getAnnotation(Validate.class);
        if (validateAnnotation != null) {
            final Class<? extends Validator>[] validatorsArray = validateAnnotation.validator();
            for (int i = 0; i < validatorsArray.length; i++) {
                Validator validator = createDefaultObject(validatorsArray[i]);
                if (validator != null && !validator.isValid(parameter)) {
                    validationMessages.add(validateAnnotation.invalidMessages()[i]);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Attempts to store a value arrived from a {@code SlingHttpRequest} parameter map to the specified field
     * of the given adaptation object instance
     *
     * @param newObject Reference to the {@code <T>}-typed adaptation object instance
     * @param parameter An arbitrary parameter value
     * @param field     {@code Field} instance used to populate the parameter value
     * @param <T>       Type of the adaptation object instance
     */
    private <T> void initField(final T newObject, final Object parameter, final Field field) {
        Class<?> fieldType = field.getType();
        if (parameter != null) {
            String[] arrayParams = (String[]) parameter;
            if (ArrayUtils.isNotEmpty(arrayParams)) {
                if (fieldType.isArray()) {
                    handleArray(newObject, field, arrayParams);
                } else if (fieldType == List.class) {
                    handleList(newObject, field, arrayParams);
                } else if (isSupportedType(fieldType)) {
                    setFieldValue(newObject, field, convert(fieldType, arrayParams[0]));
                }
            }
        }
    }

    /**
     * Called by {@link RequestAdapterImpl#initField(Object, Object, Field)} to populate an array value the given adaptation
     * object instance
     *
     * @param newObject   Reference to the {@code <T>}-typed adaptation object instance
     * @param field       {@code Field} instance used to populate the parameter value
     * @param arrayParams An arbitrary parameter value, castable to a string array
     * @param <T>         Type of the adaptation object instance
     */
    private <T> void handleArray(final T newObject, final Field field, final String[] arrayParams) {
        Class<?> componentType = field.getType().getComponentType();
        if (isSupportedType(componentType)) {
            Object[] arrayNewInstance = (Object[]) Array.newInstance(componentType, arrayParams.length);
            for (int i = 0; i < arrayNewInstance.length; i++) {
                final Object converted = convert(componentType, arrayParams[i]);
                if (converted != null) {
                    arrayNewInstance[i] = converted;
                }
            }
            setFieldValue(newObject, field, arrayNewInstance);
        }
    }

    /**
     * Called by {@link RequestAdapterImpl#initField(Object, Object, Field)} to populate an array value the given adaptation
     * object instance
     *
     * @param newObject   Reference to the {@code <T>}-typed adaptation object instance
     * @param field       {@code Field} instance used to populate the parameter value
     * @param arrayParams An arbitrary parameter value, castable to a list of strings
     * @param <T>         Type of the adaptation object instance
     */
    private <T> void handleList(final T newObject, final Field field, final String[] arrayParams) {
        List<Object> list = new ArrayList<>();
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType genericTypes = (ParameterizedType) type;
            Type genericType = genericTypes.getActualTypeArguments()[0];
            if (genericType instanceof Class) {
                Class genericClass = (Class) genericType;
                if (isSupportedType(genericClass)) {
                    for (String param : arrayParams) {
                        final Object converted = convert(genericClass, param);
                        if (converted != null) {
                            list.add(converted);
                        }
                    }
                    setFieldValue(newObject, field, list);
                }
            }
        }
    }

    /**
     * Casts the provided value to the specified type via the appropriate casting routine
     *
     * @param clazz Target class
     * @param value Value to cast
     * @return Casted entity, {@code Object}-wrapped
     */
    private Object convert(Class<?> clazz, String value) {
        if (isSupportedType(clazz)) {
            try {
                return SUPPORTED_TYPES.get(clazz).apply(value);
            } catch (Exception e) {
                LOGGER.error("Exception during parameter adaptation", e);
            }
        }
        return null;
    }

    /**
     * Gets whether the provided value type can be targeted by value casting
     *
     * @param clazz Target class
     * @return True or false
     * @see RequestAdapterImpl#convert(Class, String)
     */
    private boolean isSupportedType(Class<?> clazz) {
        return SUPPORTED_TYPES.containsKey(clazz);
    }

    /**
     * Sets the provided parameter value to the field of the adapted object instance
     *
     * @param newObject Reference to the {@code <T>}-typed adaptation object instance
     * @param field     {@code Field} instance used to populate the parameter value
     * @param param     An arbitrary parameter value
     * @param <T>       Type of the adaptation object instance
     */
    private <T> void setFieldValue(final T newObject, final Field field, final Object param) {
        try {
            field.setAccessible(true);
            field.set(newObject, param);
        } catch (IllegalAccessException e) {
            LOGGER.error("Can't access field {}", field.getName(), e);
        }
    }

    /**
     * Gets a new instance of the provided object type and wraps underlying reflection exceptions
     *
     * @param tClazz Target class
     * @param <T>    Type of the instance to create
     * @return {@code <T>}-typed object, or null
     */
    private static <T> T createDefaultObject(final Class<T> tClazz) {
        try {
            return tClazz.getConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.error("Object instantiation exception", e);
        }
        return null;
    }
}
