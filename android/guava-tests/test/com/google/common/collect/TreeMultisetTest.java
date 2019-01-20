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
import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.sort;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers.NullsBeforeB;
import com.google.common.collect.testing.NavigableSetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.MultisetFeature;
import com.google.common.collect.testing.google.SortedMultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for {@link TreeMultiset}.
 *
 * @author Neal Kanodia
 */
@GwtCompatible(emulated = true)
public class TreeMultisetTest extends TestCase {

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        SortedMultisetTestSuiteBuilder.using(
                new TestStringMultisetGenerator() {
                  @Override
                  protected Multiset<String> create(String[] elements) {
                    return TreeMultiset.create(Arrays.asList(elements));
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Ordering.natural().sortedCopy(insertionOrder);
                  }
                })
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.GENERAL_PURPOSE,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES,
                MultisetFeature.ENTRIES_ARE_VIEWS)
            .named("TreeMultiset, Ordering.natural")
            .createTestSuite());
    suite.addTest(
        SortedMultisetTestSuiteBuilder.using(
                new TestStringMultisetGenerator() {
                  @Override
                  protected Multiset<String> create(String[] elements) {
                    Multiset<String> result = TreeMultiset.create(NullsBeforeB.INSTANCE);
                    Collections.addAll(result, elements);
                    return result;
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    sort(insertionOrder, NullsBeforeB.INSTANCE);
                    return insertionOrder;
                  }
                })
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.GENERAL_PURPOSE,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_VALUES,
                MultisetFeature.ENTRIES_ARE_VIEWS)
            .named("TreeMultiset, NullsBeforeB")
            .createTestSuite());
    suite.addTest(
        NavigableSetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return TreeMultiset.create(Arrays.asList(elements)).elementSet();
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Lists.newArrayList(Sets.newTreeSet(insertionOrder));
                  }
                })
            .named("TreeMultiset[Ordering.natural].elementSet")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.REMOVE_OPERATIONS,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTestSuite(TreeMultisetTest.class);
    return suite;
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
    Multiset<String> multiset = TreeMultiset.create(Arrays.asList("foo", "bar", "foo"));
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
    assertEquals("[bar, foo x 2]", multiset.toString());
  }

  public void testToString() {
    Multiset<String> ms = TreeMultiset.create();
    ms.add("a", 3);
    ms.add("c", 1);
    ms.add("b", 2);

    assertEquals("[a x 3, b x 2, c]", ms.toString());
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

    assertThat(elementSet.headSet("b")).containsExactly("a");
    assertThat(elementSet.tailSet("b")).containsExactly("b", "c").inOrder();
    assertThat(elementSet.subSet("a", "c")).containsExactly("a", "b").inOrder();
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
    assertThat(elementSet).containsExactly("a", "b", "c", "d", "e", "f").inOrder();
    SortedSet<String> subset = elementSet.subSet("b", "f");
    assertThat(subset).containsExactly("b", "c", "d", "e").inOrder();

    assertTrue(subset.remove("c"));
    assertThat(elementSet).containsExactly("a", "b", "d", "e", "f").inOrder();
    assertThat(subset).containsExactly("b", "d", "e").inOrder();
    assertEquals(10, ms.size());

    assertFalse(subset.remove("a"));
    assertThat(elementSet).containsExactly("a", "b", "d", "e", "f").inOrder();
    assertThat(subset).containsExactly("b", "d", "e").inOrder();
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
    assertThat(elementSet).containsExactly("a", "b", "c", "d", "e", "f").inOrder();
    SortedSet<String> subset = elementSet.subSet("b", "f");
    assertThat(subset).containsExactly("b", "c", "d", "e").inOrder();

    assertTrue(subset.removeAll(Arrays.asList("a", "c")));
    assertThat(elementSet).containsExactly("a", "b", "d", "e", "f").inOrder();
    assertThat(subset).containsExactly("b", "d", "e").inOrder();
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
    assertThat(elementSet).containsExactly("a", "b", "c", "d", "e", "f").inOrder();
    SortedSet<String> subset = elementSet.subSet("b", "f");
    assertThat(subset).containsExactly("b", "c", "d", "e").inOrder();

    assertTrue(subset.retainAll(Arrays.asList("a", "c")));
    assertThat(elementSet).containsExactly("a", "c", "f").inOrder();
    assertThat(subset).containsExactly("c");
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
    assertThat(elementSet).containsExactly("a", "b", "c", "d", "e", "f").inOrder();
    SortedSet<String> subset = elementSet.subSet("b", "f");
    assertThat(subset).containsExactly("b", "c", "d", "e").inOrder();

    subset.clear();
    assertThat(elementSet).containsExactly("a", "f").inOrder();
    assertThat(subset).isEmpty();
    assertEquals(3, ms.size());
  }

  public void testCustomComparator() throws Exception {
    Comparator<String> comparator =
        new Comparator<String>() {
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

    assertThat(ms).containsExactly("d", "c", "b", "b", "a").inOrder();

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

    assertThat(ms).containsExactly(null, null, null, "a", "b", "b").inOrder();
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

  /** Test a TreeMultiset with a comparator that can return 0 when comparing unequal values. */
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

  @GwtIncompatible // reflection
  @AndroidIncompatible // Reflection bug, or actual binary compatibility problem?
  public void testElementSetBridgeMethods() {
    for (Method m : TreeMultiset.class.getMethods()) {
      if (m.getName().equals("elementSet") && m.getReturnType().equals(SortedSet.class)) {
        return;
      }
    }
    fail("No bridge method found");
  }
}
