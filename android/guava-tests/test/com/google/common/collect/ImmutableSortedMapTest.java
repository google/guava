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

import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.singletonMap;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableSortedMap.Builder;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.NavigableMapTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.SortedMapGenerators.ImmutableSortedMapCopyOfEntriesGenerator;
import com.google.common.collect.testing.google.SortedMapGenerators.ImmutableSortedMapEntryListGenerator;
import com.google.common.collect.testing.google.SortedMapGenerators.ImmutableSortedMapGenerator;
import com.google.common.collect.testing.google.SortedMapGenerators.ImmutableSortedMapKeyListGenerator;
import com.google.common.collect.testing.google.SortedMapGenerators.ImmutableSortedMapValueListGenerator;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@link ImmutableSortedMap}.
 *
 * @author Kevin Bourrillion
 * @author Jesse Wilson
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("AlwaysThrows")
@ElementTypesAreNonnullByDefault
public class ImmutableSortedMapTest extends TestCase {
  // TODO: Avoid duplicating code in ImmutableMapTest

  @J2ktIncompatible
  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ImmutableSortedMapTest.class);

    suite.addTest(
        NavigableMapTestSuiteBuilder.using(new ImmutableSortedMapGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .named("ImmutableSortedMap")
            .createTestSuite());
    suite.addTest(
        MapTestSuiteBuilder.using(new ImmutableSortedMapCopyOfEntriesGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .named("ImmutableSortedMap.copyOf[Iterable<Entry>]")
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableSortedMapEntryListGenerator())
            .named("ImmutableSortedMap.entrySet.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableSortedMapKeyListGenerator())
            .named("ImmutableSortedMap.keySet.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableSortedMapValueListGenerator())
            .named("ImmutableSortedMap.values.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    return suite;
  }

  // Creation tests

  public void testEmptyBuilder() {
    ImmutableSortedMap<String, Integer> map =
        ImmutableSortedMap.<String, Integer>naturalOrder().build();
    assertEquals(Collections.<String, Integer>emptyMap(), map);
  }

  public void testSingletonBuilder() {
    ImmutableSortedMap<String, Integer> map =
        ImmutableSortedMap.<String, Integer>naturalOrder().put("one", 1).build();
    assertMapEquals(map, "one", 1);
  }

  public void testBuilder() {
    ImmutableSortedMap<String, Integer> map =
        ImmutableSortedMap.<String, Integer>naturalOrder()
            .put("one", 1)
            .put("two", 2)
            .put("three", 3)
            .put("four", 4)
            .put("five", 5)
            .build();
    assertMapEquals(map, "five", 5, "four", 4, "one", 1, "three", 3, "two", 2);
  }

  @SuppressWarnings("DoNotCall")
  public void testBuilder_orderEntriesByValueFails() {
    ImmutableSortedMap.Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
    assertThrows(
        UnsupportedOperationException.class, () -> builder.orderEntriesByValue(Ordering.natural()));
  }

  public void testBuilder_withImmutableEntry() {
    ImmutableSortedMap<String, Integer> map =
        ImmutableSortedMap.<String, Integer>naturalOrder().put(immutableEntry("one", 1)).build();
    assertMapEquals(map, "one", 1);
  }

  public void testBuilder_withImmutableEntryAndNullContents() {
    Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
    assertThrows(
        NullPointerException.class, () -> builder.put(immutableEntry("one", (Integer) null)));
    assertThrows(NullPointerException.class, () -> builder.put(immutableEntry((String) null, 1)));
  }

  private static class StringHolder {
    @Nullable String string;
  }

  public void testBuilder_withMutableEntry() {
    ImmutableSortedMap.Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
    final StringHolder holder = new StringHolder();
    holder.string = "one";
    Entry<String, Integer> entry =
        new AbstractMapEntry<String, Integer>() {
          @Override
          public String getKey() {
            return holder.string;
          }

          @Override
          public Integer getValue() {
            return 1;
          }
        };

    builder.put(entry);
    holder.string = "two";
    assertMapEquals(builder.build(), "one", 1);
  }

  public void testBuilderPutAllWithEmptyMap() {
    ImmutableSortedMap<String, Integer> map =
        ImmutableSortedMap.<String, Integer>naturalOrder()
            .putAll(Collections.<String, Integer>emptyMap())
            .build();
    assertEquals(Collections.<String, Integer>emptyMap(), map);
  }

  public void testBuilderPutAll() {
    Map<String, Integer> toPut = new LinkedHashMap<>();
    toPut.put("one", 1);
    toPut.put("two", 2);
    toPut.put("three", 3);
    Map<String, Integer> moreToPut = new LinkedHashMap<>();
    moreToPut.put("four", 4);
    moreToPut.put("five", 5);

    ImmutableSortedMap<String, Integer> map =
        ImmutableSortedMap.<String, Integer>naturalOrder().putAll(toPut).putAll(moreToPut).build();
    assertMapEquals(map, "five", 5, "four", 4, "one", 1, "three", 3, "two", 2);
  }

  public void testBuilderReuse() {
    Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
    ImmutableSortedMap<String, Integer> mapOne = builder.put("one", 1).put("two", 2).build();
    ImmutableSortedMap<String, Integer> mapTwo = builder.put("three", 3).put("four", 4).build();

    assertMapEquals(mapOne, "one", 1, "two", 2);
    assertMapEquals(mapTwo, "four", 4, "one", 1, "three", 3, "two", 2);
  }

  public void testBuilderPutNullKey() {
    Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
    assertThrows(NullPointerException.class, () -> builder.put(null, 1));
  }

  public void testBuilderPutNullValue() {
    Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
    assertThrows(NullPointerException.class, () -> builder.put("one", null));
  }

  public void testBuilderPutNullKeyViaPutAll() {
    Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
    assertThrows(
        NullPointerException.class,
        () -> builder.putAll(Collections.<String, Integer>singletonMap(null, 1)));
  }

  public void testBuilderPutNullValueViaPutAll() {
    Builder<String, Integer> builder = ImmutableSortedMap.naturalOrder();
    assertThrows(
        NullPointerException.class,
        () -> builder.putAll(Collections.<String, Integer>singletonMap("one", null)));
  }

  public void testPuttingTheSameKeyTwiceThrowsOnBuild() {
    Builder<String, Integer> builder =
        ImmutableSortedMap.<String, Integer>naturalOrder()
            .put("one", 1)
            .put("one", 2); // throwing on this line would be even better

    assertThrows(IllegalArgumentException.class, () -> builder.build());
  }

  public void testOf() {
    assertMapEquals(ImmutableSortedMap.of("one", 1), "one", 1);
    assertMapEquals(ImmutableSortedMap.of("one", 1, "two", 2), "one", 1, "two", 2);
    assertMapEquals(
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3), "one", 1, "three", 3, "two", 2);
    assertMapEquals(
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3, "four", 4),
        "four",
        4,
        "one",
        1,
        "three",
        3,
        "two",
        2);
    assertMapEquals(
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5),
        "five",
        5,
        "four",
        4,
        "one",
        1,
        "three",
        3,
        "two",
        2);
    assertMapEquals(
        ImmutableSortedMap.of(
            "one", 1,
            "two", 2,
            "three", 3,
            "four", 4,
            "five", 5,
            "six", 6),
        "five",
        5,
        "four",
        4,
        "one",
        1,
        "six",
        6,
        "three",
        3,
        "two",
        2);
    assertMapEquals(
        ImmutableSortedMap.of(
            "one", 1,
            "two", 2,
            "three", 3,
            "four", 4,
            "five", 5,
            "six", 6,
            "seven", 7),
        "five",
        5,
        "four",
        4,
        "one",
        1,
        "seven",
        7,
        "six",
        6,
        "three",
        3,
        "two",
        2);
    assertMapEquals(
        ImmutableSortedMap.of(
            "one", 1,
            "two", 2,
            "three", 3,
            "four", 4,
            "five", 5,
            "six", 6,
            "seven", 7,
            "eight", 8),
        "eight",
        8,
        "five",
        5,
        "four",
        4,
        "one",
        1,
        "seven",
        7,
        "six",
        6,
        "three",
        3,
        "two",
        2);
    assertMapEquals(
        ImmutableSortedMap.of(
            "one", 1,
            "two", 2,
            "three", 3,
            "four", 4,
            "five", 5,
            "six", 6,
            "seven", 7,
            "eight", 8,
            "nine", 9),
        "eight",
        8,
        "five",
        5,
        "four",
        4,
        "nine",
        9,
        "one",
        1,
        "seven",
        7,
        "six",
        6,
        "three",
        3,
        "two",
        2);
    assertMapEquals(
        ImmutableSortedMap.of(
            "one", 1,
            "two", 2,
            "three", 3,
            "four", 4,
            "five", 5,
            "six", 6,
            "seven", 7,
            "eight", 8,
            "nine", 9,
            "ten", 10),
        "eight",
        8,
        "five",
        5,
        "four",
        4,
        "nine",
        9,
        "one",
        1,
        "seven",
        7,
        "six",
        6,
        "ten",
        10,
        "three",
        3,
        "two",
        2);
  }

  public void testOfNullKey() {
    Integer n = null;
    assertThrows(NullPointerException.class, () -> ImmutableSortedMap.of(n, 1));

    assertThrows(NullPointerException.class, () -> ImmutableSortedMap.of("one", 1, null, 2));
  }

  public void testOfNullValue() {
    assertThrows(NullPointerException.class, () -> ImmutableSortedMap.of("one", null));

    assertThrows(NullPointerException.class, () -> ImmutableSortedMap.of("one", 1, "two", null));
  }

  @SuppressWarnings("DistinctVarargsChecker")
  public void testOfWithDuplicateKey() {
    assertThrows(IllegalArgumentException.class, () -> ImmutableSortedMap.of("one", 1, "one", 1));
  }

  public void testCopyOfEmptyMap() {
    ImmutableSortedMap<String, Integer> copy =
        ImmutableSortedMap.copyOf(Collections.<String, Integer>emptyMap());
    assertEquals(Collections.<String, Integer>emptyMap(), copy);
    assertSame(copy, ImmutableSortedMap.copyOf(copy));
    assertSame(Ordering.natural(), copy.comparator());
  }

  public void testCopyOfSingletonMap() {
    ImmutableSortedMap<String, Integer> copy = ImmutableSortedMap.copyOf(singletonMap("one", 1));
    assertMapEquals(copy, "one", 1);
    assertSame(copy, ImmutableSortedMap.copyOf(copy));
    assertSame(Ordering.natural(), copy.comparator());
  }

  public void testCopyOf() {
    Map<String, Integer> original = new LinkedHashMap<>();
    original.put("one", 1);
    original.put("two", 2);
    original.put("three", 3);

    ImmutableSortedMap<String, Integer> copy = ImmutableSortedMap.copyOf(original);
    assertMapEquals(copy, "one", 1, "three", 3, "two", 2);
    assertSame(copy, ImmutableSortedMap.copyOf(copy));
    assertSame(Ordering.natural(), copy.comparator());
  }

  public void testCopyOfExplicitComparator() {
    Comparator<String> comparator = Ordering.<String>natural().reverse();
    Map<String, Integer> original = new LinkedHashMap<>();
    original.put("one", 1);
    original.put("two", 2);
    original.put("three", 3);

    ImmutableSortedMap<String, Integer> copy = ImmutableSortedMap.copyOf(original, comparator);
    assertMapEquals(copy, "two", 2, "three", 3, "one", 1);
    assertSame(copy, ImmutableSortedMap.copyOf(copy, comparator));
    assertSame(comparator, copy.comparator());
  }

  public void testCopyOfImmutableSortedSetDifferentComparator() {
    Comparator<String> comparator = Ordering.<String>natural().reverse();
    Map<String, Integer> original = ImmutableSortedMap.of("one", 1, "two", 2, "three", 3);
    ImmutableSortedMap<String, Integer> copy = ImmutableSortedMap.copyOf(original, comparator);
    assertMapEquals(copy, "two", 2, "three", 3, "one", 1);
    assertSame(copy, ImmutableSortedMap.copyOf(copy, comparator));
    assertSame(comparator, copy.comparator());
  }

  public void testCopyOfSortedNatural() {
    SortedMap<String, Integer> original = Maps.newTreeMap();
    original.put("one", 1);
    original.put("two", 2);
    original.put("three", 3);

    ImmutableSortedMap<String, Integer> copy = ImmutableSortedMap.copyOfSorted(original);
    assertMapEquals(copy, "one", 1, "three", 3, "two", 2);
    assertSame(copy, ImmutableSortedMap.copyOfSorted(copy));
    assertSame(Ordering.natural(), copy.comparator());
  }

  public void testCopyOfSortedExplicit() {
    Comparator<String> comparator = Ordering.<String>natural().reverse();
    SortedMap<String, Integer> original = Maps.newTreeMap(comparator);
    original.put("one", 1);
    original.put("two", 2);
    original.put("three", 3);

    ImmutableSortedMap<String, Integer> copy = ImmutableSortedMap.copyOfSorted(original);
    assertMapEquals(copy, "two", 2, "three", 3, "one", 1);
    assertSame(copy, ImmutableSortedMap.copyOfSorted(copy));
    assertSame(comparator, copy.comparator());
  }

  private static class IntegerDiv10 implements Comparable<IntegerDiv10> {
    final int value;

    IntegerDiv10(int value) {
      this.value = value;
    }

    @Override
    public int compareTo(IntegerDiv10 o) {
      return value / 10 - o.value / 10;
    }

    @Override
    public String toString() {
      return Integer.toString(value);
    }
  }

  public void testCopyOfDuplicateKey() {
    Map<IntegerDiv10, String> original =
        ImmutableMap.of(
            new IntegerDiv10(3), "three",
            new IntegerDiv10(20), "twenty",
            new IntegerDiv10(11), "eleven",
            new IntegerDiv10(35), "thirty five",
            new IntegerDiv10(12), "twelve");

    assertThrows(IllegalArgumentException.class, () -> ImmutableSortedMap.copyOf(original));
  }

  public void testImmutableMapCopyOfImmutableSortedMap() {
    IntegerDiv10 three = new IntegerDiv10(3);
    IntegerDiv10 eleven = new IntegerDiv10(11);
    IntegerDiv10 twelve = new IntegerDiv10(12);
    IntegerDiv10 twenty = new IntegerDiv10(20);
    Map<IntegerDiv10, String> original =
        ImmutableSortedMap.of(three, "three", eleven, "eleven", twenty, "twenty");
    Map<IntegerDiv10, String> copy = ImmutableMap.copyOf(original);
    assertTrue(original.containsKey(twelve));
    assertFalse(copy.containsKey(twelve));
  }

  public void testBuilderReverseOrder() {
    ImmutableSortedMap<String, Integer> map =
        ImmutableSortedMap.<String, Integer>reverseOrder()
            .put("one", 1)
            .put("two", 2)
            .put("three", 3)
            .put("four", 4)
            .put("five", 5)
            .build();
    assertMapEquals(map, "two", 2, "three", 3, "one", 1, "four", 4, "five", 5);
    assertEquals(Ordering.<String>natural().reverse(), map.comparator());
  }

  public void testBuilderComparator() {
    Comparator<String> comparator = Ordering.<String>natural().reverse();
    ImmutableSortedMap<String, Integer> map =
        new ImmutableSortedMap.Builder<String, Integer>(comparator)
            .put("one", 1)
            .put("two", 2)
            .put("three", 3)
            .put("four", 4)
            .put("five", 5)
            .build();
    assertMapEquals(map, "two", 2, "three", 3, "one", 1, "four", 4, "five", 5);
    assertSame(comparator, map.comparator());
  }

  // TODO(b/172823566): Use mainline testToImmutableSortedMap once CollectorTester is usable.
  public void testToImmutableSortedMap_java7_combine() {
    ImmutableSortedMap.Builder<String, Integer> zis =
        ImmutableSortedMap.<String, Integer>naturalOrder().put("one", 1).put("four", 4);
    ImmutableSortedMap.Builder<String, Integer> zat =
        ImmutableSortedMap.<String, Integer>naturalOrder().put("two", 2).put("three", 3);
    ImmutableSortedMap<String, Integer> sortedMap = zis.combine(zat).build();
    assertMapEquals(sortedMap, "four", 4, "one", 1, "three", 3, "two", 2);
  }

  // TODO(b/172823566): Use mainline testToImmutableSortedMap once CollectorTester is usable.
  public void testToImmutableSortedMap_exceptionOnDuplicateKey_java7_combine() {
    ImmutableSortedMap.Builder<String, Integer> zis =
        ImmutableSortedMap.<String, Integer>naturalOrder().put("one", 1).put("two", 2);
    ImmutableSortedMap.Builder<String, Integer> zat =
        ImmutableSortedMap.<String, Integer>naturalOrder().put("two", 22).put("three", 3);
    ImmutableSortedMap.Builder<String, Integer> combined = zis.combine(zat);
    assertThrows(IllegalArgumentException.class, () -> combined.build());
  }

  // Other tests

  public void testNullGet() {
    ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("one", 1);
    assertNull(map.get(null));
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(ImmutableSortedMap.class);
    tester.testAllPublicInstanceMethods(ImmutableSortedMap.<String, Integer>naturalOrder());
    tester.testAllPublicInstanceMethods(ImmutableSortedMap.of());
    tester.testAllPublicInstanceMethods(ImmutableSortedMap.of("one", 1));
    tester.testAllPublicInstanceMethods(ImmutableSortedMap.of("one", 1, "two", 2, "three", 3));
  }

  public void testNullValuesInCopyOfMap() {
    for (int i = 1; i <= 10; i++) {
      for (int j = 0; j < i; j++) {
        Map<Integer, @Nullable Integer> source = new TreeMap<>();
        for (int k = 0; k < i; k++) {
          source.put(k, k);
        }
        source.put(j, null);
        assertThrows(
            NullPointerException.class,
            () -> ImmutableSortedMap.copyOf((Map<Integer, Integer>) source));
      }
    }
  }

  public void testNullValuesInCopyOfEntries() {
    for (int i = 1; i <= 10; i++) {
      for (int j = 0; j < i; j++) {
        Map<Integer, @Nullable Integer> source = new TreeMap<>();
        for (int k = 0; k < i; k++) {
          source.put(k, k);
        }
        source.put(j, null);
        assertThrows(
            NullPointerException.class,
            () -> ImmutableSortedMap.copyOf((Set<Entry<Integer, Integer>>) source.entrySet()));
      }
    }
  }

  private static <K, V> void assertMapEquals(Map<K, V> map, Object... alternatingKeysAndValues) {
    Map<Object, Object> expected = new LinkedHashMap<>();
    for (int i = 0; i < alternatingKeysAndValues.length; i += 2) {
      expected.put(alternatingKeysAndValues[i], alternatingKeysAndValues[i + 1]);
    }
    assertThat(map).containsExactlyEntriesIn(expected).inOrder();
  }

  private static class IntHolder implements Serializable {
    public int value;

    public IntHolder(int value) {
      this.value = value;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      return (o instanceof IntHolder) && ((IntHolder) o).value == value;
    }

    @Override
    public int hashCode() {
      return value;
    }

    private static final long serialVersionUID = 5;
  }

  public void testMutableValues() {
    IntHolder holderA = new IntHolder(1);
    IntHolder holderB = new IntHolder(2);
    Map<String, IntHolder> map = ImmutableSortedMap.of("a", holderA, "b", holderB);
    holderA.value = 3;
    assertTrue(map.entrySet().contains(immutableEntry("a", new IntHolder(3))));
    Map<String, Integer> intMap = ImmutableSortedMap.of("a", 3, "b", 2);
    assertEquals(intMap.hashCode(), map.entrySet().hashCode());
    assertEquals(intMap.hashCode(), map.hashCode());
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testViewSerialization() {
    Map<String, Integer> map = ImmutableSortedMap.of("one", 1, "two", 2, "three", 3);
    SerializableTester.reserializeAndAssert(map.entrySet());
    SerializableTester.reserializeAndAssert(map.keySet());
    assertEquals(
        Lists.newArrayList(map.values()),
        Lists.newArrayList(SerializableTester.reserialize(map.values())));
  }

  public void testHeadMapInclusive() {
    Map<String, Integer> map =
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3).headMap("three", true);
    assertThat(map.entrySet())
        .containsExactly(immutableEntry("one", 1), immutableEntry("three", 3))
        .inOrder();
  }

  public void testHeadMapExclusive() {
    Map<String, Integer> map =
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3).headMap("three", false);
    assertThat(map.entrySet()).containsExactly(immutableEntry("one", 1));
  }

  public void testTailMapInclusive() {
    Map<String, Integer> map =
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3).tailMap("three", true);
    assertThat(map.entrySet())
        .containsExactly(immutableEntry("three", 3), immutableEntry("two", 2))
        .inOrder();
  }

  public void testTailMapExclusive() {
    Map<String, Integer> map =
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3).tailMap("three", false);
    assertThat(map.entrySet()).containsExactly(immutableEntry("two", 2));
  }

  public void testSubMapExclusiveExclusive() {
    Map<String, Integer> map =
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3).subMap("one", false, "two", false);
    assertThat(map.entrySet()).containsExactly(immutableEntry("three", 3));
  }

  public void testSubMapInclusiveExclusive() {
    Map<String, Integer> map =
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3).subMap("one", true, "two", false);
    assertThat(map.entrySet())
        .containsExactly(immutableEntry("one", 1), immutableEntry("three", 3))
        .inOrder();
  }

  public void testSubMapExclusiveInclusive() {
    Map<String, Integer> map =
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3).subMap("one", false, "two", true);
    assertThat(map.entrySet())
        .containsExactly(immutableEntry("three", 3), immutableEntry("two", 2))
        .inOrder();
  }

  public void testSubMapInclusiveInclusive() {
    Map<String, Integer> map =
        ImmutableSortedMap.of("one", 1, "two", 2, "three", 3).subMap("one", true, "two", true);
    assertThat(map.entrySet())
        .containsExactly(
            immutableEntry("one", 1), immutableEntry("three", 3), immutableEntry("two", 2))
        .inOrder();
  }

  private static class SelfComparableExample implements Comparable<SelfComparableExample> {
    @Override
    public int compareTo(SelfComparableExample o) {
      return 0;
    }
  }

  public void testBuilderGenerics_selfComparable() {
    ImmutableSortedMap.Builder<SelfComparableExample, Object> unusedNatural =
        ImmutableSortedMap.naturalOrder();

    ImmutableSortedMap.Builder<SelfComparableExample, Object> unusedReverse =
        ImmutableSortedMap.reverseOrder();
  }

  private static class SuperComparableExample extends SelfComparableExample {}

  public void testBuilderGenerics_superComparable() {
    ImmutableSortedMap.Builder<SuperComparableExample, Object> unusedNatural =
        ImmutableSortedMap.naturalOrder();

    ImmutableSortedMap.Builder<SuperComparableExample, Object> unusedReverse =
        ImmutableSortedMap.reverseOrder();
  }
}
