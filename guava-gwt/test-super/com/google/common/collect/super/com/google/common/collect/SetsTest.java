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

import static com.google.common.collect.Iterables.unmodifiableIterable;
import static com.google.common.collect.Sets.newEnumSet;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.google.common.collect.Sets.powerSet;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.MinimalIterable;
import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

/**
 * Unit test for {@code Sets}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class SetsTest extends TestCase {

  private static final IteratorTester.KnownOrder KNOWN_ORDER =
      IteratorTester.KnownOrder.KNOWN_ORDER;

  private static final Collection<Integer> EMPTY_COLLECTION
      = Arrays.<Integer>asList();

  private static final Collection<Integer> SOME_COLLECTION
      = Arrays.asList(0, 1, 1);

  private static final Iterable<Integer> SOME_ITERABLE
      = new Iterable<Integer>() {
        @Override
        public Iterator<Integer> iterator() {
          return SOME_COLLECTION.iterator();
        }
      };

  private static final List<Integer> LONGER_LIST
      = Arrays.asList(8, 6, 7, 5, 3, 0, 9);

  private static final Comparator<Integer> SOME_COMPARATOR
      = Collections.reverseOrder();

  private enum SomeEnum { A, B, C, D }

  public void testImmutableEnumSet() {
    Set<SomeEnum> units = Sets.immutableEnumSet(SomeEnum.D, SomeEnum.B);

    ASSERT.that(units).has().exactly(SomeEnum.B, SomeEnum.D).inOrder();
    try {
      units.remove(SomeEnum.B);
      fail("ImmutableEnumSet should throw an exception on remove()");
    } catch (UnsupportedOperationException expected) {}
    try {
      units.add(SomeEnum.C);
      fail("ImmutableEnumSet should throw an exception on add()");
    } catch (UnsupportedOperationException expected) {}
  }

  public void testImmutableEnumSet_fromIterable() {
    ImmutableSet<SomeEnum> none
        = Sets.immutableEnumSet(MinimalIterable.<SomeEnum>of());
    ASSERT.that(none).isEmpty();

    ImmutableSet<SomeEnum> one
        = Sets.immutableEnumSet(MinimalIterable.of(SomeEnum.B));
    ASSERT.that(one).has().item(SomeEnum.B);

    ImmutableSet<SomeEnum> two
        = Sets.immutableEnumSet(MinimalIterable.of(SomeEnum.D, SomeEnum.B));
    ASSERT.that(two).has().exactly(SomeEnum.B, SomeEnum.D).inOrder();
  }

  private static byte[] prepended(byte b, byte[] array) {
    byte[] out = new byte[array.length + 1];
    out[0] = b;
    System.arraycopy(array, 0, out, 1, array.length);
    return out;
  }

  public void testNewEnumSet_empty() {
    EnumSet<SomeEnum> copy =
        newEnumSet(Collections.<SomeEnum>emptySet(), SomeEnum.class);
    assertEquals(EnumSet.noneOf(SomeEnum.class), copy);
  }

  public void testNewEnumSet_enumSet() {
    EnumSet<SomeEnum> set = EnumSet.of(SomeEnum.A, SomeEnum.D);
    assertEquals(set, newEnumSet(set, SomeEnum.class));
  }

  public void testNewEnumSet_collection() {
    Set<SomeEnum> set = ImmutableSet.of(SomeEnum.B, SomeEnum.C);
    assertEquals(set, newEnumSet(set, SomeEnum.class));
  }

  public void testNewEnumSet_iterable() {
    Set<SomeEnum> set = ImmutableSet.of(SomeEnum.A, SomeEnum.B, SomeEnum.C);
    assertEquals(set, newEnumSet(unmodifiableIterable(set), SomeEnum.class));
  }

  public void testNewHashSetEmpty() {
    HashSet<Integer> set = Sets.newHashSet();
    verifySetContents(set, EMPTY_COLLECTION);
  }

  public void testNewHashSetVarArgs() {
    HashSet<Integer> set = Sets.newHashSet(0, 1, 1);
    verifySetContents(set, Arrays.asList(0, 1));
  }

  public void testNewHashSetFromCollection() {
    HashSet<Integer> set = Sets.newHashSet(SOME_COLLECTION);
    verifySetContents(set, SOME_COLLECTION);
  }

  public void testNewHashSetFromIterable() {
    HashSet<Integer> set = Sets.newHashSet(SOME_ITERABLE);
    verifySetContents(set, SOME_ITERABLE);
  }

  public void testNewHashSetWithExpectedSizeSmall() {
    HashSet<Integer> set = Sets.newHashSetWithExpectedSize(0);
    verifySetContents(set, EMPTY_COLLECTION);
  }

  public void testNewHashSetWithExpectedSizeLarge() {
    HashSet<Integer> set = Sets.newHashSetWithExpectedSize(1000);
    verifySetContents(set, EMPTY_COLLECTION);
  }

  public void testNewHashSetFromIterator() {
    HashSet<Integer> set = Sets.newHashSet(SOME_COLLECTION.iterator());
    verifySetContents(set, SOME_COLLECTION);
  }

  public void testNewConcurrentHashSetEmpty() {
    Set<Integer> set = Sets.newConcurrentHashSet();
    verifySetContents(set, EMPTY_COLLECTION);
  }

  public void testNewConcurrentHashSetFromCollection() {
    Set<Integer> set = Sets.newConcurrentHashSet(SOME_COLLECTION);
    verifySetContents(set, SOME_COLLECTION);
  }

  public void testNewLinkedHashSetEmpty() {
    LinkedHashSet<Integer> set = Sets.newLinkedHashSet();
    verifyLinkedHashSetContents(set, EMPTY_COLLECTION);
  }

  public void testNewLinkedHashSetFromCollection() {
    LinkedHashSet<Integer> set = Sets.newLinkedHashSet(LONGER_LIST);
    verifyLinkedHashSetContents(set, LONGER_LIST);
  }

  public void testNewLinkedHashSetFromIterable() {
    LinkedHashSet<Integer> set = Sets.newLinkedHashSet(new Iterable<Integer>()
    {
      @Override
      public Iterator<Integer> iterator() {
        return LONGER_LIST.iterator();
      }
    });
    verifyLinkedHashSetContents(set, LONGER_LIST);
  }

  public void testNewLinkedHashSetWithExpectedSizeSmall() {
    LinkedHashSet<Integer> set = Sets.newLinkedHashSetWithExpectedSize(0);
    verifySetContents(set, EMPTY_COLLECTION);
  }

  public void testNewLinkedHashSetWithExpectedSizeLarge() {
    LinkedHashSet<Integer> set = Sets.newLinkedHashSetWithExpectedSize(1000);
    verifySetContents(set, EMPTY_COLLECTION);
  }

  public void testNewTreeSetEmpty() {
    TreeSet<Integer> set = Sets.newTreeSet();
    verifySortedSetContents(set, EMPTY_COLLECTION, null);
  }

  public void testNewTreeSetEmptyDerived() {
    TreeSet<Derived> set = Sets.newTreeSet();
    assertTrue(set.isEmpty());
    set.add(new Derived("foo"));
    set.add(new Derived("bar"));
    ASSERT.that(set).has().exactly(new Derived("bar"), new Derived("foo")).inOrder();
  }

  public void testNewTreeSetEmptyNonGeneric() {
    TreeSet<LegacyComparable> set = Sets.newTreeSet();
    assertTrue(set.isEmpty());
    set.add(new LegacyComparable("foo"));
    set.add(new LegacyComparable("bar"));
    ASSERT.that(set).has()
        .exactly(new LegacyComparable("bar"), new LegacyComparable("foo")).inOrder();
  }

  public void testNewTreeSetFromCollection() {
    TreeSet<Integer> set = Sets.newTreeSet(SOME_COLLECTION);
    verifySortedSetContents(set, SOME_COLLECTION, null);
  }

  public void testNewTreeSetFromIterable() {
    TreeSet<Integer> set = Sets.newTreeSet(SOME_ITERABLE);
    verifySortedSetContents(set, SOME_ITERABLE, null);
  }

  public void testNewTreeSetFromIterableDerived() {
    Iterable<Derived> iterable =
        Arrays.asList(new Derived("foo"), new Derived("bar"));
    TreeSet<Derived> set = Sets.newTreeSet(iterable);
    ASSERT.that(set).has().exactly(
        new Derived("bar"), new Derived("foo")).inOrder();
  }

  public void testNewTreeSetFromIterableNonGeneric() {
    Iterable<LegacyComparable> iterable =
        Arrays.asList(new LegacyComparable("foo"), new LegacyComparable("bar"));
    TreeSet<LegacyComparable> set = Sets.newTreeSet(iterable);
    ASSERT.that(set).has().exactly(
        new LegacyComparable("bar"), new LegacyComparable("foo")).inOrder();
  }

  public void testNewTreeSetEmptyWithComparator() {
    TreeSet<Integer> set = Sets.newTreeSet(SOME_COMPARATOR);
    verifySortedSetContents(set, EMPTY_COLLECTION, SOME_COMPARATOR);
  }

  public void testNewIdentityHashSet() {
    Set<Integer> set = Sets.newIdentityHashSet();
    Integer value1 = new Integer(12357);
    Integer value2 = new Integer(12357);
    assertTrue(set.add(value1));
    assertFalse(set.contains(value2));
    assertTrue(set.contains(value1));
    assertTrue(set.add(value2));
    assertEquals(2, set.size());
  }

  public void testComplementOfEnumSet() {
    Set<SomeEnum> units = EnumSet.of(SomeEnum.B, SomeEnum.D);
    EnumSet<SomeEnum> otherUnits = Sets.complementOf(units);
    verifySetContents(otherUnits, EnumSet.of(SomeEnum.A, SomeEnum.C));
  }

  public void testComplementOfEnumSetWithType() {
    Set<SomeEnum> units = EnumSet.of(SomeEnum.B, SomeEnum.D);
    EnumSet<SomeEnum> otherUnits = Sets.complementOf(units, SomeEnum.class);
    verifySetContents(otherUnits, EnumSet.of(SomeEnum.A, SomeEnum.C));
  }

  public void testComplementOfRegularSet() {
    Set<SomeEnum> units = Sets.newHashSet(SomeEnum.B, SomeEnum.D);
    EnumSet<SomeEnum> otherUnits = Sets.complementOf(units);
    verifySetContents(otherUnits, EnumSet.of(SomeEnum.A, SomeEnum.C));
  }

  public void testComplementOfRegularSetWithType() {
    Set<SomeEnum> units = Sets.newHashSet(SomeEnum.B, SomeEnum.D);
    EnumSet<SomeEnum> otherUnits = Sets.complementOf(units, SomeEnum.class);
    verifySetContents(otherUnits, EnumSet.of(SomeEnum.A, SomeEnum.C));
  }

  public void testComplementOfEmptySet() {
    Set<SomeEnum> noUnits = Collections.emptySet();
    EnumSet<SomeEnum> allUnits = Sets.complementOf(noUnits, SomeEnum.class);
    verifySetContents(EnumSet.allOf(SomeEnum.class), allUnits);
  }

  public void testComplementOfFullSet() {
    Set<SomeEnum> allUnits = Sets.newHashSet(SomeEnum.values());
    EnumSet<SomeEnum> noUnits = Sets.complementOf(allUnits, SomeEnum.class);
    verifySetContents(noUnits, EnumSet.noneOf(SomeEnum.class));
  }

  public void testComplementOfEmptyEnumSetWithoutType() {
    Set<SomeEnum> noUnits = EnumSet.noneOf(SomeEnum.class);
    EnumSet<SomeEnum> allUnits = Sets.complementOf(noUnits);
    verifySetContents(allUnits, EnumSet.allOf(SomeEnum.class));
  }

  public void testComplementOfEmptySetWithoutTypeDoesntWork() {
    Set<SomeEnum> set = Collections.emptySet();
    try {
      Sets.complementOf(set);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testNewSetFromMap() {
    Set<Integer> set = Sets.newSetFromMap(new HashMap<Integer, Boolean>());
    set.addAll(SOME_COLLECTION);
    verifySetContents(set, SOME_COLLECTION);
  }

  public void testNewSetFromMapIllegal() {
    Map<Integer, Boolean> map = new LinkedHashMap<Integer, Boolean>();
    map.put(2, true);
    try {
      Sets.newSetFromMap(map);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  // TODO: the overwhelming number of suppressions below suggests that maybe
  // it's not worth having a varargs form of this method at all...

  /**
   * The 0-ary cartesian product is a single empty list.
   */
  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_zeroary() {
    ASSERT.that(Sets.cartesianProduct()).has().exactly(list());
  }

  /**
   * A unary cartesian product is one list of size 1 for each element in the
   * input set.
   */
  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_unary() {
    ASSERT.that(Sets.cartesianProduct(set(1, 2))).has().exactly(list(1), list(2));
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_binary0x0() {
    Set<Integer> mt = emptySet();
    assertEmpty(Sets.cartesianProduct(mt, mt));
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_binary0x1() {
    Set<Integer> mt = emptySet();
    assertEmpty(Sets.cartesianProduct(mt, set(1)));
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_binary1x0() {
    Set<Integer> mt = emptySet();
    assertEmpty(Sets.cartesianProduct(set(1), mt));
  }

  private static void assertEmpty(Set<? extends List<?>> set) {
    assertTrue(set.isEmpty());
    assertEquals(0, set.size());
    assertFalse(set.iterator().hasNext());
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_binary1x1() {
    ASSERT.that(Sets.cartesianProduct(set(1), set(2))).has().item(list(1, 2));
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_binary1x2() {
    ASSERT.that(Sets.cartesianProduct(set(1), set(2, 3)))
        .has().exactly(list(1, 2), list(1, 3)).inOrder();
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_binary2x2() {
    ASSERT.that(Sets.cartesianProduct(set(1, 2), set(3, 4)))
        .has().exactly(list(1, 3), list(1, 4), list(2, 3), list(2, 4)).inOrder();
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_2x2x2() {
    ASSERT.that(Sets.cartesianProduct(set(0, 1), set(0, 1), set(0, 1))).has().exactly(
        list(0, 0, 0), list(0, 0, 1), list(0, 1, 0), list(0, 1, 1),
        list(1, 0, 0), list(1, 0, 1), list(1, 1, 0), list(1, 1, 1)).inOrder();
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_contains() {
    Set<List<Integer>> actual = Sets.cartesianProduct(set(1, 2), set(3, 4));
    assertTrue(actual.contains(list(1, 3)));
    assertTrue(actual.contains(list(1, 4)));
    assertTrue(actual.contains(list(2, 3)));
    assertTrue(actual.contains(list(2, 4)));
    assertFalse(actual.contains(list(3, 1)));
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_unrelatedTypes() {
    Set<Integer> x = set(1, 2);
    Set<String> y = set("3", "4");

    List<Object> exp1 = list((Object) 1, "3");
    List<Object> exp2 = list((Object) 1, "4");
    List<Object> exp3 = list((Object) 2, "3");
    List<Object> exp4 = list((Object) 2, "4");

    ASSERT.that(Sets.<Object>cartesianProduct(x, y))
        .has().exactly(exp1, exp2, exp3, exp4).inOrder();
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProductTooBig() {
    Set<Integer> set = ContiguousSet.create(Range.closed(0, 10000), DiscreteDomain.integers());
    try {
      Sets.cartesianProduct(set, set, set, set, set);
      fail("Expected IAE");
    } catch (IllegalArgumentException expected) {}
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_hashCode() {
    // Run through the same cartesian products we tested above

    Set<List<Integer>> degenerate = Sets.cartesianProduct();
    checkHashCode(degenerate);

    checkHashCode(Sets.cartesianProduct(set(1, 2)));

    int num = Integer.MAX_VALUE / 3 * 2; // tickle overflow-related problems
    checkHashCode(Sets.cartesianProduct(set(1, 2, num)));

    Set<Integer> mt = emptySet();
    checkHashCode(Sets.cartesianProduct(mt, mt));
    checkHashCode(Sets.cartesianProduct(mt, set(num)));
    checkHashCode(Sets.cartesianProduct(set(num), mt));
    checkHashCode(Sets.cartesianProduct(set(num), set(1)));
    checkHashCode(Sets.cartesianProduct(set(1), set(2, num)));
    checkHashCode(Sets.cartesianProduct(set(1, num), set(2, num - 1)));
    checkHashCode(Sets.cartesianProduct(
        set(1, num), set(2, num - 1), set(3, num + 1)));

    // a bigger one
    checkHashCode(Sets.cartesianProduct(
        set(1, num, num + 1), set(2), set(3, num + 2), set(4, 5, 6, 7, 8)));
  }

  public void testPowerSetEmpty() {
    ImmutableSet<Integer> elements = ImmutableSet.of();
    Set<Set<Integer>> powerSet = powerSet(elements);
    assertEquals(1, powerSet.size());
    assertEquals(ImmutableSet.of(ImmutableSet.of()), powerSet);
    assertEquals(0, powerSet.hashCode());
  }

  public void testPowerSetContents() {
    ImmutableSet<Integer> elements = ImmutableSet.of(1, 2, 3);
    Set<Set<Integer>> powerSet = powerSet(elements);
    assertEquals(8, powerSet.size());
    assertEquals(4 * 1 + 4 * 2 + 4 * 3, powerSet.hashCode());

    Set<Set<Integer>> expected = newHashSet();
    expected.add(ImmutableSet.<Integer>of());
    expected.add(ImmutableSet.of(1));
    expected.add(ImmutableSet.of(2));
    expected.add(ImmutableSet.of(3));
    expected.add(ImmutableSet.of(1, 2));
    expected.add(ImmutableSet.of(1, 3));
    expected.add(ImmutableSet.of(2, 3));
    expected.add(ImmutableSet.of(1, 2, 3));

    Set<Set<Integer>> almostPowerSet = newHashSet(expected);
    almostPowerSet.remove(ImmutableSet.of(1, 2, 3));
    almostPowerSet.add(ImmutableSet.of(1, 2, 4));

    new EqualsTester()
        .addEqualityGroup(expected, powerSet)
        .addEqualityGroup(ImmutableSet.of(1, 2, 3))
        .addEqualityGroup(almostPowerSet)
        .testEquals();

    for (Set<Integer> subset : expected) {
      assertTrue(powerSet.contains(subset));
    }
    assertFalse(powerSet.contains(ImmutableSet.of(1, 2, 4)));
    assertFalse(powerSet.contains(singleton(null)));
    assertFalse(powerSet.contains(null));
    assertFalse(powerSet.contains("notASet"));
  }

  public void testPowerSetIteration_manual() {
    ImmutableSet<Integer> elements = ImmutableSet.of(1, 2, 3);
    Set<Set<Integer>> powerSet = powerSet(elements);
    // The API doesn't promise this iteration order, but it's convenient here.
    Iterator<Set<Integer>> i = powerSet.iterator();
    assertEquals(ImmutableSet.of(), i.next());
    assertEquals(ImmutableSet.of(1), i.next());
    assertEquals(ImmutableSet.of(2), i.next());
    assertEquals(ImmutableSet.of(2, 1), i.next());
    assertEquals(ImmutableSet.of(3), i.next());
    assertEquals(ImmutableSet.of(3, 1), i.next());
    assertEquals(ImmutableSet.of(3, 2), i.next());
    assertEquals(ImmutableSet.of(3, 2, 1), i.next());
    assertFalse(i.hasNext());
    try {
      i.next();
      fail();
    } catch (NoSuchElementException expected) {
    }
  }

  public void testPowerSetIteration_iteratorTester_fast() {
    ImmutableSet<Integer> elements = ImmutableSet.of(1, 2);

    Set<Set<Integer>> expected = newLinkedHashSet();
    expected.add(ImmutableSet.<Integer>of());
    expected.add(ImmutableSet.of(1));
    expected.add(ImmutableSet.of(2));
    expected.add(ImmutableSet.of(1, 2));

    final Set<Set<Integer>> powerSet = powerSet(elements);
    new IteratorTester<Set<Integer>>(4, UNMODIFIABLE, expected, KNOWN_ORDER) {
      @Override protected Iterator<Set<Integer>> newTargetIterator() {
        return powerSet.iterator();
      }
    }.test();
  }

  public void testPowerSetSize() {
    assertPowerSetSize(1);
    assertPowerSetSize(2, 'a');
    assertPowerSetSize(4, 'a', 'b');
    assertPowerSetSize(8, 'a', 'b', 'c');
    assertPowerSetSize(16, 'a', 'b', 'd', 'e');
    assertPowerSetSize(1 << 30,
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2',
        '3', '4');
  }

  public void testPowerSetCreationErrors() {
    try {
      powerSet(newHashSet('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
          'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
          'y', 'z', '1', '2', '3', '4', '5'));
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      powerSet(singleton(null));
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testPowerSetEqualsAndHashCode_verifyAgainstHashSet() {
    ImmutableList<Integer> allElements = ImmutableList.of(4233352, 3284593,
        3794208, 3849533, 4013967, 2902658, 1886275, 2131109, 985872, 1843868);
    for (int i = 0; i < allElements.size(); i++) {
      Set<Integer> elements = newHashSet(allElements.subList(0, i));
      Set<Set<Integer>> powerSet1 = powerSet(elements);
      Set<Set<Integer>> powerSet2 = powerSet(elements);
      new EqualsTester()
          .addEqualityGroup(powerSet1, powerSet2, toHashSets(powerSet1))
          .addEqualityGroup(ImmutableSet.of())
          .addEqualityGroup(ImmutableSet.of(9999999))
          .addEqualityGroup("notASet")
          .testEquals();
      assertEquals(toHashSets(powerSet1).hashCode(), powerSet1.hashCode());
    }
  }

  /**
   * Test that a hash code miscomputed by "input.hashCode() * tooFarValue / 2"
   * is correct under our {@code hashCode} implementation.
   */
  public void testPowerSetHashCode_inputHashCodeTimesTooFarValueIsZero() {
    Set<Object> sumToEighthMaxIntElements =
        newHashSet(objectWithHashCode(1 << 29), objectWithHashCode(0));
    assertPowerSetHashCode(1 << 30, sumToEighthMaxIntElements);

    Set<Object> sumToQuarterMaxIntElements =
        newHashSet(objectWithHashCode(1 << 30), objectWithHashCode(0));
    assertPowerSetHashCode(1 << 31, sumToQuarterMaxIntElements);
  }

  public void testPowerSetShowOff() {
    Set<Object> zero = ImmutableSet.of();
    Set<Set<Object>> one = powerSet(zero);
    Set<Set<Set<Object>>> two = powerSet(one);
    Set<Set<Set<Set<Object>>>> four = powerSet(two);
    Set<Set<Set<Set<Set<Object>>>>> sixteen = powerSet(four);
    Set<Set<Set<Set<Set<Set<Object>>>>>> sixtyFiveThousandish =
        powerSet(sixteen);
    assertEquals(1 << 16, sixtyFiveThousandish.size());

    assertTrue(powerSet(makeSetOfZeroToTwentyNine())
        .contains(makeSetOfZeroToTwentyNine()));
    assertFalse(powerSet(makeSetOfZeroToTwentyNine())
        .contains(ImmutableSet.of(30)));
  }

  private static Set<Integer> makeSetOfZeroToTwentyNine() {
    // TODO: use Range once it's publicly available
    Set<Integer> zeroToTwentyNine = newHashSet();
    for (int i = 0; i < 30; i++) {
      zeroToTwentyNine.add(i);
    }
    return zeroToTwentyNine;
  }

  private static <E> Set<Set<E>> toHashSets(Set<Set<E>> powerSet) {
    Set<Set<E>> result = newHashSet();
    for (Set<E> subset : powerSet) {
      result.add(new HashSet<E>(subset));
    }
    return result;
  }

  private static Object objectWithHashCode(final int hashCode) {
    return new Object() {
      @Override public int hashCode() {
        return hashCode;
      }
    };
  }

  private static void assertPowerSetHashCode(int expected, Set<?> elements) {
    assertEquals(expected, powerSet(elements).hashCode());
  }

  private static void assertPowerSetSize(int i, Object... elements) {
    assertEquals(i, powerSet(newHashSet(elements)).size());
  }

  private static void checkHashCode(Set<?> set) {
    assertEquals(Sets.newHashSet(set).hashCode(), set.hashCode());
  }

  private static <E> Set<E> set(E... elements) {
    return ImmutableSet.copyOf(elements);
  }

  private static <E> List<E> list(E... elements) {
    return ImmutableList.copyOf(elements);
  }

  /**
   * Utility method to verify that the given LinkedHashSet is equal to and
   * hashes identically to a set constructed with the elements in the given
   * collection.  Also verifies that the ordering in the set is the same
   * as the ordering of the given contents.
   */
  private static <E> void verifyLinkedHashSetContents(
      LinkedHashSet<E> set, Collection<E> contents) {
    assertEquals("LinkedHashSet should have preserved order for iteration",
        new ArrayList<E>(set), new ArrayList<E>(contents));
    verifySetContents(set, contents);
  }

  /**
   * Utility method to verify that the given SortedSet is equal to and
   * hashes identically to a set constructed with the elements in the
   * given iterable.  Also verifies that the comparator is the same as the
   * given comparator.
   */
  private static <E> void verifySortedSetContents(
      SortedSet<E> set, Iterable<E> iterable,
      @Nullable Comparator<E> comparator) {
    assertSame(comparator, set.comparator());
    verifySetContents(set, iterable);
  }

  /**
   * Utility method that verifies that the given set is equal to and hashes
   * identically to a set constructed with the elements in the given iterable.
   */
  private static <E> void verifySetContents(Set<E> set, Iterable<E> contents) {
    Set<E> expected = null;
    if (contents instanceof Set) {
      expected = (Set<E>) contents;
    } else {
      expected = new HashSet<E>();
      for (E element : contents) {
        expected.add(element);
      }
    }
    assertEquals(expected, set);
  }

  /**
   * Simple base class to verify that we handle generics correctly.
   */
  static class Base implements Comparable<Base>, Serializable {
    private final String s;

    public Base(String s) {
      this.s = s;
    }

    @Override public int hashCode() { // delegate to 's'
      return s.hashCode();
    }

    @Override public boolean equals(Object other) {
      if (other == null) {
        return false;
      } else if (other instanceof Base) {
        return s.equals(((Base) other).s);
      } else {
        return false;
      }
    }

    @Override
    public int compareTo(Base o) {
      return s.compareTo(o.s);
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Simple derived class to verify that we handle generics correctly.
   */
  static class Derived extends Base {
    public Derived(String s) {
      super(s);
    }

    private static final long serialVersionUID = 0;
  }

  void ensureNotDirectlyModifiable(SortedSet<Integer> unmod) {
    try {
      unmod.add(4);
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      unmod.remove(4);
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      unmod.addAll(Collections.singleton(4));
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      Iterator<Integer> iterator = unmod.iterator();
      iterator.next();
      iterator.remove();
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
  }
}
