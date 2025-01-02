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

import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.singletonMap;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapGenerators.ImmutableBiMapCopyOfEntriesGenerator;
import com.google.common.collect.testing.google.BiMapGenerators.ImmutableBiMapCopyOfGenerator;
import com.google.common.collect.testing.google.BiMapGenerators.ImmutableBiMapGenerator;
import com.google.common.collect.testing.google.BiMapInverseTester;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.testing.SerializableTester;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link ImmutableBiMap}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
@NullMarked
public class ImmutableBiMapTest extends TestCase {

  // TODO: Reduce duplication of ImmutableMapTest code

  @J2ktIncompatible
  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(
        BiMapTestSuiteBuilder.using(new ImmutableBiMapGenerator())
            .named("ImmutableBiMap")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .suppressing(BiMapInverseTester.getInverseSameAfterSerializingMethods())
            .createTestSuite());
    suite.addTest(
        BiMapTestSuiteBuilder.using(new ImmutableBiMapCopyOfGenerator())
            .named("ImmutableBiMap.copyOf[Map]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .suppressing(BiMapInverseTester.getInverseSameAfterSerializingMethods())
            .createTestSuite());
    suite.addTest(
        BiMapTestSuiteBuilder.using(new ImmutableBiMapCopyOfEntriesGenerator())
            .named("ImmutableBiMap.copyOf[Iterable<Entry>]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                MapFeature.ALLOWS_ANY_NULL_QUERIES)
            .suppressing(BiMapInverseTester.getInverseSameAfterSerializingMethods())
            .createTestSuite());
    suite.addTestSuite(ImmutableBiMapTest.class);

    return suite;
  }

  // Creation tests

  public void testEmptyBuilder() {
    ImmutableBiMap<String, Integer> map = new Builder<String, Integer>().build();
    assertEquals(Collections.<String, Integer>emptyMap(), map);
    assertEquals(Collections.<Integer, String>emptyMap(), map.inverse());
    assertSame(ImmutableBiMap.of(), map);
  }

  public void testSingletonBuilder() {
    ImmutableBiMap<String, Integer> map = new Builder<String, Integer>().put("one", 1).build();
    assertMapEquals(map, "one", 1);
    assertMapEquals(map.inverse(), 1, "one");
  }

  public void testBuilder_withImmutableEntry() {
    ImmutableBiMap<String, Integer> map =
        new Builder<String, Integer>().put(immutableEntry("one", 1)).build();
    assertMapEquals(map, "one", 1);
  }

  public void testBuilder() {
    ImmutableBiMap<String, Integer> map =
        ImmutableBiMap.<String, Integer>builder()
            .put("one", 1)
            .put("two", 2)
            .put("three", 3)
            .put("four", 4)
            .put("five", 5)
            .build();
    assertMapEquals(map, "one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
    assertMapEquals(map.inverse(), 1, "one", 2, "two", 3, "three", 4, "four", 5, "five");
  }

  @GwtIncompatible
  public void testBuilderExactlySizedReusesArray() {
    ImmutableBiMap.Builder<Integer, Integer> builder = ImmutableBiMap.builderWithExpectedSize(10);
    Object[] builderArray = builder.alternatingKeysAndValues;
    for (int i = 0; i < 10; i++) {
      builder.put(i, i);
    }
    Object[] builderArrayAfterPuts = builder.alternatingKeysAndValues;
    RegularImmutableBiMap<Integer, Integer> map =
        (RegularImmutableBiMap<Integer, Integer>) builder.build();
    Object[] mapInternalArray = map.alternatingKeysAndValues;
    assertSame(builderArray, builderArrayAfterPuts);
    assertSame(builderArray, mapInternalArray);
  }

  public void testBuilder_orderEntriesByValue() {
    ImmutableBiMap<String, Integer> map =
        ImmutableBiMap.<String, Integer>builder()
            .orderEntriesByValue(Ordering.natural())
            .put("three", 3)
            .put("one", 1)
            .put("five", 5)
            .put("four", 4)
            .put("two", 2)
            .build();
    assertMapEquals(map, "one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
    assertMapEquals(map.inverse(), 1, "one", 2, "two", 3, "three", 4, "four", 5, "five");
  }

  public void testBuilder_orderEntriesByValueAfterExactSizeBuild() {
    ImmutableBiMap.Builder<String, Integer> builder =
        new ImmutableBiMap.Builder<String, Integer>(2).put("four", 4).put("one", 1);
    ImmutableMap<String, Integer> keyOrdered = builder.build();
    ImmutableMap<String, Integer> valueOrdered =
        builder.orderEntriesByValue(Ordering.natural()).build();
    assertMapEquals(keyOrdered, "four", 4, "one", 1);
    assertMapEquals(valueOrdered, "one", 1, "four", 4);
  }

  public void testBuilder_orderEntriesByValue_usedTwiceFails() {
    ImmutableBiMap.Builder<String, Integer> builder =
        new Builder<String, Integer>().orderEntriesByValue(Ordering.natural());
    assertThrows(
        IllegalStateException.class, () -> builder.orderEntriesByValue(Ordering.natural()));
  }

  public void testBuilderPutAllWithEmptyMap() {
    ImmutableBiMap<String, Integer> map =
        new Builder<String, Integer>().putAll(Collections.<String, Integer>emptyMap()).build();
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

    ImmutableBiMap<String, Integer> map =
        new Builder<String, Integer>().putAll(toPut).putAll(moreToPut).build();
    assertMapEquals(map, "one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
    assertMapEquals(map.inverse(), 1, "one", 2, "two", 3, "three", 4, "four", 5, "five");
  }

  public void testBuilderReuse() {
    Builder<String, Integer> builder = new Builder<>();
    ImmutableBiMap<String, Integer> mapOne = builder.put("one", 1).put("two", 2).build();
    ImmutableBiMap<String, Integer> mapTwo = builder.put("three", 3).put("four", 4).build();

    assertMapEquals(mapOne, "one", 1, "two", 2);
    assertMapEquals(mapOne.inverse(), 1, "one", 2, "two");
    assertMapEquals(mapTwo, "one", 1, "two", 2, "three", 3, "four", 4);
    assertMapEquals(mapTwo.inverse(), 1, "one", 2, "two", 3, "three", 4, "four");
  }

  public void testBuilderPutNullKey() {
    Builder<String, Integer> builder = new Builder<>();
    assertThrows(NullPointerException.class, () -> builder.put(null, 1));
  }

  public void testBuilderPutNullValue() {
    Builder<String, Integer> builder = new Builder<>();
    assertThrows(NullPointerException.class, () -> builder.put("one", null));
  }

  public void testBuilderPutNullKeyViaPutAll() {
    Builder<String, Integer> builder = new Builder<>();
    assertThrows(
        NullPointerException.class,
        () -> builder.putAll(Collections.<String, Integer>singletonMap(null, 1)));
  }

  public void testBuilderPutNullValueViaPutAll() {
    Builder<String, Integer> builder = new Builder<>();
    assertThrows(
        NullPointerException.class,
        () -> builder.putAll(Collections.<String, Integer>singletonMap("one", null)));
  }

  @SuppressWarnings("AlwaysThrows")
  public void testPuttingTheSameKeyTwiceThrowsOnBuild() {
    Builder<String, Integer> builder =
        new Builder<String, Integer>()
            .put("one", 1)
            .put("one", 1); // throwing on this line would be even better

    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> builder.build());
    assertThat(expected).hasMessageThat().contains("one");
  }

  public void testOf() {
    assertMapEquals(ImmutableBiMap.of("one", 1), "one", 1);
    assertMapEquals(ImmutableBiMap.of("one", 1).inverse(), 1, "one");
    assertMapEquals(ImmutableBiMap.of("one", 1, "two", 2), "one", 1, "two", 2);
    assertMapEquals(ImmutableBiMap.of("one", 1, "two", 2).inverse(), 1, "one", 2, "two");
    assertMapEquals(
        ImmutableBiMap.of("one", 1, "two", 2, "three", 3), "one", 1, "two", 2, "three", 3);
    assertMapEquals(
        ImmutableBiMap.of("one", 1, "two", 2, "three", 3).inverse(),
        1,
        "one",
        2,
        "two",
        3,
        "three");
    assertMapEquals(
        ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4),
        "one",
        1,
        "two",
        2,
        "three",
        3,
        "four",
        4);
    assertMapEquals(
        ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4).inverse(),
        1,
        "one",
        2,
        "two",
        3,
        "three",
        4,
        "four");
    assertMapEquals(
        ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5),
        "one",
        1,
        "two",
        2,
        "three",
        3,
        "four",
        4,
        "five",
        5);
    assertMapEquals(
        ImmutableBiMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5).inverse(),
        1,
        "one",
        2,
        "two",
        3,
        "three",
        4,
        "four",
        5,
        "five");
    assertMapEquals(
        ImmutableBiMap.of(
            "one", 1,
            "two", 2,
            "three", 3,
            "four", 4,
            "five", 5,
            "six", 6),
        "one",
        1,
        "two",
        2,
        "three",
        3,
        "four",
        4,
        "five",
        5,
        "six",
        6);
    assertMapEquals(
        ImmutableBiMap.of(
            "one", 1,
            "two", 2,
            "three", 3,
            "four", 4,
            "five", 5,
            "six", 6,
            "seven", 7),
        "one",
        1,
        "two",
        2,
        "three",
        3,
        "four",
        4,
        "five",
        5,
        "six",
        6,
        "seven",
        7);
    assertMapEquals(
        ImmutableBiMap.of(
            "one", 1,
            "two", 2,
            "three", 3,
            "four", 4,
            "five", 5,
            "six", 6,
            "seven", 7,
            "eight", 8),
        "one",
        1,
        "two",
        2,
        "three",
        3,
        "four",
        4,
        "five",
        5,
        "six",
        6,
        "seven",
        7,
        "eight",
        8);
    assertMapEquals(
        ImmutableBiMap.of(
            "one", 1,
            "two", 2,
            "three", 3,
            "four", 4,
            "five", 5,
            "six", 6,
            "seven", 7,
            "eight", 8,
            "nine", 9),
        "one",
        1,
        "two",
        2,
        "three",
        3,
        "four",
        4,
        "five",
        5,
        "six",
        6,
        "seven",
        7,
        "eight",
        8,
        "nine",
        9);
    assertMapEquals(
        ImmutableBiMap.of(
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
        "one",
        1,
        "two",
        2,
        "three",
        3,
        "four",
        4,
        "five",
        5,
        "six",
        6,
        "seven",
        7,
        "eight",
        8,
        "nine",
        9,
        "ten",
        10);
  }

