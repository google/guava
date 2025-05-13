/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterators.emptyIterator;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.symmetricDifference;
import static com.google.common.collect.Sets.union;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.EqualsTester;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit tests for {@link SetView}s: {@link Sets#union}, {@link Sets#intersection}, {@link
 * Sets#difference}, and {@link Sets#symmetricDifference}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@NullMarked
public class SetViewTest extends TestCase {
  @J2ktIncompatible
  @GwtIncompatible // suite
  @AndroidIncompatible // test-suite builders
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return union(emptySet(), emptySet());
                  }
                })
            .named("empty U empty")
            .withFeatures(CollectionSize.ZERO, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return union(singleton(elements[0]), singleton(elements[0]));
                  }
                })
            .named("singleton U itself")
            .withFeatures(CollectionSize.ONE, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return union(emptySet(), newHashSet(elements));
                  }
                })
            .named("empty U set")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return union(newHashSet(elements), emptySet());
                  }
                })
            .named("set U empty")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    // Put the sets in different orders for the hell of it
                    return union(
                        new LinkedHashSet<>(asList(elements[0], elements[1], elements[2])),
                        new LinkedHashSet<>(asList(elements[1], elements[0], elements[2])));
                  }
                })
            .named("set U itself")
            .withFeatures(CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return union(newHashSet(elements[0], elements[1]), newHashSet(elements[2]));
                  }
                })
            .named("set U disjoint")
            .withFeatures(CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return union(
                        newHashSet(elements[0], elements[1]), newHashSet(elements[1], elements[2]));
                  }
                })
            .named("set U set")
            .withFeatures(CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return intersection(emptySet(), emptySet());
                  }
                })
            .named("empty & empty")
            .withFeatures(CollectionSize.ZERO, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return intersection(emptySet(), newHashSet(samples()));
                  }
                })
            .named("empty & set")
            .withFeatures(CollectionSize.ZERO, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return intersection(newHashSet(samples()), emptySet());
                  }
                })
            .named("set & empty")
            .withFeatures(CollectionSize.ZERO, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return intersection(
                        newHashSet(samples().e1(), samples().e3()),
                        newHashSet(samples().e2(), samples().e4()));
                  }
                })
            .named("set & disjoint")
            .withFeatures(CollectionSize.ZERO, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return intersection(newHashSet(elements), newHashSet(elements));
                  }
                })
            .named("set & itself")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Set<String> set1 = newHashSet(elements);
                    set1.add(samples().e3());
                    Set<String> set2 = newHashSet(elements);
                    set2.add(samples().e4());
                    return intersection(set1, set2);
                  }
                })
            .named("set & set")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return difference(emptySet(), emptySet());
                  }
                })
            .named("empty - empty")
            .withFeatures(CollectionSize.ZERO, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return difference(emptySet(), newHashSet(samples()));
                  }
                })
            .named("empty - set")
            .withFeatures(CollectionSize.ZERO, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return difference(newHashSet(samples()), newHashSet(samples()));
                  }
                })
            .named("set - itself")
            .withFeatures(CollectionSize.ZERO, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return difference(
                        newHashSet(samples().e3(), samples().e4()), newHashSet(samples()));
                  }
                })
            .named("set - superset")
            .withFeatures(CollectionSize.ZERO, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Set<String> difference = newHashSet(elements);
                    Set<String> set = newHashSet(samples());
                    set.addAll(difference);
                    Set<String> subset = newHashSet(samples());
                    subset.removeAll(difference);
                    return difference(set, subset);
                  }
                })
            .named("set - subset")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Set<String> set = newHashSet(elements);
                    set.add(samples().e3());
                    return difference(set, newHashSet(samples().e3(), samples().e4()));
                  }
                })
            .named("set - set")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return difference(newHashSet(elements), emptySet());
                  }
                })
            .named("set - empty")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return difference(
                        newHashSet(elements), newHashSet(samples().e3(), samples().e4()));
                  }
                })
            .named("set - disjoint")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return symmetricDifference(emptySet(), emptySet());
                  }
                })
            .named("empty ^ empty")
            .withFeatures(CollectionSize.ZERO, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return symmetricDifference(newHashSet(samples()), newHashSet(samples()));
                  }
                })
            .named("set ^ itself")
            .withFeatures(CollectionSize.ZERO, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return symmetricDifference(emptySet(), newHashSet(elements));
                  }
                })
            .named("empty ^ set")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return symmetricDifference(newHashSet(elements), emptySet());
                  }
                })
            .named("set ^ empty")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Set<String> difference = newHashSet(elements);
                    Set<String> set = newHashSet(samples());
                    set.removeAll(difference);
                    Set<String> superset = newHashSet(samples());
                    superset.addAll(difference);
                    return symmetricDifference(set, superset);
                  }
                })
            .named("set ^ superset")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Set<String> difference = newHashSet(elements);
                    Set<String> set = newHashSet(samples());
                    set.addAll(difference);
                    Set<String> subset = newHashSet(samples());
                    subset.removeAll(difference);
                    return symmetricDifference(set, subset);
                  }
                })
            .named("set ^ subset")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return symmetricDifference(
                        newHashSet(elements[0], elements[1]), newHashSet(elements[2]));
                  }
                })
            .named("set ^ disjoint")
            .withFeatures(CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return symmetricDifference(
                        newHashSet(elements[0], elements[1], samples().e3(), samples().e4()),
                        newHashSet(elements[2], samples().e3(), samples().e4()));
                  }
                })
            .named("set ^ set")
            .withFeatures(CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTestSuite(SetViewTest.class);
    return suite;
  }

  public void testUnion_isView() {
    Set<Integer> set1 = newHashSet(1, 2);
    Set<Integer> set2 = newHashSet(2, 3);
    Set<Integer> union = union(set1, set2);
    assertThat(union).containsExactly(1, 2, 3);
    set1.add(0);
    assertThat(union).containsExactly(0, 1, 2, 3);
    set2.remove(3);
    assertThat(union).containsExactly(0, 1, 2);
  }

  public void testIntersection_isView() {
    Set<Integer> set1 = newHashSet(1, 2);
    Set<Integer> set2 = newHashSet(2, 3);
    Set<Integer> intersection = intersection(set1, set2);
    assertThat(intersection).containsExactly(2);
    set1.add(3);
    assertThat(intersection).containsExactly(2, 3);
    set2.add(1);
    assertThat(intersection).containsExactly(1, 2, 3);
  }

  public void testDifference_isView() {
    Set<Integer> set1 = newHashSet(1, 2);
    Set<Integer> set2 = newHashSet(2, 3);
    Set<Integer> difference = difference(set1, set2);
    assertThat(difference).containsExactly(1);
    set1.add(0);
    assertThat(difference).containsExactly(0, 1);
    set2.remove(2);
    assertThat(difference).containsExactly(0, 1, 2);
  }

  public void testSymmetricDifference_isView() {
    Set<Integer> set1 = newHashSet(1, 2);
    Set<Integer> set2 = newHashSet(2, 3);
    Set<Integer> difference = symmetricDifference(set1, set2);
    assertThat(difference).containsExactly(1, 3);
    set1.add(0);
    assertThat(difference).containsExactly(0, 1, 3);
    set2.remove(2);
    assertThat(difference).containsExactly(0, 1, 2, 3);
  }

  public void testImmutableCopy_empty() {
    assertThat(union(emptySet(), emptySet()).immutableCopy()).isEmpty();
    assertThat(intersection(newHashSet(1, 2), newHashSet(3, 4)).immutableCopy()).isEmpty();
    assertThat(difference(newHashSet(1, 2), newHashSet(1, 2)).immutableCopy()).isEmpty();
    assertThat(symmetricDifference(newHashSet(1, 2), newHashSet(1, 2)).immutableCopy()).isEmpty();
  }

  public void testImmutableCopy() {
    assertThat(union(newHashSet(1, 2), newHashSet(2, 3)).immutableCopy()).containsExactly(1, 2, 3);
    assertThat(intersection(newHashSet(1, 2), newHashSet(2, 3)).immutableCopy()).containsExactly(2);
    assertThat(difference(newHashSet(1, 2), newHashSet(2, 3)).immutableCopy()).containsExactly(1);
    assertThat(symmetricDifference(newHashSet(1, 2), newHashSet(2, 3)).immutableCopy())
        .containsExactly(1, 3);
  }

  public void testCopyInto_returnsSameInstance() {
    Set<Object> set = new HashSet<>();
    assertThat(union(emptySet(), emptySet()).copyInto(set)).isSameInstanceAs(set);
    assertThat(intersection(emptySet(), emptySet()).copyInto(set)).isSameInstanceAs(set);
    assertThat(difference(emptySet(), emptySet()).copyInto(set)).isSameInstanceAs(set);
    assertThat(symmetricDifference(emptySet(), emptySet()).copyInto(set)).isSameInstanceAs(set);
  }

  public void testCopyInto_emptySet() {
    assertThat(union(newHashSet(1, 2), newHashSet(2, 3)).copyInto(new HashSet<>()))
        .containsExactly(1, 2, 3);
    assertThat(intersection(newHashSet(1, 2), newHashSet(2, 3)).copyInto(new HashSet<>()))
        .containsExactly(2);
    assertThat(difference(newHashSet(1, 2), newHashSet(2, 3)).copyInto(new HashSet<>()))
        .containsExactly(1);
    assertThat(symmetricDifference(newHashSet(1, 2), newHashSet(2, 3)).copyInto(new HashSet<>()))
        .containsExactly(1, 3);
  }

  public void testCopyInto_nonEmptySet() {
    assertThat(union(newHashSet(1, 2), newHashSet(2, 3)).copyInto(newHashSet(0, 1)))
        .containsExactly(0, 1, 2, 3);
    assertThat(intersection(newHashSet(1, 2), newHashSet(2, 3)).copyInto(newHashSet(0, 1)))
        .containsExactly(0, 1, 2);
    assertThat(difference(newHashSet(1, 2), newHashSet(2, 3)).copyInto(newHashSet(0, 1)))
        .containsExactly(0, 1);
    assertThat(symmetricDifference(newHashSet(1, 2), newHashSet(2, 3)).copyInto(newHashSet(0, 1)))
        .containsExactly(0, 1, 3);
  }

  public void testUnion_minSize() {
    assertMinSize(union(emptySet(), emptySet()), 0);
    assertMinSize(union(setSize(2), setSize(3)), 3);
    assertMinSize(union(setSize(3), setSize(2)), 3);
    assertMinSize(union(setSizeRange(10, 20), setSizeRange(11, 12)), 11);
    assertMinSize(union(setSizeRange(11, 12), setSizeRange(10, 20)), 11);
  }

  public void testUnion_maxSize() {
    assertMaxSize(union(emptySet(), emptySet()), 0);
    assertMaxSize(union(setSize(2), setSize(3)), 5);
    assertMaxSize(union(setSize(3), setSize(2)), 5);
    assertMaxSize(union(setSizeRange(10, 20), setSizeRange(11, 12)), 32);
    assertMaxSize(union(setSizeRange(11, 12), setSizeRange(10, 20)), 32);
  }

  public void testUnion_maxSize_saturated() {
    assertThat(union(setSize(Integer.MAX_VALUE), setSize(1)).maxSize())
        .isEqualTo(Integer.MAX_VALUE);
    assertThat(union(setSize(1), setSize(Integer.MAX_VALUE)).maxSize())
        .isEqualTo(Integer.MAX_VALUE);
  }

  public void testIntersection_minSize() {
    assertMinSize(intersection(emptySet(), emptySet()), 0);
    assertMinSize(intersection(setSize(2), setSize(3)), 0);
    assertMinSize(intersection(setSize(3), setSize(2)), 0);
    assertMinSize(intersection(setSizeRange(10, 20), setSizeRange(11, 12)), 0);
    assertMinSize(intersection(setSizeRange(11, 12), setSizeRange(10, 20)), 0);
  }

  public void testIntersection_maxSize() {
    assertMaxSize(intersection(emptySet(), emptySet()), 0);
    assertMaxSize(intersection(setSize(2), setSize(3)), 2);
    assertMaxSize(intersection(setSize(3), setSize(2)), 2);
    assertMaxSize(intersection(setSizeRange(10, 20), setSizeRange(11, 12)), 12);
    assertMaxSize(intersection(setSizeRange(11, 12), setSizeRange(10, 20)), 12);
  }

  public void testDifference_minSize() {
    assertMinSize(difference(emptySet(), emptySet()), 0);
    assertMinSize(difference(setSize(2), setSize(3)), 0);
    assertMinSize(difference(setSize(3), setSize(2)), 1);
    assertMinSize(difference(setSizeRange(10, 20), setSizeRange(1, 2)), 8);
    assertMinSize(difference(setSizeRange(1, 2), setSizeRange(10, 20)), 0);
    assertMinSize(difference(setSizeRange(10, 20), setSizeRange(11, 12)), 0);
    assertMinSize(difference(setSizeRange(11, 12), setSizeRange(10, 20)), 0);
  }

  public void testDifference_maxSize() {
    assertMaxSize(difference(emptySet(), emptySet()), 0);
    assertMaxSize(difference(setSize(2), setSize(3)), 2);
    assertMaxSize(difference(setSize(3), setSize(2)), 3);
    assertMaxSize(difference(setSizeRange(10, 20), setSizeRange(1, 2)), 20);
    assertMaxSize(difference(setSizeRange(1, 2), setSizeRange(10, 20)), 2);
    assertMaxSize(difference(setSizeRange(10, 20), setSizeRange(11, 12)), 20);
    assertMaxSize(difference(setSizeRange(11, 12), setSizeRange(10, 20)), 12);
  }

  public void testSymmetricDifference_minSize() {
    assertMinSize(symmetricDifference(emptySet(), emptySet()), 0);
    assertMinSize(symmetricDifference(setSize(2), setSize(3)), 1);
    assertMinSize(symmetricDifference(setSize(3), setSize(2)), 1);
    assertMinSize(symmetricDifference(setSizeRange(10, 20), setSizeRange(1, 2)), 8);
    assertMinSize(symmetricDifference(setSizeRange(1, 2), setSizeRange(10, 20)), 8);
    assertMinSize(symmetricDifference(setSizeRange(10, 20), setSizeRange(11, 12)), 0);
    assertMinSize(symmetricDifference(setSizeRange(11, 12), setSizeRange(10, 20)), 0);
  }

  public void testSymmetricDifference_maxSize() {
    assertMaxSize(symmetricDifference(emptySet(), emptySet()), 0);
    assertMaxSize(symmetricDifference(setSize(2), setSize(3)), 5);
    assertMaxSize(symmetricDifference(setSize(3), setSize(2)), 5);
    assertMaxSize(symmetricDifference(setSizeRange(10, 20), setSizeRange(1, 2)), 22);
    assertMaxSize(symmetricDifference(setSizeRange(1, 2), setSizeRange(10, 20)), 22);
    assertMaxSize(symmetricDifference(setSizeRange(10, 20), setSizeRange(11, 12)), 32);
    assertMaxSize(symmetricDifference(setSizeRange(11, 12), setSizeRange(10, 20)), 32);
  }

  public void testSymmetricDifference_maxSize_saturated() {
    assertThat(symmetricDifference(setSize(Integer.MAX_VALUE), setSize(1)).maxSize())
        .isEqualTo(Integer.MAX_VALUE);
    assertThat(symmetricDifference(setSize(1), setSize(Integer.MAX_VALUE)).maxSize())
        .isEqualTo(Integer.MAX_VALUE);
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            emptySet(),
            union(emptySet(), emptySet()),
            intersection(newHashSet(1, 2), newHashSet(3, 4)),
            difference(newHashSet(1, 2), newHashSet(1, 2)),
            symmetricDifference(newHashSet(1, 2), newHashSet(1, 2)))
        .addEqualityGroup(
            singleton(1),
            union(singleton(1), singleton(1)),
            intersection(newHashSet(1, 2), newHashSet(1, 3)),
            difference(newHashSet(1, 2), newHashSet(2, 3)),
            symmetricDifference(newHashSet(1, 2, 3), newHashSet(2, 3)))
        .addEqualityGroup(
            singleton(2),
            union(singleton(2), singleton(2)),
            intersection(newHashSet(1, 2), newHashSet(2, 3)),
            difference(newHashSet(1, 2), newHashSet(1, 3)),
            symmetricDifference(newHashSet(1, 2, 3), newHashSet(1, 3)))
        .addEqualityGroup(
            newHashSet(1, 2),
            union(singleton(1), singleton(2)),
            intersection(newHashSet(1, 2), newHashSet(1, 2, 3)),
            difference(newHashSet(1, 2, 3), newHashSet(3)),
            symmetricDifference(newHashSet(1, 3), newHashSet(2, 3)))
        .addEqualityGroup(
            newHashSet(3, 2),
            union(singleton(3), singleton(2)),
            intersection(newHashSet(3, 2), newHashSet(3, 2, 1)),
            difference(newHashSet(3, 2, 1), newHashSet(1)),
            symmetricDifference(newHashSet(3, 1), newHashSet(2, 1)))
        .addEqualityGroup(
            newHashSet(1, 2, 3),
            union(newHashSet(1, 2), newHashSet(2, 3)),
            intersection(newHashSet(1, 2, 3), newHashSet(1, 2, 3)),
            difference(newHashSet(1, 2, 3), emptySet()),
            symmetricDifference(emptySet(), newHashSet(1, 2, 3)))
        .testEquals();
  }

  public void testEquals_otherSetContainsThrows() {
    new EqualsTester()
        .addEqualityGroup(new SetContainsThrows())
        .addEqualityGroup(intersection(singleton(null), singleton(null))) // NPE
        .addEqualityGroup(intersection(singleton(0), singleton(0))) // CCE
        .testEquals();
  }

  /** Returns a {@link Set} with a {@link Set#size()} of {@code size}. */
  private static ContiguousSet<Integer> setSize(int size) {
    checkArgument(size >= 0);
    ContiguousSet<Integer> set = ContiguousSet.closedOpen(0, size);
    checkState(set.size() == size);
    return set;
  }

  /**
   * Returns a {@link SetView} with a {@link SetView#minSize()} of {@code min} and a {@link
   * SetView#maxSize()} of {@code max}.
   */
  private static SetView<Integer> setSizeRange(int min, int max) {
    checkArgument(min >= 0 && max >= min);
    SetView<Integer> set = difference(setSize(max), setSize(max - min));
    checkState(set.minSize() == min && set.maxSize() == max);
    return set;
  }

  /**
   * Asserts that {@code code} has a {@link SetView#minSize()} of {@code min} and a {@link
   * Set#size()} of at least {@code min}.
   */
  private static void assertMinSize(SetView<?> set, int min) {
    assertThat(set.minSize()).isEqualTo(min);
    assertThat(set.size()).isAtLeast(min);
  }

  /**
   * Asserts that {@code code} has a {@link SetView#maxSize()} of {@code max} and a {@link
   * Set#size()} of at most {@code max}.
   */
  private static void assertMaxSize(SetView<?> set, int max) {
    assertThat(set.maxSize()).isEqualTo(max);
    assertThat(set.size()).isAtMost(max);
  }

  /**
   * A {@link Set} that throws {@link NullPointerException} and {@link ClassCastException} from
   * {@link #contains}.
   */
  private static final class SetContainsThrows extends AbstractSet<Void> {
    @Override
    public boolean contains(@Nullable Object o) {
      throw o == null ? new NullPointerException() : new ClassCastException();
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public Iterator<Void> iterator() {
      return emptyIterator();
    }
  }
}
