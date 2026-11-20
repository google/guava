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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.SetTestSuiteBuilder;
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
import com.google.common.testing.EqualsTester;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullMarked;

/**
 * Unit test for {@link ImmutableSet}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Nick Kralevich
 */
@GwtCompatible
@NullMarked
public class ImmutableSetTest extends AbstractImmutableSetTest {

  @J2ktIncompatible
  @GwtIncompatible // suite
  @AndroidIncompatible // test-suite builders
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
  <E extends Comparable<? super E>> Set<E> of() {
    return ImmutableSet.of();
  }

  @Override
  <E extends Comparable<? super E>> Set<E> of(E e) {
    return ImmutableSet.of(e);
  }

  @Override
  <E extends Comparable<? super E>> Set<E> of(E e1, E e2) {
    return ImmutableSet.of(e1, e2);
  }

  @Override
  <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3) {
    return ImmutableSet.of(e1, e2, e3);
  }

  @Override
  <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3, E e4) {
    return ImmutableSet.of(e1, e2, e3, e4);
  }

  @Override
  <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
    return ImmutableSet.of(e1, e2, e3, e4, e5);
  }

  @Override
  <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E... rest) {
    return ImmutableSet.of(e1, e2, e3, e4, e5, e6, rest);
  }

  @Override
  <E extends Comparable<? super E>> Set<E> copyOf(E[] elements) {
    return ImmutableSet.copyOf(elements);
  }

  @Override
  <E extends Comparable<? super E>> Set<E> copyOf(Collection<? extends E> elements) {
    return ImmutableSet.copyOf(elements);
  }

  @Override
  <E extends Comparable<? super E>> Set<E> copyOf(Iterable<? extends E> elements) {
    return ImmutableSet.copyOf(elements);
  }

  @Override
  <E extends Comparable<? super E>> Set<E> copyOf(Iterator<? extends E> elements) {
    return ImmutableSet.copyOf(elements);
  }

  public void testCreation_allDuplicates() {
    ImmutableSet<String> set = ImmutableSet.copyOf(newArrayList("a", "a"));
    assertTrue(set instanceof SingletonImmutableSet);
    assertEquals(newArrayList("a"), new ArrayList<>(set));
  }

  public void testCreation_oneDuplicate() {
    // now we'll get the varargs overload
    @SuppressWarnings("DistinctVarargsChecker") // deliberately testing deduplication
    ImmutableSet<String> set =
        ImmutableSet.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "a");
    assertEquals(
        newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m"),
        new ArrayList<>(set));
  }

  public void testCreation_manyDuplicates() {
    // now we'll get the varargs overload
    @SuppressWarnings("DistinctVarargsChecker") // deliberately testing deduplication
    ImmutableSet<String> set =
        ImmutableSet.of("a", "b", "c", "c", "c", "c", "b", "b", "a", "a", "c", "c", "c", "a");
    assertThat(set).containsExactly("a", "b", "c").inOrder();
  }

  @GwtIncompatible // Builder impl
  public void testBuilderForceCopy() {
    ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
    builder.add(-1);
    Object[] prevArray = null;
    for (int i = 0; i < 10; i++) {
      builder.add(i);
      assertNotSame(builder.contents, prevArray);
      prevArray = builder.contents;
      ImmutableSet<Integer> unused = builder.build();
    }
  }

  @GwtIncompatible // Builder impl
  public void testPresizedBuilderDedups() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builderWithExpectedSize(4);
    builder.add("a");
    assertEquals(1, builder.size);
    builder.add("a");
    assertEquals(1, builder.size);
    builder.add("b", "c", "d");
    assertEquals(4, builder.size);
    int[] table = builder.hashTable;
    assertNotNull(table);
    assertSame(table, ((RegularImmutableSet<String>) builder.build()).table);
  }

  @GwtIncompatible // Builder impl
  public void testPresizedBuilderForceCopy() {
    for (int expectedSize = 1; expectedSize < 4; expectedSize++) {
      ImmutableSet.Builder<Integer> builder = ImmutableSet.builderWithExpectedSize(expectedSize);
      builder.add(-1);
      Object[] prevArray = null;
      for (int i = 0; i < 10; i++) {
        ImmutableSet<Integer> prevBuilt = builder.build();
        builder.add(i);
        assertFalse(prevBuilt.contains(i));
        assertNotSame(builder.contents, prevArray);
        prevArray = builder.contents;
      }
    }
  }

  public void testCreation_arrayOfArray() {
    String[] array = new String[] {"a"};
    Set<String[]> set = ImmutableSet.of(array);
    assertEquals(singleton(array), set);
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
    verifyTableSize(100, 2, 4);
    verifyTableSize(100, 5, 8);
    verifyTableSize(100, 33, 64);
    verifyTableSize(60, 60, 128);
    verifyTableSize(120, 60, 256);
    // if the table is only double the necessary size, we don't bother resizing it
    verifyTableSize(180, 60, 128);
    // but if it's even bigger than double, we rebuild the table
    verifyTableSize(17, 17, 32);
    verifyTableSize(17, 16, 32);
    verifyTableSize(17, 15, 32);
  }

  @GwtIncompatible // RegularImmutableSet.table not in emulation
  private static void verifyTableSize(int inputSize, int setSize, int tableSize) {
    Builder<Integer> builder = ImmutableSet.builder();
    for (int i = 0; i < inputSize; i++) {
      builder.add(i % setSize);
    }
    ImmutableSet<Integer> set = builder.build();
    assertTrue(set instanceof RegularImmutableSet);
    assertEquals(
        "Input size " + inputSize + " and set size " + setSize,
        tableSize,
        requireNonNull(((RegularImmutableSet<Integer>) set).table).length);
  }

  public void testCopyOf_copiesImmutableSortedSet() {
    ImmutableSortedSet<String> sortedSet = ImmutableSortedSet.of("a");
    ImmutableSet<String> copy = ImmutableSet.copyOf(sortedSet);
    assertThat(copy).isNotSameInstanceAs(sortedSet);
  }

  // TODO(b/172823566): Use mainline testToImmutableSet once CollectorTester is usable to java7.
  public void testToImmutableSet_java7() {
    ImmutableSet.Builder<String> zis = ImmutableSet.<String>builder().add("a", "b", "a");
    ImmutableSet.Builder<String> zat = ImmutableSet.<String>builder().add("c", "b", "d", "c");
    ImmutableSet<String> set = zis.combine(zat).build();
    assertThat(set).containsExactly("a", "b", "c", "d").inOrder();
  }

  @Override
  <E extends Comparable<E>> Builder<E> builder() {
    return ImmutableSet.builder();
  }

  @Override
  int getComplexBuilderSetLastElement() {
    return LAST_COLOR_ADDED;
  }

  @SuppressWarnings("DistinctVarargsChecker") // deliberately testing deduplication
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(ImmutableSet.of(), ImmutableSet.of())
        .addEqualityGroup(ImmutableSet.of(1), ImmutableSet.of(1), ImmutableSet.of(1, 1))
        .addEqualityGroup(ImmutableSet.of(1, 2, 1), ImmutableSet.of(2, 1, 1))
        .testEquals();
  }

  @GwtIncompatible // internals
  public void testControlsArraySize() {
    ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<String>();
    for (int i = 0; i < 10; i++) {
      builder.add("foo");
    }
    builder.add("bar");
    RegularImmutableSet<String> set = (RegularImmutableSet<String>) builder.build();
    assertTrue(set.elements.length <= 2 * set.size());
  }

  @GwtIncompatible // internals
  public void testReusedBuilder() {
    ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<String>();
    for (int i = 0; i < 10; i++) {
      builder.add("foo");
    }
    builder.add("bar");
    RegularImmutableSet<String> set = (RegularImmutableSet<String>) builder.build();
    builder.add("baz");
    assertTrue(set.elements != builder.contents);
  }

  public void testReuseBuilderReducingHashTableSizeWithPowerOfTwoTotalElements() {
    ImmutableSet.Builder<Object> builder = ImmutableSet.builderWithExpectedSize(6);
    builder.add(0);
    ImmutableSet<Object> unused = builder.build();
    ImmutableSet<Object> subject = builder.add(1).add(2).add(3).build();
    assertFalse(subject.contains(4));
  }

  public void testElementZeroWithZeroSmearedHash() {
    // 0 has hashCode() == 0 and Hashing.smear(0) == 0, so index 0 and tag bits 0 must not
    // collide with RegularImmutableSet.ABSENT (0).
    ImmutableSet<Integer> fromOf = ImmutableSet.of(0, 1);
    assertTrue(fromOf.contains(0));
    assertTrue(fromOf.contains(1));
    assertFalse(fromOf.contains(2));

    ImmutableSet<Integer> fromBuilder =
        ImmutableSet.<Integer>builderWithExpectedSize(2).add(0).add(1).add(0).build();
    assertEquals(2, fromBuilder.size());
    assertTrue(fromBuilder.contains(0));
    assertTrue(fromBuilder.contains(1));
    assertFalse(fromBuilder.contains(2));
  }

  @GwtIncompatible // RegularImmutableSet internals
  public void testIndexMaskForTableLength() {
    assertEquals(0xFFFF, RegularImmutableSet.indexMaskForTableLength(8));
    assertEquals(0xFFFF, RegularImmutableSet.indexMaskForTableLength(1 << 16));
    assertEquals(0xFF_FFFF, RegularImmutableSet.indexMaskForTableLength(1 << 17));
    assertEquals(0xFF_FFFF, RegularImmutableSet.indexMaskForTableLength(1 << 24));
    assertEquals(0x7FFF_FFFF, RegularImmutableSet.indexMaskForTableLength(1 << 25));
    assertEquals(0x7FFF_FFFF, RegularImmutableSet.indexMaskForTableLength(1 << 30));
  }

  public void testLargeSetCrossing16BitIndexBoundary() {
    int count = 70_000;
    ImmutableSet.Builder<Integer> presized = ImmutableSet.builderWithExpectedSize(count);
    ImmutableSet.Builder<Integer> unsized = ImmutableSet.builder();
    for (int i = 0; i < count; i++) {
      presized.add(i);
      unsized.add(i);
    }
    // Re-add an existing element whose index exceeds 16 bits to verify deduping.
    presized.add(count - 1);
    unsized.add(count - 1);
    ImmutableSet<Integer> fromPresized = presized.build();
    ImmutableSet<Integer> fromUnsized = unsized.build();
    assertEquals(count, fromPresized.size());
    assertEquals(count, fromUnsized.size());
    assertTrue(fromPresized.contains(0));
    assertTrue(fromPresized.contains(65_536));
    assertTrue(fromPresized.contains(count - 1));
    assertFalse(fromPresized.contains(count));
    assertTrue(fromUnsized.contains(0));
    assertTrue(fromUnsized.contains(65_536));
    assertTrue(fromUnsized.contains(count - 1));
    assertFalse(fromUnsized.contains(count));
  }

  public void testCopyOf_emptyCollectionSkipsToArray() {
    Collection<String> emptyThrowingToArray =
        new AbstractCollection<String>() {
          @Override
          public Iterator<String> iterator() {
            return Collections.emptyIterator();
          }

          @Override
          public int size() {
            return 0;
          }

          @Override
          public Object[] toArray() {
            throw new AssertionError("toArray() should not be called on an empty Collection");
          }
        };
    assertSame(ImmutableSet.of(), ImmutableSet.copyOf(emptyThrowingToArray));
  }
}
