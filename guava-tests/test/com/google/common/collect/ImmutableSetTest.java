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

import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * Unit test for {@link ImmutableSet}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Nick Kralevich
 */
@GwtCompatible(emulated = true)
public class ImmutableSetTest extends AbstractImmutableSetTest {

  @Override protected Set<String> of() {
    return ImmutableSet.of();
  }

  @Override protected Set<String> of(String e) {
    return ImmutableSet.of(e);
  }

  @Override protected Set<String> of(String e1, String e2) {
    return ImmutableSet.of(e1, e2);
  }

  @Override protected Set<String> of(String e1, String e2, String e3) {
    return ImmutableSet.of(e1, e2, e3);
  }

  @Override protected Set<String> of(
      String e1, String e2, String e3, String e4) {
    return ImmutableSet.of(e1, e2, e3, e4);
  }

  @Override protected Set<String> of(
      String e1, String e2, String e3, String e4, String e5) {
    return ImmutableSet.of(e1, e2, e3, e4, e5);
  }

  @Override protected Set<String> of(String e1, String e2, String e3,
      String e4, String e5, String e6, String... rest) {
    return ImmutableSet.of(e1, e2, e3, e4, e5, e6, rest);
  }

  @Override protected Set<String> copyOf(String[] elements) {
    return ImmutableSet.copyOf(elements);
  }

  @Override protected Set<String> copyOf(Collection<String> elements) {
    return ImmutableSet.copyOf(elements);
  }

  @Override protected Set<String> copyOf(Iterable<String> elements) {
    return ImmutableSet.copyOf(elements);
  }

  @Override protected Set<String> copyOf(Iterator<String> elements) {
    return ImmutableSet.copyOf(elements);
  }

  public void testCreation_allDuplicates() {
    ImmutableSet<String> set = ImmutableSet.copyOf(Lists.newArrayList("a", "a"));
    assertTrue(set instanceof SingletonImmutableSet);
    assertEquals(Lists.newArrayList("a"), Lists.newArrayList(set));
  }

  public void testCreation_oneDuplicate() {
    // now we'll get the varargs overload
    ImmutableSet<String> set = ImmutableSet.of(
        "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "a");
    assertEquals(Lists.newArrayList(
        "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m"),
        Lists.newArrayList(set));
  }

  public void testCreation_manyDuplicates() {
    // now we'll get the varargs overload
    ImmutableSet<String> set = ImmutableSet.of(
        "a", "b", "c", "c", "c", "c", "b", "b", "a", "a", "c", "c", "c", "a");
    ASSERT.that(set).hasContentsInOrder("a", "b", "c");
  }

  public void testCreation_arrayOfArray() {
    String[] array = new String[] { "a" };
    Set<String[]> set = ImmutableSet.<String[]>of(array);
    assertEquals(Collections.singleton(array), set);
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointers() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(ImmutableSet.class);
  }

  @GwtIncompatible("ImmutableSet.chooseTableSize")
  public void testChooseTableSize() {
    assertEquals(8, ImmutableSet.chooseTableSize(3));
    assertEquals(16, ImmutableSet.chooseTableSize(4));

    assertEquals(1 << 30, ImmutableSet.chooseTableSize(1 << 28));
    assertEquals(1 << 30, ImmutableSet.chooseTableSize(1 << 29 - 1));

    // Now we hit the cap
    assertEquals(1 << 30, ImmutableSet.chooseTableSize(1 << 29));
    assertEquals(1 << 30, ImmutableSet.chooseTableSize(1 << 30 - 1));

    // Now we've gone too far
    try {
      ImmutableSet.chooseTableSize(1 << 30);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible("RegularImmutableSet.table not in emulation")
  public void testResizeTable() {
    verifyTableSize(100, 2, 8);
    verifyTableSize(100, 5, 16);
    verifyTableSize(100, 33, 256);
    verifyTableSize(17, 17, 64);
    verifyTableSize(17, 16, 64);
    verifyTableSize(17, 15, 64);
  }

  @GwtIncompatible("RegularImmutableSet.table not in emulation")
  private void verifyTableSize(int inputSize, int setSize, int tableSize) {
    Builder<Integer> builder = ImmutableSet.builder();
    for (int i = 0; i < inputSize; i++) {
      builder.add(i % setSize);
    }
    ImmutableSet<Integer> set = builder.build();
    assertTrue(set instanceof RegularImmutableSet);
    assertEquals("Input size " + inputSize + " and set size " + setSize,
        tableSize, ((RegularImmutableSet<Integer>) set).table.length);
  }

  public void testCopyOf_copiesImmutableSortedSet() {
    ImmutableSortedSet<String> sortedSet = ImmutableSortedSet.of("a");
    ImmutableSet<String> copy = ImmutableSet.copyOf(sortedSet);
    assertNotSame(sortedSet, copy);
  }

  @GwtIncompatible("GWT is single threaded")
  public void testCopyOf_threadSafe() {
    verifyThreadSafe();
  }

  public void testAsList() {
    ImmutableSet<String> set = ImmutableSet.of("a", "b", "c", "d", "e");
    ImmutableList<String> list = set.asList();
    assertEquals(ImmutableList.of("a", "b", "c", "d", "e"), list);
  }

  @GwtIncompatible("SerializableTester, ImmutableAsList")
  public void testAsListReturnTypeAndSerialization() {
    ImmutableSet<String> set = ImmutableSet.of("a", "b", "c", "d", "e");
    ImmutableList<String> list = set.asList();
    assertTrue(list instanceof ImmutableAsList);
    ImmutableList<String> copy = SerializableTester.reserializeAndAssert(list);
    assertTrue(copy instanceof ImmutableAsList);
  }

  @Override <E extends Comparable<E>> Builder<E> builder() {
    return ImmutableSet.builder();
  }

  @Override int getComplexBuilderSetLastElement() {
    return LAST_COLOR_ADDED;
  }
}
