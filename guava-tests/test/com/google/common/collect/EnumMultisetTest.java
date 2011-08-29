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
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Tests for an {@link EnumMultiset}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class EnumMultisetTest extends TestCase {
  private static enum Color {
    BLUE, RED, YELLOW, GREEN, WHITE
  }

  public void testClassCreate() {
    Multiset<Color> ms = EnumMultiset.create(Color.class);
    ms.add(Color.RED);
    ms.add(Color.YELLOW);
    ms.add(Color.RED);
    assertEquals(0, ms.count(Color.BLUE));
    assertEquals(1, ms.count(Color.YELLOW));
    assertEquals(2, ms.count(Color.RED));
  }

  public void testCollectionCreate() {
    Multiset<Color> ms = EnumMultiset.create(
        asList(Color.RED, Color.YELLOW, Color.RED));
    assertEquals(0, ms.count(Color.BLUE));
    assertEquals(1, ms.count(Color.YELLOW));
    assertEquals(2, ms.count(Color.RED));
  }

  public void testIllegalCreate() {
    Collection<Color> empty = EnumSet.noneOf(Color.class);
    try {
      EnumMultiset.create(empty);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testToString() {
    Multiset<Color> ms = EnumMultiset.create(Color.class);
    ms.add(Color.BLUE, 3);
    ms.add(Color.YELLOW, 1);
    ms.add(Color.RED, 2);
    assertEquals("[BLUE x 3, RED x 2, YELLOW]", ms.toString());
  }

  @GwtIncompatible("SerializableTester")
  public void testSerializable() {
    Multiset<Color> ms = EnumMultiset.create(
        asList(Color.RED, Color.YELLOW, Color.RED));
    assertEquals(ms, SerializableTester.reserialize(ms));
  }
  
  public void testEntrySet() {
    Multiset<Color> ms = EnumMultiset.create(Color.class);
    ms.add(Color.BLUE, 3);
    ms.add(Color.YELLOW, 1);
    ms.add(Color.RED, 2);
    
    Set<Object> uniqueEntries = Sets.newIdentityHashSet();
    uniqueEntries.addAll(ms.entrySet());
    assertEquals(3, uniqueEntries.size());
  }
}
