/*
 * Copyright (C) 2006 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.base;

import com.google.common.annotations.*;
import junit.framework.*;

import static com.google.common.base.MoreObjects.*;

/**
 * Tests for {@link MoreObjects}.
 */
@GwtCompatible(emulated = true)
public class MoreObjectsTest extends TestCase {
  public void testIfNonNull() throws Exception {
    Function<CharSequence, Integer> lengthFunction = new Function<CharSequence, Integer>() {
      @Override public Integer apply(CharSequence input) { return input.length(); }
    };
    Supplier<Integer> dummySupplier = new Supplier<Integer>() {
      @Override public Integer get() { return 1; }
    };

    CharSequence input = null;
    assertEquals(null, ifNonNull(input, lengthFunction));
    
    input = "12";
    assertEquals(2, (int) ifNonNull(input, lengthFunction));

    input = null;
    assertEquals(1, (int) ifNonNull(input, lengthFunction, 1));
    assertEquals(1, (int) ifNonNull(input, lengthFunction, dummySupplier));

    input = new StringBuilder();
    assertEquals(0, (int) ifNonNull(input, lengthFunction, -1)); 
    assertEquals(0, (int) ifNonNull(input, lengthFunction, dummySupplier));
  }
}