/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.base.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableMap;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link MoreObjects#toStringHelper(Object)}.
 *
 * @author Jason Lee
 */
@GwtCompatible
@NullUnmarked
public class ToStringHelperTest extends TestCase {

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testConstructor_instance() {
    String toTest = MoreObjects.toStringHelper(this).toString();
    assertEquals("ToStringHelperTest{}", toTest);
  }

  public void testConstructorLenient_instance() {
    String toTest = MoreObjects.toStringHelper(this).toString();
    assertTrue(toTest, toTest.matches(".*\\{\\}"));
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testConstructor_innerClass() {
    String toTest = MoreObjects.toStringHelper(new TestClass()).toString();
    assertEquals("TestClass{}", toTest);
  }

  public void testConstructorLenient_innerClass() {
    String toTest = MoreObjects.toStringHelper(new TestClass()).toString();
    assertTrue(toTest, toTest.matches(".*\\{\\}"));
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testConstructor_anonymousClass() {
    String toTest = MoreObjects.toStringHelper(new Object() {}).toString();
    assertEquals("{}", toTest);
  }

  public void testConstructorLenient_anonymousClass() {
    String toTest = MoreObjects.toStringHelper(new Object() {}).toString();
    assertTrue(toTest, toTest.matches(".*\\{\\}"));
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testConstructor_classObject() {
    String toTest = MoreObjects.toStringHelper(TestClass.class).toString();
    assertEquals("TestClass{}", toTest);
  }

  public void testConstructorLenient_classObject() {
    String toTest = MoreObjects.toStringHelper(TestClass.class).toString();
    assertTrue(toTest, toTest.matches(".*\\{\\}"));
  }

  public void testConstructor_stringObject() {
    String toTest = MoreObjects.toStringHelper("FooBar").toString();
    assertEquals("FooBar{}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringHelper_localInnerClass() {
    // Local inner classes have names ending like "Outer.$1Inner"
    class LocalInnerClass {}
    String toTest = MoreObjects.toStringHelper(new LocalInnerClass()).toString();
    assertEquals("LocalInnerClass{}", toTest);
  }

  public void testToStringHelperLenient_localInnerClass() {
    class LocalInnerClass {}
    String toTest = MoreObjects.toStringHelper(new LocalInnerClass()).toString();
    assertTrue(toTest, toTest.matches(".*\\{\\}"));
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringHelper_localInnerNestedClass() {
    class LocalInnerClass {
      class LocalInnerNestedClass {}
    }
    String toTest =
        MoreObjects.toStringHelper(new LocalInnerClass().new LocalInnerNestedClass()).toString();
    assertEquals("LocalInnerNestedClass{}", toTest);
  }

  public void testToStringHelperLenient_localInnerNestedClass() {
    class LocalInnerClass {
      class LocalInnerNestedClass {}
    }
    String toTest =
        MoreObjects.toStringHelper(new LocalInnerClass().new LocalInnerNestedClass()).toString();
    assertTrue(toTest, toTest.matches(".*\\{\\}"));
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringHelper_moreThanNineAnonymousClasses() {
    // The nth anonymous class has a name ending like "Outer.$n"
    Object unused1 = new Object() {};
    Object unused2 = new Object() {};
    Object unused3 = new Object() {};
    Object unused4 = new Object() {};
    Object unused5 = new Object() {};
    Object unused6 = new Object() {};
    Object unused7 = new Object() {};
    Object unused8 = new Object() {};
    Object unused9 = new Object() {};
    Object o10 = new Object() {};
    String toTest = MoreObjects.toStringHelper(o10).toString();
    assertEquals("{}", toTest);
  }

  public void testToStringHelperLenient_moreThanNineAnonymousClasses() {
    // The nth anonymous class has a name ending like "Outer.$n"
    Object unused1 = new Object() {};
    Object unused2 = new Object() {};
    Object unused3 = new Object() {};
    Object unused4 = new Object() {};
    Object unused5 = new Object() {};
    Object unused6 = new Object() {};
    Object unused7 = new Object() {};
    Object unused8 = new Object() {};
    Object unused9 = new Object() {};
    Object o10 = new Object() {};
    String toTest = MoreObjects.toStringHelper(o10).toString();
    assertTrue(toTest, toTest.matches(".*\\{\\}"));
  }

  // all remaining test are on an inner class with various fields
  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToString_oneField() {
    String toTest = MoreObjects.toStringHelper(new TestClass()).add("field1", "Hello").toString();
    assertEquals("TestClass{field1=Hello}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToString_oneIntegerField() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass()).add("field1", Integer.valueOf(42)).toString();
    assertEquals("TestClass{field1=42}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToString_nullInteger() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass()).add("field1", (Integer) null).toString();
    assertEquals("TestClass{field1=null}", toTest);
  }

  public void testToStringLenient_oneField() {
    String toTest = MoreObjects.toStringHelper(new TestClass()).add("field1", "Hello").toString();
    assertTrue(toTest, toTest.matches(".*\\{field1\\=Hello\\}"));
  }

  public void testToStringLenient_oneIntegerField() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass()).add("field1", Integer.valueOf(42)).toString();
    assertTrue(toTest, toTest.matches(".*\\{field1\\=42\\}"));
  }

  public void testToStringLenient_nullInteger() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass()).add("field1", (Integer) null).toString();
    assertTrue(toTest, toTest.matches(".*\\{field1\\=null\\}"));
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToString_complexFields() {
    Map<String, Integer> map =
        ImmutableMap.<String, Integer>builder().put("abc", 1).put("def", 2).put("ghi", 3).build();
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .add("field1", "This is string.")
            .add("field2", Arrays.asList("abc", "def", "ghi"))
            .add("field3", map)
            .toString();
    String expected =
        "TestClass{"
            + "field1=This is string., field2=[abc, def, ghi], field3={abc=1, def=2, ghi=3}}";

    assertEquals(expected, toTest);
  }

  public void testToStringLenient_complexFields() {
    Map<String, Integer> map =
        ImmutableMap.<String, Integer>builder().put("abc", 1).put("def", 2).put("ghi", 3).build();
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .add("field1", "This is string.")
            .add("field2", Arrays.asList("abc", "def", "ghi"))
            .add("field3", map)
            .toString();
    String expectedRegex =
        ".*\\{"
            + "field1\\=This is string\\., "
            + "field2\\=\\[abc, def, ghi\\], "
            + "field3=\\{abc\\=1, def\\=2, ghi\\=3\\}\\}";

    assertTrue(toTest, toTest.matches(expectedRegex));
  }

  public void testToString_addWithNullName() {
    MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(new TestClass());
    assertThrows(NullPointerException.class, () -> helper.add(null, "Hello"));
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToString_addWithNullValue() {
    String result = MoreObjects.toStringHelper(new TestClass()).add("Hello", null).toString();

    assertEquals("TestClass{Hello=null}", result);
  }

  public void testToStringLenient_addWithNullValue() {
    String result = MoreObjects.toStringHelper(new TestClass()).add("Hello", null).toString();
    assertTrue(result, result.matches(".*\\{Hello\\=null\\}"));
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToString_toStringTwice() {
    MoreObjects.ToStringHelper helper =
        MoreObjects.toStringHelper(new TestClass())
            .add("field1", 1)
            .addValue("value1")
            .add("field2", "value2");
    String expected = "TestClass{field1=1, value1, field2=value2}";

    assertEquals(expected, helper.toString());
    // Call toString again
    assertEquals(expected, helper.toString());

    // Make sure the cached value is reset when we modify the helper at all
    String expected2 = "TestClass{field1=1, value1, field2=value2, 2}";
    helper.addValue(2);
    assertEquals(expected2, helper.toString());
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToString_addValue() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .add("field1", 1)
            .addValue("value1")
            .add("field2", "value2")
            .addValue(2)
            .toString();
    String expected = "TestClass{field1=1, value1, field2=value2, 2}";

    assertEquals(expected, toTest);
  }

  public void testToStringLenient_addValue() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .add("field1", 1)
            .addValue("value1")
            .add("field2", "value2")
            .addValue(2)
            .toString();
    String expected = ".*\\{field1\\=1, value1, field2\\=value2, 2\\}";

    assertTrue(toTest, toTest.matches(expected));
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToString_addValueWithNullValue() {
    String result =
        MoreObjects.toStringHelper(new TestClass())
            .addValue(null)
            .addValue("Hello")
            .addValue(null)
            .toString();
    String expected = "TestClass{null, Hello, null}";

    assertEquals(expected, result);
  }

  public void testToStringLenient_addValueWithNullValue() {
    String result =
        MoreObjects.toStringHelper(new TestClass())
            .addValue(null)
            .addValue("Hello")
            .addValue(null)
            .toString();
    String expected = ".*\\{null, Hello, null\\}";

    assertTrue(result, result.matches(expected));
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitNullValues_oneField() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass()).omitNullValues().add("field1", null).toString();
    assertEquals("TestClass{}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitEmptyValues_oneField() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass()).omitEmptyValues().add("field1", "").toString();
    assertEquals("TestClass{}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitNullValues_manyFieldsFirstNull() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitNullValues()
            .add("field1", null)
            .add("field2", "Googley")
            .add("field3", "World")
            .toString();
    assertEquals("TestClass{field2=Googley, field3=World}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitEmptyValues_manyFieldsFirstEmpty() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitEmptyValues()
            .add("field1", "")
            .add("field2", "Googley")
            .add("field3", "World")
            .toString();
    assertEquals("TestClass{field2=Googley, field3=World}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitNullValues_manyFieldsOmitAfterNull() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .add("field1", null)
            .add("field2", "Googley")
            .add("field3", "World")
            .omitNullValues()
            .toString();
    assertEquals("TestClass{field2=Googley, field3=World}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitEmptyValues_manyFieldsOmitAfterEmpty() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .add("field1", "")
            .add("field2", "Googley")
            .add("field3", "World")
            .omitEmptyValues()
            .toString();
    assertEquals("TestClass{field2=Googley, field3=World}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitNullValues_manyFieldsLastNull() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitNullValues()
            .add("field1", "Hello")
            .add("field2", "Googley")
            .add("field3", null)
            .toString();
    assertEquals("TestClass{field1=Hello, field2=Googley}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitEmptyValues_manyFieldsLastEmpty() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitEmptyValues()
            .add("field1", "Hello")
            .add("field2", "Googley")
            .add("field3", "")
            .toString();
    assertEquals("TestClass{field1=Hello, field2=Googley}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitNullValues_oneValue() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass()).omitEmptyValues().addValue("").toString();
    assertEquals("TestClass{}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitEmptyValues_oneValue() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass()).omitNullValues().addValue(null).toString();
    assertEquals("TestClass{}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitNullValues_manyValuesFirstNull() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitNullValues()
            .addValue(null)
            .addValue("Googley")
            .addValue("World")
            .toString();
    assertEquals("TestClass{Googley, World}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitEmptyValues_manyValuesFirstEmpty() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitEmptyValues()
            .addValue("")
            .addValue("Googley")
            .addValue("World")
            .toString();
    assertEquals("TestClass{Googley, World}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitNullValues_manyValuesLastNull() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitNullValues()
            .addValue("Hello")
            .addValue("Googley")
            .addValue(null)
            .toString();
    assertEquals("TestClass{Hello, Googley}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitEmptyValues_manyValuesLastEmpty() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitEmptyValues()
            .addValue("Hello")
            .addValue("Googley")
            .addValue("")
            .toString();
    assertEquals("TestClass{Hello, Googley}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitNullValues_differentOrder() {
    String expected = "TestClass{field1=Hello, field2=Googley, field3=World}";
    String toTest1 =
        MoreObjects.toStringHelper(new TestClass())
            .omitNullValues()
            .add("field1", "Hello")
            .add("field2", "Googley")
            .add("field3", "World")
            .toString();
    String toTest2 =
        MoreObjects.toStringHelper(new TestClass())
            .add("field1", "Hello")
            .add("field2", "Googley")
            .omitNullValues()
            .add("field3", "World")
            .toString();
    assertEquals(expected, toTest1);
    assertEquals(expected, toTest2);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitEmptyValues_differentOrder() {
    String expected = "TestClass{field1=Hello, field2=Googley, field3=World}";
    String toTest1 =
        MoreObjects.toStringHelper(new TestClass())
            .omitEmptyValues()
            .add("field1", "Hello")
            .add("field2", "Googley")
            .add("field3", "World")
            .toString();
    String toTest2 =
        MoreObjects.toStringHelper(new TestClass())
            .add("field1", "Hello")
            .add("field2", "Googley")
            .omitEmptyValues()
            .add("field3", "World")
            .toString();
    assertEquals(expected, toTest1);
    assertEquals(expected, toTest2);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitNullValues_canBeCalledManyTimes() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitNullValues()
            .omitNullValues()
            .add("field1", "Hello")
            .omitNullValues()
            .add("field2", "Googley")
            .omitNullValues()
            .add("field3", "World")
            .toString();
    assertEquals("TestClass{field1=Hello, field2=Googley, field3=World}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitEmptyValues_canBeCalledManyTimes() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitEmptyValues()
            .omitEmptyValues()
            .add("field1", "Hello")
            .omitEmptyValues()
            .add("field2", "Googley")
            .omitEmptyValues()
            .add("field3", "World")
            .toString();
    assertEquals("TestClass{field1=Hello, field2=Googley, field3=World}", toTest);
  }

  @GwtIncompatible // Class names are obfuscated in GWT
  public void testToStringOmitEmptyValues_allEmptyTypes() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitEmptyValues()
            // CharSequences
            .add("field1", "")
            .add("field2", new StringBuilder())
            // nio CharBuffer (implements CharSequence) is tested separately below
            // Collections and Maps
            .add("field11", Arrays.asList("Hello"))
            .add("field12", new ArrayList<>())
            .add("field13", new HashMap<>())
            // Optionals
            .add("field26", Optional.of("World"))
            .add("field27", Optional.absent())
            // Arrays
            .add("field31", new Object[] {"!!!"})
            .add("field32", new boolean[0])
            .add("field33", new byte[0])
            .add("field34", new char[0])
            .add("field35", new short[0])
            .add("field36", new int[0])
            .add("field37", new long[0])
            .add("field38", new float[0])
            .add("field39", new double[0])
            .add("field40", new Object[0])
            .toString();
    assertEquals("TestClass{field11=[Hello], field26=Optional.of(World), field31=[!!!]}", toTest);
  }

  @J2ktIncompatible // J2kt CharBuffer does not implement CharSequence so not recognized as empty
  @GwtIncompatible // CharBuffer not available
  public void testToStringOmitEmptyValues_charBuffer() {
    String toTest =
        MoreObjects.toStringHelper(new TestClass())
            .omitEmptyValues()
            .add("field1", "Hello")
            .add("field2", CharBuffer.allocate(0))
            .toString();
    assertEquals("TestClass{field1=Hello}", toTest);
  }

  public void testToStringHelperWithArrays() {
    String[] strings = {"hello", "world"};
    int[] ints = {2, 42};
    Object[] objects = {"obj"};
    @Nullable String[] arrayWithNull = new @Nullable String[] {null};
    Object[] empty = {};
    String toTest =
        MoreObjects.toStringHelper("TSH")
            .add("strings", strings)
            .add("ints", ints)
            .add("objects", objects)
            .add("arrayWithNull", arrayWithNull)
            .add("empty", empty)
            .toString();
    assertEquals(
        "TSH{strings=[hello, world], ints=[2, 42], objects=[obj], arrayWithNull=[null], empty=[]}",
        toTest);
  }

  /** Test class for testing formatting of inner classes. */
  private static class TestClass {}
}
