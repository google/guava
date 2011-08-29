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

/**
 * Unit test for {@link ConcurrentHashMultiset}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
public abstract class AbstractConcurrentHashMultisetTest
    extends AbstractMultisetTest {
  // we don't support null
  @Override public void testToStringNull() {}

  // our entries are snapshots, not live views. at least for now.

  @Override public void testEntryAfterRemove() {}
  @Override public void testEntryAfterClear() {}
  @Override public void testEntryAfterEntrySetClear() {}
  @Override public void testEntryAfterEntrySetIteratorRemove() {}
  @Override public void testEntryAfterElementSetIteratorRemove() {}

  public void testCopyConstructor() {
    ms = ConcurrentHashMultiset.create(asList("a", "b", "a", "c"));
    assertEquals(4, ms.size());
    assertEquals(2, ms.count("a"));
    assertEquals(1, ms.count("b"));
    assertEquals(1, ms.count("c"));
  }

  public void testSetCount() {
    ConcurrentHashMultiset<String> cms = ConcurrentHashMultiset.create();
    cms.add("a", 2);
    cms.add("b", 3);

    try {
      cms.setCount("a", -1);
      fail();
    } catch (IllegalArgumentException expected) {}
    assertEquals(2, cms.count("a"));

    assertEquals(2, cms.setCount("a", 0));
    assertEquals(0, cms.count("a"));
    assertEquals(3, cms.setCount("b", 4));
    assertEquals(4, cms.count("b"));
    assertEquals(0, cms.setCount("c", 5));
    assertEquals(5, cms.count("c"));
  }

  public void testSetCountConditional() {
    ConcurrentHashMultiset<String> cms = ConcurrentHashMultiset.create();
    cms.add("a", 2);
    cms.add("b", 3);

    try {
      cms.setCount("a", -1, 1);
      fail();
    } catch (IllegalArgumentException expected) {}
    try {
      cms.setCount("a", 1, -1);
      fail();
    } catch (IllegalArgumentException expected) {}
    assertEquals(2, cms.count("a"));

    assertTrue(cms.setCount("c", 0, 0));
    assertEquals(0, cms.count("c"));
    assertFalse(cms.setCount("c", 1, 0));
    assertEquals(0, cms.count("c"));
    assertFalse(cms.setCount("a", 0, 0));
    assertEquals(2, cms.count("a"));
    assertFalse(cms.setCount("a", 1, 0));
    assertEquals(2, cms.count("a"));
    assertTrue(cms.setCount("a", 2, 0));
    assertEquals(0, cms.count("a"));

    assertTrue(cms.setCount("d", 0, 4));
    assertEquals(4, cms.count("d"));
    assertFalse(cms.setCount("b", 0, 5));
    assertEquals(3, cms.count("b"));
    assertFalse(cms.setCount("b", 1, 5));
    assertEquals(3, cms.count("b"));
    assertTrue(cms.setCount("b", 3, 5));
    assertEquals(5, cms.count("b"));
  }

  public void testRemoveExactly() {
    ConcurrentHashMultiset<String> cms = ConcurrentHashMultiset.create();
    cms.add("a", 2);
    cms.add("b", 3);

    try {
      cms.removeExactly("a", -2);
    } catch (IllegalArgumentException expected) {}

    assertTrue(cms.removeExactly("a", 0));
    assertEquals(2, cms.count("a"));
    assertTrue(cms.removeExactly("c", 0));
    assertEquals(0, cms.count("c"));

    assertFalse(cms.removeExactly("a", 4));
    assertEquals(2, cms.count("a"));
    assertTrue(cms.removeExactly("a", 2));
    assertEquals(0, cms.count("a"));
    assertTrue(cms.removeExactly("b", 2));
    assertEquals(1, cms.count("b"));
  }
}
