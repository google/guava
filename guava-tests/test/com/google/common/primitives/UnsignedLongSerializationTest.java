/*
 * Copyright (C) 2011 The Guava Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.primitives;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableList;
import com.google.gwt.corp.testing.serialization.SerializationTestCase;

/**
 * Tests for the GWT serialization of UnsignedLong.
 * 
 * @author Louis Wasserman
 */
@GwtCompatible
public class UnsignedLongSerializationTest extends SerializationTestCase<UnsignedLong> {
  public String getModuleName() {
    return "com.google.common.primitives.testModule";
  }

  private static final ImmutableList<UnsignedLong> TEST_CASES = ImmutableList.of(
      UnsignedLong.asUnsigned(0L), UnsignedLong.asUnsigned(1L), UnsignedLong.asUnsigned(-1L),
      UnsignedLong.asUnsigned(Integer.MAX_VALUE), UnsignedLong.asUnsigned(Integer.MIN_VALUE),
      UnsignedLong.asUnsigned(Long.MAX_VALUE), UnsignedLong.asUnsigned(Long.MIN_VALUE));

  public void testCases() {
    for (UnsignedLong x : TEST_CASES) {
      doTest(x);
    }
  }
}
