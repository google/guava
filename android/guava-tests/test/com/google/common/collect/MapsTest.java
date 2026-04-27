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

import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.collect.Maps.toMap;
import static com.google.common.collect.Maps.transformEntries;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Maps.unmodifiableNavigableMap;
import static com.google.common.collect.Sets.newTreeSet;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Converter;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.collect.Maps.ValueDifferenceImpl;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit test for {@code Maps}.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Jared Levy
 */
@GwtCompatible
@NullMarked
@SuppressWarnings("JUnitIncompatibleType") // Many intentional violations here.
public class MapsTest extends TestCase {

  private static final Comparator<Integer> SOME_COMPARATOR = Collections.reverseOrder();

  public void testHashMap() {
    @SuppressWarnings("UseCollectionConstructor") // test of factory method
    HashMap<Integer, Integer> map = Maps.newHashMap();
    assertEquals(emptyMap(), map);
  }

  public void testHashMapWithInitialMap() {
    Map<String, Integer> original = new TreeMap<>();
    original.put("a", 1);
    original.put("b", 2);
    original.put("c", 3);
    @SuppressWarnings("UseCollectionConstructor") // test of factory method
    HashMap<String, Integer> map = Maps.newHashMap(original);
    assertEquals(original, map);
  }

  public void testHashMapGeneralizesTypes() {
    Map<String, Integer> original = new TreeMap<>();
    original.put("a", 1);
    original.put("b", 2);
    original.put("c", 3);
    @SuppressWarnings("UseCollectionConstructor") // test of factory method
    HashMap<Object, Object> map = Maps.newHashMap(original);
    assertEquals(original, map);
  }

  public void testCapacityForNegativeSizeFails() {
    assertThrows(IllegalArgumentException.class, () -> Maps.capacity(-1));
  }

