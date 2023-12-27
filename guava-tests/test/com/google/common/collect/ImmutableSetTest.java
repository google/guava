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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.SetGenerators.DegeneratedImmutableSetGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSetAsListGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSetCopyOfGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSetSizedBuilderGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSetTooBigBuilderGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSetTooSmallBuilderGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSetUnsizedBuilderGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSetWithBadHashesGenerator;
import com.google.common.testing.CollectorTester;
import com.google.common.testing.EqualsTester;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link ImmutableSet}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Nick Kralevich
 */
@GwtCompatible(emulated = true)
public class ImmutableSetTest extends AbstractImmutableSetTest {

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(
        SetTestSuiteBuilder.using(new ImmutableSetCopyOfGenerator())
            .named(ImmutableSetTest.class.getName())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(new ImmutableSetUnsizedBuilderGenerator())
            .named(ImmutableSetTest.class.getName() + ", with unsized builder")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                    builder.forceJdk();
                    builder.add(elements);
                    return builder.build();
                  }
                })
            .named(ImmutableSetTest.class.getName() + ", with JDK builder")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(new ImmutableSetSizedBuilderGenerator())
            .named(ImmutableSetTest.class.getName() + ", with exactly sized builder")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(new ImmutableSetTooBigBuilderGenerator())
            .named(ImmutableSetTest.class.getName() + ", with oversized builder")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(new ImmutableSetTooSmallBuilderGenerator())
            .named(ImmutableSetTest.class.getName() + ", with undersized builder")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(new ImmutableSetWithBadHashesGenerator())
            .named(ImmutableSetTest.class.getName() + ", with bad hashes")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(new DegeneratedImmutableSetGenerator())
            .named(ImmutableSetTest.class.getName() + ", degenerate")
            .withFeatures(
                CollectionSize.ONE,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableSetAsListGenerator())
            .named("ImmutableSet.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTestSuite(ImmutableSetTest.class);

    return suite;
  }

  @Override
  protected <E extends Comparable<? super E>> Set<E> of() {
    return ImmutableSet.of();
  }

  @Override
  protected <E extends Comparable<? super E>> Set<E> of(E e) {
    return ImmutableSet.of(e);
  }

  @Override
  protected <E extends Comparable<? super E>> Set<E> of(E e1, E e2) {
    return ImmutableSet.of(e1, e2);
  }

  @Override
  protected <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3) {
    return ImmutableSet.of(e1, e2, e3);
  }

  @Override
  protected <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3, E e4) {
    return ImmutableSet.of(e1, e2, e3, e4);
  }

  @Override
  protected <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
    return ImmutableSet.of(e1, e2, e3, e4, e5);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected <E extends Comparable<? super E>> Set<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E... rest) {
    return ImmutableSet.of(e1, e2, e3, e4, e5, e6, rest);
  }

  @Override
  protected <E extends Comparable<? super E>> Set<E> copyOf(E[] elements) {
    return ImmutableSet.copyOf(elements);
  }

  @Override
  protected <E extends Comparable<? super E>> Set<E> copyOf(Collection<? extends E> elements) {
    return ImmutableSet.copyOf(elements);
  }

  @Override
  protected <E extends Comparable<? super E>> Set<E> copyOf(Iterable<? extends E> elements) {
    return ImmutableSet.copyOf(elements);
  }

  @Override
  protected <E extends Comparable<? super E>> Set<E> copyOf(Iterator<? extends E> elements) {
    return ImmutableSet.copyOf(elements);
  }

  public void testCreation_allDuplicates() {
    ImmutableSet<String> set = ImmutableSet.copyOf(Lists.newArrayList("a", "a"));
    assertTrue(set instanceof SingletonImmutableSet);
    assertEquals(Lists.newArrayList("a"), Lists.newArrayList(set));
  }

  public void testCreation_oneDuplicate() {
    // now we'll get the varargs overload
    ImmutableSet<String> set =
        ImmutableSet.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "a");
    assertEquals(
        Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m"),
        Lists.newArrayList(set));
  }

  public void testCreation_manyDuplicates() {
    // now we'll get the varargs overload
    ImmutableSet<String> set =
        ImmutableSet.of("a", "b", "c", "c", "c", "c", "b", "b", "a", "a", "c", "c", "c", "a");
    assertThat(set).containsExactly("a", "b", "c").inOrder();
  }

  public void testCreation_arrayOfArray() {
    String[] array = new String[] {"a"};
    Set<String[]> set = ImmutableSet.<String[]>of(array);
    assertEquals(Collections.singleton(array), set);
  }

  @GwtIncompatible // ImmutableSet.chooseTableSize
  public void testChooseTableSize() {
    assertEquals(8, ImmutableSet.chooseTableSize(3));
    assertEquals(8, ImmutableSet.chooseTableSize(4));

    assertEquals(1 << 29, ImmutableSet.chooseTableSize(1 << 28));
    assertEquals(1 << 29, ImmutableSet.chooseTableSize((1 << 29) * 3 / 5));

    // Now we hit the cap
    assertEquals(1 << 30, ImmutableSet.chooseTableSize(1 << 29));
    assertEquals(1 << 30, ImmutableSet.chooseTableSize((1 << 30) - 1));

    // Now we've gone too far
    assertThrows(IllegalArgumentException.class, () -> ImmutableSet.chooseTableSize(1 << 30));
  }

  @GwtIncompatible // RegularImmutableSet.table not in emulation
  public void testResizeTable() {
    verifyTableSize(100, 2, 8);
    verifyTableSize(100, 5, 8);
    verifyTableSize(100, 33, 64);
    verifyTableSize(17, 17, 32);
    verifyTableSize(17, 16, 32);
    verifyTableSize(17, 15, 32);
  }

  @GwtIncompatible // RegularImmutableSet.table not in emulation
  private void verifyTableSize(int inputSize, int setSize, int tableSize) {
    Builder<Integer> builder = ImmutableSet.builder();
    for (int i = 0; i < inputSize; i++) {
      builder.add(i % setSize);
    }
    ImmutableSet<Integer> set = builder.build();
    assertTrue(set instanceof RegularImmutableSet);
    assertEquals(
        "Input size " + inputSize + " and set size " + setSize,
        tableSize,
        ((RegularImmutableSet<Integer>) set).table.length);
  }

  public void testCopyOf_copiesImmutableSortedSet() {
    ImmutableSortedSet<String> sortedSet = ImmutableSortedSet.of("a");
    ImmutableSet<String> copy = ImmutableSet.copyOf(sortedSet);
    assertNotSame(sortedSet, copy);
  }

  public void testToImmutableSet() {
    Collector<String, ?, ImmutableSet<String>> collector = ImmutableSet.toImmutableSet();
    Equivalence<ImmutableSet<String>> equivalence =
        Equivalence.equals().onResultOf(ImmutableSet::asList);
    CollectorTester.of(collector, equivalence)
        .expectCollects(ImmutableSet.of("a", "b", "c", "d"), "a", "b", "a", "c", "b", "b", "d");
  }

  public void testToImmutableSet_duplicates() {
    class TypeWithDuplicates {
      final int a;
      final int b;

      TypeWithDuplicates(int a, int b) {
        this.a = a;
        this.b = b;
      }

      @Override
      public int hashCode() {
        return a;
      }

      @Override
      public boolean equals(@Nullable Object obj) {
        return obj instanceof TypeWithDuplicates && ((TypeWithDuplicates) obj).a == a;
      }

      public boolean fullEquals(@Nullable TypeWithDuplicates other) {
        return other != null && a == other.a && b == other.b;
      }
    }

    Collector<TypeWithDuplicates, ?, ImmutableSet<TypeWithDuplicates>> collector =
        ImmutableSet.toImmutableSet();
    BiPredicate<ImmutableSet<TypeWithDuplicates>, ImmutableSet<TypeWithDuplicates>> equivalence =
        (set1, set2) -> {
          if (!set1.equals(set2)) {
            return false;
          }
          for (int i = 0; i < set1.size(); i++) {
            if (!set1.asList().get(i).fullEquals(set2.asList().get(i))) {
              return false;
            }
          }
          return true;
        };
    TypeWithDuplicates a = new TypeWithDuplicates(1, 1);
    TypeWithDuplicates b1 = new TypeWithDuplicates(2, 1);
    TypeWithDuplicates b2 = new TypeWithDuplicates(2, 2);
    TypeWithDuplicates c = new TypeWithDuplicates(3, 1);
    CollectorTester.of(collector, equivalence)
        .expectCollects(ImmutableSet.of(a, b1, c), a, b1, c, b2);
  }

  @Override
  <E extends Comparable<E>> Builder<E> builder() {
    return ImmutableSet.builder();
  }

  @Override
  int getComplexBuilderSetLastElement() {
    return LAST_COLOR_ADDED;
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(ImmutableSet.of(), ImmutableSet.of())
        .addEqualityGroup(ImmutableSet.of(1), ImmutableSet.of(1), ImmutableSet.of(1, 1))
        .addEqualityGroup(ImmutableSet.of(1, 2, 1), ImmutableSet.of(2, 1, 1))
        .testEquals();
  }

  /**
   * The maximum allowed probability of falsely detecting a hash flooding attack if the input is
   * randomly generated.
   */
  private static final double HASH_FLOODING_FPP = 0.001;

  public void testReuseBuilderReducingHashTableSizeWithPowerOfTwoTotalElements() {
    ImmutableSet.Builder<Object> builder = ImmutableSet.builderWithExpectedSize(6);
    builder.add(0);
    ImmutableSet<Object> unused = builder.build();
    ImmutableSet<Object> subject = builder.add(1).add(2).add(3).build();
    assertFalse(subject.contains(4));
  }
}
