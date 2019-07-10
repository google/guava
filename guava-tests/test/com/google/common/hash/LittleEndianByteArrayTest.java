/*
 * Copyright (C) 2019 The Guava Authors
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

package com.google.common.hash;

import org.junit.Assert;
import org.junit.Test;

public class LittleEndianByteArrayTest {

    @Test
    public void testLoad64() {
        LittleEndianByteArray.store64(
                new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, 0, 1L);

        Assert.assertEquals(578437695752307201L, LittleEndianByteArray
                .load64(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, 0));
        Assert.assertEquals(72623859790382856L, LittleEndianByteArray
                .load64(new byte[]{8, 7, 6, 5, 4, 3, 2, 1}, 0));
    }

    @Test
    public void testLoad64Safely() {
        Assert.assertEquals(0, LittleEndianByteArray
                .load64Safely(new byte[0], 0, 0));
        Assert.assertEquals(1, LittleEndianByteArray
                .load64Safely(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, 0, 1));
        Assert.assertEquals(513, LittleEndianByteArray
                .load64Safely(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, 0, 2));
        Assert.assertEquals(578437695752307201L, LittleEndianByteArray
                .load64Safely(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, 0, 10));
    }

    @Test
    public void testLoad32() {
        Assert.assertEquals(84148994, LittleEndianByteArray
                .load32(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, 1));
    }
}