  /**
   * Tests that nHMWES makes hash maps large enough that adding the expected number of elements
   * won't cause a rehash.
   *
   * <p>As of jdk7u40, HashMap has an empty-map optimization. The argument to new HashMap(int) is
   * noted, but the initial table is a zero-length array.
   *
   * <p>This test may fail miserably on non-OpenJDK environments...
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  @AndroidIncompatible // relies on assumptions about OpenJDK
  public void testNewHashMapWithExpectedSize_wontGrow() throws Exception {
    // before jdk7u40: creates one-bucket table
    // after  jdk7u40: creates empty table
    assertThat(bucketsOf(Maps.newHashMapWithExpectedSize(0))).isAtMost(1);

    for (int size = 1; size < 200; size++) {
      assertWontGrow(
          size,
          new HashMap<>(),
          Maps.newHashMapWithExpectedSize(size),
          Maps.newHashMapWithExpectedSize(size));
    }
  }

  /** Same test as above but for newLinkedHashMapWithExpectedSize */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  @AndroidIncompatible // relies on assumptions about OpenJDK
  public void testNewLinkedHashMapWithExpectedSize_wontGrow() throws Exception {
    assertThat(bucketsOf(Maps.newLinkedHashMapWithExpectedSize(0))).isAtMost(1);

    for (int size = 1; size < 200; size++) {
      assertWontGrow(
          size,
          new LinkedHashMap<>(),
          Maps.newLinkedHashMapWithExpectedSize(size),
          Maps.newLinkedHashMapWithExpectedSize(size));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  private static void assertWontGrow(
      int size,
      HashMap<Object, Object> referenceMap,
      HashMap<Object, Object> map1,
      HashMap<Object, Object> map2)
      throws Exception {
    // Only start measuring table size after the first element inserted, to
    // deal with empty-map optimization.
    map1.put(0, null);

    int initialBuckets = bucketsOf(map1);

    for (int i = 1; i < size; i++) {
      map1.put(i, null);
    }
    assertWithMessage("table size after adding %s elements", size)
        .that(bucketsOf(map1))
        .isEqualTo(initialBuckets);

    /*
     * Something slightly different happens when the entries are added all at
     * once; make sure that passes too.
     */
    map2.putAll(map1);
    assertWithMessage("table size after adding %s elements", size)
        .that(bucketsOf(map1))
        .isEqualTo(initialBuckets);

    // Ensure that referenceMap, which doesn't use WithExpectedSize, ends up with the same table
    // size as the other two maps. If it ended up with a smaller size that would imply that we
    // computed the wrong initial capacity.
    for (int i = 0; i < size; i++) {
      referenceMap.put(i, null);
    }
    assertWithMessage("table size after adding %s elements", size)
        .that(initialBuckets)
        .isAtMost(bucketsOf(referenceMap));
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  private static int bucketsOf(HashMap<?, ?> hashMap) throws Exception {
    Field tableField = HashMap.class.getDeclaredField("table");
    tableField.setAccessible(true);
    Object[] table = (Object[]) tableField.get(hashMap);
    // In JDK8, table is set lazily, so it may be null.
    return table == null ? 0 : table.length;
  }

  public void testCapacityForLargeSizes() {
    int[] largeExpectedSizes =
        new int[] {
          Integer.MAX_VALUE / 2 - 1,
          Integer.MAX_VALUE / 2,
          Integer.MAX_VALUE / 2 + 1,
          Integer.MAX_VALUE - 1,
          Integer.MAX_VALUE
        };
    for (int expectedSize : largeExpectedSizes) {
      int capacity = Maps.capacity(expectedSize);
      assertTrue(
          "capacity (" + capacity + ") must be >= expectedSize (" + expectedSize + ")",
          capacity >= expectedSize);
    }
  }

  public void testLinkedHashMap() {
    @SuppressWarnings("UseCollectionConstructor") // test of factory method
    LinkedHashMap<Integer, Integer> map = Maps.newLinkedHashMap();
    assertEquals(emptyMap(), map);
  }

  public void testLinkedHashMapWithInitialMap() {
    Map<String, String> map =
        new LinkedHashMap<String, String>(
            ImmutableMap.of(
                "Hello", "World",
                "first", "second",
                "polygene", "lubricants",
                "alpha", "betical"));

    @SuppressWarnings("UseCollectionConstructor") // test of factory method
    LinkedHashMap<String, String> copy = Maps.newLinkedHashMap(map);

    Iterator<Entry<String, String>> iter = copy.entrySet().iterator();
    assertTrue(iter.hasNext());
    Entry<String, String> entry = iter.next();
    assertThat(entry.getKey()).isEqualTo("Hello");
    assertThat(entry.getValue()).isEqualTo("World");
    assertTrue(iter.hasNext());

    entry = iter.next();
    assertThat(entry.getKey()).isEqualTo("first");
    assertThat(entry.getValue()).isEqualTo("second");
    assertTrue(iter.hasNext());

    entry = iter.next();
    assertThat(entry.getKey()).isEqualTo("polygene");
    assertThat(entry.getValue()).isEqualTo("lubricants");
    assertTrue(iter.hasNext());

    entry = iter.next();
    assertThat(entry.getKey()).isEqualTo("alpha");
    assertThat(entry.getValue()).isEqualTo("betical");
    assertFalse(iter.hasNext());
  }

  public void testLinkedHashMapGeneralizesTypes() {
    Map<String, Integer> original = new LinkedHashMap<>();
    original.put("a", 1);
    original.put("b", 2);
    original.put("c", 3);
    @SuppressWarnings("UseCollectionConstructor") // test of factory method
    HashMap<Object, Object> map = Maps.newLinkedHashMap(original);
    assertEquals(original, map);
  }

  // Intentionally using IdentityHashMap to test creation.
  @SuppressWarnings("IdentityHashMapBoxing")
  public void testIdentityHashMap() {
    IdentityHashMap<Integer, Integer> map = Maps.newIdentityHashMap();
    assertEquals(emptyMap(), map);
  }

  public void testConcurrentMap() {
    ConcurrentMap<Integer, Integer> map = Maps.newConcurrentMap();
    assertEquals(emptyMap(), map);
  }

  public void testTreeMap() {
    TreeMap<Integer, Integer> map = Maps.newTreeMap();
    assertEquals(emptyMap(), map);
    assertThat(map.comparator()).isNull();
  }

  public void testTreeMapDerived() {
    TreeMap<Derived, Integer> map = Maps.newTreeMap();
    assertEquals(emptyMap(), map);
    map.put(new Derived("foo"), 1);
    map.put(new Derived("bar"), 2);
    assertThat(map.keySet()).containsExactly(new Derived("bar"), new Derived("foo")).inOrder();
    assertThat(map.values()).containsExactly(2, 1).inOrder();
    assertThat(map.comparator()).isNull();
  }

  public void testTreeMapNonGeneric() {
    TreeMap<LegacyComparable, Integer> map = Maps.newTreeMap();
    assertEquals(emptyMap(), map);
    map.put(new LegacyComparable("foo"), 1);
    map.put(new LegacyComparable("bar"), 2);
    assertThat(map.keySet())
        .containsExactly(new LegacyComparable("bar"), new LegacyComparable("foo"))
        .inOrder();
    assertThat(map.values()).containsExactly(2, 1).inOrder();
    assertThat(map.comparator()).isNull();
  }

  public void testTreeMapWithComparator() {
    TreeMap<Integer, Integer> map = Maps.newTreeMap(SOME_COMPARATOR);
    assertEquals(emptyMap(), map);
    assertThat(map.comparator()).isEqualTo(SOME_COMPARATOR);
  }

  public void testTreeMapWithInitialMap() {
    SortedMap<Integer, Integer> map = Maps.newTreeMap();
    map.put(5, 10);
    map.put(3, 20);
    map.put(1, 30);
    TreeMap<Integer, Integer> copy = Maps.newTreeMap(map);
    assertEquals(copy, map);
    assertThat(map.comparator()).isEqualTo(copy.comparator());
  }

  public enum SomeEnum {
    SOME_INSTANCE
  }

  public void testEnumMap() {
    EnumMap<SomeEnum, Integer> map = Maps.newEnumMap(SomeEnum.class);
    assertEquals(emptyMap(), map);
    map.put(SomeEnum.SOME_INSTANCE, 0);
    assertEquals(singletonMap(SomeEnum.SOME_INSTANCE, 0), map);
  }

  public void testEnumMapNullClass() {
    assertThrows(
        NullPointerException.class,
        () -> Maps.<SomeEnum, Long>newEnumMap((Class<MapsTest.SomeEnum>) null));
  }

  public void testEnumMapWithInitialEnumMap() {
    EnumMap<SomeEnum, Integer> original = Maps.newEnumMap(SomeEnum.class);
    original.put(SomeEnum.SOME_INSTANCE, 0);
    EnumMap<SomeEnum, Integer> copy = Maps.newEnumMap(original);
    assertEquals(original, copy);
  }

  public void testEnumMapWithInitialEmptyEnumMap() {
    EnumMap<SomeEnum, Integer> original = Maps.newEnumMap(SomeEnum.class);
    EnumMap<SomeEnum, Integer> copy = Maps.newEnumMap(original);
    assertEquals(original, copy);
    assertThat(copy).isNotSameInstanceAs(original);
  }

  public void testEnumMapWithInitialMap() {
    HashMap<SomeEnum, Integer> original = new HashMap<>();
    original.put(SomeEnum.SOME_INSTANCE, 0);
    EnumMap<SomeEnum, Integer> copy = Maps.newEnumMap(original);
    assertEquals(original, copy);
  }

  public void testEnumMapWithInitialEmptyMap() {
    Map<SomeEnum, Integer> original = new HashMap<>();
    assertThrows(IllegalArgumentException.class, () -> Maps.newEnumMap(original));
  }

  public void testToStringImplWithNullKeys() throws Exception {
    Map<@Nullable String, String> hashmap = new HashMap<>();
    hashmap.put("foo", "bar");
    hashmap.put(null, "baz");

    assertThat(Maps.toStringImpl(hashmap)).isEqualTo(hashmap.toString());
  }

  public void testToStringImplWithNullValues() throws Exception {
    Map<String, @Nullable String> hashmap = new HashMap<>();
    hashmap.put("foo", "bar");
    hashmap.put("baz", null);

    assertThat(Maps.toStringImpl(hashmap)).isEqualTo(hashmap.toString());
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointerExceptions() {
    new NullPointerTester().testAllPublicStaticMethods(Maps.class);
  }

  private static final Map<Integer, Integer> EMPTY = emptyMap();
  private static final Map<Integer, Integer> SINGLETON = singletonMap(1, 2);

  public void testMapDifferenceEmptyEmpty() {
    MapDifference<Integer, Integer> diff = Maps.difference(EMPTY, EMPTY);
    assertTrue(diff.areEqual());
    assertEquals(EMPTY, diff.entriesOnlyOnLeft());
    assertEquals(EMPTY, diff.entriesOnlyOnRight());
    assertEquals(EMPTY, diff.entriesInCommon());
    assertEquals(EMPTY, diff.entriesDiffering());
    assertThat(diff.toString()).isEqualTo("equal");
  }

  public void testMapDifferenceEmptySingleton() {
    MapDifference<Integer, Integer> diff = Maps.difference(EMPTY, SINGLETON);
    assertFalse(diff.areEqual());
    assertEquals(EMPTY, diff.entriesOnlyOnLeft());
    assertEquals(SINGLETON, diff.entriesOnlyOnRight());
    assertEquals(EMPTY, diff.entriesInCommon());
    assertEquals(EMPTY, diff.entriesDiffering());
    assertThat(diff.toString()).isEqualTo("not equal: only on right={1=2}");
  }

  public void testMapDifferenceSingletonEmpty() {
    MapDifference<Integer, Integer> diff = Maps.difference(SINGLETON, EMPTY);
    assertFalse(diff.areEqual());
    assertEquals(SINGLETON, diff.entriesOnlyOnLeft());
    assertEquals(EMPTY, diff.entriesOnlyOnRight());
    assertEquals(EMPTY, diff.entriesInCommon());
    assertEquals(EMPTY, diff.entriesDiffering());
    assertThat(diff.toString()).isEqualTo("not equal: only on left={1=2}");
  }

  public void testMapDifferenceTypical() {
    Map<Integer, String> left = ImmutableMap.of(1, "a", 2, "b", 3, "c", 4, "d", 5, "e");
    Map<Integer, String> right = ImmutableMap.of(1, "a", 3, "f", 5, "g", 6, "z");

    MapDifference<Integer, String> diff1 = Maps.difference(left, right);
    assertFalse(diff1.areEqual());
    assertEquals(ImmutableMap.of(2, "b", 4, "d"), diff1.entriesOnlyOnLeft());
    assertEquals(ImmutableMap.of(6, "z"), diff1.entriesOnlyOnRight());
    assertEquals(ImmutableMap.of(1, "a"), diff1.entriesInCommon());
    assertEquals(
        ImmutableMap.of(
            3, ValueDifferenceImpl.create("c", "f"), 5, ValueDifferenceImpl.create("e", "g")),
        diff1.entriesDiffering());
    assertThat(diff1.toString())
        .isEqualTo(
            "not equal: only on left={2=b, 4=d}: only on right={6=z}: "
                + "value differences={3=(c, f), 5=(e, g)}");

    MapDifference<Integer, String> diff2 = Maps.difference(right, left);
    assertFalse(diff2.areEqual());
    assertEquals(ImmutableMap.of(6, "z"), diff2.entriesOnlyOnLeft());
    assertEquals(ImmutableMap.of(2, "b", 4, "d"), diff2.entriesOnlyOnRight());
    assertEquals(ImmutableMap.of(1, "a"), diff2.entriesInCommon());
    assertEquals(
        ImmutableMap.of(
            3, ValueDifferenceImpl.create("f", "c"), 5, ValueDifferenceImpl.create("g", "e")),
        diff2.entriesDiffering());
    assertThat(diff2.toString())
        .isEqualTo(
            "not equal: only on left={6=z}: only on right={2=b, 4=d}: "
                + "value differences={3=(f, c), 5=(g, e)}");
  }

  public void testMapDifferenceEquals() {
    Map<Integer, String> left = ImmutableMap.of(1, "a", 2, "b", 3, "c", 4, "d", 5, "e");
    Map<Integer, String> right = ImmutableMap.of(1, "a", 3, "f", 5, "g", 6, "z");
    Map<Integer, String> right2 = ImmutableMap.of(1, "a", 3, "h", 5, "g", 6, "z");
    MapDifference<Integer, String> original = Maps.difference(left, right);
    MapDifference<Integer, String> same = Maps.difference(left, right);
    MapDifference<Integer, String> reverse = Maps.difference(right, left);
    MapDifference<Integer, String> diff2 = Maps.difference(left, right2);

    new EqualsTester()
        .addEqualityGroup(original, same)
        .addEqualityGroup(reverse)
        .addEqualityGroup(diff2)
        .testEquals();
  }

  public void testMapDifferencePredicateTypical() {
    Map<Integer, String> left = ImmutableMap.of(1, "a", 2, "b", 3, "c", 4, "d", 5, "e");
    Map<Integer, String> right = ImmutableMap.of(1, "A", 3, "F", 5, "G", 6, "Z");

    // TODO(kevinb): replace with Ascii.caseInsensitiveEquivalence() when it
    // exists
    Equivalence<String> caseInsensitiveEquivalence =
        Equivalence.equals()
            .onResultOf(
                new Function<String, String>() {
                  @Override
                  public String apply(String input) {
                    return input.toLowerCase();
                  }
                });

    MapDifference<Integer, String> diff1 = Maps.difference(left, right, caseInsensitiveEquivalence);
    assertFalse(diff1.areEqual());
    assertEquals(ImmutableMap.of(2, "b", 4, "d"), diff1.entriesOnlyOnLeft());
    assertEquals(ImmutableMap.of(6, "Z"), diff1.entriesOnlyOnRight());
    assertEquals(ImmutableMap.of(1, "a"), diff1.entriesInCommon());
    assertEquals(
        ImmutableMap.of(
            3, ValueDifferenceImpl.create("c", "F"), 5, ValueDifferenceImpl.create("e", "G")),
        diff1.entriesDiffering());
    assertThat(diff1.toString())
        .isEqualTo(
            "not equal: only on left={2=b, 4=d}: only on right={6=Z}: "
                + "value differences={3=(c, F), 5=(e, G)}");

    MapDifference<Integer, String> diff2 = Maps.difference(right, left, caseInsensitiveEquivalence);
    assertFalse(diff2.areEqual());
    assertEquals(ImmutableMap.of(6, "Z"), diff2.entriesOnlyOnLeft());
    assertEquals(ImmutableMap.of(2, "b", 4, "d"), diff2.entriesOnlyOnRight());
    assertEquals(ImmutableMap.of(1, "A"), diff2.entriesInCommon());
    assertEquals(
        ImmutableMap.of(
            3, ValueDifferenceImpl.create("F", "c"), 5, ValueDifferenceImpl.create("G", "e")),
        diff2.entriesDiffering());
    assertThat(diff2.toString())
        .isEqualTo(
            "not equal: only on left={6=Z}: only on right={2=b, 4=d}: "
                + "value differences={3=(F, c), 5=(G, e)}");
  }

  private static final SortedMap<Integer, Integer> SORTED_EMPTY = Maps.newTreeMap();
  private static final ImmutableSortedMap<Integer, Integer> SORTED_SINGLETON =
      ImmutableSortedMap.of(1, 2);

  public void testMapDifferenceOfSortedMapIsSorted() {
    Map<Integer, Integer> map = SORTED_SINGLETON;
    MapDifference<Integer, Integer> difference = Maps.difference(map, EMPTY);
    assertTrue(difference instanceof SortedMapDifference);
  }

  public void testSortedMapDifferenceEmptyEmpty() {
    SortedMapDifference<Integer, Integer> diff = Maps.difference(SORTED_EMPTY, SORTED_EMPTY);
    assertTrue(diff.areEqual());
    assertEquals(SORTED_EMPTY, diff.entriesOnlyOnLeft());
    assertEquals(SORTED_EMPTY, diff.entriesOnlyOnRight());
    assertEquals(SORTED_EMPTY, diff.entriesInCommon());
    assertEquals(SORTED_EMPTY, diff.entriesDiffering());
    assertThat(diff.toString()).isEqualTo("equal");
  }

  public void testSortedMapDifferenceEmptySingleton() {
    SortedMapDifference<Integer, Integer> diff = Maps.difference(SORTED_EMPTY, SORTED_SINGLETON);
    assertFalse(diff.areEqual());
    assertEquals(SORTED_EMPTY, diff.entriesOnlyOnLeft());
    assertEquals(SORTED_SINGLETON, diff.entriesOnlyOnRight());
    assertEquals(SORTED_EMPTY, diff.entriesInCommon());
    assertEquals(SORTED_EMPTY, diff.entriesDiffering());
    assertThat(diff.toString()).isEqualTo("not equal: only on right={1=2}");
  }

  public void testSortedMapDifferenceSingletonEmpty() {
    SortedMapDifference<Integer, Integer> diff = Maps.difference(SORTED_SINGLETON, SORTED_EMPTY);
    assertFalse(diff.areEqual());
    assertEquals(SORTED_SINGLETON, diff.entriesOnlyOnLeft());
    assertEquals(SORTED_EMPTY, diff.entriesOnlyOnRight());
    assertEquals(SORTED_EMPTY, diff.entriesInCommon());
    assertEquals(SORTED_EMPTY, diff.entriesDiffering());
    assertThat(diff.toString()).isEqualTo("not equal: only on left={1=2}");
  }

  public void testSortedMapDifferenceTypical() {
    SortedMap<Integer, String> left =
        ImmutableSortedMap.<Integer, String>reverseOrder()
            .put(1, "a")
            .put(2, "b")
            .put(3, "c")
            .put(4, "d")
            .put(5, "e")
            .build();

    SortedMap<Integer, String> right = ImmutableSortedMap.of(1, "a", 3, "f", 5, "g", 6, "z");

    SortedMapDifference<Integer, String> diff1 = Maps.difference(left, right);
    assertFalse(diff1.areEqual());
    assertThat(diff1.entriesOnlyOnLeft().entrySet())
        .containsExactly(immutableEntry(4, "d"), immutableEntry(2, "b"))
        .inOrder();
    assertThat(diff1.entriesOnlyOnRight().entrySet()).contains(immutableEntry(6, "z"));
    assertThat(diff1.entriesInCommon().entrySet()).contains(immutableEntry(1, "a"));
    assertThat(diff1.entriesDiffering().entrySet())
        .containsExactly(
            immutableEntry(5, ValueDifferenceImpl.create("e", "g")),
            immutableEntry(3, ValueDifferenceImpl.create("c", "f")))
        .inOrder();
    assertThat(diff1.toString())
        .isEqualTo(
            "not equal: only on left={4=d, 2=b}: only on right={6=z}: "
                + "value differences={5=(e, g), 3=(c, f)}");

    SortedMapDifference<Integer, String> diff2 = Maps.difference(right, left);
    assertFalse(diff2.areEqual());
    assertThat(diff2.entriesOnlyOnLeft().entrySet()).contains(immutableEntry(6, "z"));
    assertThat(diff2.entriesOnlyOnRight().entrySet())
        .containsExactly(immutableEntry(2, "b"), immutableEntry(4, "d"))
        .inOrder();
    assertThat(diff1.entriesInCommon().entrySet()).contains(immutableEntry(1, "a"));
    assertEquals(
        ImmutableMap.of(
            3, ValueDifferenceImpl.create("f", "c"),
            5, ValueDifferenceImpl.create("g", "e")),
        diff2.entriesDiffering());
    assertThat(diff2.toString())
        .isEqualTo(
            "not equal: only on left={6=z}: only on right={2=b, 4=d}: "
                + "value differences={3=(f, c), 5=(g, e)}");
  }

  public void testSortedMapDifferenceImmutable() {
    SortedMap<Integer, String> left =
        Maps.newTreeMap(ImmutableSortedMap.of(1, "a", 2, "b", 3, "c", 4, "d", 5, "e"));
    SortedMap<Integer, String> right =
        Maps.newTreeMap(ImmutableSortedMap.of(1, "a", 3, "f", 5, "g", 6, "z"));

    SortedMapDifference<Integer, String> diff1 = Maps.difference(left, right);
    left.put(6, "z");
    assertFalse(diff1.areEqual());
    assertThat(diff1.entriesOnlyOnLeft().entrySet())
        .containsExactly(immutableEntry(2, "b"), immutableEntry(4, "d"))
        .inOrder();
    assertThat(diff1.entriesOnlyOnRight().entrySet()).contains(immutableEntry(6, "z"));
    assertThat(diff1.entriesInCommon().entrySet()).contains(immutableEntry(1, "a"));
    assertThat(diff1.entriesDiffering().entrySet())
        .containsExactly(
            immutableEntry(3, ValueDifferenceImpl.create("c", "f")),
            immutableEntry(5, ValueDifferenceImpl.create("e", "g")))
        .inOrder();
    assertThrows(UnsupportedOperationException.class, () -> diff1.entriesInCommon().put(7, "x"));
    assertThrows(UnsupportedOperationException.class, () -> diff1.entriesOnlyOnLeft().put(7, "x"));
    assertThrows(UnsupportedOperationException.class, () -> diff1.entriesOnlyOnRight().put(7, "x"));
  }

  public void testSortedMapDifferenceEquals() {
    SortedMap<Integer, String> left = ImmutableSortedMap.of(1, "a", 2, "b", 3, "c", 4, "d", 5, "e");
    SortedMap<Integer, String> right = ImmutableSortedMap.of(1, "a", 3, "f", 5, "g", 6, "z");
    SortedMap<Integer, String> right2 = ImmutableSortedMap.of(1, "a", 3, "h", 5, "g", 6, "z");
    SortedMapDifference<Integer, String> original = Maps.difference(left, right);
    SortedMapDifference<Integer, String> same = Maps.difference(left, right);
    SortedMapDifference<Integer, String> reverse = Maps.difference(right, left);
    SortedMapDifference<Integer, String> diff2 = Maps.difference(left, right2);

    new EqualsTester()
        .addEqualityGroup(original, same)
        .addEqualityGroup(reverse)
        .addEqualityGroup(diff2)
        .testEquals();
  }

  private static final Function<String, Integer> LENGTH_FUNCTION =
      new Function<String, Integer>() {
        @Override
        public Integer apply(String input) {
          return input.length();
        }
      };

  public void testAsMap() {
    Set<String> strings = ImmutableSet.of("one", "two", "three");
    Map<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertEquals(ImmutableMap.of("one", 3, "two", 3, "three", 5), map);
    assertEquals(Integer.valueOf(5), map.get("three"));
    assertThat(map.get("five")).isNull();
    assertThat(map.entrySet())
        .containsExactly(mapEntry("one", 3), mapEntry("two", 3), mapEntry("three", 5))
        .inOrder();
  }

  public void testAsMapReadsThrough() {
    Set<String> strings = new LinkedHashSet<>();
    Collections.addAll(strings, "one", "two", "three");
    Map<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertEquals(ImmutableMap.of("one", 3, "two", 3, "three", 5), map);
    assertThat(map.get("four")).isNull();
    strings.add("four");
    assertEquals(ImmutableMap.of("one", 3, "two", 3, "three", 5, "four", 4), map);
    assertEquals(Integer.valueOf(4), map.get("four"));
  }

  public void testAsMapWritesThrough() {
    Set<String> strings = new LinkedHashSet<>();
    Collections.addAll(strings, "one", "two", "three");
    Map<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertEquals(ImmutableMap.of("one", 3, "two", 3, "three", 5), map);
    assertEquals(Integer.valueOf(3), map.remove("two"));
    assertThat(strings).containsExactly("one", "three").inOrder();
  }

  public void testAsMapEmpty() {
    Set<String> strings = ImmutableSet.of();
    Map<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertThat(map.entrySet()).isEmpty();
    assertTrue(map.isEmpty());
    assertThat(map.get("five")).isNull();
  }

  private static class NonNavigableSortedSet extends ForwardingSortedSet<String> {
    private final SortedSet<String> delegate = newTreeSet();

    @Override
    protected SortedSet<String> delegate() {
      return delegate;
    }
  }

  public void testAsMapSorted() {
    SortedSet<String> strings = new NonNavigableSortedSet();
    Collections.addAll(strings, "one", "two", "three");
    SortedMap<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertEquals(ImmutableMap.of("one", 3, "two", 3, "three", 5), map);
    assertEquals(Integer.valueOf(5), map.get("three"));
    assertThat(map.get("five")).isNull();
    assertThat(map.entrySet())
        .containsExactly(mapEntry("one", 3), mapEntry("three", 5), mapEntry("two", 3))
        .inOrder();
    assertThat(map.tailMap("onea").entrySet())
        .containsExactly(mapEntry("three", 5), mapEntry("two", 3))
        .inOrder();
    assertThat(map.subMap("one", "two").entrySet())
        .containsExactly(mapEntry("one", 3), mapEntry("three", 5))
        .inOrder();
  }

  public void testAsMapSortedReadsThrough() {
    SortedSet<String> strings = new NonNavigableSortedSet();
    Collections.addAll(strings, "one", "two", "three");
    SortedMap<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertThat(map.comparator()).isNull();
    assertEquals(ImmutableSortedMap.of("one", 3, "two", 3, "three", 5), map);
    assertThat(map.get("four")).isNull();
    strings.add("four");
    assertEquals(ImmutableSortedMap.of("one", 3, "two", 3, "three", 5, "four", 4), map);
    assertEquals(Integer.valueOf(4), map.get("four"));
    SortedMap<String, Integer> headMap = map.headMap("two");
    assertEquals(ImmutableSortedMap.of("four", 4, "one", 3, "three", 5), headMap);
    strings.add("five");
    strings.remove("one");
    assertEquals(ImmutableSortedMap.of("five", 4, "four", 4, "three", 5), headMap);
    assertThat(map.entrySet())
        .containsExactly(
            mapEntry("five", 4), mapEntry("four", 4), mapEntry("three", 5), mapEntry("two", 3))
        .inOrder();
  }

  public void testAsMapSortedWritesThrough() {
    SortedSet<String> strings = new NonNavigableSortedSet();
    Collections.addAll(strings, "one", "two", "three");
    SortedMap<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertEquals(ImmutableMap.of("one", 3, "two", 3, "three", 5), map);
    assertEquals(Integer.valueOf(3), map.remove("two"));
    assertThat(strings).containsExactly("one", "three").inOrder();
  }

  public void testAsMapSortedSubViewKeySetsDoNotSupportAdd() {
    SortedMap<String, Integer> map = Maps.asMap(new NonNavigableSortedSet(), LENGTH_FUNCTION);
    assertThrows(UnsupportedOperationException.class, () -> map.subMap("a", "z").keySet().add("a"));
    assertThrows(UnsupportedOperationException.class, () -> map.tailMap("a").keySet().add("a"));
    assertThrows(UnsupportedOperationException.class, () -> map.headMap("r").keySet().add("a"));
    assertThrows(
        UnsupportedOperationException.class, () -> map.headMap("r").tailMap("m").keySet().add("a"));
  }

  public void testAsMapSortedEmpty() {
    SortedSet<String> strings = new NonNavigableSortedSet();
    SortedMap<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertThat(map.entrySet()).isEmpty();
    assertTrue(map.isEmpty());
    assertThat(map.get("five")).isNull();
  }

  @GwtIncompatible // NavigableMap
  public void testAsMapNavigable() {
    NavigableSet<String> strings = newTreeSet(asList("one", "two", "three"));
    NavigableMap<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertEquals(ImmutableMap.of("one", 3, "two", 3, "three", 5), map);
    assertEquals(Integer.valueOf(5), map.get("three"));
    assertThat(map.get("five")).isNull();
    assertThat(map.entrySet())
        .containsExactly(mapEntry("one", 3), mapEntry("three", 5), mapEntry("two", 3))
        .inOrder();
    assertThat(map.tailMap("onea").entrySet())
        .containsExactly(mapEntry("three", 5), mapEntry("two", 3))
        .inOrder();
    assertThat(map.subMap("one", "two").entrySet())
        .containsExactly(mapEntry("one", 3), mapEntry("three", 5))
        .inOrder();

    assertEquals(ImmutableSortedMap.of("two", 3, "three", 5), map.tailMap("three", true));
    assertEquals(ImmutableSortedMap.of("one", 3, "three", 5), map.headMap("two", false));
    assertEquals(ImmutableSortedMap.of("three", 5), map.subMap("one", false, "tr", true));

    assertThat(map.higherKey("one")).isEqualTo("three");
    assertThat(map.higherKey("r")).isEqualTo("three");
    assertThat(map.ceilingKey("r")).isEqualTo("three");
    assertThat(map.ceilingKey("one")).isEqualTo("one");
    assertEquals(mapEntry("three", 5), map.higherEntry("one"));
    assertEquals(mapEntry("one", 3), map.ceilingEntry("one"));
    assertThat(map.lowerKey("three")).isEqualTo("one");
    assertThat(map.lowerKey("r")).isEqualTo("one");
    assertThat(map.floorKey("r")).isEqualTo("one");
    assertThat(map.floorKey("three")).isEqualTo("three");

    assertThat(map.descendingMap().entrySet())
        .containsExactly(mapEntry("two", 3), mapEntry("three", 5), mapEntry("one", 3))
        .inOrder();
    assertEquals(map.headMap("three", true), map.descendingMap().tailMap("three", true));
    assertThat(map.tailMap("three", false).entrySet()).contains(mapEntry("two", 3));
    assertThat(map.tailMap("three", true).lowerEntry("three")).isNull();
    assertThat(map.headMap("two", false).values()).containsExactly(3, 5).inOrder();
    assertThat(map.headMap("two", false).descendingMap().values()).containsExactly(5, 3).inOrder();
    assertThat(map.descendingKeySet()).containsExactly("two", "three", "one").inOrder();

    assertEquals(mapEntry("one", 3), map.pollFirstEntry());
    assertEquals(mapEntry("two", 3), map.pollLastEntry());
    assertEquals(1, map.size());
  }

  @GwtIncompatible // NavigableMap
  public void testAsMapNavigableReadsThrough() {
    NavigableSet<String> strings = newTreeSet();
    Collections.addAll(strings, "one", "two", "three");
    NavigableMap<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertThat(map.comparator()).isNull();
    assertEquals(ImmutableSortedMap.of("one", 3, "two", 3, "three", 5), map);
    assertThat(map.get("four")).isNull();
    strings.add("four");
    assertEquals(ImmutableSortedMap.of("one", 3, "two", 3, "three", 5, "four", 4), map);
    assertEquals(Integer.valueOf(4), map.get("four"));
    SortedMap<String, Integer> headMap = map.headMap("two");
    assertEquals(ImmutableSortedMap.of("four", 4, "one", 3, "three", 5), headMap);
    strings.add("five");
    strings.remove("one");
    assertEquals(ImmutableSortedMap.of("five", 4, "four", 4, "three", 5), headMap);
    assertThat(map.entrySet())
        .containsExactly(
            mapEntry("five", 4), mapEntry("four", 4), mapEntry("three", 5), mapEntry("two", 3))
        .inOrder();

    NavigableMap<String, Integer> tailMap = map.tailMap("s", true);
    NavigableMap<String, Integer> subMap = map.subMap("a", true, "t", false);

    strings.add("six");
    strings.remove("two");
    assertThat(tailMap.entrySet())
        .containsExactly(mapEntry("six", 3), mapEntry("three", 5))
        .inOrder();
    assertThat(subMap.entrySet())
        .containsExactly(mapEntry("five", 4), mapEntry("four", 4), mapEntry("six", 3))
        .inOrder();
  }

  @GwtIncompatible // NavigableMap
  public void testAsMapNavigableWritesThrough() {
    NavigableSet<String> strings = newTreeSet();
    Collections.addAll(strings, "one", "two", "three");
    NavigableMap<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertEquals(ImmutableMap.of("one", 3, "two", 3, "three", 5), map);
    assertEquals(Integer.valueOf(3), map.remove("two"));
    assertThat(strings).containsExactly("one", "three").inOrder();
    assertEquals(mapEntry("three", 5), map.subMap("one", false, "zzz", true).pollLastEntry());
    assertThat(strings).contains("one");
  }

  @GwtIncompatible // NavigableMap
  public void testAsMapNavigableSubViewKeySetsDoNotSupportAdd() {
    NavigableMap<String, Integer> map = Maps.asMap(Sets.newTreeSet(), LENGTH_FUNCTION);
    assertThrows(UnsupportedOperationException.class, () -> map.descendingKeySet().add("a"));
    assertThrows(
        UnsupportedOperationException.class,
        () -> map.subMap("a", true, "z", false).keySet().add("a"));
    assertThrows(
        UnsupportedOperationException.class, () -> map.tailMap("a", true).keySet().add("a"));
    assertThrows(
        UnsupportedOperationException.class, () -> map.headMap("r", true).keySet().add("a"));
    assertThrows(
        UnsupportedOperationException.class,
        () -> map.headMap("r", false).tailMap("m", true).keySet().add("a"));
  }

  @GwtIncompatible // NavigableMap
  public void testAsMapNavigableEmpty() {
    NavigableSet<String> strings = ImmutableSortedSet.of();
    NavigableMap<String, Integer> map = Maps.asMap(strings, LENGTH_FUNCTION);
    assertThat(map.entrySet()).isEmpty();
    assertTrue(map.isEmpty());
    assertThat(map.get("five")).isNull();
  }

  public void testToMap() {
    Iterable<String> strings = ImmutableList.of("one", "two", "three");
    ImmutableMap<String, Integer> map = toMap(strings, LENGTH_FUNCTION);
    assertEquals(ImmutableMap.of("one", 3, "two", 3, "three", 5), map);
    assertThat(map.entrySet())
        .containsExactly(mapEntry("one", 3), mapEntry("two", 3), mapEntry("three", 5))
        .inOrder();
  }

  public void testToMapIterator() {
    Iterator<String> strings = ImmutableList.of("one", "two", "three").iterator();
    ImmutableMap<String, Integer> map = toMap(strings, LENGTH_FUNCTION);
    assertEquals(ImmutableMap.of("one", 3, "two", 3, "three", 5), map);
    assertThat(map.entrySet())
        .containsExactly(mapEntry("one", 3), mapEntry("two", 3), mapEntry("three", 5))
        .inOrder();
  }

  public void testToMapWithDuplicateKeys() {
    Iterable<String> strings = ImmutableList.of("one", "two", "three", "two", "one");
    ImmutableMap<String, Integer> map = toMap(strings, LENGTH_FUNCTION);
    assertEquals(ImmutableMap.of("one", 3, "two", 3, "three", 5), map);
    assertThat(map.entrySet())
        .containsExactly(mapEntry("one", 3), mapEntry("two", 3), mapEntry("three", 5))
        .inOrder();
  }

  public void testToMapWithNullKeys() {
    Iterable<@Nullable String> strings = asList("one", null, "three");
    assertThrows(
        NullPointerException.class,
        () -> toMap((Iterable<String>) strings, Functions.constant("foo")));
  }

  public void testToMapWithNullValues() {
    Iterable<String> strings = ImmutableList.of("one", "two", "three");
    assertThrows(NullPointerException.class, () -> toMap(strings, Functions.constant(null)));
  }

  private static final ImmutableBiMap<Integer, String> INT_TO_STRING_MAP =
      new ImmutableBiMap.Builder<Integer, String>()
          .put(1, "one")
          .put(2, "two")
          .put(3, "three")
          .build();

  public void testUniqueIndexCollection() {
    ImmutableMap<Integer, String> outputMap =
        Maps.uniqueIndex(INT_TO_STRING_MAP.values(), Functions.forMap(INT_TO_STRING_MAP.inverse()));
    assertEquals(INT_TO_STRING_MAP, outputMap);
  }

  public void testUniqueIndexIterable() {
    ImmutableMap<Integer, String> outputMap =
        Maps.uniqueIndex(
            new Iterable<String>() {
              @Override
              public Iterator<String> iterator() {
                return INT_TO_STRING_MAP.values().iterator();
              }
            },
            Functions.forMap(INT_TO_STRING_MAP.inverse()));
    assertEquals(INT_TO_STRING_MAP, outputMap);
  }

  public void testUniqueIndexIterator() {
    ImmutableMap<Integer, String> outputMap =
        Maps.uniqueIndex(
            INT_TO_STRING_MAP.values().iterator(), Functions.forMap(INT_TO_STRING_MAP.inverse()));
    assertEquals(INT_TO_STRING_MAP, outputMap);
  }

  /** Can't create the map if more than one value maps to the same key. */
  public void testUniqueIndexDuplicates() {
    IllegalArgumentException expected =
        assertThrows(
            IllegalArgumentException.class,
            () -> Maps.uniqueIndex(ImmutableSet.of("one", "uno"), Functions.constant(1)));
    assertThat(expected).hasMessageThat().contains("Multimaps.index");
  }

  /** Null values are not allowed. */
  public void testUniqueIndexNullValue() {
    List<@Nullable String> listWithNull = Lists.newArrayList((String) null);
    assertThrows(
        NullPointerException.class,
        () -> Maps.uniqueIndex((List<String>) listWithNull, Functions.constant(1)));
  }

  /** Null keys aren't allowed either. */
  public void testUniqueIndexNullKey() {
    List<String> oneStringList = Lists.newArrayList("foo");
    assertThrows(
        NullPointerException.class,
        () -> Maps.uniqueIndex(oneStringList, Functions.constant(null)));
  }

  @J2ktIncompatible
  @GwtIncompatible // Maps.fromProperties
  public void testFromProperties() throws IOException {
    Properties testProp = new Properties();

    Map<String, String> result = Maps.fromProperties(testProp);
    assertTrue(result.isEmpty());
    testProp.setProperty("first", "true");

    result = Maps.fromProperties(testProp);
    assertThat(result.get("first")).isEqualTo("true");
    assertEquals(1, result.size());
    testProp.setProperty("second", "null");

    result = Maps.fromProperties(testProp);
    assertThat(result.get("first")).isEqualTo("true");
    assertThat(result.get("second")).isEqualTo("null");
    assertEquals(2, result.size());

    // Now test values loaded from a stream.
    String props = "test\n second = 2\n Third item :   a short  phrase   ";

    testProp.load(new StringReader(props));

    result = Maps.fromProperties(testProp);
    assertEquals(4, result.size());
    assertThat(result.get("first")).isEqualTo("true");
    assertThat(result.get("test")).isEqualTo("");
    assertThat(result.get("second")).isEqualTo("2");
    assertThat(result.get("Third")).isEqualTo("item :   a short  phrase   ");
    assertFalse(result.containsKey("not here"));

    // Test loading system properties
    result = Maps.fromProperties(System.getProperties());
    assertTrue(result.containsKey("java.version"));

    // Test that defaults work, too.
    testProp = new Properties(System.getProperties());
    String override = "test\njava.version : hidden";

    testProp.load(new StringReader(override));

    result = Maps.fromProperties(testProp);
    assertThat(result.size()).isGreaterThan(2);
    assertThat(result.get("test")).isEqualTo("");
    assertThat(result.get("java.version")).isEqualTo("hidden");
    assertThat(result.get("java.version")).isNotEqualTo(System.getProperty("java.version"));
  }

  @J2ktIncompatible
  @GwtIncompatible // Maps.fromProperties
  public void testFromPropertiesNullKey() {
    Properties properties =
        new Properties() {
          @Override
          public Enumeration<?> propertyNames() {
            return Iterators.asEnumeration(asList(null, "first", "second").iterator());
          }
        };
    properties.setProperty("first", "true");
    properties.setProperty("second", "null");

    assertThrows(NullPointerException.class, () -> Maps.fromProperties(properties));
  }

  @J2ktIncompatible
  @GwtIncompatible // Maps.fromProperties
  public void testFromPropertiesNonStringKeys() {
    Properties properties =
        new Properties() {
          @Override
          public Enumeration<?> propertyNames() {
            return Iterators.asEnumeration(
                Arrays.<Object>asList(Integer.valueOf(123), "first").iterator());
          }
        };

    assertThrows(ClassCastException.class, () -> Maps.fromProperties(properties));
  }

  public void testAsConverter_nominal() throws Exception {
    ImmutableBiMap<String, Integer> biMap =
        ImmutableBiMap.of(
            "one", 1,
            "two", 2);
    Converter<String, Integer> converter = Maps.asConverter(biMap);
    for (Entry<String, Integer> entry : biMap.entrySet()) {
      assertThat(converter.convert(entry.getKey())).isEqualTo(entry.getValue());
    }
  }

  public void testAsConverter_inverse() throws Exception {
    ImmutableBiMap<String, Integer> biMap =
        ImmutableBiMap.of(
            "one", 1,
            "two", 2);
    Converter<String, Integer> converter = Maps.asConverter(biMap);
    for (Entry<String, Integer> entry : biMap.entrySet()) {
      assertThat(converter.reverse().convert(entry.getValue())).isEqualTo(entry.getKey());
    }
  }

  public void testAsConverter_noMapping() throws Exception {
    ImmutableBiMap<String, Integer> biMap =
        ImmutableBiMap.of(
            "one", 1,
            "two", 2);
    Converter<String, Integer> converter = Maps.asConverter(biMap);
    assertThrows(IllegalArgumentException.class, () -> converter.convert("three"));
  }

  public void testAsConverter_nullConversions() throws Exception {
    ImmutableBiMap<String, Integer> biMap =
        ImmutableBiMap.of(
            "one", 1,
            "two", 2);
    Converter<String, Integer> converter = Maps.asConverter(biMap);
    assertThat(converter.convert(null)).isNull();
    assertThat(converter.reverse().convert(null)).isNull();
  }

  public void testAsConverter_isAView() throws Exception {
    BiMap<String, Integer> biMap = HashBiMap.create();
    biMap.put("one", 1);
    biMap.put("two", 2);
    Converter<String, Integer> converter = Maps.asConverter(biMap);

    assertEquals((Integer) 1, converter.convert("one"));
    assertEquals((Integer) 2, converter.convert("two"));
    assertThrows(IllegalArgumentException.class, () -> converter.convert("three"));

    biMap.put("three", 3);

    assertEquals((Integer) 1, converter.convert("one"));
    assertEquals((Integer) 2, converter.convert("two"));
    assertEquals((Integer) 3, converter.convert("three"));
  }

  public void testAsConverter_withNullMapping() throws Exception {
    BiMap<String, @Nullable Integer> biMap = HashBiMap.create();
    biMap.put("one", 1);
    biMap.put("two", 2);
    biMap.put("three", null);
    assertThrows(
        IllegalArgumentException.class,
        () -> Maps.asConverter((BiMap<String, Integer>) biMap).convert("three"));
  }

  public void testAsConverter_toString() {
    ImmutableBiMap<String, Integer> biMap =
        ImmutableBiMap.of(
            "one", 1,
            "two", 2);
    Converter<String, Integer> converter = Maps.asConverter(biMap);
    assertThat(converter.toString()).isEqualTo("Maps.asConverter({one=1, two=2})");
  }

  public void testAsConverter_serialization() {
    ImmutableBiMap<String, Integer> biMap =
        ImmutableBiMap.of(
            "one", 1,
            "two", 2);
    Converter<String, Integer> converter = Maps.asConverter(biMap);
    reserializeAndAssert(converter);
  }

  public void testUnmodifiableBiMap() {
    BiMap<Integer, String> mod = HashBiMap.create();
    mod.put(1, "one");
    mod.put(2, "two");
    mod.put(3, "three");

    BiMap<Number, String> unmod = Maps.unmodifiableBiMap(mod);

    /* No aliasing on inverse operations. */
    assertThat(unmod.inverse()).isSameInstanceAs(unmod.inverse());
    assertThat(unmod.inverse().inverse()).isSameInstanceAs(unmod);

    /* Unmodifiable is a view. */
    mod.put(4, "four");
    assertEquals(true, unmod.get(4).equals("four"));
    assertEquals(true, unmod.inverse().get("four").equals(4));

    /* UnsupportedOperationException on direct modifications. */
    assertThrows(UnsupportedOperationException.class, () -> unmod.put(4, "four"));
    assertThrows(UnsupportedOperationException.class, () -> unmod.forcePut(4, "four"));
    assertThrows(UnsupportedOperationException.class, () -> unmod.putAll(singletonMap(4, "four")));
    assertThrows(UnsupportedOperationException.class, () -> unmod.clear());

    /* UnsupportedOperationException on indirect modifications. */
    BiMap<String, Number> inverse = unmod.inverse();
    assertThrows(UnsupportedOperationException.class, () -> inverse.put("four", 4));
    assertThrows(UnsupportedOperationException.class, () -> inverse.forcePut("four", 4));
    assertThrows(
        UnsupportedOperationException.class, () -> inverse.putAll(singletonMap("four", 4)));
    Set<String> values = unmod.values();
    assertThrows(UnsupportedOperationException.class, () -> values.remove("four"));
    Set<Entry<Number, String>> entries = unmod.entrySet();
    Entry<Number, String> entry = entries.iterator().next();
    assertThrows(UnsupportedOperationException.class, () -> entry.setValue("four"));
    @SuppressWarnings("unchecked")
    Entry<Integer, String> entry2 = (Entry<Integer, String>) entries.toArray()[0];
    assertThrows(UnsupportedOperationException.class, () -> entry2.setValue("four"));
  }

  public void testImmutableEntry() {
    Entry<String, Integer> e = immutableEntry("foo", 1);
    assertThat(e.getKey()).isEqualTo("foo");
    assertEquals(1, (int) e.getValue());
    assertThrows(UnsupportedOperationException.class, () -> e.setValue(2));
    assertThat(e.toString()).isEqualTo("foo=1");
    assertEquals(101575, e.hashCode());
  }

  public void testImmutableEntryNull() {
    Entry<@Nullable String, @Nullable Integer> e = immutableEntry((String) null, (Integer) null);
    assertThat(e.getKey()).isNull();
    assertThat(e.getValue()).isNull();
    assertThrows(UnsupportedOperationException.class, () -> e.setValue(null));
    assertThat(e.toString()).isEqualTo("null=null");
    assertEquals(0, e.hashCode());
  }

  /** See {@link SynchronizedBiMapTest} for more tests. */
  @J2ktIncompatible // Synchronized
  public void testSynchronizedBiMap() {
    BiMap<String, Integer> bimap = HashBiMap.create();
    bimap.put("one", 1);
    BiMap<String, Integer> sync = Maps.synchronizedBiMap(bimap);
    bimap.put("two", 2);
    sync.put("three", 3);
    assertEquals(ImmutableSet.of(1, 2, 3), bimap.inverse().keySet());
    assertEquals(ImmutableSet.of(1, 2, 3), sync.inverse().keySet());
  }

  private static final Function<Integer, Double> SQRT_FUNCTION = in -> Math.sqrt(in);

  public void testTransformValues() {
    Map<String, Integer> map = ImmutableMap.of("a", 4, "b", 9);
    Map<String, Double> transformed = transformValues(map, SQRT_FUNCTION);

    assertEquals(ImmutableMap.of("a", 2.0, "b", 3.0), transformed);
  }

  public void testTransformEntries() {
    Map<String, String> map = ImmutableMap.of("a", "4", "b", "9");
    EntryTransformer<String, String, String> concat =
        new EntryTransformer<String, String, String>() {
          @Override
          public String transformEntry(String key, String value) {
            return key + value;
          }
        };
    Map<String, String> transformed = transformEntries(map, concat);

    assertEquals(ImmutableMap.of("a", "a4", "b", "b9"), transformed);
  }

  @SuppressWarnings("unused")
  public void testTransformEntriesGenerics() {
    Map<Object, Object> map1 = ImmutableMap.of(1, 2);
    Map<Object, Number> map2 = ImmutableMap.of(1, 2);
    Map<Object, Integer> map3 = ImmutableMap.of(1, 2);
    Map<Number, Object> map4 = ImmutableMap.of(1, 2);
    Map<Number, Number> map5 = ImmutableMap.of(1, 2);
    Map<Number, Integer> map6 = ImmutableMap.of(1, 2);
    Map<Integer, Object> map7 = ImmutableMap.of(1, 2);
    Map<Integer, Number> map8 = ImmutableMap.of(1, 2);
    Map<Integer, Integer> map9 = ImmutableMap.of(1, 2);
    Map<? extends Number, ? extends Number> map0 = ImmutableMap.of(1, 2);

    EntryTransformer<Number, Number, Double> transformer =
        new EntryTransformer<Number, Number, Double>() {
          @Override
          public Double transformEntry(Number key, Number value) {
            return key.doubleValue() + value.doubleValue();
          }
        };

    Map<Object, Double> objectKeyed;
    Map<Number, Double> numberKeyed;
    Map<Integer, Double> integerKeyed;

    numberKeyed = transformEntries(map5, transformer);
    numberKeyed = transformEntries(map6, transformer);
    integerKeyed = transformEntries(map8, transformer);
    integerKeyed = transformEntries(map9, transformer);

    Map<? extends Number, Double> wildcarded = transformEntries(map0, transformer);

    // Can't loosen the key type:
    // objectKeyed = transformEntries(map5, transformer);
    // objectKeyed = transformEntries(map6, transformer);
    // objectKeyed = transformEntries(map8, transformer);
    // objectKeyed = transformEntries(map9, transformer);
    // numberKeyed = transformEntries(map8, transformer);
    // numberKeyed = transformEntries(map9, transformer);

    // Can't loosen the value type:
    // Map<Number, Number> looseValued1 = transformEntries(map5, transformer);
    // Map<Number, Number> looseValued2 = transformEntries(map6, transformer);
    // Map<Integer, Number> looseValued3 = transformEntries(map8, transformer);
    // Map<Integer, Number> looseValued4 = transformEntries(map9, transformer);

    // Can't call with too loose a key:
    // transformEntries(map1, transformer);
    // transformEntries(map2, transformer);
    // transformEntries(map3, transformer);

    // Can't call with too loose a value:
    // transformEntries(map1, transformer);
    // transformEntries(map4, transformer);
    // transformEntries(map7, transformer);
  }

  public void testTransformEntriesExample() {
    Map<String, Boolean> options = ImmutableMap.of("verbose", true, "sort", false);
    EntryTransformer<String, Boolean, String> flagPrefixer =
        new EntryTransformer<String, Boolean, String>() {
          @Override
          public String transformEntry(String key, Boolean value) {
            return value ? key : "no" + key;
          }
        };
    Map<String, String> transformed = transformEntries(options, flagPrefixer);
    assertThat(transformed.toString()).isEqualTo("{verbose=verbose, sort=nosort}");
  }

  // Logically this would accept a NavigableMap, but that won't work under GWT.
  private static <K, V> SortedMap<K, V> sortedNotNavigable(SortedMap<K, V> map) {
    return new ForwardingSortedMap<K, V>() {
      @Override
      protected SortedMap<K, V> delegate() {
        return map;
      }
    };
  }

  public void testSortedMapTransformValues() {
    SortedMap<String, Integer> map = sortedNotNavigable(ImmutableSortedMap.of("a", 4, "b", 9));
    SortedMap<String, Double> transformed = transformValues(map, SQRT_FUNCTION);

    /*
     * We'd like to sanity check that we didn't get a NavigableMap out, but we
     * can't easily do so while maintaining GWT compatibility.
     */
    assertEquals(ImmutableSortedMap.of("a", 2.0, "b", 3.0), transformed);
  }

  @GwtIncompatible // NavigableMap
  public void testNavigableMapTransformValues() {
    NavigableMap<String, Integer> map = ImmutableSortedMap.of("a", 4, "b", 9);
    NavigableMap<String, Double> transformed = transformValues(map, SQRT_FUNCTION);

    assertEquals(ImmutableSortedMap.of("a", 2.0, "b", 3.0), transformed);
  }

  public void testSortedMapTransformEntries() {
    SortedMap<String, String> map = sortedNotNavigable(ImmutableSortedMap.of("a", "4", "b", "9"));
    EntryTransformer<String, String, String> concat =
        new EntryTransformer<String, String, String>() {
          @Override
          public String transformEntry(String key, String value) {
            return key + value;
          }
        };
    SortedMap<String, String> transformed = transformEntries(map, concat);

    /*
     * We'd like to sanity check that we didn't get a NavigableMap out, but we
     * can't easily do so while maintaining GWT compatibility.
     */
    assertEquals(ImmutableSortedMap.of("a", "a4", "b", "b9"), transformed);
  }

  @GwtIncompatible // NavigableMap
  public void testNavigableMapTransformEntries() {
    NavigableMap<String, String> map = ImmutableSortedMap.of("a", "4", "b", "9");
    EntryTransformer<String, String, String> concat =
        new EntryTransformer<String, String, String>() {
          @Override
          public String transformEntry(String key, String value) {
            return key + value;
          }
        };
    NavigableMap<String, String> transformed = transformEntries(map, concat);

    assertEquals(ImmutableSortedMap.of("a", "a4", "b", "b9"), transformed);
  }

  @GwtIncompatible // NavigableMap
  public void testUnmodifiableNavigableMap() {
    TreeMap<Integer, String> mod = Maps.newTreeMap();
    mod.put(1, "one");
    mod.put(2, "two");
    mod.put(3, "three");

    NavigableMap<Integer, String> unmod = unmodifiableNavigableMap(mod);

    /* unmod is a view. */
    mod.put(4, "four");
    assertThat(unmod.get(4)).isEqualTo("four");
    assertThat(unmod.descendingMap().get(4)).isEqualTo("four");

    ensureNotDirectlyModifiable(unmod);
    ensureNotDirectlyModifiable(unmod.descendingMap());
    ensureNotDirectlyModifiable(unmod.headMap(2, true));
    ensureNotDirectlyModifiable(unmod.subMap(1, true, 3, true));
    ensureNotDirectlyModifiable(unmod.tailMap(2, true));

    Collection<String> values = unmod.values();
    assertThrows(UnsupportedOperationException.class, () -> values.add("4"));
    assertThrows(UnsupportedOperationException.class, () -> values.remove("four"));
    assertThrows(UnsupportedOperationException.class, () -> values.removeAll(singleton("four")));
    assertThrows(UnsupportedOperationException.class, () -> values.retainAll(singleton("four")));
    Iterator<String> valuesIterator = values.iterator();
    valuesIterator.next();
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          valuesIterator.remove();
        });

    Set<Entry<Integer, String>> entries = unmod.entrySet();
    Iterator<Entry<Integer, String>> entriesIterator = entries.iterator();
    entriesIterator.next();
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          entriesIterator.remove();
        });
    {
      Entry<Integer, String> entry = entries.iterator().next();
      assertThrows(UnsupportedOperationException.class, () -> entry.setValue("four"));
    }
    {
      Entry<Integer, String> entry = unmod.lowerEntry(1);
      assertThat(entry).isNull();
    }
    {
      Entry<Integer, String> entry = unmod.floorEntry(2);
      assertThrows(UnsupportedOperationException.class, () -> entry.setValue("four"));
    }
    {
      Entry<Integer, String> entry = unmod.ceilingEntry(2);
      assertThrows(UnsupportedOperationException.class, () -> entry.setValue("four"));
    }
    {
      Entry<Integer, String> entry = unmod.lowerEntry(2);
      assertThrows(UnsupportedOperationException.class, () -> entry.setValue("four"));
    }
    {
      Entry<Integer, String> entry = unmod.higherEntry(2);
      assertThrows(UnsupportedOperationException.class, () -> entry.setValue("four"));
    }
    {
      Entry<Integer, String> entry = unmod.firstEntry();
      assertThrows(UnsupportedOperationException.class, () -> entry.setValue("four"));
    }
    {
      Entry<Integer, String> entry = unmod.lastEntry();
      assertThrows(UnsupportedOperationException.class, () -> entry.setValue("four"));
    }
    {
      @SuppressWarnings("unchecked")
      Entry<Integer, String> entry = (Entry<Integer, String>) entries.toArray()[0];
      assertThrows(UnsupportedOperationException.class, () -> entry.setValue("four"));
    }
  }

