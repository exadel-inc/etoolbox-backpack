package com.exadel.aem.request.annotations;

import com.exadel.aem.request.validator.Validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Validate {

    Class<? extends Validator>[] validator() default {};

    String[] invalidMessages() default {};
}
