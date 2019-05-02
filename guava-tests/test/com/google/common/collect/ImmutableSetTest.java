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
import static com.google.common.truth.Truth.assertWithMessage;

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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
    try {
      ImmutableSet.chooseTableSize(1 << 30);
      fail();
    } catch (IllegalArgumentException expected) {
    }
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
      public boolean equals(Object obj) {
        return obj instanceof TypeWithDuplicates && ((TypeWithDuplicates) obj).a == a;
      }

      public boolean fullEquals(TypeWithDuplicates other) {
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

  @GwtIncompatible // GWT is single threaded
  public void testCopyOf_threadSafe() {
    verifyThreadSafe();
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
   * A Comparable wrapper around a String which executes callbacks on calls to hashCode, equals, and
   * compareTo.
   */
  private static class CountsHashCodeAndEquals implements Comparable<CountsHashCodeAndEquals> {
    private final String delegateString;
    private final Runnable onHashCode;
    private final Runnable onEquals;
    private final Runnable onCompareTo;

    CountsHashCodeAndEquals(
        String delegateString, Runnable onHashCode, Runnable onEquals, Runnable onCompareTo) {
      this.delegateString = delegateString;
      this.onHashCode = onHashCode;
      this.onEquals = onEquals;
      this.onCompareTo = onCompareTo;
    }

    @Override
    public int hashCode() {
      onHashCode.run();
      return delegateString.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
      onEquals.run();
      return other instanceof CountsHashCodeAndEquals
          && delegateString.equals(((CountsHashCodeAndEquals) other).delegateString);
    }

    @Override
    public int compareTo(CountsHashCodeAndEquals o) {
      onCompareTo.run();
      return delegateString.compareTo(o.delegateString);
    }
  }

  /** A holder of counters for calls to hashCode, equals, and compareTo. */
  private static final class CallsCounter {
    long hashCode;
    long equals;
    long compareTo;

    long total() {
      return hashCode + equals + compareTo;
    }

    void zero() {
      hashCode = 0;
      equals = 0;
      compareTo = 0;
    }
  }

  /** All the ways to construct an ImmutableSet. */
  enum ConstructionPathway {
    OF {
      @Override
      ImmutableSet<?> create(List<?> list) {
        Object o1 = list.get(0);
        Object o2 = list.get(1);
        Object o3 = list.get(2);
        Object o4 = list.get(3);
        Object o5 = list.get(4);
        Object o6 = list.get(5);
        Object[] rest = list.subList(6, list.size()).toArray();
        return ImmutableSet.of(o1, o2, o3, o4, o5, o6, rest);
      }
    },
    COPY_OF_ARRAY {
      @Override
      ImmutableSet<?> create(List<?> list) {
        return ImmutableSet.copyOf(list.toArray());
      }
    },
    COPY_OF_LIST {
      @Override
      ImmutableSet<?> create(List<?> list) {
        return ImmutableSet.copyOf(list);
      }
    },
    BUILDER_ADD_ONE_BY_ONE {
      @Override
      ImmutableSet<?> create(List<?> list) {
        ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
        for (Object o : list) {
          builder.add(o);
        }
        return builder.build();
      }
    },
    BUILDER_ADD_ARRAY {
      @Override
      ImmutableSet<?> create(List<?> list) {
        ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
        builder.add(list.toArray());
        return builder.build();
      }
    },
    BUILDER_ADD_LIST {
      @Override
      ImmutableSet<?> create(List<?> list) {
        ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
        builder.addAll(list);
        return builder.build();
      }
    };

    @CanIgnoreReturnValue
    abstract ImmutableSet<?> create(List<?> list);
  }

  /**
   * Returns a list of objects with the same hash code, of size 2^power, counting calls to equals,
   * hashCode, and compareTo in counter.
   */
  static List<CountsHashCodeAndEquals> createAdversarialInput(int power, CallsCounter counter) {
    String str1 = "Aa";
    String str2 = "BB";
    assertEquals(str1.hashCode(), str2.hashCode());
    List<String> haveSameHashes2 = Arrays.asList(str1, str2);
    List<CountsHashCodeAndEquals> result =
        Lists.newArrayList(
            Lists.transform(
                Lists.cartesianProduct(Collections.nCopies(power, haveSameHashes2)),
                strs ->
                    new CountsHashCodeAndEquals(
                        String.join("", strs),
                        () -> counter.hashCode++,
                        () -> counter.equals++,
                        () -> counter.compareTo++)));
    assertEquals(
        result.get(0).delegateString.hashCode(),
        result.get(result.size() - 1).delegateString.hashCode());
    return result;
  }

  @GwtIncompatible
  public void testResistsHashFloodingInConstruction() {
    CallsCounter smallCounter = new CallsCounter();
    List<CountsHashCodeAndEquals> haveSameHashesSmall = createAdversarialInput(10, smallCounter);
    int smallSize = haveSameHashesSmall.size();

    CallsCounter largeCounter = new CallsCounter();
    List<CountsHashCodeAndEquals> haveSameHashesLarge = createAdversarialInput(15, largeCounter);
    int largeSize = haveSameHashesLarge.size();

    for (ConstructionPathway pathway : ConstructionPathway.values()) {
      smallCounter.zero();
      pathway.create(haveSameHashesSmall);

      largeCounter.zero();
      pathway.create(haveSameHashesLarge);

      double ratio = (double) largeCounter.total() / smallCounter.total();

      assertWithMessage(
              "ratio of equals/hashCode/compareTo operations to build an ImmutableSet via pathway "
                  + "%s of size %s versus size %s",
              pathway, haveSameHashesLarge.size(), haveSameHashesSmall.size())
          .that(ratio)
          .isAtMost(2.0 * (largeSize * Math.log(largeSize)) / (smallSize * Math.log(smallSize)));
      // We allow up to 2x wobble in the constant factors.
    }
  }

  @GwtIncompatible
  public void testResistsHashFloodingOnContains() {
    CallsCounter smallCounter = new CallsCounter();
    List<CountsHashCodeAndEquals> haveSameHashesSmall = createAdversarialInput(10, smallCounter);
    ImmutableSet<?> smallSet = ConstructionPathway.COPY_OF_LIST.create(haveSameHashesSmall);
    long worstCaseOpsSmall = worstCaseQueryOperations(smallSet, smallCounter);

    CallsCounter largeCounter = new CallsCounter();
    List<CountsHashCodeAndEquals> haveSameHashesLarge = createAdversarialInput(15, largeCounter);
    ImmutableSet<?> largeSet = ConstructionPathway.COPY_OF_LIST.create(haveSameHashesLarge);
    long worstCaseOpsLarge = worstCaseQueryOperations(largeSet, largeCounter);

    double ratio = (double) worstCaseOpsLarge / worstCaseOpsSmall;
    int smallSize = haveSameHashesSmall.size();
    int largeSize = haveSameHashesLarge.size();

    assertWithMessage(
            "ratio of equals/hashCode/compareTo operations to worst-case query an ImmutableSet "
                + "of size %s versus size %s",
            haveSameHashesLarge.size(), haveSameHashesSmall.size())
        .that(ratio)
        .isAtMost(2 * Math.log(largeSize) / Math.log(smallSize));
    // We allow up to 2x wobble in the constant factors.
  }

  private static long worstCaseQueryOperations(Set<?> set, CallsCounter counter) {
    long worstCalls = 0;
    for (Object k : set) {
      counter.zero();
      if (set.contains(k)) {
        worstCalls = Math.max(worstCalls, counter.total());
      }
    }
    return worstCalls;
  }
}
