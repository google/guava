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

import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static org.junit.contrib.truth.Truth.ASSERT;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.IteratorTester;

/**
 * Unit test for {@link TreeMultiset}.
 *
 * @author Neal Kanodia
 */
@GwtCompatible(emulated = true)
public class TreeMultisetTest extends AbstractMultisetTest {
  @SuppressWarnings("unchecked")
  @Override protected <E> Multiset<E> create() {
    return (Multiset) TreeMultiset.create();
  }

  public void testCreate() {
    TreeMultiset<String> multiset = TreeMultiset.create();
    multiset.add("foo", 2);
    multiset.add("bar");
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
    assertEquals(Ordering.natural(), multiset.comparator());
    assertEquals("[bar, foo x 2]", multiset.toString());
  }

  public void testCreateWithComparator() {
    Multiset<String> multiset = TreeMultiset.create(Collections.reverseOrder());
    multiset.add("foo", 2);
    multiset.add("bar");
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
    assertEquals("[foo x 2, bar]", multiset.toString());
  }

  public void testCreateFromIterable() {
    Multiset<String> multiset
        = TreeMultiset.create(Arrays.asList("foo", "bar", "foo"));
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
    assertEquals("[bar, foo x 2]", multiset.toString());
  }

  public void testToString() {
    ms.add("a", 3);
    ms.add("c", 1);
    ms.add("b", 2);

    assertEquals("[a x 3, b x 2, c]", ms.toString());
  }

  @GwtIncompatible("unreasonable slow")
  public void testIteratorBashing() {
    IteratorTester<String> tester =
        new IteratorTester<String>(createSample().size() + 2, MODIFIABLE,
            newArrayList(createSample()),
            IteratorTester.KnownOrder.KNOWN_ORDER) {
          private Multiset<String> targetMultiset;

          @Override protected Iterator<String> newTargetIterator() {
            targetMultiset = createSample();
            return targetMultiset.iterator();
          }

          @Override protected void verify(List<String> elements) {
            assertEquals(elements, Lists.newArrayList(targetMultiset));
          }
        };

    /* This next line added as a stopgap until JDK6 bug is fixed. */
    tester.ignoreSunJavaBug6529795();

    tester.test();
  }

  @GwtIncompatible("slow (~30s)")
  public void testElementSetIteratorBashing() {
    IteratorTester<String> tester = new IteratorTester<String>(5, MODIFIABLE,
        newArrayList("a", "b", "c"), IteratorTester.KnownOrder.KNOWN_ORDER) {
      private Set<String> targetSet;
      @Override protected Iterator<String> newTargetIterator() {
        Multiset<String> multiset = create();
        multiset.add("a", 3);
        multiset.add("c", 1);
        multiset.add("b", 2);
        targetSet = multiset.elementSet();
        return targetSet.iterator();
      }
      @Override protected void verify(List<String> elements) {
        assertEquals(elements, Lists.newArrayList(targetSet));
      }
    };

    /* This next line added as a stopgap until JDK6 bug is fixed. */
    tester.ignoreSunJavaBug6529795();

    tester.test();
  }

  public void testElementSetSortedSetMethods() {
    TreeMultiset<String> ms = TreeMultiset.create();
    ms.add("c", 1);
    ms.add("a", 3);
    ms.add("b", 2);
    SortedSet<String> elementSet = ms.elementSet();

    assertEquals("a", elementSet.first());
    assertEquals("c", elementSet.last());
    assertEquals(Ordering.natural(), elementSet.comparator());

    ASSERT.that(elementSet.headSet("b")).hasContentsInOrder("a");
    ASSERT.that(elementSet.tailSet("b")).hasContentsInOrder("b", "c");
    ASSERT.that(elementSet.subSet("a", "c")).hasContentsInOrder("a", "b");
  }

  public void testElementSetSubsetRemove() {
    TreeMultiset<String> ms = TreeMultiset.create();
    ms.add("a", 1);
    ms.add("b", 3);
    ms.add("c", 2);
    ms.add("d", 1);
    ms.add("e", 3);
    ms.add("f", 2);

    SortedSet<String> elementSet = ms.elementSet();
    ASSERT.that(elementSet).hasContentsInOrder("a", "b", "c", "d", "e", "f");
    SortedSet<String> subset = elementSet.subSet("b", "f");
    ASSERT.that(subset).hasContentsInOrder("b", "c", "d", "e");

    assertTrue(subset.remove("c"));
    ASSERT.that(elementSet).hasContentsInOrder("a", "b", "d", "e", "f");
    ASSERT.that(subset).hasContentsInOrder("b", "d", "e");
    assertEquals(10, ms.size());

    assertFalse(subset.remove("a"));
    ASSERT.that(elementSet).hasContentsInOrder("a", "b", "d", "e", "f");
    ASSERT.that(subset).hasContentsInOrder("b", "d", "e");
    assertEquals(10, ms.size());
  }

  public void testElementSetSubsetRemoveAll() {
    TreeMultiset<String> ms = TreeMultiset.create();
    ms.add("a", 1);
    ms.add("b", 3);
    ms.add("c", 2);
    ms.add("d", 1);
    ms.add("e", 3);
    ms.add("f", 2);

    SortedSet<String> elementSet = ms.elementSet();
    ASSERT.that(elementSet).hasContentsInOrder("a", "b", "c", "d", "e", "f");
    SortedSet<String> subset = elementSet.subSet("b", "f");
    ASSERT.that(subset).hasContentsInOrder("b", "c", "d", "e");

    assertTrue(subset.removeAll(Arrays.asList("a", "c")));
    ASSERT.that(elementSet).hasContentsInOrder("a", "b", "d", "e", "f");
    ASSERT.that(subset).hasContentsInOrder("b", "d", "e");
    assertEquals(10, ms.size());
  }

