/*
 * Copyright (C) 2008 Google Inc.
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

import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.testing.util.NullPointerTester;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * Unit test for {@link Joiner}.
 *
 * @author Kevin Bourrillion
 */
public class JoinerTest extends TestCase {
  static final Joiner J = Joiner.on("-");

  // <Integer> needed to prevent warning :(
  static final Iterable<Integer> ITERABLE_ = Arrays.<Integer>asList();
  static final Iterable<Integer> ITERABLE_1 = Arrays.asList(1);
  static final Iterable<Integer> ITERABLE_12 = Arrays.asList(1, 2);
  static final Iterable<Integer> ITERABLE_123 = Arrays.asList(1, 2, 3);
  static final Iterable<Integer> ITERABLE_NULL = Arrays.asList((Integer) null);
  static final Iterable<Integer> ITERABLE_NULL_NULL
      = Arrays.asList((Integer) null, null);
  static final Iterable<Integer> ITERABLE_NULL_1 = Arrays.asList(null, 1);
  static final Iterable<Integer> ITERABLE_1_NULL = Arrays.asList(1, null);
  static final Iterable<Integer> ITERABLE_1_NULL_2 = Arrays.asList(1, null, 2);
  static final Iterable<Integer> ITERABLE_FOUR_NULLS
      = Arrays.asList((Integer) null, null, null, null);

