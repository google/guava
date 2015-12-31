/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.collect.testing;

import static com.google.common.collect.testing.Helpers.NullsBeforeB;
import static com.google.common.collect.testing.Helpers.testComparator;

import com.google.common.annotations.GwtCompatible;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit test for {@link Helpers}.
 *
 * @author Chris Povirk
 */
@GwtCompatible
public class HelpersTest extends TestCase {
  public void testNullsBeforeB() {
    testComparator(NullsBeforeB.INSTANCE, "a", "azzzzzz", null, "b", "c");
  }

  public void testIsEmpty_iterable() {
    List<Object> list = new ArrayList<Object>();
    Helpers.assertEmpty(list);

    list.add("a");
    try {
      Helpers.assertEmpty(list);
      fail();
    } catch (AssertionFailedError expected) {
    }
  }

  public void testIsEmpty_map() {
    Map<Object, Object> map = new HashMap<Object, Object>();
    Helpers.assertEmpty(map);

    map.put("a", "b");
    try {
      Helpers.assertEmpty(map);
      fail();
    } catch (AssertionFailedError expected) {
    }
  }

  public void testAssertEqualInOrder() {
    List<?> list = Arrays.asList("a", "b", "c");
    Helpers.assertEqualInOrder(list, list);

    List<?> fewer = Arrays.asList("a", "b");
    try {
      Helpers.assertEqualInOrder(list, fewer);
      fail();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertEqualInOrder(fewer, list);
      fail();
    } catch (AssertionFailedError expected) {
    }

    List<?> differentOrder = Arrays.asList("a", "c", "b");
    try {
      Helpers.assertEqualInOrder(list, differentOrder);
      fail();
    } catch (AssertionFailedError expected) {
    }

    List<?> differentContents = Arrays.asList("a", "b", "C");
    try {
      Helpers.assertEqualInOrder(list, differentContents);
      fail();
    } catch (AssertionFailedError expected) {
    }
  }

  public void testAssertContentsInOrder() {
    List<?> list = Arrays.asList("a", "b", "c");
    Helpers.assertContentsInOrder(list, "a", "b", "c");

    try {
      Helpers.assertContentsInOrder(list, "a", "b");
      fail();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertContentsInOrder(list, "a", "b", "c", "d");
      fail();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertContentsInOrder(list, "a", "c", "b");
      fail();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertContentsInOrder(list, "a", "B", "c");
      fail();
    } catch (AssertionFailedError expected) {
    }
  }

  public void testAssertContains() {
    List<?> list = Arrays.asList("a", "b");
    Helpers.assertContains(list, "a");
    Helpers.assertContains(list, "b");

    try {
      Helpers.assertContains(list, "c");
      fail();
    } catch (AssertionFailedError expected) {
    }
  }

  public void testAssertContainsAllOf() {
    List<?> list = Arrays.asList("a", "a", "b", "c");
    Helpers.assertContainsAllOf(list, "a");
    Helpers.assertContainsAllOf(list, "a", "a");
    Helpers.assertContainsAllOf(list, "a", "b", "c");
    Helpers.assertContainsAllOf(list, "a", "b", "c", "a");

    try {
      Helpers.assertContainsAllOf(list, "d");
      fail();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertContainsAllOf(list, "a", "b", "c", "d");
      fail();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertContainsAllOf(list, "a", "a", "a");
      fail();
    } catch (AssertionFailedError expected) {
    }
  }
}