  public void testElementSetSubsetRetainAll() {
    TreeMultiset<String> ms = TreeMultiset.create();
    ms.add("a", 1);
    ms.add("b", 3);
    ms.add("c", 2);
    ms.add("d", 1);
    ms.add("e", 3);
    ms.add("f", 2);

    SortedSet<String> elementSet = ms.elementSet();
    ASSERT.that(elementSet).hasContentsInOrder("a", "b", "c", "d", "e", "f");
    SortedSet<String> subset = elementSet.subSet("b", "f");
    ASSERT.that(subset).hasContentsInOrder("b", "c", "d", "e");

    assertTrue(subset.retainAll(Arrays.asList("a", "c")));
    ASSERT.that(elementSet).hasContentsInOrder("a", "c", "f");
    ASSERT.that(subset).hasContentsInOrder("c");
    assertEquals(5, ms.size());
  }

  public void testElementSetSubsetClear() {
    TreeMultiset<String> ms = TreeMultiset.create();
    ms.add("a", 1);
    ms.add("b", 3);
    ms.add("c", 2);
    ms.add("d", 1);
    ms.add("e", 3);
    ms.add("f", 2);

    SortedSet<String> elementSet = ms.elementSet();
    ASSERT.that(elementSet).hasContentsInOrder("a", "b", "c", "d", "e", "f");
    SortedSet<String> subset = elementSet.subSet("b", "f");
    ASSERT.that(subset).hasContentsInOrder("b", "c", "d", "e");

    subset.clear();
    ASSERT.that(elementSet).hasContentsInOrder("a", "f");
    ASSERT.that(subset).hasContentsInOrder();
    assertEquals(3, ms.size());
  }

  public void testCustomComparator() throws Exception {
    Comparator<String> comparator = new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return o2.compareTo(o1);
      }
    };
    TreeMultiset<String> ms = TreeMultiset.create(comparator);

    ms.add("b");
    ms.add("c");
    ms.add("a");
    ms.add("b");
    ms.add("d");

    ASSERT.that(ms).hasContentsInOrder("d", "c", "b", "b", "a");

    SortedSet<String> elementSet = ms.elementSet();
    assertEquals("d", elementSet.first());
    assertEquals("a", elementSet.last());
    assertEquals(comparator, elementSet.comparator());
  }

  public void testNullAcceptingComparator() throws Exception {
    Comparator<String> comparator = Ordering.<String>natural().nullsFirst();
    TreeMultiset<String> ms = TreeMultiset.create(comparator);

    ms.add("b");
    ms.add(null);
    ms.add("a");
    ms.add("b");
    ms.add(null, 2);

    ASSERT.that(ms).hasContentsInOrder(null, null, null, "a", "b", "b");
    assertEquals(3, ms.count(null));

    SortedSet<String> elementSet = ms.elementSet();
    assertEquals(null, elementSet.first());
    assertEquals("b", elementSet.last());
    assertEquals(comparator, elementSet.comparator());
  }

  private static final Comparator<String> DEGENERATE_COMPARATOR =
      new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          return o1.length() - o2.length();
        }
      };

  /**
   * Test a TreeMultiset with a comparator that can return 0 when comparing
   * unequal values.
   */
  public void testDegenerateComparator() throws Exception {
    TreeMultiset<String> ms = TreeMultiset.create(DEGENERATE_COMPARATOR);

    ms.add("foo");
    ms.add("a");
    ms.add("bar");
    ms.add("b");
    ms.add("c");

    assertEquals(2, ms.count("bar"));
    assertEquals(3, ms.count("b"));

    Multiset<String> ms2 = TreeMultiset.create(DEGENERATE_COMPARATOR);

    ms2.add("cat", 2);
    ms2.add("x", 3);

    assertEquals(ms, ms2);
    assertEquals(ms2, ms);

    SortedSet<String> elementSet = ms.elementSet();
    assertEquals("a", elementSet.first());
    assertEquals("foo", elementSet.last());
    assertEquals(DEGENERATE_COMPARATOR, elementSet.comparator());
  }

  public void testSubMultisetSize() {
    TreeMultiset<String> ms = TreeMultiset.create();
    ms.add("a", Integer.MAX_VALUE);
    ms.add("b", Integer.MAX_VALUE);
    ms.add("c", 3);

    assertEquals(Integer.MAX_VALUE, ms.count("a"));
    assertEquals(Integer.MAX_VALUE, ms.count("b"));
    assertEquals(3, ms.count("c"));

    assertEquals(Integer.MAX_VALUE, ms.headMultiset("c", CLOSED).size());
    assertEquals(Integer.MAX_VALUE, ms.headMultiset("b", CLOSED).size());
    assertEquals(Integer.MAX_VALUE, ms.headMultiset("a", CLOSED).size());

    assertEquals(3, ms.tailMultiset("c", CLOSED).size());
    assertEquals(Integer.MAX_VALUE, ms.tailMultiset("b", CLOSED).size());
    assertEquals(Integer.MAX_VALUE, ms.tailMultiset("a", CLOSED).size());
  }

  @Override public void testToStringNull() {
    c = ms = TreeMultiset.create(Ordering.natural().nullsFirst());
    super.testToStringNull();
  }
}