  @GwtIncompatible // NavigableMap
  void ensureNotDirectlyModifiable(NavigableMap<Integer, String> unmod) {
    assertThrows(UnsupportedOperationException.class, () -> unmod.put(4, "four"));
    assertThrows(UnsupportedOperationException.class, () -> unmod.putAll(singletonMap(4, "four")));
    assertThrows(UnsupportedOperationException.class, () -> unmod.remove(4));
    assertThrows(UnsupportedOperationException.class, () -> unmod.pollFirstEntry());
    assertThrows(UnsupportedOperationException.class, () -> unmod.pollLastEntry());
    assertThrows(UnsupportedOperationException.class, () -> unmod.clear());
  }

  @GwtIncompatible // NavigableMap
  public void testSubMap_boundedRange() {
    ImmutableSortedMap<Integer, Integer> map = ImmutableSortedMap.of(2, 0, 4, 0, 6, 0, 8, 0, 10, 0);
    ImmutableSortedMap<Integer, Integer> empty = ImmutableSortedMap.of();

    assertEquals(map, Maps.subMap(map, Range.closed(0, 12)));
    assertEquals(ImmutableSortedMap.of(2, 0, 4, 0), Maps.subMap(map, Range.closed(0, 4)));
    assertEquals(ImmutableSortedMap.of(2, 0, 4, 0, 6, 0), Maps.subMap(map, Range.closed(2, 6)));
    assertEquals(ImmutableSortedMap.of(4, 0, 6, 0), Maps.subMap(map, Range.closed(3, 7)));
    assertEquals(empty, Maps.subMap(map, Range.closed(20, 30)));

    assertEquals(map, Maps.subMap(map, Range.open(0, 12)));
    assertEquals(ImmutableSortedMap.of(2, 0), Maps.subMap(map, Range.open(0, 4)));
    assertEquals(ImmutableSortedMap.of(4, 0), Maps.subMap(map, Range.open(2, 6)));
    assertEquals(ImmutableSortedMap.of(4, 0, 6, 0), Maps.subMap(map, Range.open(3, 7)));
    assertEquals(empty, Maps.subMap(map, Range.open(20, 30)));

    assertEquals(map, Maps.subMap(map, Range.openClosed(0, 12)));
    assertEquals(ImmutableSortedMap.of(2, 0, 4, 0), Maps.subMap(map, Range.openClosed(0, 4)));
    assertEquals(ImmutableSortedMap.of(4, 0, 6, 0), Maps.subMap(map, Range.openClosed(2, 6)));
    assertEquals(ImmutableSortedMap.of(4, 0, 6, 0), Maps.subMap(map, Range.openClosed(3, 7)));
    assertEquals(empty, Maps.subMap(map, Range.openClosed(20, 30)));

    assertEquals(map, Maps.subMap(map, Range.closedOpen(0, 12)));
    assertEquals(ImmutableSortedMap.of(2, 0), Maps.subMap(map, Range.closedOpen(0, 4)));
    assertEquals(ImmutableSortedMap.of(2, 0, 4, 0), Maps.subMap(map, Range.closedOpen(2, 6)));
    assertEquals(ImmutableSortedMap.of(4, 0, 6, 0), Maps.subMap(map, Range.closedOpen(3, 7)));
    assertEquals(empty, Maps.subMap(map, Range.closedOpen(20, 30)));
  }