  public void testOfNullKey() {
    assertThrows(NullPointerException.class, () -> ImmutableBiMap.of(null, 1));

    assertThrows(NullPointerException.class, () -> ImmutableBiMap.of("one", 1, null, 2));
  }

  public void testOfNullValue() {
    assertThrows(NullPointerException.class, () -> ImmutableBiMap.of("one", null));

    assertThrows(NullPointerException.class, () -> ImmutableBiMap.of("one", 1, "two", null));
  }

  @SuppressWarnings({"AlwaysThrows", "DistinctVarargsChecker"})
  public void testOfWithDuplicateKey() {
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> ImmutableBiMap.of("one", 1, "one", 1));
    assertThat(expected).hasMessageThat().contains("one");
  }

  public void testOfEntries() {
    assertMapEquals(ImmutableBiMap.ofEntries(entry("one", 1), entry("two", 2)), "one", 1, "two", 2);
  }

  public void testOfEntriesNull() {
    Entry<@Nullable Integer, Integer> nullKey = entry(null, 23);
    assertThrows(
        NullPointerException.class,
        () -> ImmutableBiMap.ofEntries((Entry<Integer, Integer>) nullKey));
    Entry<Integer, @Nullable Integer> nullValue =
        ImmutableBiMapTest.<@Nullable Integer>entry(23, null);
    assertThrows(
        NullPointerException.class,
        () -> ImmutableBiMap.ofEntries((Entry<Integer, Integer>) nullValue));
  }

  private static <T extends @Nullable Object> Entry<T, T> entry(T key, T value) {
    return new AbstractMap.SimpleImmutableEntry<>(key, value);
  }

  public void testCopyOfEmptyMap() {
    ImmutableBiMap<String, Integer> copy =
        ImmutableBiMap.copyOf(Collections.<String, Integer>emptyMap());
    assertEquals(Collections.<String, Integer>emptyMap(), copy);
    assertSame(copy, ImmutableBiMap.copyOf(copy));
    assertSame(ImmutableBiMap.of(), copy);
  }

  public void testCopyOfSingletonMap() {
    ImmutableBiMap<String, Integer> copy = ImmutableBiMap.copyOf(singletonMap("one", 1));
    assertMapEquals(copy, "one", 1);
    assertSame(copy, ImmutableBiMap.copyOf(copy));
  }

  public void testCopyOf() {
    Map<String, Integer> original = new LinkedHashMap<>();
    original.put("one", 1);
    original.put("two", 2);
    original.put("three", 3);

    ImmutableBiMap<String, Integer> copy = ImmutableBiMap.copyOf(original);
    assertMapEquals(copy, "one", 1, "two", 2, "three", 3);
    assertSame(copy, ImmutableBiMap.copyOf(copy));
  }

  public void testEmpty() {
    ImmutableBiMap<String, Integer> bimap = ImmutableBiMap.of();
    assertEquals(Collections.<String, Integer>emptyMap(), bimap);
    assertEquals(Collections.<Integer, String>emptyMap(), bimap.inverse());
  }

  public void testFromHashMap() {
    Map<String, Integer> hashMap = Maps.newLinkedHashMap();
    hashMap.put("one", 1);
    hashMap.put("two", 2);
    ImmutableBiMap<String, Integer> bimap = ImmutableBiMap.copyOf(hashMap);
    assertMapEquals(bimap, "one", 1, "two", 2);
    assertMapEquals(bimap.inverse(), 1, "one", 2, "two");
  }

  public void testFromImmutableMap() {
    ImmutableBiMap<String, Integer> bimap =
        ImmutableBiMap.copyOf(
            new ImmutableMap.Builder<String, Integer>()
                .put("one", 1)
                .put("two", 2)
                .put("three", 3)
                .put("four", 4)
                .put("five", 5)
                .buildOrThrow());
    assertMapEquals(bimap, "one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
    assertMapEquals(bimap.inverse(), 1, "one", 2, "two", 3, "three", 4, "four", 5, "five");
  }

  public void testDuplicateValues() {
    ImmutableMap<String, Integer> map =
        new ImmutableMap.Builder<String, Integer>()
            .put("one", 1)
            .put("two", 2)
            .put("uno", 1)
            .put("dos", 2)
            .buildOrThrow();

    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> ImmutableBiMap.copyOf(map));
    assertThat(expected).hasMessageThat().containsMatch("1|2");
  }

  // TODO(b/172823566): Use mainline testToImmutableBiMap once CollectorTester is usable to java7.
  public void testToImmutableBiMap_java7_combine() {
    ImmutableBiMap.Builder<String, Integer> zis =
        ImmutableBiMap.<String, Integer>builder().put("one", 1);
    ImmutableBiMap.Builder<String, Integer> zat =
        ImmutableBiMap.<String, Integer>builder().put("two", 2).put("three", 3);
    ImmutableBiMap<String, Integer> biMap = zis.combine(zat).build();
    assertMapEquals(biMap, "one", 1, "two", 2, "three", 3);
  }

  // TODO(b/172823566): Use mainline testToImmutableBiMap once CollectorTester is usable to java7.
  public void testToImmutableBiMap_exceptionOnDuplicateKey_java7_combine() {
    ImmutableBiMap.Builder<String, Integer> zis =
        ImmutableBiMap.<String, Integer>builder().put("one", 1).put("two", 2);
    ImmutableBiMap.Builder<String, Integer> zat =
        ImmutableBiMap.<String, Integer>builder().put("two", 22).put("three", 3);
    assertThrows(IllegalArgumentException.class, () -> zis.combine(zat).build());
  }

  // BiMap-specific tests

  @SuppressWarnings("DoNotCall")
  public void testForcePut() {
    BiMap<String, Integer> bimap = ImmutableBiMap.copyOf(ImmutableMap.of("one", 1, "two", 2));
    assertThrows(UnsupportedOperationException.class, () -> bimap.forcePut("three", 3));
  }

  public void testKeySet() {
    ImmutableBiMap<String, Integer> bimap =
        ImmutableBiMap.copyOf(ImmutableMap.of("one", 1, "two", 2, "three", 3, "four", 4));
    Set<String> keys = bimap.keySet();
    assertEquals(newHashSet("one", "two", "three", "four"), keys);
    assertThat(keys).containsExactly("one", "two", "three", "four").inOrder();
  }

  public void testValues() {
    ImmutableBiMap<String, Integer> bimap =
        ImmutableBiMap.copyOf(ImmutableMap.of("one", 1, "two", 2, "three", 3, "four", 4));
    Set<Integer> values = bimap.values();
    assertEquals(newHashSet(1, 2, 3, 4), values);
    assertThat(values).containsExactly(1, 2, 3, 4).inOrder();
  }

  public void testDoubleInverse() {
    ImmutableBiMap<String, Integer> bimap =
        ImmutableBiMap.copyOf(ImmutableMap.of("one", 1, "two", 2));
    assertSame(bimap, bimap.inverse().inverse());
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testEmptySerialization() {
    ImmutableBiMap<String, Integer> bimap = ImmutableBiMap.of();
    assertSame(bimap, SerializableTester.reserializeAndAssert(bimap));
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testSerialization() {
    ImmutableBiMap<String, Integer> bimap =
        ImmutableBiMap.copyOf(ImmutableMap.of("one", 1, "two", 2));
    ImmutableBiMap<String, Integer> copy = SerializableTester.reserializeAndAssert(bimap);
    assertEquals(Integer.valueOf(1), copy.get("one"));
    assertEquals("one", copy.inverse().get(1));
    assertSame(copy, copy.inverse().inverse());
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testInverseSerialization() {
    ImmutableBiMap<String, Integer> bimap =
        ImmutableBiMap.copyOf(ImmutableMap.of(1, "one", 2, "two")).inverse();
    ImmutableBiMap<String, Integer> copy = SerializableTester.reserializeAndAssert(bimap);
    assertEquals(Integer.valueOf(1), copy.get("one"));
    assertEquals("one", copy.inverse().get(1));
    assertSame(copy, copy.inverse().inverse());
  }

  private static <K, V> void assertMapEquals(Map<K, V> map, Object... alternatingKeysAndValues) {
    Map<Object, Object> expected = new LinkedHashMap<>();
    for (int i = 0; i < alternatingKeysAndValues.length; i += 2) {
      expected.put(alternatingKeysAndValues[i], alternatingKeysAndValues[i + 1]);
    }
    assertThat(map).containsExactlyEntriesIn(expected).inOrder();
  }

  /** No-op test so that the class has at least one method, making Maven's test runner happy. */
  public void testNoop() {}
}
