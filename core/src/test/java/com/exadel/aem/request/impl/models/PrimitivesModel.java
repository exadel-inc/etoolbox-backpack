package com.exadel.aem.request.impl.models;

import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;

@RequestMapping
public class PrimitivesModel {

    @RequestParam
    private boolean aBoolean;

    @RequestParam
    private byte aByte;

    @RequestParam
    private short aShort;

    @RequestParam
    private int anInt;

    @RequestParam
    private long aLong;

    @RequestParam
    private float aFloat;

    @RequestParam
    private double aDouble;

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
