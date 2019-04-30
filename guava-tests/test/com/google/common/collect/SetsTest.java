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
import static com.google.common.collect.Sets.unmodifiableNavigableSet;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.io.ObjectStreamConstants.TC_REFERENCE;
import static java.io.ObjectStreamConstants.baseWireHandle;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Predicate;
import com.google.common.collect.testing.AnEnum;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.MinimalIterable;
import com.google.common.collect.testing.NavigableSetTestSuiteBuilder;
import com.google.common.collect.testing.SafeTreeSet;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestEnumSetGenerator;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.SetFeature;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
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
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

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

  private static final Collection<Integer> EMPTY_COLLECTION = Arrays.<Integer>asList();

  private static final Collection<Integer> SOME_COLLECTION = Arrays.asList(0, 1, 1);

  private static final Iterable<Integer> SOME_ITERABLE =
      new Iterable<Integer>() {
        @Override
        public Iterator<Integer> iterator() {
          return SOME_COLLECTION.iterator();
        }
      };

  private static final List<Integer> LONGER_LIST = Arrays.asList(8, 6, 7, 5, 3, 0, 9);

  private static final Comparator<Integer> SOME_COMPARATOR = Collections.reverseOrder();

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(SetsTest.class);

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.newConcurrentHashSet(Arrays.asList(elements));
                  }
                })
            .named("Sets.newConcurrentHashSet")
            .withFeatures(CollectionSize.ANY, SetFeature.GENERAL_PURPOSE)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    int size = elements.length;
                    // Remove last element, if size > 1
                    Set<String> set1 =
                        (size > 1)
                            ? Sets.newHashSet(Arrays.asList(elements).subList(0, size - 1))
                            : Sets.newHashSet(elements);
                    // Remove first element, if size > 0
                    Set<String> set2 =
                        (size > 0)
                            ? Sets.newHashSet(Arrays.asList(elements).subList(1, size))
                            : Sets.<String>newHashSet();
                    return Sets.union(set1, set2);
                  }
                })
            .named("Sets.union")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Set<String> set1 = Sets.newHashSet(elements);
                    set1.add(samples().e3());
                    Set<String> set2 = Sets.newHashSet(elements);
                    set2.add(samples().e4());
                    return Sets.intersection(set1, set2);
                  }
                })
            .named("Sets.intersection")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Set<String> set1 = Sets.newHashSet(elements);
                    set1.add(samples().e3());
                    Set<String> set2 = Sets.newHashSet(samples().e3());
                    return Sets.difference(set1, set2);
                  }
                })
            .named("Sets.difference")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestEnumSetGenerator() {
                  @Override
                  protected Set<AnEnum> create(AnEnum[] elements) {
                    AnEnum[] otherElements = new AnEnum[elements.length - 1];
                    System.arraycopy(elements, 1, otherElements, 0, otherElements.length);
                    return Sets.immutableEnumSet(elements[0], otherElements);
                  }
                })
            .named("Sets.immutableEnumSet")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());

    suite.addTest(
        NavigableSetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    SafeTreeSet<String> set = new SafeTreeSet<>(Arrays.asList(elements));
                    return Sets.unmodifiableNavigableSet(set);
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Ordering.natural().sortedCopy(insertionOrder);
                  }
                })
            .named("Sets.unmodifiableNavigableSet[TreeSet]")
            .withFeatures(
                CollectionSize.ANY, CollectionFeature.KNOWN_ORDER, CollectionFeature.SERIALIZABLE)
            .createTestSuite());

    suite.addTest(testsForFilter());
    suite.addTest(testsForFilterNoNulls());
    suite.addTest(testsForFilterFiltered());

    return suite;
  }

  @GwtIncompatible // suite
  private static Test testsForFilter() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              public Set<String> create(String[] elements) {
                Set<String> unfiltered = Sets.newLinkedHashSet();
                unfiltered.add("yyy");
                Collections.addAll(unfiltered, elements);
                unfiltered.add("zzz");
                return Sets.filter(unfiltered, Collections2Test.NOT_YYY_ZZZ);
              }
            })
        .named("Sets.filter")
        .withFeatures(
            CollectionFeature.SUPPORTS_ADD,
            CollectionFeature.SUPPORTS_REMOVE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .createTestSuite();
  }

  @GwtIncompatible // suite
  private static Test testsForFilterNoNulls() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  public Set<String> create(String[] elements) {
                    Set<String> unfiltered = Sets.newLinkedHashSet();
                    unfiltered.add("yyy");
                    unfiltered.addAll(ImmutableList.copyOf(elements));
                    unfiltered.add("zzz");
                    return Sets.filter(unfiltered, Collections2Test.LENGTH_1);
                  }
                })
            .named("Sets.filter, no nulls")
            .withFeatures(
                CollectionFeature.SUPPORTS_ADD,
                CollectionFeature.SUPPORTS_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        NavigableSetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  public NavigableSet<String> create(String[] elements) {
                    NavigableSet<String> unfiltered = Sets.newTreeSet();
                    unfiltered.add("yyy");
                    unfiltered.addAll(ImmutableList.copyOf(elements));
                    unfiltered.add("zzz");
                    return Sets.filter(unfiltered, Collections2Test.LENGTH_1);
                  }

                  @Override
                  public List<String> order(List<String> insertionOrder) {
                    return Ordering.natural().sortedCopy(insertionOrder);
                  }
                })
            .named("Sets.filter[NavigableSet]")
            .withFeatures(
                CollectionFeature.SUPPORTS_ADD,
                CollectionFeature.SUPPORTS_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .createTestSuite());
    return suite;
  }

  @GwtIncompatible // suite
  private static Test testsForFilterFiltered() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              public Set<String> create(String[] elements) {
                Set<String> unfiltered = Sets.newLinkedHashSet();
                unfiltered.add("yyy");
                unfiltered.addAll(ImmutableList.copyOf(elements));
                unfiltered.add("zzz");
                unfiltered.add("abc");
                return Sets.filter(
                    Sets.filter(unfiltered, Collections2Test.LENGTH_1),
                    Collections2Test.NOT_YYY_ZZZ);
              }
            })
        .named("Sets.filter, filtered input")
        .withFeatures(
            CollectionFeature.SUPPORTS_ADD,
            CollectionFeature.SUPPORTS_REMOVE,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite();
  }

  private enum SomeEnum {
    A,
    B,
    C,
    D
  }

  public void testImmutableEnumSet() {
    Set<SomeEnum> units = Sets.immutableEnumSet(SomeEnum.D, SomeEnum.B);

    assertThat(units).containsExactly(SomeEnum.B, SomeEnum.D).inOrder();
    try {
      units.remove(SomeEnum.B);
      fail("ImmutableEnumSet should throw an exception on remove()");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      units.add(SomeEnum.C);
      fail("ImmutableEnumSet should throw an exception on add()");
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testToImmutableEnumSet() {
    Set<SomeEnum> units = Stream.of(SomeEnum.D, SomeEnum.B).collect(Sets.toImmutableEnumSet());

    assertThat(units).containsExactly(SomeEnum.B, SomeEnum.D).inOrder();
  }

  public void testToImmutableEnumSetEmpty() {
    Set<SomeEnum> units = Stream.<SomeEnum>empty().collect(Sets.toImmutableEnumSet());
    assertThat(units).isEmpty();
  }

  @GwtIncompatible // SerializableTester
  public void testImmutableEnumSet_serialized() {
    Set<SomeEnum> units = Sets.immutableEnumSet(SomeEnum.D, SomeEnum.B);

    assertThat(units).containsExactly(SomeEnum.B, SomeEnum.D).inOrder();

    Set<SomeEnum> copy = SerializableTester.reserializeAndAssert(units);
    assertTrue(copy instanceof ImmutableEnumSet);
  }

  public void testImmutableEnumSet_fromIterable() {
    ImmutableSet<SomeEnum> none = Sets.immutableEnumSet(MinimalIterable.<SomeEnum>of());
    assertThat(none).isEmpty();

    ImmutableSet<SomeEnum> one = Sets.immutableEnumSet(MinimalIterable.of(SomeEnum.B));
    assertThat(one).contains(SomeEnum.B);

    ImmutableSet<SomeEnum> two = Sets.immutableEnumSet(MinimalIterable.of(SomeEnum.D, SomeEnum.B));
    assertThat(two).containsExactly(SomeEnum.B, SomeEnum.D).inOrder();
  }

  @GwtIncompatible // java serialization not supported in GWT.
  public void testImmutableEnumSet_deserializationMakesDefensiveCopy() throws Exception {
    ImmutableSet<SomeEnum> original = Sets.immutableEnumSet(SomeEnum.A, SomeEnum.B);
    int handleOffset = 6;
    byte[] serializedForm = serializeWithBackReference(original, handleOffset);
    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedForm));

    ImmutableSet<?> deserialized = (ImmutableSet<?>) in.readObject();
    EnumSet<?> delegate = (EnumSet<?>) in.readObject();

    assertEquals(original, deserialized);
    assertTrue(delegate.remove(SomeEnum.A));
    assertTrue(deserialized.contains(SomeEnum.A));
  }

  @GwtIncompatible // java serialization not supported in GWT.
  private static byte[] serializeWithBackReference(Object original, int handleOffset)
      throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bos);

    out.writeObject(original);

    byte[] handle = toByteArray(baseWireHandle + handleOffset);
    byte[] ref = prepended(TC_REFERENCE, handle);
    bos.write(ref);

    return bos.toByteArray();
  }

  private static byte[] prepended(byte b, byte[] array) {
    byte[] out = new byte[array.length + 1];
    out[0] = b;
    System.arraycopy(array, 0, out, 1, array.length);
    return out;
  }

  @GwtIncompatible // java.nio.ByteBuffer
  private static byte[] toByteArray(int h) {
    return ByteBuffer.allocate(4).putInt(h).array();
  }

  public void testNewEnumSet_empty() {
    EnumSet<SomeEnum> copy = newEnumSet(Collections.<SomeEnum>emptySet(), SomeEnum.class);
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
    LinkedHashSet<Integer> set =
        Sets.newLinkedHashSet(
            new Iterable<Integer>() {
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
    assertThat(set).containsExactly(new Derived("bar"), new Derived("foo")).inOrder();
  }

  public void testNewTreeSetEmptyNonGeneric() {
    TreeSet<LegacyComparable> set = Sets.newTreeSet();
    assertTrue(set.isEmpty());
    set.add(new LegacyComparable("foo"));
    set.add(new LegacyComparable("bar"));
    assertThat(set)
        .containsExactly(new LegacyComparable("bar"), new LegacyComparable("foo"))
        .inOrder();
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
    Iterable<Derived> iterable = Arrays.asList(new Derived("foo"), new Derived("bar"));
    TreeSet<Derived> set = Sets.newTreeSet(iterable);
    assertThat(set).containsExactly(new Derived("bar"), new Derived("foo")).inOrder();
  }

  public void testNewTreeSetFromIterableNonGeneric() {
    Iterable<LegacyComparable> iterable =
        Arrays.asList(new LegacyComparable("foo"), new LegacyComparable("bar"));
    TreeSet<LegacyComparable> set = Sets.newTreeSet(iterable);
    assertThat(set)
        .containsExactly(new LegacyComparable("bar"), new LegacyComparable("foo"))
        .inOrder();
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

  @GwtIncompatible // CopyOnWriteArraySet
  public void testNewCOWASEmpty() {
    CopyOnWriteArraySet<Integer> set = Sets.newCopyOnWriteArraySet();
    verifySetContents(set, EMPTY_COLLECTION);
  }

  @GwtIncompatible // CopyOnWriteArraySet
  public void testNewCOWASFromIterable() {
    CopyOnWriteArraySet<Integer> set = Sets.newCopyOnWriteArraySet(SOME_ITERABLE);
    verifySetContents(set, SOME_COLLECTION);
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
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointerExceptions() {
    new NullPointerTester()
        .setDefault(Enum.class, SomeEnum.A)
        .setDefault(Class.class, SomeEnum.class) // for newEnumSet
        .testAllPublicStaticMethods(Sets.class);
  }

  public void testNewSetFromMap() {
    Set<Integer> set = Sets.newSetFromMap(new HashMap<Integer, Boolean>());
    set.addAll(SOME_COLLECTION);
    verifySetContents(set, SOME_COLLECTION);
  }

  @GwtIncompatible // SerializableTester
  public void testNewSetFromMapSerialization() {
    Set<Integer> set = Sets.newSetFromMap(new LinkedHashMap<Integer, Boolean>());
    set.addAll(SOME_COLLECTION);
    Set<Integer> copy = SerializableTester.reserializeAndAssert(set);
    assertThat(copy).containsExactly(0, 1).inOrder();
  }

  public void testNewSetFromMapIllegal() {
    Map<Integer, Boolean> map = new LinkedHashMap<>();
    map.put(2, true);
    try {
      Sets.newSetFromMap(map);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  // TODO: the overwhelming number of suppressions below suggests that maybe
  // it's not worth having a varargs form of this method at all...

  /** The 0-ary cartesian product is a single empty list. */
  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_zeroary() {
    assertThat(Sets.cartesianProduct()).containsExactly(list());
  }

  /** A unary cartesian product is one list of size 1 for each element in the input set. */
  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_unary() {
    assertThat(Sets.cartesianProduct(set(1, 2))).containsExactly(list(1), list(2));
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
    assertThat(Sets.cartesianProduct(set(1), set(2))).contains(list(1, 2));
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_binary1x2() {
    assertThat(Sets.cartesianProduct(set(1), set(2, 3)))
        .containsExactly(list(1, 2), list(1, 3))
        .inOrder();
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_binary2x2() {
    assertThat(Sets.cartesianProduct(set(1, 2), set(3, 4)))
        .containsExactly(list(1, 3), list(1, 4), list(2, 3), list(2, 4))
        .inOrder();
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProduct_2x2x2() {
    assertThat(Sets.cartesianProduct(set(0, 1), set(0, 1), set(0, 1)))
        .containsExactly(
            list(0, 0, 0),
            list(0, 0, 1),
            list(0, 1, 0),
            list(0, 1, 1),
            list(1, 0, 0),
            list(1, 0, 1),
            list(1, 1, 0),
            list(1, 1, 1))
        .inOrder();
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

    assertThat(Sets.<Object>cartesianProduct(x, y))
        .containsExactly(exp1, exp2, exp3, exp4)
        .inOrder();
  }

  @SuppressWarnings("unchecked") // varargs!
  public void testCartesianProductTooBig() {
    Set<Integer> set = ContiguousSet.create(Range.closed(0, 10000), DiscreteDomain.integers());
    try {
      Sets.cartesianProduct(set, set, set, set, set);
      fail("Expected IAE");
    } catch (IllegalArgumentException expected) {
    }
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
    checkHashCode(Sets.cartesianProduct(set(1, num), set(2, num - 1), set(3, num + 1)));

    // a bigger one
    checkHashCode(
        Sets.cartesianProduct(set(1, num, num + 1), set(2), set(3, num + 2), set(4, 5, 6, 7, 8)));
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
    assertFalse(powerSet.contains((Object) "notASet"));
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

  @GwtIncompatible // too slow for GWT
  public void testPowerSetIteration_iteratorTester() {
    ImmutableSet<Integer> elements = ImmutableSet.of(1, 2);

    Set<Set<Integer>> expected = newLinkedHashSet();
    expected.add(ImmutableSet.<Integer>of());
    expected.add(ImmutableSet.of(1));
    expected.add(ImmutableSet.of(2));
    expected.add(ImmutableSet.of(1, 2));

    final Set<Set<Integer>> powerSet = powerSet(elements);
    new IteratorTester<Set<Integer>>(6, UNMODIFIABLE, expected, KNOWN_ORDER) {
      @Override
      protected Iterator<Set<Integer>> newTargetIterator() {
        return powerSet.iterator();
      }
    }.test();
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
      @Override
      protected Iterator<Set<Integer>> newTargetIterator() {
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
    assertPowerSetSize(
        1 << 30, 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
        'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4');
  }

  public void testPowerSetCreationErrors() {
    try {
      Set<Set<Character>> unused =
          powerSet(
              newHashSet(
                  'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
                  'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4', '5'));
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      Set<Set<Integer>> unused = powerSet(ContiguousSet.closed(0, Integer.MAX_VALUE / 2));
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
    ImmutableList<Integer> allElements =
        ImmutableList.of(
            4233352, 3284593, 3794208, 3849533, 4013967, 2902658, 1886275, 2131109, 985872,
            1843868);
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
   * Test that a hash code miscomputed by "input.hashCode() * tooFarValue / 2" is correct under our
   * {@code hashCode} implementation.
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
    Set<Set<Set<Set<Set<Set<Object>>>>>> sixtyFiveThousandish = powerSet(sixteen);
    assertEquals(1 << 16, sixtyFiveThousandish.size());

    assertTrue(powerSet(makeSetOfZeroToTwentyNine()).contains(makeSetOfZeroToTwentyNine()));
    assertFalse(powerSet(makeSetOfZeroToTwentyNine()).contains(ImmutableSet.of(30)));
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
      @Override
      public int hashCode() {
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

  public void testCombinations() {
    ImmutableList<Set<Integer>> sampleSets =
        ImmutableList.<Set<Integer>>of(
            ImmutableSet.<Integer>of(),
            ImmutableSet.of(1, 2),
            ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    for (Set<Integer> sampleSet : sampleSets) {
      for (int k = 0; k <= sampleSet.size(); k++) {
        final int size = k;
        Set<Set<Integer>> expected =
            Sets.filter(
                Sets.powerSet(sampleSet),
                new Predicate<Set<Integer>>() {

                  @Override
                  public boolean apply(Set<Integer> input) {
                    return input.size() == size;
                  }
                });
        assertWithMessage("Sets.combinations(%s, %s)", sampleSet, k)
            .that(Sets.combinations(sampleSet, k))
            .containsExactlyElementsIn(expected)
            .inOrder();
      }
    }
  }

  private static <E> Set<E> set(E... elements) {
    return ImmutableSet.copyOf(elements);
  }

  private static <E> List<E> list(E... elements) {
    return ImmutableList.copyOf(elements);
  }

  /**
   * Utility method to verify that the given LinkedHashSet is equal to and hashes identically to a
   * set constructed with the elements in the given collection. Also verifies that the ordering in
   * the set is the same as the ordering of the given contents.
   */
  private static <E> void verifyLinkedHashSetContents(
      LinkedHashSet<E> set, Collection<E> contents) {
    assertEquals(
        "LinkedHashSet should have preserved order for iteration",
        new ArrayList<E>(set),
        new ArrayList<E>(contents));
    verifySetContents(set, contents);
  }

  /**
   * Utility method to verify that the given SortedSet is equal to and hashes identically to a set
   * constructed with the elements in the given iterable. Also verifies that the comparator is the
   * same as the given comparator.
   */
  private static <E> void verifySortedSetContents(
      SortedSet<E> set, Iterable<E> iterable, @Nullable Comparator<E> comparator) {
    assertSame(comparator, set.comparator());
    verifySetContents(set, iterable);
  }

  /**
   * Utility method that verifies that the given set is equal to and hashes identically to a set
   * constructed with the elements in the given iterable.
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

  /** Simple base class to verify that we handle generics correctly. */
  static class Base implements Comparable<Base>, Serializable {
    private final String s;

    public Base(String s) {
      this.s = s;
    }

    @Override
    public int hashCode() { // delegate to 's'
      return s.hashCode();
    }

    @Override
    public boolean equals(Object other) {
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

  /** Simple derived class to verify that we handle generics correctly. */
  static class Derived extends Base {
    public Derived(String s) {
      super(s);
    }

    private static final long serialVersionUID = 0;
  }

  @GwtIncompatible // NavigableSet
  public void testUnmodifiableNavigableSet() {
    TreeSet<Integer> mod = Sets.newTreeSet();
    mod.add(1);
    mod.add(2);
    mod.add(3);

    NavigableSet<Integer> unmod = unmodifiableNavigableSet(mod);

    /* Unmodifiable is a view. */
    mod.add(4);
    assertTrue(unmod.contains(4));
    assertTrue(unmod.descendingSet().contains(4));

    ensureNotDirectlyModifiable(unmod);
    ensureNotDirectlyModifiable(unmod.descendingSet());
    ensureNotDirectlyModifiable(unmod.headSet(2));
    ensureNotDirectlyModifiable(unmod.headSet(2, true));
    ensureNotDirectlyModifiable(unmod.tailSet(2));
    ensureNotDirectlyModifiable(unmod.tailSet(2, true));
    ensureNotDirectlyModifiable(unmod.subSet(1, 3));
    ensureNotDirectlyModifiable(unmod.subSet(1, true, 3, true));

    /* UnsupportedOperationException on indirect modifications. */
    NavigableSet<Integer> reverse = unmod.descendingSet();
    try {
      reverse.add(4);
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      reverse.addAll(Collections.singleton(4));
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      reverse.remove(4);
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
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

  @GwtIncompatible // NavigableSet
  void ensureNotDirectlyModifiable(NavigableSet<Integer> unmod) {
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
      unmod.pollFirst();
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
    try {
      unmod.pollLast();
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
    try {
      Iterator<Integer> iterator = unmod.descendingIterator();
      iterator.next();
      iterator.remove();
      fail("UnsupportedOperationException expected");
    } catch (UnsupportedOperationException expected) {
    }
  }

  @GwtIncompatible // NavigableSet
  public void testSubSet_boundedRange() {
    ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(2, 4, 6, 8, 10);
    ImmutableSortedSet<Integer> empty = ImmutableSortedSet.of();

    assertEquals(set, Sets.subSet(set, Range.closed(0, 12)));
    assertEquals(ImmutableSortedSet.of(2, 4), Sets.subSet(set, Range.closed(0, 4)));
    assertEquals(ImmutableSortedSet.of(2, 4, 6), Sets.subSet(set, Range.closed(2, 6)));
    assertEquals(ImmutableSortedSet.of(4, 6), Sets.subSet(set, Range.closed(3, 7)));
    assertEquals(empty, Sets.subSet(set, Range.closed(20, 30)));

    assertEquals(set, Sets.subSet(set, Range.open(0, 12)));
    assertEquals(ImmutableSortedSet.of(2), Sets.subSet(set, Range.open(0, 4)));
    assertEquals(ImmutableSortedSet.of(4), Sets.subSet(set, Range.open(2, 6)));
    assertEquals(ImmutableSortedSet.of(4, 6), Sets.subSet(set, Range.open(3, 7)));
    assertEquals(empty, Sets.subSet(set, Range.open(20, 30)));

    assertEquals(set, Sets.subSet(set, Range.openClosed(0, 12)));
    assertEquals(ImmutableSortedSet.of(2, 4), Sets.subSet(set, Range.openClosed(0, 4)));
    assertEquals(ImmutableSortedSet.of(4, 6), Sets.subSet(set, Range.openClosed(2, 6)));
    assertEquals(ImmutableSortedSet.of(4, 6), Sets.subSet(set, Range.openClosed(3, 7)));
    assertEquals(empty, Sets.subSet(set, Range.openClosed(20, 30)));

    assertEquals(set, Sets.subSet(set, Range.closedOpen(0, 12)));
    assertEquals(ImmutableSortedSet.of(2), Sets.subSet(set, Range.closedOpen(0, 4)));
    assertEquals(ImmutableSortedSet.of(2, 4), Sets.subSet(set, Range.closedOpen(2, 6)));
    assertEquals(ImmutableSortedSet.of(4, 6), Sets.subSet(set, Range.closedOpen(3, 7)));
    assertEquals(empty, Sets.subSet(set, Range.closedOpen(20, 30)));
  }

  @GwtIncompatible // NavigableSet
  public void testSubSet_halfBoundedRange() {
    ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(2, 4, 6, 8, 10);
    ImmutableSortedSet<Integer> empty = ImmutableSortedSet.of();

    assertEquals(set, Sets.subSet(set, Range.atLeast(0)));
    assertEquals(ImmutableSortedSet.of(4, 6, 8, 10), Sets.subSet(set, Range.atLeast(4)));
    assertEquals(ImmutableSortedSet.of(8, 10), Sets.subSet(set, Range.atLeast(7)));
    assertEquals(empty, Sets.subSet(set, Range.atLeast(20)));

    assertEquals(set, Sets.subSet(set, Range.greaterThan(0)));
    assertEquals(ImmutableSortedSet.of(6, 8, 10), Sets.subSet(set, Range.greaterThan(4)));
    assertEquals(ImmutableSortedSet.of(8, 10), Sets.subSet(set, Range.greaterThan(7)));
    assertEquals(empty, Sets.subSet(set, Range.greaterThan(20)));

    assertEquals(empty, Sets.subSet(set, Range.lessThan(0)));
    assertEquals(ImmutableSortedSet.of(2), Sets.subSet(set, Range.lessThan(4)));
    assertEquals(ImmutableSortedSet.of(2, 4, 6), Sets.subSet(set, Range.lessThan(7)));
    assertEquals(set, Sets.subSet(set, Range.lessThan(20)));

    assertEquals(empty, Sets.subSet(set, Range.atMost(0)));
    assertEquals(ImmutableSortedSet.of(2, 4), Sets.subSet(set, Range.atMost(4)));
    assertEquals(ImmutableSortedSet.of(2, 4, 6), Sets.subSet(set, Range.atMost(7)));
    assertEquals(set, Sets.subSet(set, Range.atMost(20)));
  }

  @GwtIncompatible // NavigableSet
  public void testSubSet_unboundedRange() {
    ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(2, 4, 6, 8, 10);

    assertEquals(set, Sets.subSet(set, Range.<Integer>all()));
  }

  @GwtIncompatible // NavigableSet
  public void testSubSet_unnaturalOrdering() {
    ImmutableSortedSet<Integer> set =
        ImmutableSortedSet.<Integer>reverseOrder().add(2, 4, 6, 8, 10).build();

    try {
      Sets.subSet(set, Range.closed(4, 8));
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException expected) {
    }

    // These results are all incorrect, but there's no way (short of iterating over the result)
    // to verify that with an arbitrary ordering or comparator.
    assertEquals(ImmutableSortedSet.of(2, 4), Sets.subSet(set, Range.atLeast(4)));
    assertEquals(ImmutableSortedSet.of(8, 10), Sets.subSet(set, Range.atMost(8)));
    assertEquals(ImmutableSortedSet.of(2, 4, 6, 8, 10), Sets.subSet(set, Range.<Integer>all()));
  }
}