  public void testNoSpecialNullBehavior() {
    checkNoOutput(J, ITERABLE_);
    checkResult(J, ITERABLE_1, "1");
    checkResult(J, ITERABLE_12, "1-2");
    checkResult(J, ITERABLE_123, "1-2-3");

    try {
      J.join(ITERABLE_NULL);
      fail();
    } catch (NullPointerException expected) {
    }
    try {
      J.join(ITERABLE_1_NULL_2);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testOnCharOverride() {
    Joiner onChar = Joiner.on('-');
    checkNoOutput(onChar, ITERABLE_);
    checkResult(onChar, ITERABLE_1, "1");
    checkResult(onChar, ITERABLE_12, "1-2");
    checkResult(onChar, ITERABLE_123, "1-2-3");
  }

  public void testSkipNulls() {
    Joiner skipNulls = J.skipNulls();
    checkNoOutput(skipNulls, ITERABLE_);
    checkNoOutput(skipNulls, ITERABLE_NULL);
    checkNoOutput(skipNulls, ITERABLE_NULL_NULL);
    checkNoOutput(skipNulls, ITERABLE_FOUR_NULLS);
    checkResult(skipNulls, ITERABLE_1, "1");
    checkResult(skipNulls, ITERABLE_12, "1-2");
    checkResult(skipNulls, ITERABLE_123, "1-2-3");
    checkResult(skipNulls, ITERABLE_NULL_1, "1");
    checkResult(skipNulls, ITERABLE_1_NULL, "1");
    checkResult(skipNulls, ITERABLE_1_NULL_2, "1-2");
  }

  public void testUseForNull() {
    Joiner zeroForNull = J.useForNull("0");
    checkNoOutput(zeroForNull, ITERABLE_);
    checkResult(zeroForNull, ITERABLE_1, "1");
    checkResult(zeroForNull, ITERABLE_12, "1-2");
    checkResult(zeroForNull, ITERABLE_123, "1-2-3");
    checkResult(zeroForNull, ITERABLE_NULL, "0");
    checkResult(zeroForNull, ITERABLE_NULL_NULL, "0-0");
    checkResult(zeroForNull, ITERABLE_NULL_1, "0-1");
    checkResult(zeroForNull, ITERABLE_1_NULL, "1-0");
    checkResult(zeroForNull, ITERABLE_1_NULL_2, "1-0-2");
    checkResult(zeroForNull, ITERABLE_FOUR_NULLS, "0-0-0-0");
  }

  private static void checkNoOutput(Joiner joiner, Iterable<Integer> set) {
    Object[] array = Iterables.toArray(set, Integer.class);
    assertEquals("", joiner.join(set));
    assertEquals("", joiner.join(array));

    StringBuilder sb1 = new StringBuilder();
    assertSame(sb1, joiner.appendTo(sb1, set));
    assertEquals(0, sb1.length());

    StringBuilder sb2 = new StringBuilder();
    assertSame(sb2, joiner.appendTo(sb2, array));
    assertEquals(0, sb2.length());

    try {
      joiner.appendTo(NASTY_APPENDABLE, set);
      joiner.appendTo(NASTY_APPENDABLE, array);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private static final Appendable NASTY_APPENDABLE = new Appendable() {
    public Appendable append(CharSequence csq) throws IOException {
      throw new IOException();
    }
    public Appendable append(CharSequence csq, int start, int end)
        throws IOException {
      throw new IOException();
    }
    public Appendable append(char c) throws IOException {
      throw new IOException();
    }
  };

  private static void checkResult(
      Joiner joiner, Iterable<Integer> parts, String expected) {
    Integer[] partsArray = Iterables.toArray(parts, Integer.class);

    assertEquals(expected, joiner.join(parts));

    StringBuilder sb1 = new StringBuilder().append('x');
    joiner.appendTo(sb1, parts);
    assertEquals("x" + expected, sb1.toString());

    assertEquals(expected, joiner.join(partsArray));

    StringBuilder sb2 = new StringBuilder().append('x');
    joiner.appendTo(sb2, partsArray);
    assertEquals("x" + expected, sb2.toString());

    int num = partsArray.length - 2;
    if (num >= 0) {
      Object[] rest = new Integer[num];
      for (int i = 0; i < num; i++) {
        rest[i] = partsArray[i + 2];
      }

      assertEquals(expected, joiner.join(partsArray[0], partsArray[1], rest));

      StringBuilder sb3 = new StringBuilder().append('x');
      joiner.appendTo(sb3, partsArray[0], partsArray[1], rest);
      assertEquals("x" + expected, sb3.toString());
    }
  }

  public void test_useForNull_skipNulls() {
    Joiner j = Joiner.on("x").useForNull("y");
    try {
      j.skipNulls();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void test_skipNulls_useForNull() {
    Joiner j = Joiner.on("x").skipNulls();
    try {
      j.useForNull("y");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void test_useForNull_twice() {
    Joiner j = Joiner.on("x").useForNull("y");
    try {
      j.useForNull("y");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testMap() {
    MapJoiner j = Joiner.on(";").withKeyValueSeparator(":");
    assertEquals("", j.join(ImmutableMap.of()));
    assertEquals(":", j.join(ImmutableMap.of("", "")));

    Map<String, String> mapWithNulls = Maps.newLinkedHashMap();
    mapWithNulls.put("a", null);
    mapWithNulls.put(null, "b");

    try {
      j.join(mapWithNulls);
      fail();
    } catch (NullPointerException expected) {
    }

    assertEquals("a:00;00:b", j.useForNull("00").join(mapWithNulls));

    StringBuilder sb = new StringBuilder();
    j.appendTo(sb, ImmutableMap.of(1, 2, 3, 4, 5, 6));
    assertEquals("1:2;3:4;5:6", sb.toString());
  }

  public void test_skipNulls_onMap() {
    Joiner j = Joiner.on(",").skipNulls();
    try {
      j.withKeyValueSeparator("/");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  private static class DontStringMeBro implements CharSequence {
    public int length() {
      return 3;
    }
    public char charAt(int index) {
      return "foo".charAt(index);
    }
    public CharSequence subSequence(int start, int end) {
      return "foo".subSequence(start, end);
    }
    @Override public String toString() {
      Assert.fail("shouldn't be invoked");
      return null;
    }
  }

  public void testDontConvertCharSequenceToString() {
    assertEquals("foo,foo", Joiner.on(",").join(
        new DontStringMeBro(), new DontStringMeBro()));
    assertEquals("foo,bar,foo", Joiner.on(",").useForNull("bar").join(
        new DontStringMeBro(), null, new DontStringMeBro()));
  }

  public void testNullPointers() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(StringBuilder.class, new StringBuilder());
    tester.testAllPublicStaticMethods(Joiner.class);
    tester.testAllPublicInstanceMethods(Joiner.on(","));
    tester.testAllPublicInstanceMethods(Joiner.on(",").skipNulls());
    tester.testAllPublicInstanceMethods(Joiner.on(",").useForNull("x"));
    tester.testAllPublicInstanceMethods(
        Joiner.on(",").withKeyValueSeparator("="));
  }
}
