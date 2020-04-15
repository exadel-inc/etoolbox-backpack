package com.exadel.aem.request.impl.models;

import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;

@RequestMapping
public class WrappersModel {

    @RequestParam
    private Boolean aBoolean;

    @RequestParam
    private Byte aByte;

    @RequestParam
    private Short aShort;

    @RequestParam
    private Integer anInt;

    @RequestParam
    private Long aLong;

    @RequestParam
    private Float aFloat;

    @RequestParam
    private Double aDouble;

    public boolean isBoolean() {
        return aBoolean;
    }

    public byte getByte() {
        return aByte;
    }

    public short getShort() {
        return aShort;
    }

    public int getInt() {
        return anInt;
    }

    public long getLong() {
        return aLong;
    }

    public float getFloat() {
        return aFloat;
    }

    public double getDouble() {
        return aDouble;
    }
}
