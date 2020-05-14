/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.math;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Dhruv Thakkar
 */
@GwtCompatible(emulated = true)
public final class BigDecimalMath {
    /**
     * return the double after round to +∞ from long
     */
    @Beta
    public static double nextUpDouble(long x) {
        return Math.nextUp((double) x);
    }

    /**
     * return the double after round to +∞ from BigInteger
     */
    @Beta
    public static double nextUpDouble(BigInteger x) {
        return Math.nextUp(x.doubleValue());
    }

    /**
     * return the double after round to +∞ from BigDecimal
     */
    @Beta
    public static double nextUpDouble(BigDecimal x) {
        return Math.nextUp(x.doubleValue());
    }

    /**
     * return the double after round to -∞ from long
     */
    @Beta
    public static double nextDownDouble(long x) {
        return Math.nextDown((double) x);
    }

    /**
     * return the double after round to -∞ from BigInteger
     */
    @Beta
    public static double nextDownDouble(BigInteger x) {
        return Math.nextDown(x.doubleValue());
    }

    /**
     * return the double after round to -∞ from BigDecimal
     */
    @Beta
    public static double nextDownDouble(BigDecimal x) {
        return Math.nextDown(x.doubleValue());
    }
}
