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
import com.google.common.annotations.GwtCompatible;
/**
 * A class that focus on performing Addition operation using bitwise operators and checking whether the number is even or odd is
 * not covered in {@link java.lang.Math} and {@Link com.google.common.math}
 * @author Harish Babu
 * @since 11.0
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public final class PrimitiveMath{
    /*
     * This method returns an integer value by performing addition operations using bitwise XOR and bitwise AND
     * It performs faster than + operator.
     * The input takes 2 integer values.
     */
    public static int add(int value1,int value2){
        return (value1^value2)+2*(value1&value2);
    }

    /*
     * This method returns a short value by performing addition operations using bitwise XOR and bitwise AND
     * It performs faster than + operator.
     * The input takes 2 short values.
     */
    public static short add(short value1,short value2){
        return (value1^value2)+2*(value1&value2);
    }

    /*
     * This method returns a long value by performing addition operations using bitwise XOR and bitwise AND
     * It performs faster than + operator.
     * The input takes 2 long values.
     */
    public static long add(long value1,long value2){
        return (value1^value2)+2*(value1&value2);
    }

    /*
     * This method returns a boolean value by checking whether number is even or odd using bitwise AND operator
     * The input required is a single integer value
     */
    public boolean isEven(int value){
        return value&1;
    }

    /*
     * This method returns a boolean value by checking whether number is even or odd using bitwise AND operator
     * The input required is a single short value
     */
    public boolean isEven(short value){
        return value&1;
    }
    /*
     * This method returns a boolean value by checking whether number is even or odd using bitwise AND operator
     * The input required is a long integer value
     */
    public boolean isEven(long value){
        return value&1;
    }
}