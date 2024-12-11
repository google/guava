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

import static com.google.common.collect.ImmutableSetMultimap.flatteningToImmutableSetMultimap;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;
import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_ANY_NULL_QUERIES;
import static com.google.common.primitives.Chars.asList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.emptySet;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableSetMultimap.Builder;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.SetMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringSetMultimapGenerator;
import com.google.common.collect.testing.google.UnmodifiableCollectionTests;
import com.google.common.testing.CollectorTester;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@link ImmutableSetMultimap}.
 *
 * @author Mike Ward
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public class ImmutableSetMultimapTest extends TestCase {
  private static final class ImmutableSetMultimapGenerator extends TestStringSetMultimapGenerator {
    @Override
    protected SetMultimap<String, String> create(Entry<String, String>[] entries) {
      ImmutableSetMultimap.Builder<String, String> builder = ImmutableSetMultimap.builder();
      for (Entry<String, String> entry : entries) {
        builder.put(entry.getKey(), entry.getValue());
      }
      return builder.build();
    }
  }

  private static final class ImmutableSetMultimapCopyOfEntriesGenerator
      extends TestStringSetMultimapGenerator {
    @Override
    protected SetMultimap<String, String> create(Entry<String, String>[] entries) {
      return ImmutableSetMultimap.copyOf(Arrays.asList(entries));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ImmutableSetMultimapTest.class);
    suite.addTest(
        SetMultimapTestSuiteBuilder.using(new ImmutableSetMultimapGenerator())
            .named("ImmutableSetMultimap")
            .withFeatures(ALLOWS_ANY_NULL_QUERIES, KNOWN_ORDER, SERIALIZABLE, CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        SetMultimapTestSuiteBuilder.using(new ImmutableSetMultimapCopyOfEntriesGenerator())
            .named("ImmutableSetMultimap.copyOf[Iterable<Entry>]")
            .withFeatures(ALLOWS_ANY_NULL_QUERIES, KNOWN_ORDER, SERIALIZABLE, CollectionSize.ANY)
            .createTestSuite());
    return suite;
  }

  public void testBuilderWithExpectedKeysNegative() {
    assertThrows(
        IllegalArgumentException.class, () -> ImmutableSetMultimap.builderWithExpectedKeys(-1));
  }

  public void testBuilderWithExpectedKeysZero() {
    ImmutableSetMultimap.Builder<String, String> builder =
        ImmutableSetMultimap.builderWithExpectedKeys(0);
    builder.put("key", "value");
    assertThat(builder.build().entries()).containsExactly(Maps.immutableEntry("key", "value"));
  }

  public void testBuilderWithExpectedKeysPositive() {
    ImmutableSetMultimap.Builder<String, String> builder =
        ImmutableSetMultimap.builderWithExpectedKeys(1);
    builder.put("key", "value");
    assertThat(builder.build().entries()).containsExactly(Maps.immutableEntry("key", "value"));
  }

  public void testBuilderWithExpectedValuesPerKeyNegative() {
    ImmutableSetMultimap.Builder<String, String> builder = ImmutableSetMultimap.builder();
    assertThrows(IllegalArgumentException.class, () -> builder.expectedValuesPerKey(-1));
  }

  public void testBuilderWithExpectedValuesPerKeyZero() {
    ImmutableSetMultimap.Builder<String, String> builder =
        ImmutableSetMultimap.<String, String>builder().expectedValuesPerKey(0);
    builder.put("key", "value");
    assertThat(builder.build().entries()).containsExactly(Maps.immutableEntry("key", "value"));
  }

  public void testBuilderWithExpectedValuesPerKeyPositive() {
    ImmutableSetMultimap.Builder<String, String> builder =
        ImmutableSetMultimap.<String, String>builder().expectedValuesPerKey(1);
    builder.put("key", "value");
    assertThat(builder.build().entries()).containsExactly(Maps.immutableEntry("key", "value"));
  }

  public void testBuilderWithExpectedValuesPerKeyNegativeOrderValuesBy() {
    ImmutableSetMultimap.Builder<String, String> builder =
        ImmutableSetMultimap.<String, String>builder().orderValuesBy(Ordering.natural());
    assertThrows(IllegalArgumentException.class, () -> builder.expectedValuesPerKey(-1));
  }

  public void testBuilderWithExpectedValuesPerKeyZeroOrderValuesBy() {
    ImmutableSetMultimap.Builder<String, String> builder =
        ImmutableSetMultimap.<String, String>builder()
            .orderValuesBy(Ordering.natural())
            .expectedValuesPerKey(0);
    builder.put("key", "value");
    assertThat(builder.build().entries()).containsExactly(Maps.immutableEntry("key", "value"));
  }

  public void testBuilderWithExpectedValuesPerKeyPositiveOrderValuesBy() {
    ImmutableSetMultimap.Builder<String, String> builder =
        ImmutableSetMultimap.<String, String>builder()
            .orderValuesBy(Ordering.natural())
            .expectedValuesPerKey(1);
    builder.put("key", "value");
    assertThat(builder.build().entries()).containsExactly(Maps.immutableEntry("key", "value"));
  }

  static class HashHostileComparable implements Comparable<HashHostileComparable> {
    final String string;

    public HashHostileComparable(String string) {
      this.string = string;
    }

    @Override
    public int hashCode() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(HashHostileComparable o) {
      return string.compareTo(o.string);
    }
  }

  public void testSortedBuilderWithExpectedValuesPerKeyPositive() {
    ImmutableSetMultimap.Builder<String, HashHostileComparable> builder =
        ImmutableSetMultimap.<String, HashHostileComparable>builder()
            .expectedValuesPerKey(2)
            .orderValuesBy(Ordering.natural());
    HashHostileComparable v1 = new HashHostileComparable("value1");
    HashHostileComparable v2 = new HashHostileComparable("value2");
    builder.put("key", v1);
    builder.put("key", v2);
    assertThat(builder.build().entries()).hasSize(2);
  }

  public void testBuilder_withImmutableEntry() {
    ImmutableSetMultimap<String, Integer> multimap =
        new Builder<String, Integer>().put(Maps.immutableEntry("one", 1)).build();
    assertEquals(ImmutableSet.of(1), multimap.get("one"));
  }

  public void testBuilder_withImmutableEntryAndNullContents() {
    Builder<String, Integer> builder = new Builder<>();
    assertThrows(
        NullPointerException.class, () -> builder.put(Maps.immutableEntry("one", (Integer) null)));
    assertThrows(
        NullPointerException.class, () -> builder.put(Maps.immutableEntry((String) null, 1)));
  }

  private static class StringHolder {
    @Nullable String string;
  }

  public void testBuilder_withMutableEntry() {
    ImmutableSetMultimap.Builder<String, Integer> builder = new Builder<>();
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
    assertEquals(ImmutableSet.of(1), builder.build().get("one"));
  }

  public void testBuilderPutAllIterable() {
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    builder.putAll("foo", Arrays.asList(1, 2, 3));
    builder.putAll("bar", Arrays.asList(4, 5));
    builder.putAll("foo", Arrays.asList(6, 7));
    Multimap<String, Integer> multimap = builder.build();
    assertEquals(ImmutableSet.of(1, 2, 3, 6, 7), multimap.get("foo"));
    assertEquals(ImmutableSet.of(4, 5), multimap.get("bar"));
    assertEquals(7, multimap.size());
  }

  public void testBuilderPutAllVarargs() {
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    builder.putAll("foo", 1, 2, 3);
    builder.putAll("bar", 4, 5);
    builder.putAll("foo", 6, 7);
    Multimap<String, Integer> multimap = builder.build();
    assertEquals(ImmutableSet.of(1, 2, 3, 6, 7), multimap.get("foo"));
    assertEquals(ImmutableSet.of(4, 5), multimap.get("bar"));
    assertEquals(7, multimap.size());
  }

  public void testBuilderPutAllMultimap() {
    Multimap<String, Integer> toPut = LinkedListMultimap.create();
    toPut.put("foo", 1);
    toPut.put("bar", 4);
    toPut.put("foo", 2);
    toPut.put("foo", 3);
    Multimap<String, Integer> moreToPut = LinkedListMultimap.create();
    moreToPut.put("foo", 6);
    moreToPut.put("bar", 5);
    moreToPut.put("foo", 7);
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    builder.putAll(toPut);
    builder.putAll(moreToPut);
    Multimap<String, Integer> multimap = builder.build();
    assertEquals(ImmutableSet.of(1, 2, 3, 6, 7), multimap.get("foo"));
    assertEquals(ImmutableSet.of(4, 5), multimap.get("bar"));
    assertEquals(7, multimap.size());
  }

  public void testBuilderPutAllWithDuplicates() {
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    builder.putAll("foo", 1, 2, 3);
    builder.putAll("bar", 4, 5);
    builder.putAll("foo", 1, 6, 7);
    ImmutableSetMultimap<String, Integer> multimap = builder.build();
    assertEquals(7, multimap.size());
  }

  public void testBuilderPutWithDuplicates() {
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    builder.putAll("foo", 1, 2, 3);
    builder.putAll("bar", 4, 5);
    builder.put("foo", 1);
    ImmutableSetMultimap<String, Integer> multimap = builder.build();
    assertEquals(5, multimap.size());
  }

  public void testBuilderPutAllMultimapWithDuplicates() {
    Multimap<String, Integer> toPut = LinkedListMultimap.create();
    toPut.put("foo", 1);
    toPut.put("bar", 4);
    toPut.put("foo", 2);
    toPut.put("foo", 1);
    toPut.put("bar", 5);
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    builder.putAll(toPut);
    ImmutableSetMultimap<String, Integer> multimap = builder.build();
    assertEquals(4, multimap.size());
  }

  public void testBuilderPutNullKey() {
    Multimap<@Nullable String, Integer> toPut = LinkedListMultimap.create();
    toPut.put(null, 1);
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    assertThrows(NullPointerException.class, () -> builder.put(null, 1));
    assertThrows(NullPointerException.class, () -> builder.putAll(null, Arrays.asList(1, 2, 3)));
    assertThrows(NullPointerException.class, () -> builder.putAll(null, 1, 2, 3));
    assertThrows(
        NullPointerException.class, () -> builder.putAll((Multimap<String, Integer>) toPut));
  }

  public void testBuilderPutNullValue() {
    Multimap<String, @Nullable Integer> toPut = LinkedListMultimap.create();
    toPut.put("foo", null);
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    assertThrows(NullPointerException.class, () -> builder.put("foo", null));
    assertThrows(
        NullPointerException.class, () -> builder.putAll("foo", Arrays.asList(1, null, 3)));
    assertThrows(NullPointerException.class, () -> builder.putAll("foo", 4, null, 6));
    assertThrows(
        NullPointerException.class, () -> builder.putAll((Multimap<String, Integer>) toPut));
  }

  public void testBuilderOrderKeysBy() {
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    builder.put("b", 3);
    builder.put("d", 2);
    builder.put("a", 5);
    builder.orderKeysBy(Collections.reverseOrder());
    builder.put("c", 4);
    builder.put("a", 2);
    builder.put("b", 6);
    ImmutableSetMultimap<String, Integer> multimap = builder.build();
    assertThat(multimap.keySet()).containsExactly("d", "c", "b", "a").inOrder();
    assertThat(multimap.values()).containsExactly(2, 4, 3, 6, 5, 2).inOrder();
    assertThat(multimap.get("a")).containsExactly(5, 2).inOrder();
    assertThat(multimap.get("b")).containsExactly(3, 6).inOrder();
    assertFalse(multimap.get("a") instanceof ImmutableSortedSet);
    assertFalse(multimap.get("x") instanceof ImmutableSortedSet);
    assertFalse(multimap.asMap().get("a") instanceof ImmutableSortedSet);
  }

  public void testBuilderOrderKeysByDuplicates() {
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    builder.put("bb", 3);
    builder.put("d", 2);
    builder.put("a", 5);
    builder.orderKeysBy(
        new Ordering<String>() {
          @Override
          public int compare(String left, String right) {
            return left.length() - right.length();
          }
        });
    builder.put("cc", 4);
    builder.put("a", 2);
    builder.put("bb", 6);
    ImmutableSetMultimap<String, Integer> multimap = builder.build();
    assertThat(multimap.keySet()).containsExactly("d", "a", "bb", "cc").inOrder();
    assertThat(multimap.values()).containsExactly(2, 5, 2, 3, 6, 4).inOrder();
    assertThat(multimap.get("a")).containsExactly(5, 2).inOrder();
    assertThat(multimap.get("bb")).containsExactly(3, 6).inOrder();
    assertFalse(multimap.get("a") instanceof ImmutableSortedSet);
    assertFalse(multimap.get("x") instanceof ImmutableSortedSet);
    assertFalse(multimap.asMap().get("a") instanceof ImmutableSortedSet);
  }

  public void testBuilderOrderValuesBy() {
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    builder.put("b", 3);
    builder.put("d", 2);
    builder.put("a", 5);
    builder.orderValuesBy(Collections.reverseOrder());
    builder.put("c", 4);
    builder.put("a", 2);
    builder.put("b", 6);
    ImmutableSetMultimap<String, Integer> multimap = builder.build();
    assertThat(multimap.keySet()).containsExactly("b", "d", "a", "c").inOrder();
    assertThat(multimap.values()).containsExactly(6, 3, 2, 5, 2, 4).inOrder();
    assertThat(multimap.get("a")).containsExactly(5, 2).inOrder();
    assertThat(multimap.get("b")).containsExactly(6, 3).inOrder();
    assertTrue(multimap.get("a") instanceof ImmutableSortedSet);
    assertEquals(
        Collections.reverseOrder(), ((ImmutableSortedSet<Integer>) multimap.get("a")).comparator());
    assertTrue(multimap.get("x") instanceof ImmutableSortedSet);
    assertEquals(
        Collections.reverseOrder(), ((ImmutableSortedSet<Integer>) multimap.get("x")).comparator());
    assertTrue(multimap.asMap().get("a") instanceof ImmutableSortedSet);
    assertEquals(
        Collections.reverseOrder(),
        ((ImmutableSortedSet<Integer>) multimap.asMap().get("a")).comparator());
  }

  public void testBuilderOrderKeysAndValuesBy() {
    ImmutableSetMultimap.Builder<String, Integer> builder = ImmutableSetMultimap.builder();
    builder.put("b", 3);
    builder.put("d", 2);
    builder.put("a", 5);
    builder.orderKeysBy(Collections.reverseOrder());
    builder.orderValuesBy(Collections.reverseOrder());
    builder.put("c", 4);
    builder.put("a", 2);
    builder.put("b", 6);
    ImmutableSetMultimap<String, Integer> multimap = builder.build();
    assertThat(multimap.keySet()).containsExactly("d", "c", "b", "a").inOrder();
    assertThat(multimap.values()).containsExactly(2, 4, 6, 3, 5, 2).inOrder();
    assertThat(multimap.get("a")).containsExactly(5, 2).inOrder();
    assertThat(multimap.get("b")).containsExactly(6, 3).inOrder();
    assertTrue(multimap.get("a") instanceof ImmutableSortedSet);
    assertEquals(
        Collections.reverseOrder(), ((ImmutableSortedSet<Integer>) multimap.get("a")).comparator());
    assertTrue(multimap.get("x") instanceof ImmutableSortedSet);
    assertEquals(
        Collections.reverseOrder(), ((ImmutableSortedSet<Integer>) multimap.get("x")).comparator());
    assertTrue(multimap.asMap().get("a") instanceof ImmutableSortedSet);
    assertEquals(
        Collections.reverseOrder(),
        ((ImmutableSortedSet<Integer>) multimap.asMap().get("a")).comparator());
  }

  public void testCopyOf() {
    HashMultimap<String, Integer> input = HashMultimap.create();
    input.put("foo", 1);
    input.put("bar", 2);
    input.put("foo", 3);
    Multimap<String, Integer> multimap = ImmutableSetMultimap.copyOf(input);
    assertEquals(multimap, input);
    assertEquals(input, multimap);
  }

  public void testCopyOfWithDuplicates() {
    ArrayListMultimap<Object, Object> input = ArrayListMultimap.create();
    input.put("foo", 1);
    input.put("bar", 2);
    input.put("foo", 3);
    input.put("foo", 1);
    ImmutableSetMultimap<Object, Object> copy = ImmutableSetMultimap.copyOf(input);
    assertEquals(3, copy.size());
  }

  public void testCopyOfEmpty() {
    HashMultimap<String, Integer> input = HashMultimap.create();
    Multimap<String, Integer> multimap = ImmutableSetMultimap.copyOf(input);
    assertEquals(multimap, input);
    assertEquals(input, multimap);
  }

  public void testCopyOfImmutableSetMultimap() {
    Multimap<String, Integer> multimap = createMultimap();
    assertSame(multimap, ImmutableSetMultimap.copyOf(multimap));
  }

  public void testCopyOfNullKey() {
    HashMultimap<@Nullable String, Integer> input = HashMultimap.create();
    input.put(null, 1);
    assertThrows(
        NullPointerException.class,
        () -> ImmutableSetMultimap.copyOf((Multimap<String, Integer>) input));
  }

  public void testCopyOfNullValue() {
    HashMultimap<String, @Nullable Integer> input = HashMultimap.create();
    input.putAll("foo", Arrays.<@Nullable Integer>asList(1, null, 3));
    assertThrows(
        NullPointerException.class,
        () -> ImmutableSetMultimap.copyOf((Multimap<String, Integer>) input));
  }

  public void testToImmutableSetMultimap() {
    Collector<Entry<String, Integer>, ?, ImmutableSetMultimap<String, Integer>> collector =
        toImmutableSetMultimap(Entry::getKey, Entry::getValue);
    BiPredicate<ImmutableSetMultimap<?, ?>, ImmutableSetMultimap<?, ?>> equivalence =
        Equivalence.equals()
            .onResultOf(
                (ImmutableSetMultimap<?, ?> mm) ->
                    ImmutableListMultimap.copyOf(mm).asMap().entrySet().asList())
            .and(Equivalence.equals());
    CollectorTester.of(collector, equivalence)
        .expectCollects(ImmutableSetMultimap.of())
        .expectCollects(
            ImmutableSetMultimap.of("a", 1, "b", 2, "a", 3, "c", 4),
            mapEntry("a", 1),
            mapEntry("b", 2),
            mapEntry("a", 3),
            mapEntry("c", 4));
  }

  public void testFlatteningToImmutableSetMultimap() {
    Collector<String, ?, ImmutableSetMultimap<Character, Character>> collector =
        flatteningToImmutableSetMultimap(
            str -> str.charAt(0), str -> asList(str.substring(1).toCharArray()).stream());
    BiPredicate<Multimap<?, ?>, Multimap<?, ?>> equivalence =
        Equivalence.equals()
            .onResultOf((Multimap<?, ?> mm) -> ImmutableList.copyOf(mm.asMap().entrySet()))
            .and(Equivalence.equals());
    ImmutableSetMultimap<Character, Character> empty = ImmutableSetMultimap.of();
    ImmutableSetMultimap<Character, Character> filled =
        ImmutableSetMultimap.<Character, Character>builder()
            .putAll('b', Arrays.asList('a', 'n', 'a', 'n', 'a'))
            .putAll('a', Arrays.asList('p', 'p', 'l', 'e'))
            .putAll('c', Arrays.asList('a', 'r', 'r', 'o', 't'))
            .putAll('a', Arrays.asList('s', 'p', 'a', 'r', 'a', 'g', 'u', 's'))
            .putAll('c', Arrays.asList('h', 'e', 'r', 'r', 'y'))
            .build();
    CollectorTester.of(collector, equivalence)
        .expectCollects(empty)
        .expectCollects(filled, "banana", "apple", "carrot", "asparagus", "cherry");
  }

  public void testEmptyMultimapReads() {
    Multimap<String, Integer> multimap = ImmutableSetMultimap.of();
    assertFalse(multimap.containsKey("foo"));
    assertFalse(multimap.containsValue(1));
    assertFalse(multimap.containsEntry("foo", 1));
    assertTrue(multimap.entries().isEmpty());
    assertTrue(multimap.equals(HashMultimap.create()));
    assertEquals(emptySet(), multimap.get("foo"));
    assertEquals(0, multimap.hashCode());
    assertTrue(multimap.isEmpty());
    assertEquals(HashMultiset.create(), multimap.keys());
    assertEquals(emptySet(), multimap.keySet());
    assertEquals(0, multimap.size());
    assertTrue(multimap.values().isEmpty());
    assertEquals("{}", multimap.toString());
  }

  public void testEmptyMultimapWrites() {
    Multimap<String, Integer> multimap = ImmutableSetMultimap.of();
    UnmodifiableCollectionTests.assertMultimapIsUnmodifiable(multimap, "foo", 1);
  }

  public void testMultimapReads() {
    Multimap<String, Integer> multimap = createMultimap();
    assertTrue(multimap.containsKey("foo"));
    assertFalse(multimap.containsKey("cat"));
    assertTrue(multimap.containsValue(1));
    assertFalse(multimap.containsValue(5));
    assertTrue(multimap.containsEntry("foo", 1));
    assertFalse(multimap.containsEntry("cat", 1));
    assertFalse(multimap.containsEntry("foo", 5));
    assertFalse(multimap.entries().isEmpty());
    assertEquals(3, multimap.size());
    assertFalse(multimap.isEmpty());
    assertEquals("{foo=[1, 3], bar=[2]}", multimap.toString());
  }

  public void testMultimapWrites() {
    Multimap<String, Integer> multimap = createMultimap();
    UnmodifiableCollectionTests.assertMultimapIsUnmodifiable(multimap, "bar", 2);
  }

  public void testMultimapEquals() {
    Multimap<String, Integer> multimap = createMultimap();
    Multimap<String, Integer> hashMultimap = HashMultimap.create();
    hashMultimap.putAll("foo", Arrays.asList(1, 3));
    hashMultimap.put("bar", 2);

    new EqualsTester()
        .addEqualityGroup(
            multimap,
            createMultimap(),
            hashMultimap,
            ImmutableSetMultimap.<String, Integer>builder()
                .put("bar", 2)
                .put("foo", 1)
                .put("foo", 3)
                .build(),
            ImmutableSetMultimap.<String, Integer>builder()
                .put("bar", 2)
                .put("foo", 3)
                .put("foo", 1)
                .build())
        .addEqualityGroup(
            ImmutableSetMultimap.<String, Integer>builder()
                .put("foo", 2)
                .put("foo", 3)
                .put("foo", 1)
                .build())
        .addEqualityGroup(
            ImmutableSetMultimap.<String, Integer>builder().put("bar", 2).put("foo", 3).build())
        .testEquals();
  }

  public void testOf() {
    assertMultimapEquals(ImmutableSetMultimap.of("one", 1), "one", 1);
    assertMultimapEquals(ImmutableSetMultimap.of("one", 1, "two", 2), "one", 1, "two", 2);
    assertMultimapEquals(
        ImmutableSetMultimap.of("one", 1, "two", 2, "three", 3), "one", 1, "two", 2, "three", 3);
    assertMultimapEquals(
        ImmutableSetMultimap.of("one", 1, "two", 2, "three", 3, "four", 4),
        "one",
        1,
        "two",
        2,
        "three",
        3,
        "four",
        4);
    assertMultimapEquals(
        ImmutableSetMultimap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5),
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
  }

  public void testInverse() {
    assertEquals(
        ImmutableSetMultimap.<Integer, String>of(),
        ImmutableSetMultimap.<String, Integer>of().inverse());
    assertEquals(ImmutableSetMultimap.of(1, "one"), ImmutableSetMultimap.of("one", 1).inverse());
    assertEquals(
        ImmutableSetMultimap.of(1, "one", 2, "two"),
        ImmutableSetMultimap.of("one", 1, "two", 2).inverse());
    assertEquals(
        ImmutableSetMultimap.of('o', "of", 'f', "of", 't', "to", 'o', "to"),
        ImmutableSetMultimap.of("of", 'o', "of", 'f', "to", 't', "to", 'o').inverse());
  }

  public void testInverseMinimizesWork() {
    ImmutableSetMultimap<String, Character> multimap =
        ImmutableSetMultimap.of("of", 'o', "of", 'f', "to", 't', "to", 'o');
    assertSame(multimap.inverse(), multimap.inverse());
    assertSame(multimap, multimap.inverse().inverse());
  }

  private static <K, V> void assertMultimapEquals(
      Multimap<K, V> multimap, Object... alternatingKeysAndValues) {
    assertEquals(multimap.size(), alternatingKeysAndValues.length / 2);
    int i = 0;
    for (Entry<K, V> entry : multimap.entries()) {
      assertEquals(alternatingKeysAndValues[i++], entry.getKey());
      assertEquals(alternatingKeysAndValues[i++], entry.getValue());
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testSerialization() {
    Multimap<String, Integer> multimap = createMultimap();
    SerializableTester.reserializeAndAssert(multimap);
    assertEquals(multimap.size(), SerializableTester.reserialize(multimap).size());
    SerializableTester.reserializeAndAssert(multimap.get("foo"));
    LenientSerializableTester.reserializeAndAssertLenient(multimap.keySet());
    LenientSerializableTester.reserializeAndAssertLenient(multimap.keys());
    SerializableTester.reserializeAndAssert(multimap.asMap());
    Collection<Integer> valuesCopy = SerializableTester.reserialize(multimap.values());
    assertEquals(HashMultiset.create(multimap.values()), HashMultiset.create(valuesCopy));
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testEmptySerialization() {
    Multimap<String, Integer> multimap = ImmutableSetMultimap.of();
    assertSame(multimap, SerializableTester.reserialize(multimap));
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testSortedSerialization() {
    Multimap<String, Integer> multimap =
        new ImmutableSetMultimap.Builder<String, Integer>()
            .orderKeysBy(Ordering.natural().reverse())
            .orderValuesBy(Ordering.usingToString())
            .put("a", 2)
            .put("a", 10)
            .put("b", 1)
            .build();
    multimap = SerializableTester.reserialize(multimap);
    assertThat(multimap.keySet()).containsExactly("b", "a").inOrder();
    assertThat(multimap.get("a")).containsExactly(10, 2).inOrder();
    assertEquals(
        Ordering.usingToString(), ((ImmutableSortedSet<Integer>) multimap.get("a")).comparator());
    assertEquals(
        Ordering.usingToString(), ((ImmutableSortedSet<Integer>) multimap.get("z")).comparator());
  }

  private ImmutableSetMultimap<String, Integer> createMultimap() {
    return ImmutableSetMultimap.<String, Integer>builder()
        .put("foo", 1)
        .put("bar", 2)
        .put("foo", 3)
        .build();
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(ImmutableSetMultimap.class);
    tester.ignore(ImmutableSetMultimap.class.getMethod("get", Object.class));
    tester.testAllPublicInstanceMethods(ImmutableSetMultimap.of());
    tester.testAllPublicInstanceMethods(ImmutableSetMultimap.of("a", 1));
  }
}
