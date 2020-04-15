package com.exadel.aem.request.impl.models;

import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;

@RequestMapping
public class StringsModel {

    @RequestParam
    private String string;

    @RequestParam
    private StringBuilder stringBuilder;

    @RequestParam
    private StringBuffer stringBuffer;

    public String getString() {
        return string;
    }

    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }

    public StringBuffer getStringBuffer() {
        return stringBuffer;
    }
}