  @GwtIncompatible // NavigableMap
  public void testSubMap_halfBoundedRange() {
    ImmutableSortedMap<Integer, Integer> map = ImmutableSortedMap.of(2, 0, 4, 0, 6, 0, 8, 0, 10, 0);
    ImmutableSortedMap<Integer, Integer> empty = ImmutableSortedMap.of();

    assertEquals(map, Maps.subMap(map, Range.atLeast(0)));
    assertEquals(
        ImmutableSortedMap.of(4, 0, 6, 0, 8, 0, 10, 0), Maps.subMap(map, Range.atLeast(4)));
    assertEquals(ImmutableSortedMap.of(8, 0, 10, 0), Maps.subMap(map, Range.atLeast(7)));
    assertEquals(empty, Maps.subMap(map, Range.atLeast(20)));

    assertEquals(map, Maps.subMap(map, Range.greaterThan(0)));
    assertEquals(ImmutableSortedMap.of(6, 0, 8, 0, 10, 0), Maps.subMap(map, Range.greaterThan(4)));
    assertEquals(ImmutableSortedMap.of(8, 0, 10, 0), Maps.subMap(map, Range.greaterThan(7)));
    assertEquals(empty, Maps.subMap(map, Range.greaterThan(20)));

    assertEquals(empty, Maps.subMap(map, Range.lessThan(0)));
    assertEquals(ImmutableSortedMap.of(2, 0), Maps.subMap(map, Range.lessThan(4)));
    assertEquals(ImmutableSortedMap.of(2, 0, 4, 0, 6, 0), Maps.subMap(map, Range.lessThan(7)));
    assertEquals(map, Maps.subMap(map, Range.lessThan(20)));

    assertEquals(empty, Maps.subMap(map, Range.atMost(0)));
    assertEquals(ImmutableSortedMap.of(2, 0, 4, 0), Maps.subMap(map, Range.atMost(4)));
    assertEquals(ImmutableSortedMap.of(2, 0, 4, 0, 6, 0), Maps.subMap(map, Range.atMost(7)));
    assertEquals(map, Maps.subMap(map, Range.atMost(20)));
  }

