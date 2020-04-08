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

public class RequestAdapterImpl implements RequestAdapter {

    private static final Map<Class, Function<String, Object>> SUPPORTED_TYPES;
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestAdapterImpl.class);

    static {
        SUPPORTED_TYPES = new HashMap<>();
        SUPPORTED_TYPES.put(Boolean.class, Boolean::valueOf);
        SUPPORTED_TYPES.put(boolean.class, Boolean::parseBoolean);
        SUPPORTED_TYPES.put(Byte.class, Boolean::valueOf);
        SUPPORTED_TYPES.put(byte.class, Byte::parseByte);
        SUPPORTED_TYPES.put(Short.class, Short::valueOf);
        SUPPORTED_TYPES.put(short.class, Short::parseShort);
        SUPPORTED_TYPES.put(Integer.class, Integer::valueOf);
        SUPPORTED_TYPES.put(int.class, Integer::parseInt);
        SUPPORTED_TYPES.put(Long.class, Integer::valueOf);
        SUPPORTED_TYPES.put(long.class, Long::parseLong);
        SUPPORTED_TYPES.put(Float.class, Long::valueOf);
        SUPPORTED_TYPES.put(float.class, Float::parseFloat);
        SUPPORTED_TYPES.put(Double.class, Double::valueOf);
        SUPPORTED_TYPES.put(double.class, Double::parseDouble);
        SUPPORTED_TYPES.put(String.class, s -> s);
        SUPPORTED_TYPES.put(StringBuilder.class, StringBuilder::new);
        SUPPORTED_TYPES.put(StringBuffer.class, StringBuffer::new);
    }

    @Override
    public <T> T adapt(Map<String, Object> parameterMap, Class<T> tClazz) {
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

    @Override
    public <T> ValidatorResponse<T> adaptValidate(Map<String, Object> parameterMap, Class<T> tClazz) {

        ValidatorResponse response = new ValidatorResponse();
        if (tClazz.isAnnotationPresent(RequestMapping.class)) {
            T newObject = createDefaultObject(tClazz);
            boolean objectValid = true;
            if (newObject != null) {
                List<String> validationMassages = new ArrayList<>();
                response.setLog(validationMassages);
                List<Field> allFields;
                allFields = getAllFields(new ArrayList<>(), tClazz);
                objectValid = initValidateFields(parameterMap, newObject, validationMassages, allFields);
            }
            if (objectValid) {
                response.setModel(newObject);
                response.setValid(true);
            }
        }
        return response;
    }

    protected <T> boolean initValidateFields(final Map<String, Object> parameterMap,
                                             final T newObject,
                                             final List<String> validationMassages,
                                             final List<Field> allFields) {
        boolean objectValid = true;
        for (Field field : allFields) {
            if (field.isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParamAnnotation = field.getAnnotation(RequestParam.class);
                String parameterName = requestParamAnnotation.name();
                if (StringUtils.isBlank(parameterName)) {
                    parameterName = field.getName();
                }
                boolean fieldValid = isFieldValid(parameterMap.get(parameterName), validationMassages, field);
                if (fieldValid) {
                    initField(newObject, parameterMap.get(parameterName), field);
                }
                objectValid &= fieldValid;
            }
        }
        return objectValid;
    }

    private List<Field> getAllFields(List<Field> fields, Class<?> type) {

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        return fields;
    }

    private boolean isFieldValid(Object parameter,
                                 final List<String> validationMassages,
                                 final Field field) {
        Validate validateAnnotation = field.getAnnotation(Validate.class);
        if (validateAnnotation != null) {
            final Class<? extends Validator>[] validatorsArray = validateAnnotation.validator();
            for (int i = 0; i < validatorsArray.length; i++) {
                Validator validator = createDefaultObject(validatorsArray[i]);
                if (validator != null && !validator.isValid(parameter)) {
                    validationMassages.add(validateAnnotation.invalidMessages()[i]);
                    return false;
                }
            }
        }
        return true;
    }

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

    private <T> void handleArray(final T t, final Field field, final String[] arrayParams) {
        Class<?> componentType = field.getType().getComponentType();
        if (isSupportedType(componentType)) {
            Object[] arrayNewInstance = (Object[]) Array.newInstance(componentType, arrayParams.length);
            for (int i = 0; i < arrayNewInstance.length; i++) {
                final Object converted = convert(componentType, arrayParams[i]);
                if (converted != null) {
                    arrayNewInstance[i] = converted;
                }
            }
            setFieldValue(t, field, arrayNewInstance);
        }
    }

    private <T> void handleList(final T t, final Field field, final String[] arrayParams) {
        List list = new ArrayList();
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
                    setFieldValue(t, field, list);
                }
            }
        }
    }

    private Object convert(Class clazz, String value) {
        if (isSupportedType(clazz)) {
            try {
                return SUPPORTED_TYPES.get(clazz).apply(value);
            } catch (Exception e) {
                LOGGER.error("Exception during parameter adaptation", e);
            }
        }
        return null;
    }

    private boolean isSupportedType(Class clazz) {
        return SUPPORTED_TYPES.containsKey(clazz);
    }

    protected <T> void setFieldValue(final T t, final Field field, final Object param) {
        try {
            field.setAccessible(true);
            field.set(t, param);
        } catch (IllegalAccessException e) {
            LOGGER.error("Can't access field {}", field.getName(), e);
        }
    }

    protected static <T> T createDefaultObject(final Class<T> tClazz) {
        try {
            return tClazz.getConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.error("Object instantiation exception", e);
        }
        return null;
    }
}
