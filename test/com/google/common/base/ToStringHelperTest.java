/*
 * Copyright (C) 2009 Google Inc.
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

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Map;

/**
 * Tests for {@link Objects#toStringHelper(Object)}.
 * 
 * @author Jason Lee
 */
public class ToStringHelperTest extends TestCase {

  public void testConstructor_instance() {
    String toTest = Objects.toStringHelper(this).toString();
    assertEquals("ToStringHelperTest{}", toTest);
  }

  public void testConstructor_innerClass() {
    String toTest = Objects.toStringHelper(new TestClass()).toString();
    assertEquals("TestClass{}", toTest);
  }

  public void testConstructor_anonymousClass() {
    String toTest = Objects.toStringHelper(new Object() {}).toString();
    assertTrue(toTest.matches("[0-9]+\\{\\}"));
  }

  // all remaining test are on an inner class with various fields
  public void testToString_oneField() {
    String toTest = Objects.toStringHelper(new TestClass())
        .add("field1", "Hello")
        .toString();
    assertEquals("TestClass{field1=Hello}", toTest);
  }

  public void testToString_complexFields() {

    Map<String, Integer> map = ImmutableMap.<String, Integer>builder()
        .put("abc", 1)
        .put("def", 2)
        .put("ghi", 3)
        .build();
    String toTest = Objects.toStringHelper(new TestClass())
        .add("field1", "This is string.")
        .add("field2", Arrays.asList("abc", "def", "ghi"))
        .add("field3", map)
        .toString();    
    final String expected = "TestClass{"
        + "field1=This is string., field2=[abc, def, ghi], field3={abc=1, def=2, ghi=3}}"; 
    
    assertEquals(expected, toTest);
  }

  public void testToString_addWithNullName() {
    Objects.ToStringHelper helper = Objects.toStringHelper(new TestClass());
    try {
      helper.add(null, "Hello");
      fail("No exception was thrown.");
    } catch (NullPointerException expected) {
    }
  }

  public void testToString_addWithNullValue() {
    final String result = Objects.toStringHelper(new TestClass())
        .add("Hello", null)
        .toString();

    assertEquals("TestClass{Hello=null}", result);
  }
  
  public void testToString_addValue() {
    String toTest = Objects.toStringHelper(new TestClass()) 
        .add("field1", 1)
        .addValue("value1")
        .add("field2", "value2")
        .addValue(2)
        .toString();
    final String expected = "TestClass{field1=1, value1, field2=value2, 2}";
    
    assertEquals(expected, toTest);
  }

  public void testToString_addValueWithNullValue() {
    final String result = Objects.toStringHelper(new TestClass())
        .addValue(null)
        .addValue("Hello")
        .addValue(null)
        .toString();
    final String expected = "TestClass{null, Hello, null}";

    assertEquals(expected, result);
  }

  /**
   * Test class for testing formatting of inner classes.
   */
  private static class TestClass {}

}
