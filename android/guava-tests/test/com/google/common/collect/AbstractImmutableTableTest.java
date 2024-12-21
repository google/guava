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

package com.google.common.collect;

import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;

/**
 * Tests {@link ImmutableTable}
 *
 * @author Gregory Kick
 */
@GwtCompatible
@NullMarked
public abstract class AbstractImmutableTableTest extends TestCase {

  abstract Iterable<ImmutableTable<Character, Integer, String>> getTestInstances();

  public final void testClear() {
    for (Table<Character, Integer, String> testInstance : getTestInstances()) {
      assertThrows(UnsupportedOperationException.class, () -> testInstance.clear());
    }
  }

  public final void testPut() {
    for (Table<Character, Integer, String> testInstance : getTestInstances()) {
      assertThrows(UnsupportedOperationException.class, () -> testInstance.put('a', 1, "blah"));
    }
  }

  public final void testPutAll() {
    for (Table<Character, Integer, String> testInstance : getTestInstances()) {
      assertThrows(
          UnsupportedOperationException.class,
          () -> testInstance.putAll(ImmutableTable.of('a', 1, "blah")));
    }
  }

  public final void testRemove() {
    for (Table<Character, Integer, String> testInstance : getTestInstances()) {
      assertThrows(UnsupportedOperationException.class, () -> testInstance.remove('a', 1));
    }
  }

  public final void testConsistentToString() {
    for (ImmutableTable<Character, Integer, String> testInstance : getTestInstances()) {
      assertEquals(testInstance.rowMap().toString(), testInstance.toString());
    }
  }

  public final void testConsistentHashCode() {
    for (ImmutableTable<Character, Integer, String> testInstance : getTestInstances()) {
      assertEquals(testInstance.cellSet().hashCode(), testInstance.hashCode());
    }
  }
}