  @GwtIncompatible // NavigableMap
  public void testSubMap_unboundedRange() {
    ImmutableSortedMap<Integer, Integer> map = ImmutableSortedMap.of(2, 0, 4, 0, 6, 0, 8, 0, 10, 0);

    assertEquals(map, Maps.subMap(map, Range.all()));
  }

  @GwtIncompatible // NavigableMap
  public void testSubMap_unnaturalOrdering() {
    ImmutableSortedMap<Integer, Integer> map =
        ImmutableSortedMap.<Integer, Integer>reverseOrder()
            .put(2, 0)
            .put(4, 0)
            .put(6, 0)
            .put(8, 0)
            .put(10, 0)
            .build();

    assertThrows(IllegalArgumentException.class, () -> Maps.subMap(map, Range.closed(4, 8)));

    // These results are all incorrect, but there's no way (short of iterating over the result)
    // to verify that with an arbitrary ordering or comparator.
    assertEquals(ImmutableSortedMap.of(2, 0, 4, 0), Maps.subMap(map, Range.atLeast(4)));
    assertEquals(ImmutableSortedMap.of(8, 0, 10, 0), Maps.subMap(map, Range.atMost(8)));
    assertEquals(
        ImmutableSortedMap.of(2, 0, 4, 0, 6, 0, 8, 0, 10, 0), Maps.subMap(map, Range.all()));
  }
}
