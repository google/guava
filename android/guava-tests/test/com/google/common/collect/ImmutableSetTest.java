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
import static com.google.common.truth.Truth.assertWithMessage;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
    // Up to 8 distinct elements stay inline with table == null.
    verifyTableLength(100, 2, -1);
    verifyTableLength(100, 8, -1);
    // 9..14 distinct elements -> 2 groups * 2 longs (Tier 1) = 4 longs.
    verifyTableLength(100, 9, 4);
    verifyTableLength(100, 14, 4);
    // 15..28 -> 4 groups * 2 longs = 8 longs; 29..56 -> 8 groups * 2 longs = 16 longs.
    verifyTableLength(100, 15, 8);
    verifyTableLength(100, 28, 8);
    verifyTableLength(100, 33, 16);
    verifyTableLength(17, 17, 8);
    verifyTableLength(17, 16, 8);
    verifyTableLength(17, 15, 8);
  }

  @GwtIncompatible // RegularImmutableSet.table not in emulation
  private static void verifyTableLength(int inputSize, int setSize, int expectedTableLength) {
    ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
    for (int i = 0; i < inputSize; i++) {
      builder.add(i % setSize);
    }
    ImmutableSet<Integer> set = builder.build();
    assertThat(set).isInstanceOf(RegularImmutableSet.class);
    long[] table = ((RegularImmutableSet<Integer>) set).table;
    if (expectedTableLength < 0) {
      assertWithMessage("Input size %s and set size %s", inputSize, setSize).that(table).isNull();
    } else {
      assertWithMessage("Input size %s and set size %s", inputSize, setSize)
          .that(table)
          .isNotNull();
      assertWithMessage("Input size %s and set size %s", inputSize, setSize)
          .that(requireNonNull(table).length)
          .isEqualTo(expectedTableLength);
    }
  }

  public void testEmptyContainsDoesNotInvokeHashCode() {
    Object badHash =
        new Object() {
          @Override
          public int hashCode() {
            throw new AssertionError("hashCode() should not be called on empty set");
          }
        };
    assertThat(ImmutableSet.of().contains(badHash)).isFalse();
  }

  @GwtIncompatible // RegularImmutableSet internals not in emulation
  public void testSwarMatchExhaustiveNoFalseNegativesOrEmptyFalsePositives() {
    Object nonMatching = new Object();
    Object emptyLaneTrap =
        new Object() {
          @Override
          public boolean equals(@Nullable Object other) {
            throw new AssertionError("empty lane falsely matched in SWAR contains()");
          }

          @Override
          public int hashCode() {
            return 0;
          }
        };

    @Nullable Object[] probesByH2 = new @Nullable Object[128];
    int remaining = 128;
    for (int candidate = 0; remaining > 0; candidate++) {
      Integer key = candidate;
      int idx = SwarTables.h2Top7(Hashing.smearedHash(key)) - 0x80;
      if (probesByH2[idx] == null) {
        probesByH2[idx] = key;
        remaining--;
      }
    }

    Object[] elements = {
      emptyLaneTrap,
      emptyLaneTrap,
      emptyLaneTrap,
      emptyLaneTrap,
      emptyLaneTrap,
      emptyLaneTrap,
      emptyLaneTrap,
      emptyLaneTrap
    };
    for (int lane = 0; lane < 8; lane++) {
      for (int i = 0; i < 8; i++) {
        elements[i] = emptyLaneTrap;
      }
      if (lane > 0) {
        elements[lane - 1] = nonMatching;
      }
      for (int h2 = 0x80; h2 <= 0xFF; h2++) {
        Object probe = requireNonNull(probesByH2[h2 - 0x80]);
        // Test empty lane alongside every possible adjacent preceding byte (including exact match
        // at lane - 1, which borrows 1 during SWAR subtraction).
        for (int prevByte = 0; prevByte <= 0xFF; prevByte++) {
          long ctrl = (lane == 0) ? 0L : (((long) prevByte) << ((lane - 1) << 3));
          elements[lane] = emptyLaneTrap;
          RegularImmutableSet<Object> emptyLaneSet =
              new RegularImmutableSet<>(elements, 0, null, ctrl);
          assertWithMessage("empty lane %s matched with prevByte=%s, h2=%s", lane, prevByte, h2)
              .that(emptyLaneSet.contains(probe))
              .isFalse();

          long matchCtrl = ctrl | (((long) h2) << (lane << 3));
          elements[lane] = probe;
          RegularImmutableSet<Object> occupiedLaneSet =
              new RegularImmutableSet<>(elements, 0, null, matchCtrl);
          assertWithMessage(
                  "occupied match failed at lane %s, prevByte=%s, h2=%s", lane, prevByte, h2)
              .that(occupiedLaneSet.contains(probe))
              .isTrue();
        }
      }
    }
  }

  @GwtIncompatible // RegularImmutableSet.table not in emulation
  public void testTierBoundaries() {
    // Spot-check Tier 1, Tier 2, and Tier 3 boundaries (inline, Tier 1 max 256, Tier 2 max 65536).
    int[] sizes = {
      0, 1, 2, 7, 8, 9, 14, 15, 28, 29, 224, 225, 255, 256, 257, 448, 449, 65_535, 65_536, 65_537
    };
    int[] expectedLengths = {
      -2, // EMPTY (table == null)
      -2, // SingletonImmutableSet
      -1,
      -1,
      -1, // inline RegularImmutableSet (table == null)
      4,
      4, // 2 groups * 2
      8,
      8, // 4 groups * 2
      16, // 8 groups * 2
      64, // 32 groups * 2
      128,
      128,
      128, // 64 groups * 2
      192,
      192, // Tier 2: 64 groups * 3
      384, // Tier 2: 128 groups * 3
      49_152,
      49_152, // Tier 2: 16384 groups * 3
      65_536 // Tier 3: 16384 groups * 4
    };
    for (int k = 0; k < sizes.length; k++) {
      int n = sizes[k];
      ImmutableSet.Builder<Integer> unsized = ImmutableSet.builder();
      ImmutableSet.Builder<Integer> exact = ImmutableSet.builderWithExpectedSize(n);
      for (int i = 0; i < n; i++) {
        unsized.add(i);
        exact.add(i);
      }
      ImmutableSet<Integer> s1 = unsized.build();
      ImmutableSet<Integer> s2 = exact.build();
      assertThat(s1).hasSize(n);
      assertThat(s1).isEqualTo(s2);
      assertThat(s1.asList()).isEqualTo(s2.asList());
      assertThat(s1.contains(0)).isEqualTo(n > 0);
      assertThat(n == 0 || s1.contains(n - 1)).isTrue();
      assertThat(s1).doesNotContain(n);
      assertThat(s1).doesNotContain(-1);
      assertThat(s1 instanceof SingletonImmutableSet).isEqualTo(n == 1);
      if (s1 instanceof RegularImmutableSet) {
        long[] t1 = ((RegularImmutableSet<Integer>) s1).table;
        long[] t2 = ((RegularImmutableSet<Integer>) s2).table;
        if (expectedLengths[k] < 0) {
          assertWithMessage("n=%s", n).that(t1).isNull();
          assertWithMessage("n=%s", n).that(t2).isNull();
        } else {
          assertWithMessage("n=%s", n).that(t1).isNotNull();
          assertWithMessage("n=%s", n).that(t2).isNotNull();
          assertWithMessage("n=%s", n)
              .that(requireNonNull(t1).length)
              .isEqualTo(expectedLengths[k]);
          assertWithMessage("n=%s", n)
              .that(requireNonNull(t2).length)
              .isEqualTo(expectedLengths[k]);
        }
      }
    }
  }

  @GwtIncompatible // RegularImmutableSet.table not in emulation
  public void testTier3DeduplicatesIndexAtOrAbove65536() {
    int fillerCount = 65_536;
    int totalSize = fillerCount + 8;
    int groupMask = 16_384 - 1;
    ImmutableSet.Builder<Integer> builder = ImmutableSet.builderWithExpectedSize(totalSize);

    // Fill indices 0..65_535 while keeping group 0 empty so the next 8 group-0 keys
    // deterministically populate all 8 lanes in Tier 3's split-plane 24-bit layout (high byte in
    // word 1, low 16-bit word in words 2..3).
    int candidate = 0;
    int added = 0;
    while (added < fillerCount) {
      int key = candidate++;
      if ((Hashing.smear(key) & groupMask) >= 1024) {
        builder.add(key);
        added++;
      }
    }

    int[] groupZeroKeys = new int[8];
    int found = 0;
    while (found < 8) {
      int key = candidate++;
      if ((Hashing.smear(key) & groupMask) == 0) {
        groupZeroKeys[found++] = key;
        builder.add(key);
      }
    }
    int missingGroupZeroKey;
    while (true) {
      int key = candidate++;
      if ((Hashing.smear(key) & groupMask) == 0) {
        missingGroupZeroKey = key;
        break;
      }
    }

    // Re-adding all 8 keys exercises RegularImmutableSet.readLargeIndex across all 8 Tier-3 lanes.
    for (int key : groupZeroKeys) {
      builder.add(key);
    }

    ImmutableSet<Integer> set = builder.build();
    assertThat(set).hasSize(totalSize);
    for (int key : groupZeroKeys) {
      assertThat(set).contains(key);
    }
    assertThat(set).doesNotContain(missingGroupZeroKey);
    long[] table = ((RegularImmutableSet<Integer>) set).table;
    assertThat(table).isNotNull();
    assertThat(requireNonNull(table).length).isEqualTo(65_536);
    assertThat(requireNonNull(table)[0] & SwarTables.HIGH_BITS).isEqualTo(SwarTables.HIGH_BITS);
  }

  @GwtIncompatible // RegularImmutableSet.table not in emulation
  public void testShrinkingFromTier2ToTier1() {
    ImmutableSet.Builder<Integer> oversizedToTier1 = ImmutableSet.builderWithExpectedSize(300);
    for (int i = 0; i < 256; i++) {
      oversizedToTier1.add(i);
    }
    ImmutableSet<Integer> shrunkTier1 = oversizedToTier1.build();
    assertThat(shrunkTier1).hasSize(256);
    long[] table = ((RegularImmutableSet<Integer>) shrunkTier1).table;
    assertThat(table).isNotNull();
    assertThat(requireNonNull(table).length).isEqualTo(128);
  }

  @GwtIncompatible // RegularImmutableSet.table not in emulation
  public void testShrinkingFromLargeBuilderToSmallTier1() {
    // Oversized builder with 15 elements shrinks to 4 groups (table.length = 8).
    ImmutableSet.Builder<Integer> oversizedToSmall = ImmutableSet.builderWithExpectedSize(500);
    for (int i = 0; i < 15; i++) {
      oversizedToSmall.add(i);
    }
    ImmutableSet<Integer> shrunkSmall = oversizedToSmall.build();
    assertThat(shrunkSmall).hasSize(15);
    long[] table = ((RegularImmutableSet<Integer>) shrunkSmall).table;
    assertThat(table).isNotNull();
    assertThat(requireNonNull(table).length).isEqualTo(8);
  }

  @GwtIncompatible // RegularImmutableSet.table not in emulation
  public void testShrinkingOversizedBuilderToInline() {
    // Oversized builder with <= 8 elements never allocates a table.
    ImmutableSet.Builder<Integer> oversizedToInline = ImmutableSet.builderWithExpectedSize(500);
    for (int i = 0; i < 8; i++) {
      oversizedToInline.add(i);
    }
    ImmutableSet<Integer> inlineSet = oversizedToInline.build();
    assertThat(inlineSet).hasSize(8);
    assertThat(((RegularImmutableSet<Integer>) inlineSet).table).isNull();
  }

  public void testBuilderReuseAcrossInlinePromoteAndTierBoundary() {
    ImmutableSet.Builder<Integer> builder = ImmutableSet.builderWithExpectedSize(300);
    ImmutableSet<Integer> s0 = builder.build();
    builder.add(0);
    ImmutableSet<Integer> s1 = builder.build();
    builder.add(1);
    ImmutableSet<Integer> s2 = builder.build();
    for (int i = 2; i < 8; i++) {
      builder.add(i);
    }
    ImmutableSet<Integer> s8 = builder.build();
    builder.add(8);
    ImmutableSet<Integer> s9 = builder.build();
    for (int i = 9; i < 256; i++) {
      builder.add(i);
    }
    // s256 shrinks from Tier 2 (cap 300) to Tier 1 (256); adding 256 promotes back to Tier 2.
    ImmutableSet<Integer> s256 = builder.build();
    ImmutableSet<Integer> s257 = builder.add(256).build();

    assertThat(s0).isEmpty();
    assertThat(s1).hasSize(1);
    assertThat(s2).hasSize(2);
    assertThat(s8).hasSize(8);
    assertThat(s9).hasSize(9);
    assertThat(s256).hasSize(256);
    assertThat(s257).hasSize(257);
    assertThat(s8).doesNotContain(8);
    assertThat(s256).doesNotContain(256);
    assertThat(s257).contains(256);
  }

  @GwtIncompatible // JRE copyOf(ImmutableAsList) reuse optimization
  public void testCopyOf_reusesImmutableAsListBackingSet() {
    ImmutableSet<String> set = ImmutableSet.of("a", "b", "c");
    assertThat(ImmutableSet.copyOf(set.asList())).isSameInstanceAs(set);

    ImmutableSet<Entry<String, Integer>> entrySet = ImmutableMap.of("k1", 1, "k2", 2).entrySet();
    assertThat(ImmutableSet.copyOf(entrySet.asList())).isSameInstanceAs(entrySet);

    // Partial views (KeySet, subList) and SortedSet-backed lists must copy rather than alias.
    ImmutableSet<String> keySet = ImmutableMap.of("k1", 1, "k2", 2).keySet();
    ImmutableSet<String> keySetCopy = ImmutableSet.copyOf(keySet.asList());
    assertThat(keySetCopy).isNotSameInstanceAs(keySet);
    assertThat(keySetCopy).containsExactly("k1", "k2").inOrder();

    ImmutableSet<String> subCopy = ImmutableSet.copyOf(set.asList().subList(0, 2));
    assertThat(subCopy).containsExactly("a", "b").inOrder();

    ImmutableSortedSet<String> sortedSet = ImmutableSortedSet.of("b", "a");
    ImmutableSet<String> sortedAsListCopy = ImmutableSet.copyOf(sortedSet.asList());
    assertThat(sortedAsListCopy).isNotSameInstanceAs(sortedSet);
    assertThat(sortedAsListCopy).containsExactly("a", "b").inOrder();
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

  public void testReuseBuilderReducingHashTableSizeWithPowerOfTwoTotalElements() {
    ImmutableSet.Builder<Object> builder = ImmutableSet.builderWithExpectedSize(6);
    builder.add(0);
    ImmutableSet<Object> unused = builder.build();
    ImmutableSet<Object> subject = builder.add(1).add(2).add(3).build();
    assertFalse(subject.contains(4));
  }

  public void testCopyOf_hostileToArrayRetainedArrayMutatedAfterwards() {
    for (int size : new int[] {3, 12}) {
      @Nullable Object[] retained = new @Nullable Object[size];
      List<String> expected = new ArrayList<>();
      for (int i = 0; i < size; i++) {
        String s = "e" + i;
        retained[i] = s;
        expected.add(s);
      }
      Collection<String> hostile =
          new ArrayList<String>(expected) {
            @Override
            public @Nullable Object[] toArray() {
              return retained;
            }
          };

      ImmutableSet<String> set = ImmutableSet.copyOf(hostile);
      retained[0] = null;
      retained[1] = "poison";

      assertThat(set).containsExactlyElementsIn(expected).inOrder();
      assertTrue(set.contains("e0"));
      assertTrue(set.contains("e1"));
      assertFalse(set.contains("poison"));
      assertFalse(set.contains(null));
    }
  }

  public void testCopyOf_hostileToArrayMutatedDuringConstruction() {
    class RetainedHolder {
      @Nullable Object @Nullable [] array;
    }
    RetainedHolder retainedHolder = new RetainedHolder();
    class MutatingKey {
      final int id;
      final boolean poisonOnHash;

      MutatingKey(int id, boolean poisonOnHash) {
        this.id = id;
        this.poisonOnHash = poisonOnHash;
      }

      @Override
      public int hashCode() {
        @Nullable Object[] retained = retainedHolder.array;
        if (poisonOnHash && retained != null) {
          retained[0] = null;
          retained[1] = new MutatingKey(-1, false);
        }
        return id;
      }

      @Override
      public boolean equals(@Nullable Object obj) {
        return obj instanceof MutatingKey && ((MutatingKey) obj).id == id;
      }
    }

    // Distinct elements (inline n=2 and Swiss-table n=12) + duplicates (builder deduplicates
    // in-place).
    MutatingKey k0 = new MutatingKey(0, false);
    MutatingKey k1 = new MutatingKey(1, true);
    MutatingKey nonMutating1 = new MutatingKey(1, false);
    MutatingKey duplicate0 = new MutatingKey(0, false);
    List<List<MutatingKey>> cases = new ArrayList<>();
    cases.add(asListKeys(k0, k1));
    cases.add(asListKeys(k0, k0, k1));
    cases.add(asListKeys(k0, k1, duplicate0));
    cases.add(asListKeys(k0, duplicate0, nonMutating1));
    List<MutatingKey> twelve = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      twelve.add(i == 1 ? k1 : new MutatingKey(i, false));
    }
    cases.add(twelve);

    for (List<MutatingKey> input : cases) {
      @Nullable Object[] retained = new @Nullable Object[input.size()];
      for (int i = 0; i < input.size(); i++) {
        retained[i] = input.get(i);
      }
      retainedHolder.array = retained;
      Collection<MutatingKey> hostile =
          new ArrayList<MutatingKey>(input) {
            @Override
            public @Nullable Object[] toArray() {
              return retained;
            }
          };

      ImmutableSet<MutatingKey> set = ImmutableSet.copyOf(hostile);
      retainedHolder.array = null;

      assertFalse(set.contains(null));
      assertFalse(set.contains(new MutatingKey(-1, false)));
      for (MutatingKey expected : input) {
        assertTrue(set.contains(expected));
      }
      if (input.size() == 3 && input.get(2) == nonMutating1) {
        // Copying [k0, duplicate0, nonMutating1] must not overwrite retained[1] during
        // deduplication.
        assertSame(k0, retained[0]);
        assertSame(duplicate0, retained[1]);
        assertSame(nonMutating1, retained[2]);
      }
    }
  }

  private static <T> List<T> asListKeys(T... keys) {
    List<T> list = new ArrayList<>();
    for (T key : keys) {
      list.add(key);
    }
    return list;
  }

  @GwtIncompatible
  @J2ktIncompatible
  public void testCopyOf_hostileToArraySubtypeArrayNormalized() {
    String[] subtypeArray = new String[] {"a", "b", "c"};
    Collection<Object> hostile =
        new ArrayList<Object>() {
          @Override
          public int size() {
            return subtypeArray.length;
          }

          @Override
          public boolean isEmpty() {
            return false;
          }

          @Override
          public Object[] toArray() {
            return subtypeArray;
          }
        };

    ImmutableSet<Object> set = ImmutableSet.copyOf(hostile);
    subtypeArray[0] = "poison";

    assertThat(set).containsExactly("a", "b", "c").inOrder();
    Object[] internal = requireNonNull(set.internalArray());
    assertThat(internal).isNotSameInstanceAs(subtypeArray);
    assertEquals(Object[].class, internal.getClass());
  }
}
