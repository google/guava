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
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.testing.SerializableTester.reserialize;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.testing.CollectionTestSuiteBuilder;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.MapGenerators.ImmutableMapCopyOfEntriesGenerator;
import com.google.common.collect.testing.google.MapGenerators.ImmutableMapCopyOfEnumMapGenerator;
import com.google.common.collect.testing.google.MapGenerators.ImmutableMapCopyOfGenerator;
import com.google.common.collect.testing.google.MapGenerators.ImmutableMapEntryListGenerator;
import com.google.common.collect.testing.google.MapGenerators.ImmutableMapGenerator;
import com.google.common.collect.testing.google.MapGenerators.ImmutableMapKeyListGenerator;
import com.google.common.collect.testing.google.MapGenerators.ImmutableMapUnhashableValuesGenerator;
import com.google.common.collect.testing.google.MapGenerators.ImmutableMapValueListGenerator;
import com.google.common.collect.testing.google.MapGenerators.ImmutableMapValuesAsSingletonSetGenerator;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link ImmutableMap}.
 *
 * @author Kevin Bourrillion
 * @author Jesse Wilson
 */
@GwtCompatible
@SuppressWarnings("AlwaysThrows")
@NullMarked
public class ImmutableMapTest extends TestCase {

  @J2ktIncompatible
  @GwtIncompatible // suite
  @AndroidIncompatible // test-suite builders
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ImmutableMapTest.class);

    suite.addTest(
        MapTestSuiteBuilder.using(new ImmutableMapGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .named("ImmutableMap")
            .createTestSuite());

    suite.addTest(
        MapTestSuiteBuilder.using(new ImmutableMapCopyOfGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .named("ImmutableMap.copyOf[Map]")
            .createTestSuite());

    suite.addTest(
        MapTestSuiteBuilder.using(new ImmutableMapCopyOfEntriesGenerator())
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .named("ImmutableMap.copyOf[Iterable<Entry>]")
            .createTestSuite());

    suite.addTest(
        MapTestSuiteBuilder.using(new ImmutableMapCopyOfEnumMapGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .named("ImmutableMap.copyOf[EnumMap]")
            .createTestSuite());

    suite.addTest(
        MapTestSuiteBuilder.using(new ImmutableMapValuesAsSingletonSetGenerator())
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .named("ImmutableMap.asMultimap.asMap")
            .createTestSuite());

    suite.addTest(
        CollectionTestSuiteBuilder.using(new ImmutableMapUnhashableValuesGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .named("ImmutableMap.values, unhashable")
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableMapKeyListGenerator())
            .named("ImmutableMap.keySet.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableMapEntryListGenerator())
            .named("ImmutableMap.entrySet.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableMapValueListGenerator())
            .named("ImmutableMap.values.asList")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    return suite;
  }

  // Creation tests

  public void testEmptyBuilder() {
    ImmutableMap<String, Integer> map = new Builder<String, Integer>().buildOrThrow();
    assertEquals(Collections.<String, Integer>emptyMap(), map);
  }

  public void testSingletonBuilder() {
    ImmutableMap<String, Integer> map = new Builder<String, Integer>().put("one", 1).buildOrThrow();
    assertMapEquals(map, "one", 1);
  }

  public void testBuilder() {
    ImmutableMap<String, Integer> map =
        new Builder<String, Integer>()
            .put("one", 1)
            .put("two", 2)
            .put("three", 3)
            .put("four", 4)
            .put("five", 5)
            .buildOrThrow();
    assertMapEquals(map, "one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
  }

  @GwtIncompatible
  public void testBuilderExactlySizedReusesArray() {
    ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builderWithExpectedSize(10);
    Object[] builderArray = builder.alternatingKeysAndValues;
    for (int i = 0; i < 10; i++) {
      builder.put(i, i);
    }
    Object[] builderArrayAfterPuts = builder.alternatingKeysAndValues;
    RegularImmutableMap<Integer, Integer> map =
        (RegularImmutableMap<Integer, Integer>) builder.buildOrThrow();
    Object[] mapInternalArray = map.alternatingKeysAndValues;
    assertThat(builderArrayAfterPuts).isSameInstanceAs(builderArray);
    assertThat(mapInternalArray).isSameInstanceAs(builderArray);
  }

  public void testBuilder_orderEntriesByValue() {
    ImmutableMap<String, Integer> map =
        new Builder<String, Integer>()
            .orderEntriesByValue(Ordering.natural())
            .put("three", 3)
            .put("one", 1)
            .put("five", 5)
            .put("four", 4)
            .put("two", 2)
            .buildOrThrow();
    assertMapEquals(map, "one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
  }

  public void testBuilder_orderEntriesByValueAfterExactSizeBuild() {
    Builder<String, Integer> builder = new Builder<String, Integer>(2).put("four", 4).put("one", 1);
    ImmutableMap<String, Integer> keyOrdered = builder.buildOrThrow();
    ImmutableMap<String, Integer> valueOrdered =
        builder.orderEntriesByValue(Ordering.natural()).buildOrThrow();
    assertMapEquals(keyOrdered, "four", 4, "one", 1);
    assertMapEquals(valueOrdered, "one", 1, "four", 4);
  }

  public void testBuilder_orderEntriesByValue_usedTwiceFails() {
    ImmutableMap.Builder<String, Integer> builder =
        new Builder<String, Integer>().orderEntriesByValue(Ordering.natural());
    assertThrows(
        IllegalStateException.class, () -> builder.orderEntriesByValue(Ordering.natural()));
  }

  @GwtIncompatible // we haven't implemented this
  public void testBuilder_orderEntriesByValue_keepingLast() {
    ImmutableMap.Builder<String, Integer> builder =
        new Builder<String, Integer>()
            .orderEntriesByValue(Ordering.natural())
            .put("three", 3)
            .put("one", 1)
            .put("five", 5)
            .put("four", 3)
            .put("four", 5)
            .put("four", 4) // this should win because it's last
            .put("two", 2);
    assertMapEquals(
        builder.buildKeepingLast(), "one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
    assertThrows(IllegalArgumentException.class, () -> builder.buildOrThrow());
  }

  @GwtIncompatible // we haven't implemented this
  public void testBuilder_orderEntriesByValueAfterExactSizeBuild_keepingLastWithoutDuplicates() {
    ImmutableMap.Builder<String, Integer> builder =
        new Builder<String, Integer>(3)
            .orderEntriesByValue(Ordering.natural())
            .put("three", 3)
            .put("one", 1);
    assertMapEquals(builder.buildKeepingLast(), "one", 1, "three", 3);
  }

  @GwtIncompatible // we haven't implemented this
  public void testBuilder_orderEntriesByValue_keepingLast_builderSizeFieldPreserved() {
    ImmutableMap.Builder<String, Integer> builder =
        new Builder<String, Integer>()
            .orderEntriesByValue(Ordering.natural())
            .put("one", 1)
            .put("one", 1);
    assertMapEquals(builder.buildKeepingLast(), "one", 1);
    assertThrows(IllegalArgumentException.class, () -> builder.buildOrThrow());
  }

  public void testBuilder_withImmutableEntry() {
    ImmutableMap<String, Integer> map =
        new Builder<String, Integer>().put(immutableEntry("one", 1)).buildOrThrow();
    assertMapEquals(map, "one", 1);
  }

  public void testBuilder_withImmutableEntryAndNullContents() {
    Builder<String, Integer> builder = new Builder<>();
    assertThrows(NullPointerException.class, () -> builder.put(immutableEntry("one", null)));
    assertThrows(NullPointerException.class, () -> builder.put(immutableEntry(null, 1)));
  }

  private static class StringHolder {
    @Nullable String string;
  }

  public void testBuilder_withMutableEntry() {
    ImmutableMap.Builder<String, Integer> builder = new Builder<>();
    StringHolder holder = new StringHolder();
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
    assertMapEquals(builder.buildOrThrow(), "one", 1);
  }

  public void testBuilderPutAllWithEmptyMap() {
    ImmutableMap<String, Integer> map =
        new Builder<String, Integer>().putAll(Collections.emptyMap()).buildOrThrow();
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

    ImmutableMap<String, Integer> map =
        new Builder<String, Integer>().putAll(toPut).putAll(moreToPut).buildOrThrow();
    assertMapEquals(map, "one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
  }

  public void testBuilderReuse() {
    Builder<String, Integer> builder = new Builder<>();
    ImmutableMap<String, Integer> mapOne = builder.put("one", 1).put("two", 2).buildOrThrow();
    ImmutableMap<String, Integer> mapTwo = builder.put("three", 3).put("four", 4).buildOrThrow();

    assertMapEquals(mapOne, "one", 1, "two", 2);
    assertMapEquals(mapTwo, "one", 1, "two", 2, "three", 3, "four", 4);
  }

  public void testBuilderPutNullKeyFailsAtomically() {
    Builder<String, Integer> builder = new Builder<>();
    assertThrows(NullPointerException.class, () -> builder.put(null, 1));
    builder.put("foo", 2);
    assertMapEquals(builder.buildOrThrow(), "foo", 2);
  }

  public void testBuilderPutImmutableEntryWithNullKeyFailsAtomically() {
    Builder<String, Integer> builder = new Builder<>();
    assertThrows(NullPointerException.class, () -> builder.put(immutableEntry(null, 1)));
    builder.put("foo", 2);
    assertMapEquals(builder.buildOrThrow(), "foo", 2);
  }

  public void testBuilderPutMutableEntryWithNullKeyFailsAtomically() {
    Builder<String, Integer> builder = new Builder<>();
    assertThrows(
        NullPointerException.class, () -> builder.put(new SimpleEntry<String, Integer>(null, 1)));
    builder.put("foo", 2);
    assertMapEquals(builder.buildOrThrow(), "foo", 2);
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
        NullPointerException.class, () -> builder.putAll(Collections.singletonMap(null, 1)));
  }

  public void testBuilderPutNullValueViaPutAll() {
    Builder<String, Integer> builder = new Builder<>();
    assertThrows(
        NullPointerException.class, () -> builder.putAll(Collections.singletonMap("one", null)));
  }

  public void testPuttingTheSameKeyTwiceThrowsOnBuild() {
    Builder<String, Integer> builder =
        new Builder<String, Integer>()
            .put("one", 1)
            .put("one", 1); // throwing on this line might be better but it's too late to change

    assertThrows(IllegalArgumentException.class, () -> builder.buildOrThrow());
  }

  public void testBuildKeepingLast_allowsOverwrite() {
    Builder<Integer, String> builder =
        new Builder<Integer, String>()
            .put(1, "un")
            .put(2, "deux")
            .put(70, "soixante-dix")
            .put(70, "septante")
            .put(70, "seventy")
            .put(1, "one")
            .put(2, "two");
    ImmutableMap<Integer, String> map = builder.buildKeepingLast();
    assertMapEquals(map, 1, "one", 2, "two", 70, "seventy");
  }

  public void testBuildKeepingLast_smallTableSameHash() {
    String key1 = "QED";
    String key2 = "R&D";
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    ImmutableMap<String, Integer> map =
        ImmutableMap.<String, Integer>builder()
            .put(key1, 1)
            .put(key2, 2)
            .put(key1, 3)
            .put(key2, 4)
            .buildKeepingLast();
    assertMapEquals(map, key1, 3, key2, 4);
  }

  // The java7 branch has different code depending on whether the entry indexes fit in a byte,
  // short, or int. The small table in testBuildKeepingLast_allowsOverwrite will test the byte
  // case. This method tests the short case.
  public void testBuildKeepingLast_shortTable() {
    Builder<Integer, String> builder = ImmutableMap.builder();
    Map<Integer, String> expected = new LinkedHashMap<>();
    for (int i = 0; i < 1000; i++) {
      // Truncate to even key, so we have put(0, "0") then put(0, "1"). Half the entries are
      // duplicates.
      Integer key = i & ~1;
      String value = String.valueOf(i);
      builder.put(key, value);
      expected.put(key, value);
    }
    ImmutableMap<Integer, String> map = builder.buildKeepingLast();
    assertThat(map).hasSize(500);
    assertThat(map).containsExactlyEntriesIn(expected).inOrder();
  }

  // This method tests the int case.
  public void testBuildKeepingLast_bigTable() {
    Builder<Integer, String> builder = ImmutableMap.builder();
    Map<Integer, String> expected = new LinkedHashMap<>();
    for (int i = 0; i < 200_000; i++) {
      // Truncate to even key, so we have put(0, "0") then put(0, "1"). Half the entries are
      // duplicates.
      Integer key = i & ~1;
      String value = String.valueOf(i);
      builder.put(key, value);
      expected.put(key, value);
    }
    ImmutableMap<Integer, String> map = builder.buildKeepingLast();
    assertThat(map).hasSize(100_000);
    assertThat(map).containsExactlyEntriesIn(expected).inOrder();
  }

  private static class ClassWithTerribleHashCode implements Comparable<ClassWithTerribleHashCode> {
    private final int value;

    ClassWithTerribleHashCode(int value) {
      this.value = value;
    }

    @Override
    public int compareTo(ClassWithTerribleHashCode that) {
      return Integer.compare(this.value, that.value);
    }

    @Override
    public boolean equals(@Nullable Object x) {
      return x instanceof ClassWithTerribleHashCode
          && ((ClassWithTerribleHashCode) x).value == value;
    }

    @Override
    public int hashCode() {
      return 23;
    }

    @Override
    public String toString() {
      return "ClassWithTerribleHashCode(" + value + ")";
    }
  }

  @GwtIncompatible
  public void testBuildKeepingLast_collisions() {
    for (int size : new int[] {80, 300}) { // linear-probing and SWAR-probing flood thresholds
      Map<ClassWithTerribleHashCode, Integer> expected = new LinkedHashMap<>();
      Builder<ClassWithTerribleHashCode, Integer> builder = new Builder<>();
      for (int i = 0; i < size; i++) {
        ClassWithTerribleHashCode key = new ClassWithTerribleHashCode(i);
        builder.put(key, i);
        builder.put(key, -i);
        expected.put(key, -i);
      }
      ImmutableMap<ClassWithTerribleHashCode, Integer> map = builder.buildKeepingLast();
      assertThat(map).containsExactlyEntriesIn(expected).inOrder();
    }
  }

  /**
   * Verifies that view {@code asList().get(index)} rejects indices whose {@code index << 1} bit
   * shift would wrap into valid flat-array bounds (such as {@link Integer#MIN_VALUE} or {@code 1 <<
   * 30}).
   */
  public void testKeySetAndValuesAsList_outOfBoundsBitShiftWraparound() {
    ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
    ImmutableList<Entry<String, Integer>> entries = map.entrySet().asList();
    ImmutableList<String> keys = map.keySet().asList();
    ImmutableList<Integer> values = map.values().asList();
    int[] badIndices = {-1, 2, Integer.MIN_VALUE, Integer.MIN_VALUE + 1, 1 << 30, (1 << 30) + 1};
    for (int badIndex : badIndices) {
      try {
        entries.get(badIndex);
        fail();
      } catch (IndexOutOfBoundsException expected) {
      }
      try {
        keys.get(badIndex);
        fail();
      } catch (IndexOutOfBoundsException expected) {
      }
      try {
        values.get(badIndex);
        fail();
      } catch (IndexOutOfBoundsException expected) {
      }
    }
  }

  @GwtIncompatible // Pattern, Matcher
  public void testBuilder_keepingLast_thenOrThrow() {
    ImmutableMap.Builder<String, Integer> builder =
        new Builder<String, Integer>()
            .put("three", 3)
            .put("one", 1)
            .put("five", 5)
            .put("four", 3)
            .put("four", 5)
            .put("four", 4) // this should win because it's last
            .put("two", 2);
    assertMapEquals(
        builder.buildKeepingLast(), "three", 3, "one", 1, "five", 5, "four", 4, "two", 2);
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> builder.buildOrThrow());
    // We don't really care which values the exception message contains, but they should be
    // different from each other. If buildKeepingLast() collapsed duplicates, that might end up not
    // being true.
    Pattern pattern = Pattern.compile("Multiple entries with same key: four=(.*) and four=(.*)");
    assertThat(expected).hasMessageThat().matches(pattern);
    Matcher matcher = pattern.matcher(expected.getMessage());
    assertThat(matcher.matches()).isTrue();
    assertThat(matcher.group(1)).isNotEqualTo(matcher.group(2));
  }

  public void testOf() {
    assertMapEquals(ImmutableMap.of("one", 1), "one", 1);
    assertMapEquals(ImmutableMap.of("one", 1, "two", 2), "one", 1, "two", 2);
    assertMapEquals(
        ImmutableMap.of("one", 1, "two", 2, "three", 3), "one", 1, "two", 2, "three", 3);
    assertMapEquals(
        ImmutableMap.of("one", 1, "two", 2, "three", 3, "four", 4),
        "one",
        1,
        "two",
        2,
        "three",
        3,
        "four",
        4);
    assertMapEquals(
        ImmutableMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5),
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
        ImmutableMap.of(
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
        ImmutableMap.of(
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
        ImmutableMap.of(
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
        ImmutableMap.of(
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
        ImmutableMap.of(
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
    assertThrows(NullPointerException.class, () -> ImmutableMap.of(null, 1));

    assertThrows(NullPointerException.class, () -> ImmutableMap.of("one", 1, null, 2));
  }

  public void testOfNullValue() {
    assertThrows(NullPointerException.class, () -> ImmutableMap.of("one", null));

    assertThrows(NullPointerException.class, () -> ImmutableMap.of("one", 1, "two", null));
  }

  public void testOfWithDuplicateKey() {
    assertThrows(IllegalArgumentException.class, () -> ImmutableMap.of("one", 1, "one", 1));
  }

  public void testCopyOfEmptyMap() {
    ImmutableMap<String, Integer> copy = ImmutableMap.copyOf(Collections.emptyMap());
    assertEquals(Collections.<String, Integer>emptyMap(), copy);
    assertThat(ImmutableMap.copyOf(copy)).isSameInstanceAs(copy);
  }

  public void testCopyOfSingletonMap() {
    ImmutableMap<String, Integer> copy = ImmutableMap.copyOf(singletonMap("one", 1));
    assertMapEquals(copy, "one", 1);
    assertThat(ImmutableMap.copyOf(copy)).isSameInstanceAs(copy);
  }

  public void testCopyOfNonCollectionIterable() {
    Iterable<Entry<String, Integer>> empty = Collections::emptyIterator;
    assertThat(ImmutableMap.copyOf(empty)).isSameInstanceAs(ImmutableMap.of());

    Iterable<Entry<String, Integer>> single =
        () -> Collections.singletonList(mapEntry("one", 1)).iterator();
    assertMapEquals(ImmutableMap.copyOf(single), "one", 1);

    Iterable<Entry<String, Integer>> two =
        () -> asList(mapEntry("one", 1), mapEntry("two", 2)).iterator();
    assertMapEquals(ImmutableMap.copyOf(two), "one", 1, "two", 2);

    Iterable<Entry<String, Integer>> nullEntry =
        () -> Collections.<Entry<String, Integer>>singletonList(null).iterator();
    assertThrows(NullPointerException.class, () -> ImmutableMap.copyOf(nullEntry));

    Iterable<Entry<String, Integer>> nullKey =
        () -> Collections.singletonList(mapEntry((String) null, 1)).iterator();
    assertThrows(NullPointerException.class, () -> ImmutableMap.copyOf(nullKey));

    Iterable<Entry<String, Integer>> nullValue =
        () -> Collections.singletonList(mapEntry("one", (Integer) null)).iterator();
    assertThrows(NullPointerException.class, () -> ImmutableMap.copyOf(nullValue));
  }

  public void testCopyOf() {
    Map<String, Integer> original = new LinkedHashMap<>();
    original.put("one", 1);
    original.put("two", 2);
    original.put("three", 3);

    ImmutableMap<String, Integer> copy = ImmutableMap.copyOf(original);
    assertMapEquals(copy, "one", 1, "two", 2, "three", 3);
    assertThat(ImmutableMap.copyOf(copy)).isSameInstanceAs(copy);
  }

  // TODO(b/172823566): Use mainline testToImmutableMap once CollectorTester is usable to java7.
  public void testToImmutableMap_java7_combine() {
    ImmutableMap.Builder<String, Integer> zis =
        ImmutableMap.<String, Integer>builder().put("one", 1);
    ImmutableMap.Builder<String, Integer> zat =
        ImmutableMap.<String, Integer>builder().put("two", 2).put("three", 3);
    assertMapEquals(zis.combine(zat).build(), "one", 1, "two", 2, "three", 3);
  }

  // TODO(b/172823566): Use mainline testToImmutableMap once CollectorTester is usable to java7.
  public void testToImmutableMap_exceptionOnDuplicateKey_java7_combine() {
    ImmutableMap.Builder<String, Integer> zis =
        ImmutableMap.<String, Integer>builder().put("one", 1).put("two", 2);
    ImmutableMap.Builder<String, Integer> zat =
        ImmutableMap.<String, Integer>builder().put("two", 22).put("three", 3);
    assertThrows(IllegalArgumentException.class, () -> zis.combine(zat).build());
  }

  public static void hashtableTestHelper(ImmutableList<Integer> sizes) {
    for (int size : sizes) {
      Builder<Integer, Integer> builder = ImmutableMap.builderWithExpectedSize(size);
      for (int i = 0; i < size; i++) {
        Integer integer = i;
        builder.put(integer, integer);
      }
      ImmutableMap<Integer, Integer> map = builder.build();
      assertEquals(size, map.size());
      int entries = 0;
      for (Integer key : map.keySet()) {
        assertEquals(entries, key.intValue());
        assertSame(key, map.get(key));
        entries++;
      }
      assertEquals(size, entries);
    }
  }

  public void testByteArrayHashtable() {
    hashtableTestHelper(ImmutableList.of(2, 89));
  }

  public void testShortArrayHashtable() {
    hashtableTestHelper(ImmutableList.of(90, 22937));
  }

  public void testIntArrayHashtable() {
    hashtableTestHelper(ImmutableList.of(22938));
  }

  // Non-creation tests

  public void testNullGet() {
    ImmutableMap<String, Integer> map = ImmutableMap.of("one", 1);
    assertThat(map.get(null)).isNull();
  }

  public void testAsMultimap() {
    ImmutableMap<String, Integer> map =
        ImmutableMap.of("one", 1, "won", 1, "two", 2, "too", 2, "three", 3);
    ImmutableSetMultimap<String, Integer> expected =
        ImmutableSetMultimap.of("one", 1, "won", 1, "two", 2, "too", 2, "three", 3);
    assertEquals(expected, map.asMultimap());
  }

  public void testAsMultimapWhenEmpty() {
    ImmutableMap<String, Integer> map = ImmutableMap.of();
    ImmutableSetMultimap<String, Integer> expected = ImmutableSetMultimap.of();
    assertEquals(expected, map.asMultimap());
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(ImmutableMap.class);
    tester.testAllPublicInstanceMethods(new ImmutableMap.Builder<Object, Object>());
    tester.testAllPublicInstanceMethods(ImmutableMap.of());
    tester.testAllPublicInstanceMethods(ImmutableMap.of("one", 1));
    tester.testAllPublicInstanceMethods(ImmutableMap.of("one", 1, "two", 2, "three", 3));
  }

  private static <K, V> void assertMapEquals(Map<K, V> map, Object... alternatingKeysAndValues) {
    Map<Object, Object> expected = new LinkedHashMap<>();
    for (int i = 0; i < alternatingKeysAndValues.length; i += 2) {
      expected.put(alternatingKeysAndValues[i], alternatingKeysAndValues[i + 1]);
    }
    assertThat(map).containsExactlyEntriesIn(expected).inOrder();
  }

  private static class IntHolder implements Serializable {
    private int value;

    IntHolder(int value) {
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

    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 5;
  }

  public void testMutableValues() {
    IntHolder holderA = new IntHolder(1);
    IntHolder holderB = new IntHolder(2);
    Map<String, IntHolder> map = ImmutableMap.of("a", holderA, "b", holderB);
    holderA.value = 3;
    assertTrue(map.entrySet().contains(immutableEntry("a", new IntHolder(3))));
    Map<String, Integer> intMap = ImmutableMap.of("a", 3, "b", 2);
    assertEquals(intMap.hashCode(), map.entrySet().hashCode());
    assertEquals(intMap.hashCode(), map.hashCode());
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testViewSerialization() {
    Map<String, Integer> map = ImmutableMap.of("one", 1, "two", 2, "three", 3);
    LenientSerializableTester.reserializeAndAssertLenient(map.entrySet());
    LenientSerializableTester.reserializeAndAssertLenient(map.keySet());

    Collection<Integer> reserializedValues = reserialize(map.values());
    assertEquals(new ArrayList<>(map.values()), new ArrayList<>(reserializedValues));
    assertTrue(reserializedValues instanceof ImmutableCollection);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testKeySetIsSerializable_regularImmutableMap() {
    class NonSerializableClass {}

    Map<String, NonSerializableClass> map =
        RegularImmutableMap.create(1, new Object[] {"one", new NonSerializableClass()});
    Set<String> set = map.keySet();

    LenientSerializableTester.reserializeAndAssertLenient(set);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testValuesCollectionIsSerializable_regularImmutableMap() {
    class NonSerializableClass {}

    Map<NonSerializableClass, String> map =
        RegularImmutableMap.create(1, new Object[] {new NonSerializableClass(), "value"});
    Collection<String> collection = map.values();

    LenientSerializableTester.reserializeAndAssertElementsEqual(collection);
  }

  // TODO: Re-enable this test after moving to new serialization format in ImmutableMap.
  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  @SuppressWarnings("unchecked")
  public void ignore_testSerializationNoDuplication_regularImmutableMap() throws Exception {
    // Tests that serializing a map, its keySet, and values only writes the underlying data once.

    Object[] entries = new Object[2000];
    for (int i = 0; i < entries.length; i++) {
      entries[i] = i;
    }

    ImmutableMap<Integer, Integer> map = RegularImmutableMap.create(entries.length / 2, entries);
    Set<Integer> keySet = map.keySet();
    Collection<Integer> values = map.values();

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bytes);
    oos.writeObject(map);
    oos.flush();

    int mapSize = bytes.size();
    oos.writeObject(keySet);
    oos.writeObject(values);
    oos.close();

    int finalSize = bytes.size();

    assertThat(finalSize - mapSize).isLessThan(100);
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            ImmutableMap.of(),
            ImmutableMap.builder().buildOrThrow(),
            ImmutableMap.ofEntries(),
            map())
        .addEqualityGroup(
            ImmutableMap.of(1, 1),
            ImmutableMap.builder().put(1, 1).buildOrThrow(),
            ImmutableMap.ofEntries(entry(1, 1)),
            map(1, 1))
        .addEqualityGroup(
            ImmutableMap.of(1, 1, 2, 2),
            ImmutableMap.builder().put(1, 1).put(2, 2).buildOrThrow(),
            ImmutableMap.ofEntries(entry(1, 1), entry(2, 2)),
            map(1, 1, 2, 2))
        .addEqualityGroup(
            ImmutableMap.of(1, 1, 2, 2, 3, 3),
            ImmutableMap.builder().put(1, 1).put(2, 2).put(3, 3).buildOrThrow(),
            ImmutableMap.ofEntries(entry(1, 1), entry(2, 2), entry(3, 3)),
            map(1, 1, 2, 2, 3, 3))
        .addEqualityGroup(
            ImmutableMap.of(1, 4, 2, 2, 3, 3),
            ImmutableMap.builder().put(1, 4).put(2, 2).put(3, 3).buildOrThrow(),
            ImmutableMap.ofEntries(entry(1, 4), entry(2, 2), entry(3, 3)),
            map(1, 4, 2, 2, 3, 3))
        .addEqualityGroup(
            ImmutableMap.of(1, 1, 2, 4, 3, 3),
            ImmutableMap.builder().put(1, 1).put(2, 4).put(3, 3).buildOrThrow(),
            ImmutableMap.ofEntries(entry(1, 1), entry(2, 4), entry(3, 3)),
            map(1, 1, 2, 4, 3, 3))
        .addEqualityGroup(
            ImmutableMap.of(1, 1, 2, 2, 3, 4),
            ImmutableMap.builder().put(1, 1).put(2, 2).put(3, 4).buildOrThrow(),
            ImmutableMap.ofEntries(entry(1, 1), entry(2, 2), entry(3, 4)),
            map(1, 1, 2, 2, 3, 4))
        .addEqualityGroup(
            ImmutableMap.of(1, 2, 2, 3, 3, 1),
            ImmutableMap.builder().put(1, 2).put(2, 3).put(3, 1).buildOrThrow(),
            ImmutableMap.ofEntries(entry(1, 2), entry(2, 3), entry(3, 1)),
            map(1, 2, 2, 3, 3, 1))
        .addEqualityGroup(
            ImmutableMap.of(1, 1, 2, 2, 3, 3, 4, 4),
            ImmutableMap.builder().put(1, 1).put(2, 2).put(3, 3).put(4, 4).buildOrThrow(),
            ImmutableMap.ofEntries(entry(1, 1), entry(2, 2), entry(3, 3), entry(4, 4)),
            map(1, 1, 2, 2, 3, 3, 4, 4))
        .addEqualityGroup(
            ImmutableMap.of(1, 1, 2, 2, 3, 3, 4, 4, 5, 5),
            ImmutableMap.builder().put(1, 1).put(2, 2).put(3, 3).put(4, 4).put(5, 5).buildOrThrow(),
            ImmutableMap.ofEntries(entry(1, 1), entry(2, 2), entry(3, 3), entry(4, 4), entry(5, 5)),
            map(1, 1, 2, 2, 3, 3, 4, 4, 5, 5))
        .testEquals();
  }

  public void testOfEntriesNull() {
    Entry<@Nullable Integer, @Nullable Integer> nullKey = entry(null, 23);
    assertThrows(NullPointerException.class, () -> ImmutableMap.ofEntries(nullKey));
    Entry<@Nullable Integer, @Nullable Integer> nullValue = entry(23, null);
    assertThrows(NullPointerException.class, () -> ImmutableMap.ofEntries(nullValue));
  }

  private static <T> Map<T, T> map(T... keysAndValues) {
    assertThat(keysAndValues.length % 2).isEqualTo(0);
    LinkedHashMap<T, T> map = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      T key = keysAndValues[i];
      T value = keysAndValues[i + 1];
      T old = map.put(key, value);
      assertWithMessage("Key %s set to %s and %s", key, value, old).that(old).isNull();
    }
    return map;
  }

  private static <T extends @Nullable Object> Entry<T, T> entry(T key, T value) {
    return new SimpleImmutableEntry<>(key, value);
  }

  public void testCopyOfMutableEntryList() {
    List<Entry<String, String>> entryList =
        asList(new SimpleEntry<>("a", "1"), new SimpleEntry<>("b", "2"));
    ImmutableMap<String, String> map = ImmutableMap.copyOf(entryList);
    assertThat(map).containsExactly("a", "1", "b", "2").inOrder();
    entryList.get(0).setValue("3");
    assertThat(map).containsExactly("a", "1", "b", "2").inOrder();
  }

  public void testBuilderPutAllEntryList() {
    List<Entry<String, String>> entryList =
        asList(new SimpleEntry<>("a", "1"), new SimpleEntry<>("b", "2"));
    ImmutableMap<String, String> map =
        ImmutableMap.<String, String>builder().putAll(entryList).buildOrThrow();
    assertThat(map).containsExactly("a", "1", "b", "2").inOrder();
    entryList.get(0).setValue("3");
    assertThat(map).containsExactly("a", "1", "b", "2").inOrder();
  }

  /**
   * Exercises every {@link RegularImmutableMap} hash-table tier and boundary size:
   *
   * <ul>
   *   <li>Tier 1 {@code byte[]} linear probing ({@code n <= 255}), including load-factor doublings
   *       ({@code 89->90}, {@code 179->180}), signed-byte wraparound ({@code 128}), and maximum
   *       0-based entry index {@code 254} at {@code n = 255}.
   *   <li>Tier 2 SWAR-16 ({@code 256 <= n <= 65,536}).
   *   <li>Tier 2 SWAR-24 ({@code 65,537 <= n <= 16,777,216}).
   * </ul>
   */
  @GwtIncompatible
  public void testRegularImmutableMapAllTiersAndBoundaries() {
    int[] sizes = {
      2, 8, 89, 90, 128, 129, 179, 180, 254, 255, 256, 257, 1024, 65_536, 65_537, 70_000
    };
    for (int n : sizes) {
      ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builderWithExpectedSize(n);
      for (int i = 0; i < n; i++) {
        builder.put(i, i * 3 + 1);
      }
      ImmutableMap<Integer, Integer> map = builder.buildOrThrow();
      assertThat(map).isInstanceOf(RegularImmutableMap.class);
      assertThat(map).hasSize(n);
      // Spot-check hits, misses, and null handling across all tiers.
      assertThat(map.get(0)).isEqualTo(1);
      assertThat(map.get(n / 2)).isEqualTo((n / 2) * 3 + 1);
      assertThat(map.get(n - 1)).isEqualTo((n - 1) * 3 + 1);
      assertThat(map.get(-1)).isNull();
      assertThat(map.get(n)).isNull();
      assertThat(map.get(null)).isNull();
      if (n <= 1024) {
        int expectedHash = 0;
        for (int i = 0; i < n; i++) {
          assertThat(map.get(i)).isEqualTo(i * 3 + 1);
          expectedHash += Integer.hashCode(i) ^ Integer.hashCode(i * 3 + 1);
        }
        assertThat(map.hashCode()).isEqualTo(expectedHash);
      }
    }
  }

  /**
   * Exercises {@link ImmutableMap.Builder#buildKeepingLast} when deduplication shrinks a map across
   * table-size or storage-tier boundaries (within {@code byte[]}, SWAR-16 to {@code byte[]} at the
   * {@code n = 255} max-byte boundary and below, and SWAR-32 to SWAR-16), verifying that lookups
   * succeed and a subsequent {@link ImmutableMap.Builder#buildOrThrow} still fails.
   */
  @GwtIncompatible
  public void testBuildKeepingLastCrossingTierBoundaries() {
    // 16 entries with 6 unique keys: shrinks byte[] table size from 32 to 16.
    final ImmutableMap.Builder<Integer, Integer> byteShrink = ImmutableMap.builder();
    for (int i = 0; i < 6; i++) {
      byteShrink.put(i, i);
      byteShrink.put(i, i + 100);
    }
    byteShrink.put(0, 999);
    byteShrink.put(1, 998);
    byteShrink.put(2, 997);
    byteShrink.put(3, 996);
    ImmutableMap<Integer, Integer> shrunkByteMap = byteShrink.buildKeepingLast();
    assertThat(shrunkByteMap).hasSize(6);
    assertThat(shrunkByteMap.get(0)).isEqualTo(999);
    assertThat(shrunkByteMap.get(5)).isEqualTo(105);
    try {
      byteShrink.buildOrThrow();
      fail();
    } catch (IllegalArgumentException expected) {
    }

    // 220 entries with 150 unique keys (duplicates at entry indices >= 128): stays in byte[] and
    // shrinks table size from 512 to 256.
    ImmutableMap.Builder<Integer, Integer> byteHighIndexShrink = ImmutableMap.builder();
    for (int i = 0; i < 150; i++) {
      byteHighIndexShrink.put(i, i);
    }
    for (int i = 80; i < 150; i++) {
      byteHighIndexShrink.put(i, i + 1000);
    }
    ImmutableMap<Integer, Integer> shrunkHighIndexByteMap = byteHighIndexShrink.buildKeepingLast();
    assertThat(shrunkHighIndexByteMap).hasSize(150);
    for (int i = 0; i < 150; i++) {
      assertThat(shrunkHighIndexByteMap.get(i)).isEqualTo(i < 80 ? i : i + 1000);
    }
    try {
      byteHighIndexShrink.buildOrThrow();
      fail();
    } catch (IllegalArgumentException expected) {
    }

    // 256 entries with 255 unique keys: starts in SWAR-16 (>= 256), compacts into byte[] at the
    // exact max capacity (n = 255, max entry index 254).
    ImmutableMap.Builder<Integer, Integer> swarToByteMax = ImmutableMap.builder();
    for (int i = 0; i < 255; i++) {
      swarToByteMax.put(i, i);
    }
    swarToByteMax.put(254, -254);
    ImmutableMap<Integer, Integer> swarToMaxByteMap = swarToByteMax.buildKeepingLast();
    assertThat(swarToMaxByteMap).hasSize(255);
    assertThat(swarToMaxByteMap.get(0)).isEqualTo(0);
    assertThat(swarToMaxByteMap.get(128)).isEqualTo(128);
    assertThat(swarToMaxByteMap.get(254)).isEqualTo(-254);
    try {
      swarToByteMax.buildOrThrow();
      fail();
    } catch (IllegalArgumentException expected) {
    }

    // 300 entries with 150 unique keys: starts in SWAR-16 (>= 256), compacts into byte[] (<= 255).
    ImmutableMap.Builder<Integer, Integer> swarToByte = ImmutableMap.builder();
    for (int i = 0; i < 150; i++) {
      swarToByte.put(i, i);
      swarToByte.put(i, -i);
    }
    ImmutableMap<Integer, Integer> swarToByteMap = swarToByte.buildKeepingLast();
    assertThat(swarToByteMap).hasSize(150);
    for (int i = 0; i < 150; i++) {
      assertThat(swarToByteMap.get(i)).isEqualTo(-i);
    }
    try {
      swarToByte.buildOrThrow();
      fail();
    } catch (IllegalArgumentException expected) {
    }

    // 66,000 entries with 65,000 unique keys: starts in SWAR-32 (> 65,536), compacts into SWAR-16.
    ImmutableMap.Builder<Integer, Integer> swar32To16 =
        ImmutableMap.builderWithExpectedSize(66_000);
    for (int i = 0; i < 65_000; i++) {
      swar32To16.put(i, i);
    }
    for (int i = 0; i < 1_000; i++) {
      swar32To16.put(i, -i - 1);
    }
    ImmutableMap<Integer, Integer> swar32ToSwar16Map = swar32To16.buildKeepingLast();
    assertThat(swar32ToSwar16Map).hasSize(65_000);
    assertThat(swar32ToSwar16Map.get(0)).isEqualTo(-1);
    assertThat(swar32ToSwar16Map.get(999)).isEqualTo(-1000);
    assertThat(swar32ToSwar16Map.get(1000)).isEqualTo(1000);
    assertThat(swar32ToSwar16Map.get(64_999)).isEqualTo(64_999);
    try {
      swar32To16.buildOrThrow();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible
  public void testRegularImmutableMapSwar24BitPackingAllLanes() {
    long[] table = new long[8];
    int groupBase = 4;
    int[] expected = {
      0x000001, 0x123456, 0xABCDEF, 0x800000, 0x7FFFFF, 0xFEDCB9, 0x00FF00, 0xFFFFFF
    };
    for (int lane = 0; lane < 8; lane++) {
      SwarTables.writeSlot(
          table, groupBase, SwarTables.STRIDE_24, table[groupBase], 0x80, lane, expected[lane]);
    }
    for (int lane = 0; lane < 8; lane++) {
      assertThat(SwarTables.read24BitIndex(table, groupBase, lane)).isEqualTo(expected[lane]);
    }
  }
}
