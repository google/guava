/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect;

import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Unit test for {@link HashMultiset}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class HashMultisetTest extends TestCase {

  private static TestStringMultisetGenerator hashMultisetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override protected Multiset<String> create(String[] elements) {
        return HashMultiset.create(asList(elements));
      }
    };
  }

  public void testCreate() {
    Multiset<String> multiset = HashMultiset.create();
    multiset.add("foo", 2);
    multiset.add("bar");
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
  }

  public void testCreateWithSize() {
    Multiset<String> multiset = HashMultiset.create(50);
    multiset.add("foo", 2);
    multiset.add("bar");
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
  }

  public void testCreateFromIterable() {
    Multiset<String> multiset
        = HashMultiset.create(Arrays.asList("foo", "bar", "foo"));
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
  }

  /*
   * The behavior of toString() and iteration is tested by LinkedHashMultiset,
   * which shares a lot of code with HashMultiset and has deterministic
   * iteration order.
   */
}

