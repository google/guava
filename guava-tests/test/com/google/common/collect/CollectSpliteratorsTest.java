/*
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Ascii;
import com.google.common.collect.testing.SpliteratorTester;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import junit.framework.TestCase;

/** Tests for {@code CollectSpliterators}. */
@GwtCompatible
public class CollectSpliteratorsTest extends TestCase {
  public void testMap() {
    SpliteratorTester.of(
            () ->
                CollectSpliterators.map(
                    Arrays.spliterator(new String[] {"a", "b", "c", "d", "e"}), Ascii::toUpperCase))
        .expect("A", "B", "C", "D", "E");
  }

  public void testFlatMap() {
    SpliteratorTester.of(
            () ->
                CollectSpliterators.flatMap(
                    Arrays.spliterator(new String[] {"abc", "", "de", "f", "g", ""}),
                    (String str) -> Lists.charactersOf(str).spliterator(),
                    Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.NONNULL,
                    7))
        .expect('a', 'b', 'c', 'd', 'e', 'f', 'g');
  }

  public void testMultisetsSpliterator() {
    Multiset<String> multiset = TreeMultiset.create();
    multiset.add("a", 3);
    multiset.add("b", 1);
    multiset.add("c", 2);

    List<String> actualValues = Lists.newArrayList();
    multiset.spliterator().forEachRemaining(actualValues::add);
    assertThat(multiset).containsExactly("a", "a", "a", "b", "c", "c").inOrder();
  }
}
