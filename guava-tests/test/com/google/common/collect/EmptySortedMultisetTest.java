/*
 * Copyright (C) 2011 The Guava Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

/**
 * Test cases for {@link EmptySortedMultiset}.
 * 
 * @author Louis Wasserman
 */
@GwtCompatible
public class EmptySortedMultisetTest extends TestCase {
  public void testContainsNull() {
    assertFalse(EmptySortedMultiset.natural().contains(null));
  }

  public void testCountNull() {
    assertEquals(EmptySortedMultiset.natural().count(null), 0);
  }

  public void testRemoveNull() {
    assertFalse(EmptySortedMultiset.natural().remove(null));
  }

  public void testAdd() {
    try {
      EmptySortedMultiset.natural().add("abc");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testAddNull() {
    try {
      EmptySortedMultiset.natural().add(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  public void testRemove() {
    assertFalse(EmptySortedMultiset.natural().remove("abc"));
  }

  public void testIsEmpty() {
    assertTrue(EmptySortedMultiset.natural().isEmpty());
    assertEquals(0, EmptySortedMultiset.natural().size());
    assertEquals(0, EmptySortedMultiset.natural().entrySet().size());
    assertEquals(0, EmptySortedMultiset.natural().elementSet().size());
  }

  public void testElementSetAdd() {
    try {
      EmptySortedMultiset.natural().elementSet().add("abc");
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
  }
}
