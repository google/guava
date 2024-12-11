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

import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_ANY_NULL_QUERIES;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableListMultimap.Builder;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.ListMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringListMultimapGenerator;
import com.google.common.collect.testing.google.UnmodifiableCollectionTests;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@link ImmutableListMultimap}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public class ImmutableListMultimapTest extends TestCase {
  public static class ImmutableListMultimapGenerator extends TestStringListMultimapGenerator {
    @Override
    protected ListMultimap<String, String> create(Entry<String, String>[] entries) {
      ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
      for (Entry<String, String> entry : entries) {
        builder.put(entry.getKey(), entry.getValue());
      }
      return builder.build();
    }
  }

  public static class ImmutableListMultimapCopyOfEntriesGenerator
      extends TestStringListMultimapGenerator {
    @Override
    protected ListMultimap<String, String> create(Entry<String, String>[] entries) {
      return ImmutableListMultimap.copyOf(Arrays.asList(entries));
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        ListMultimapTestSuiteBuilder.using(new ImmutableListMultimapGenerator())
            .named("ImmutableListMultimap")
            .withFeatures(ALLOWS_ANY_NULL_QUERIES, SERIALIZABLE, KNOWN_ORDER, CollectionSize.ANY)
            .createTestSuite());
    suite.addTest(
        ListMultimapTestSuiteBuilder.using(new ImmutableListMultimapCopyOfEntriesGenerator())
            .named("ImmutableListMultimap.copyOf[Iterable<Entry>]")
            .withFeatures(ALLOWS_ANY_NULL_QUERIES, SERIALIZABLE, KNOWN_ORDER, CollectionSize.ANY)
            .createTestSuite());
    suite.addTestSuite(ImmutableListMultimapTest.class);
    return suite;
  }

  public void testBuilderWithExpectedKeysNegative() {
    assertThrows(
        IllegalArgumentException.class, () -> ImmutableListMultimap.builderWithExpectedKeys(-1));
  }

  public void testBuilderWithExpectedKeysZero() {
    ImmutableListMultimap.Builder<String, String> builder =
        ImmutableListMultimap.builderWithExpectedKeys(0);
    builder.put("key", "value");
    assertThat(builder.build().entries()).containsExactly(Maps.immutableEntry("key", "value"));
  }

  public void testBuilderWithExpectedKeysPositive() {
    ImmutableListMultimap.Builder<String, String> builder =
        ImmutableListMultimap.builderWithExpectedKeys(1);
    builder.put("key", "value");
    assertThat(builder.build().entries()).containsExactly(Maps.immutableEntry("key", "value"));
  }

  public void testBuilderWithExpectedValuesPerKeyNegative() {
    ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
    assertThrows(IllegalArgumentException.class, () -> builder.expectedValuesPerKey(-1));
  }

  public void testBuilderWithExpectedValuesPerKeyZero() {
    ImmutableListMultimap.Builder<String, String> builder =
        ImmutableListMultimap.<String, String>builder().expectedValuesPerKey(0);
    builder.put("key", "value");
    assertThat(builder.build().entries()).containsExactly(Maps.immutableEntry("key", "value"));
  }

  public void testBuilderWithExpectedValuesPerKeyPositive() {
    ImmutableListMultimap.Builder<String, String> builder =
        ImmutableListMultimap.<String, String>builder().expectedValuesPerKey(1);
    builder.put("key", "value");
    assertThat(builder.build().entries()).containsExactly(Maps.immutableEntry("key", "value"));
  }

  public void testBuilder_withImmutableEntry() {
    ImmutableListMultimap<String, Integer> multimap =
        new Builder<String, Integer>().put(Maps.immutableEntry("one", 1)).build();
    assertEquals(Arrays.asList(1), multimap.get("one"));
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
    ImmutableListMultimap.Builder<String, Integer> builder = new Builder<>();
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
    assertEquals(Arrays.asList(1), builder.build().get("one"));
  }

  public void testBuilderPutAllIterable() {
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
    builder.putAll("foo", Arrays.asList(1, 2, 3));
    builder.putAll("bar", Arrays.asList(4, 5));
    builder.putAll("foo", Arrays.asList(6, 7));
    Multimap<String, Integer> multimap = builder.build();
    assertThat(multimap.get("foo")).containsExactly(1, 2, 3, 6, 7).inOrder();
    assertThat(multimap.get("bar")).containsExactly(4, 5).inOrder();
    assertEquals(7, multimap.size());
  }

  public void testBuilderPutAllVarargs() {
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
    builder.putAll("foo", 1, 2, 3);
    builder.putAll("bar", 4, 5);
    builder.putAll("foo", 6, 7);
    ImmutableListMultimap<String, Integer> multimap = builder.build();
    assertThat(multimap.get("foo")).containsExactly(1, 2, 3, 6, 7).inOrder();
    assertThat(multimap.get("bar")).containsExactly(4, 5).inOrder();
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
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
    builder.putAll(toPut);
    builder.putAll(moreToPut);
    ImmutableListMultimap<String, Integer> multimap = builder.build();
    assertThat(multimap.get("foo")).containsExactly(1, 2, 3, 6, 7).inOrder();
    assertThat(multimap.get("bar")).containsExactly(4, 5).inOrder();
    assertEquals(7, multimap.size());
  }

  public void testBuilderPutAllWithDuplicates() {
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
    builder.putAll("foo", 1, 2, 3);
    builder.putAll("bar", 4, 5);
    builder.putAll("foo", 1, 6, 7);
    ImmutableListMultimap<String, Integer> multimap = builder.build();
    assertEquals(Arrays.asList(1, 2, 3, 1, 6, 7), multimap.get("foo"));
    assertEquals(Arrays.asList(4, 5), multimap.get("bar"));
    assertEquals(8, multimap.size());
  }

  public void testBuilderPutWithDuplicates() {
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
    builder.putAll("foo", 1, 2, 3);
    builder.putAll("bar", 4, 5);
    builder.put("foo", 1);
    ImmutableListMultimap<String, Integer> multimap = builder.build();
    assertEquals(Arrays.asList(1, 2, 3, 1), multimap.get("foo"));
    assertEquals(Arrays.asList(4, 5), multimap.get("bar"));
    assertEquals(6, multimap.size());
  }

  public void testBuilderPutAllMultimapWithDuplicates() {
    Multimap<String, Integer> toPut = LinkedListMultimap.create();
    toPut.put("foo", 1);
    toPut.put("bar", 4);
    toPut.put("foo", 2);
    toPut.put("foo", 1);
    toPut.put("bar", 5);
    Multimap<String, Integer> moreToPut = LinkedListMultimap.create();
    moreToPut.put("foo", 6);
    moreToPut.put("bar", 4);
    moreToPut.put("foo", 7);
    moreToPut.put("foo", 2);
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
    builder.putAll(toPut);
    builder.putAll(moreToPut);
    ImmutableListMultimap<String, Integer> multimap = builder.build();
    assertThat(multimap.get("foo")).containsExactly(1, 2, 1, 6, 7, 2).inOrder();
    assertThat(multimap.get("bar")).containsExactly(4, 5, 4).inOrder();
    assertEquals(9, multimap.size());
  }

  public void testBuilderPutNullKey() {
    Multimap<@Nullable String, Integer> toPut = LinkedListMultimap.create();
    toPut.put(null, 1);
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
    assertThrows(NullPointerException.class, () -> builder.put(null, 1));
    assertThrows(NullPointerException.class, () -> builder.putAll(null, Arrays.asList(1, 2, 3)));
    assertThrows(NullPointerException.class, () -> builder.putAll(null, 1, 2, 3));
    assertThrows(
        NullPointerException.class, () -> builder.putAll((Multimap<String, Integer>) toPut));
  }

  public void testBuilderPutNullValue() {
    Multimap<String, @Nullable Integer> toPut = LinkedListMultimap.create();
    toPut.put("foo", null);
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
    assertThrows(NullPointerException.class, () -> builder.put("foo", null));
    assertThrows(
        NullPointerException.class, () -> builder.putAll("foo", Arrays.asList(1, null, 3)));
    assertThrows(NullPointerException.class, () -> builder.putAll("foo", 1, null, 3));
    assertThrows(
        NullPointerException.class, () -> builder.putAll((Multimap<String, Integer>) toPut));
  }

  public void testBuilderOrderKeysBy() {
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
    builder.put("b", 3);
    builder.put("d", 2);
    builder.put("a", 5);
    builder.orderKeysBy(Collections.reverseOrder());
    builder.put("c", 4);
    builder.put("a", 2);
    builder.put("b", 6);
    ImmutableListMultimap<String, Integer> multimap = builder.build();
    assertThat(multimap.keySet()).containsExactly("d", "c", "b", "a").inOrder();
    assertThat(multimap.values()).containsExactly(2, 4, 3, 6, 5, 2).inOrder();
    assertThat(multimap.get("a")).containsExactly(5, 2).inOrder();
    assertThat(multimap.get("b")).containsExactly(3, 6).inOrder();
  }

  public void testBuilderOrderKeysByDuplicates() {
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
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
    ImmutableListMultimap<String, Integer> multimap = builder.build();
    assertThat(multimap.keySet()).containsExactly("d", "a", "bb", "cc").inOrder();
    assertThat(multimap.values()).containsExactly(2, 5, 2, 3, 6, 4).inOrder();
    assertThat(multimap.get("a")).containsExactly(5, 2).inOrder();
    assertThat(multimap.get("bb")).containsExactly(3, 6).inOrder();
  }

  public void testBuilderOrderValuesBy() {
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
    builder.put("b", 3);
    builder.put("d", 2);
    builder.put("a", 5);
    builder.orderValuesBy(Collections.reverseOrder());
    builder.put("c", 4);
    builder.put("a", 2);
    builder.put("b", 6);
    ImmutableListMultimap<String, Integer> multimap = builder.build();
    assertThat(multimap.keySet()).containsExactly("b", "d", "a", "c").inOrder();
    assertThat(multimap.values()).containsExactly(6, 3, 2, 5, 2, 4).inOrder();
    assertThat(multimap.get("a")).containsExactly(5, 2).inOrder();
    assertThat(multimap.get("b")).containsExactly(6, 3).inOrder();
  }

  public void testBuilderOrderKeysAndValuesBy() {
    ImmutableListMultimap.Builder<String, Integer> builder = ImmutableListMultimap.builder();
    builder.put("b", 3);
    builder.put("d", 2);
    builder.put("a", 5);
    builder.orderKeysBy(Collections.reverseOrder());
    builder.orderValuesBy(Collections.reverseOrder());
    builder.put("c", 4);
    builder.put("a", 2);
    builder.put("b", 6);
    ImmutableListMultimap<String, Integer> multimap = builder.build();
    assertThat(multimap.keySet()).containsExactly("d", "c", "b", "a").inOrder();
    assertThat(multimap.values()).containsExactly(2, 4, 6, 3, 5, 2).inOrder();
    assertThat(multimap.get("a")).containsExactly(5, 2).inOrder();
    assertThat(multimap.get("b")).containsExactly(6, 3).inOrder();
  }

  public void testCopyOf() {
    ArrayListMultimap<String, Integer> input = ArrayListMultimap.create();
    input.put("foo", 1);
    input.put("bar", 2);
    input.put("foo", 3);
    ImmutableListMultimap<String, Integer> multimap = ImmutableListMultimap.copyOf(input);
    new EqualsTester().addEqualityGroup(input, multimap).testEquals();
  }

  public void testCopyOfWithDuplicates() {
    ArrayListMultimap<String, Integer> input = ArrayListMultimap.create();
    input.put("foo", 1);
    input.put("bar", 2);
    input.put("foo", 3);
    input.put("foo", 1);
    ImmutableListMultimap<String, Integer> multimap = ImmutableListMultimap.copyOf(input);
    new EqualsTester().addEqualityGroup(input, multimap).testEquals();
  }

  public void testCopyOfEmpty() {
    ArrayListMultimap<String, Integer> input = ArrayListMultimap.create();
    ImmutableListMultimap<String, Integer> multimap = ImmutableListMultimap.copyOf(input);
    new EqualsTester().addEqualityGroup(input, multimap).testEquals();
  }

  public void testCopyOfImmutableListMultimap() {
    Multimap<String, Integer> multimap = createMultimap();
    assertSame(multimap, ImmutableListMultimap.copyOf(multimap));
  }

  public void testCopyOfNullKey() {
    ArrayListMultimap<@Nullable String, Integer> input = ArrayListMultimap.create();
    input.put(null, 1);
    assertThrows(
        NullPointerException.class,
        () -> ImmutableListMultimap.copyOf((ArrayListMultimap<String, Integer>) input));
  }

  public void testCopyOfNullValue() {
    ArrayListMultimap<String, @Nullable Integer> input = ArrayListMultimap.create();
    input.putAll("foo", Arrays.<@Nullable Integer>asList(1, null, 3));
    assertThrows(
        NullPointerException.class,
        () -> ImmutableListMultimap.copyOf((ArrayListMultimap<String, Integer>) input));
  }

  // TODO(b/172823566): Use mainline testToImmutableListMultimap once CollectorTester is usable.
  public void testToImmutableListMultimap_java7_combine() {
    ImmutableListMultimap.Builder<String, Integer> zis =
        ImmutableListMultimap.<String, Integer>builder().put("a", 1).put("b", 2);
    ImmutableListMultimap.Builder<String, Integer> zat =
        ImmutableListMultimap.<String, Integer>builder().put("a", 3).put("c", 4);
    ImmutableListMultimap<String, Integer> multimap = zis.combine(zat).build();
    assertThat(multimap.keySet()).containsExactly("a", "b", "c").inOrder();
    assertThat(multimap.values()).containsExactly(1, 3, 2, 4).inOrder();
    assertThat(multimap.get("a")).containsExactly(1, 3).inOrder();
    assertThat(multimap.get("b")).containsExactly(2);
    assertThat(multimap.get("c")).containsExactly(4);
  }

  public void testEmptyMultimapReads() {
    ImmutableListMultimap<String, Integer> multimap = ImmutableListMultimap.of();
    assertFalse(multimap.containsKey("foo"));
    assertFalse(multimap.containsValue(1));
    assertFalse(multimap.containsEntry("foo", 1));
    assertTrue(multimap.entries().isEmpty());
    assertTrue(multimap.equals(ArrayListMultimap.create()));
    assertEquals(emptyList(), multimap.get("foo"));
    assertEquals(0, multimap.hashCode());
    assertTrue(multimap.isEmpty());
    assertEquals(HashMultiset.create(), multimap.keys());
    assertEquals(emptySet(), multimap.keySet());
    assertEquals(0, multimap.size());
    assertTrue(multimap.values().isEmpty());
    assertEquals("{}", multimap.toString());
  }

  public void testEmptyMultimapWrites() {
    Multimap<String, Integer> multimap = ImmutableListMultimap.of();
    UnmodifiableCollectionTests.assertMultimapIsUnmodifiable(multimap, "foo", 1);
  }

  private Multimap<String, Integer> createMultimap() {
    return ImmutableListMultimap.<String, Integer>builder()
        .put("foo", 1)
        .put("bar", 2)
        .put("foo", 3)
        .build();
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
    Multimap<String, Integer> arrayListMultimap = ArrayListMultimap.create();
    arrayListMultimap.putAll("foo", Arrays.asList(1, 3));
    arrayListMultimap.put("bar", 2);

    new EqualsTester()
        .addEqualityGroup(
            multimap,
            createMultimap(),
            arrayListMultimap,
            ImmutableListMultimap.<String, Integer>builder()
                .put("bar", 2)
                .put("foo", 1)
                .put("foo", 3)
                .build())
        .addEqualityGroup(
            ImmutableListMultimap.<String, Integer>builder()
                .put("bar", 2)
                .put("foo", 3)
                .put("foo", 1)
                .build())
        .addEqualityGroup(
            ImmutableListMultimap.<String, Integer>builder()
                .put("foo", 2)
                .put("foo", 3)
                .put("foo", 1)
                .build())
        .addEqualityGroup(
            ImmutableListMultimap.<String, Integer>builder().put("bar", 2).put("foo", 3).build())
        .testEquals();
  }

  public void testOf() {
    assertMultimapEquals(ImmutableListMultimap.of("one", 1), "one", 1);
    assertMultimapEquals(ImmutableListMultimap.of("one", 1, "two", 2), "one", 1, "two", 2);
    assertMultimapEquals(
        ImmutableListMultimap.of("one", 1, "two", 2, "three", 3), "one", 1, "two", 2, "three", 3);
    assertMultimapEquals(
        ImmutableListMultimap.of("one", 1, "two", 2, "three", 3, "four", 4),
        "one",
        1,
        "two",
        2,
        "three",
        3,
        "four",
        4);
    assertMultimapEquals(
        ImmutableListMultimap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5),
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
        ImmutableListMultimap.<Integer, String>of(),
        ImmutableListMultimap.<String, Integer>of().inverse());
    assertEquals(ImmutableListMultimap.of(1, "one"), ImmutableListMultimap.of("one", 1).inverse());
    assertEquals(
        ImmutableListMultimap.of(1, "one", 2, "two"),
        ImmutableListMultimap.of("one", 1, "two", 2).inverse());
    assertEquals(
        ImmutableListMultimap.of("of", 'o', "of", 'f', "to", 't', "to", 'o').inverse(),
        ImmutableListMultimap.of('o', "of", 'f', "of", 't', "to", 'o', "to"));
    assertEquals(
        ImmutableListMultimap.of('f', "foo", 'o', "foo", 'o', "foo"),
        ImmutableListMultimap.of("foo", 'f', "foo", 'o', "foo", 'o').inverse());
  }

  public void testInverseMinimizesWork() {
    ImmutableListMultimap<String, Character> multimap =
        ImmutableListMultimap.<String, Character>builder()
            .put("foo", 'f')
            .put("foo", 'o')
            .put("foo", 'o')
            .put("poo", 'p')
            .put("poo", 'o')
            .put("poo", 'o')
            .build();
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
    Multimap<String, Integer> multimap = ImmutableListMultimap.of();
    assertSame(multimap, SerializableTester.reserialize(multimap));
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(ImmutableListMultimap.class);
    tester.ignore(ImmutableListMultimap.class.getMethod("get", Object.class));
    tester.testAllPublicInstanceMethods(ImmutableListMultimap.of());
    tester.testAllPublicInstanceMethods(ImmutableListMultimap.of("a", 1));
  }
}
