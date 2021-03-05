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

package com.exadel.aembox.backpack.request.impl.models;

import com.exadel.aembox.backpack.request.annotations.RequestMapping;
import com.exadel.aembox.backpack.request.annotations.RequestParam;

@RequestMapping
@SuppressWarnings("UnusedDeclaration") // contains directly injected fields
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
