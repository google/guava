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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

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
    List<Object> list = new ArrayList<>();
    Helpers.assertEmpty(list);
    Helpers.assertEmpty(
        new Iterable<Object>() {
          @Override
          public Iterator<Object> iterator() {
            return Collections.emptyList().iterator();
          }
        });

    list.add("a");
    try {
      Helpers.assertEmpty(list);
      throw new Error();
    } catch (AssertionFailedError expected) {
    }
    try {
      Helpers.assertEmpty(
          new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
              return Collections.singleton("a").iterator();
            }
          });
      throw new Error();
    } catch (AssertionFailedError expected) {
    }
  }

  public void testIsEmpty_map() {
    Map<Object, Object> map = new HashMap<>();
    Helpers.assertEmpty(map);

    map.put("a", "b");
    try {
      Helpers.assertEmpty(map);
      throw new Error();
    } catch (AssertionFailedError expected) {
    }
  }

  public void testAssertEqualInOrder() {
    List<?> list = Arrays.asList("a", "b", "c");
    Helpers.assertEqualInOrder(list, list);

    List<?> fewer = Arrays.asList("a", "b");
    try {
      Helpers.assertEqualInOrder(list, fewer);
      throw new Error();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertEqualInOrder(fewer, list);
      throw new Error();
    } catch (AssertionFailedError expected) {
    }

    List<?> differentOrder = Arrays.asList("a", "c", "b");
    try {
      Helpers.assertEqualInOrder(list, differentOrder);
      throw new Error();
    } catch (AssertionFailedError expected) {
    }

    List<?> differentContents = Arrays.asList("a", "b", "C");
    try {
      Helpers.assertEqualInOrder(list, differentContents);
      throw new Error();
    } catch (AssertionFailedError expected) {
    }
  }

  public void testAssertContentsInOrder() {
    List<?> list = Arrays.asList("a", "b", "c");
    Helpers.assertContentsInOrder(list, "a", "b", "c");

    try {
      Helpers.assertContentsInOrder(list, "a", "b");
      throw new Error();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertContentsInOrder(list, "a", "b", "c", "d");
      throw new Error();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertContentsInOrder(list, "a", "c", "b");
      throw new Error();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertContentsInOrder(list, "a", "B", "c");
      throw new Error();
    } catch (AssertionFailedError expected) {
    }
  }

  public void testAssertContains() {
    List<?> list = Arrays.asList("a", "b");
    Helpers.assertContains(list, "a");
    Helpers.assertContains(list, "b");

    try {
      Helpers.assertContains(list, "c");
      throw new Error();
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
      throw new Error();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertContainsAllOf(list, "a", "b", "c", "d");
      throw new Error();
    } catch (AssertionFailedError expected) {
    }

    try {
      Helpers.assertContainsAllOf(list, "a", "a", "a");
      throw new Error();
    } catch (AssertionFailedError expected) {
    }
  }
}
