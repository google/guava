/*
 * Copyright (C) 2026 The Guava Authors
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

package com.google.common.collect.testing.testers;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import java.util.Spliterator;
import org.jspecify.annotations.NullMarked;

/**
 * A generic JUnit test which tests the {@code spliterator} characteristics of the collection views
 * of a concurrent map. Can't be invoked directly; please see {@link
 * com.google.common.collect.testing.ConcurrentMapTestSuiteBuilder}.
 *
 * <p>The size of a concurrent map may change while its views are being traversed, so their
 * spliterators must not report {@link Spliterator#SIZED} or {@link Spliterator#SUBSIZED}. Typically
 * they report {@link Spliterator#CONCURRENT} instead, and the {@link Spliterator} documentation
 * says that a top-level spliterator should not report both {@code CONCURRENT} and {@code SIZED}.
 */
@GwtCompatible
@NullMarked
public class ConcurrentMapSpliteratorTester<K, V> extends AbstractMapTester<K, V> {
  public void testKeySetSpliteratorNotSized() {
    assertNotSized("keySet()", getMap().keySet().spliterator());
  }

  public void testValuesSpliteratorNotSized() {
    assertNotSized("values()", getMap().values().spliterator());
  }

  public void testEntrySetSpliteratorNotSized() {
    assertNotSized("entrySet()", getMap().entrySet().spliterator());
  }

  private static void assertNotSized(String view, Spliterator<?> spliterator) {
    assertFalse(
        view + ".spliterator() should not report SIZED",
        spliterator.hasCharacteristics(Spliterator.SIZED));
    assertFalse(
        view + ".spliterator() should not report SUBSIZED",
        spliterator.hasCharacteristics(Spliterator.SUBSIZED));
  }
}
