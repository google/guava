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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

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

  public void testValueOfFunction_nullWhenNoMatchingConstant() {
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

  public void testGetIfPresent() {
    assertEquals(Optional.of(TestEnum.CHEETO), Enums.getIfPresent(TestEnum.class, "CHEETO"));
    assertEquals(Optional.of(TestEnum.HONDA), Enums.getIfPresent(TestEnum.class, "HONDA"));
    assertEquals(Optional.of(TestEnum.POODLE), Enums.getIfPresent(TestEnum.class, "POODLE"));

    assertTrue(Enums.getIfPresent(TestEnum.class, "CHEETO").isPresent());
    assertTrue(Enums.getIfPresent(TestEnum.class, "HONDA").isPresent());
    assertTrue(Enums.getIfPresent(TestEnum.class, "POODLE").isPresent());

    assertEquals(TestEnum.CHEETO, Enums.getIfPresent(TestEnum.class, "CHEETO").get());
    assertEquals(TestEnum.HONDA, Enums.getIfPresent(TestEnum.class, "HONDA").get());
    assertEquals(TestEnum.POODLE, Enums.getIfPresent(TestEnum.class, "POODLE").get());
  }

  public void testGetIfPresent_caseSensitive() {
    assertFalse(Enums.getIfPresent(TestEnum.class, "cHEETO").isPresent());
    assertFalse(Enums.getIfPresent(TestEnum.class, "Honda").isPresent());
    assertFalse(Enums.getIfPresent(TestEnum.class, "poodlE").isPresent());
  }

  public void testGetIfPresent_whenNoMatchingConstant() {
    assertEquals(Optional.absent(), Enums.getIfPresent(TestEnum.class, "WOMBAT"));
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Enums.class);
  }

  @Retention(RetentionPolicy.RUNTIME)
  private @interface ExampleAnnotation {}

  private enum AnEnum {
    @ExampleAnnotation FOO,
    BAR
  }

  @GwtIncompatible("reflection")
  public void testGetField() {
    Field foo = Enums.getField(AnEnum.FOO);
    assertEquals("FOO", foo.getName());
    assertTrue(foo.isAnnotationPresent(ExampleAnnotation.class));

    Field bar = Enums.getField(AnEnum.BAR);
    assertEquals("BAR", bar.getName());
    assertFalse(bar.isAnnotationPresent(ExampleAnnotation.class));
  }
}
