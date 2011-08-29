/*
 * Copyright (C) 2011 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

/**
 * Tests for {@link Enums}.
 *
 * @author Steve McKay
 */
@GwtCompatible(emulated = true)
public class EnumsTest extends TestCase {

  private enum TestEnum {
    CHEETO,
    HONDA,
    POODLE,
  }

  private enum OtherEnum {}

  public void testValueOfFunction() {
    Function<String, TestEnum> function = Enums.valueOfFunction(TestEnum.class);
    assertEquals(TestEnum.CHEETO, function.apply("CHEETO"));
    assertEquals(TestEnum.HONDA, function.apply("HONDA"));
    assertEquals(TestEnum.POODLE, function.apply("POODLE"));
  }

  public void testValueOfFunction_caseSensitive() {
    Function<String, TestEnum> function = Enums.valueOfFunction(TestEnum.class);
    assertNull(function.apply("cHEETO"));
    assertNull(function.apply("Honda"));
    assertNull(function.apply("poodlE"));
  }

  public void testValueOfFunction_nullWhenNotMatchingConstant() {
    Function<String, TestEnum> function = Enums.valueOfFunction(TestEnum.class);
    assertNull(function.apply("WOMBAT"));
  }

  public void testValueOfFunction_equals() {
    new EqualsTester()
        .addEqualityGroup(
            Enums.valueOfFunction(TestEnum.class), Enums.valueOfFunction(TestEnum.class))
        .addEqualityGroup(Enums.valueOfFunction(OtherEnum.class))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testValueOfFunction_serialization() {
    Function<String, TestEnum> function = Enums.valueOfFunction(TestEnum.class);
    SerializableTester.reserializeAndAssert(function);
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointerExceptions() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Enums.class);
  }
}
