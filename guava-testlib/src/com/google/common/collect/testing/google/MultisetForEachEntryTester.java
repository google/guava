/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Ignore;

/**
 * Tests for {@code Multiset#forEachEntry}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultisetForEachEntryTester<E> extends AbstractMultisetTester<E> {
  public void testForEachEntry() {
    List<Entry<E>> expected = new ArrayList<>(getMultiset().entrySet());
    List<Entry<E>> actual = new ArrayList<>();
    getMultiset()
        .forEachEntry((element, count) -> actual.add(Multisets.immutableEntry(element, count)));
    Helpers.assertEqualIgnoringOrder(expected, actual);
  }

  @CollectionFeature.Require(KNOWN_ORDER)
  public void testForEachEntryOrdered() {
    List<Entry<E>> expected = new ArrayList<>(getMultiset().entrySet());
    List<Entry<E>> actual = new ArrayList<>();
    getMultiset()
        .forEachEntry((element, count) -> actual.add(Multisets.immutableEntry(element, count)));
    assertEquals(expected, actual);
  }

  public void testForEachEntryDuplicates() {
    initThreeCopies();
    List<Entry<E>> expected = Collections.singletonList(Multisets.immutableEntry(e0(), 3));
    List<Entry<E>> actual = new ArrayList<>();
    getMultiset()
        .forEachEntry((element, count) -> actual.add(Multisets.immutableEntry(element, count)));
    assertEquals(expected, actual);
  }

  /**
   * Returns {@link Method} instances for the remove tests that assume multisets support duplicates
   * so that the test of {@code Multisets.forSet()} can suppress them.
   */
  @GwtIncompatible // reflection
  public static List<Method> getForEachEntryDuplicateInitializingMethods() {
    return Arrays.asList(
        Helpers.getMethod(MultisetForEachEntryTester.class, "testForEachEntryDuplicates"));
  }
}
