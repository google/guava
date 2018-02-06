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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestSuite;

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

  @GwtIncompatible("Builder impl")
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

  @GwtIncompatible("Builder impl")
  public void testPresizedBuilderDedups() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builderWithExpectedSize(4);
    builder.add("a");
    assertEquals(1, builder.size);
    builder.add("a");
    assertEquals(1, builder.size);
    builder.add("b", "c", "d");
    assertEquals(4, builder.size);
    Object[] table = builder.hashTable;
    assertNotNull(table);
    assertSame(table, ((RegularImmutableSet<String>) builder.build()).table);
  }

  @GwtIncompatible("Builder impl")
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

  @GwtIncompatible("internals")
  public void testControlsArraySize() {
    ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<String>();
    for (int i = 0; i < 10; i++) {
      builder.add("foo");
    }
    builder.add("bar");
    RegularImmutableSet<String> set = (RegularImmutableSet<String>) builder.build();
    assertTrue(set.elements.length <= 2 * set.size());
  }

  @GwtIncompatible("internals")
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
}
