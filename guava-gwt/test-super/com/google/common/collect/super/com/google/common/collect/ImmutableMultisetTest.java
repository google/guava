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

import static java.util.Arrays.asList;
import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.google.UnmodifiableCollectionTests;
import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Tests for {@link ImmutableMultiset}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class ImmutableMultisetTest extends TestCase {

  public void testCreation_noArgs() {
    Multiset<String> multiset = ImmutableMultiset.of();
    assertTrue(multiset.isEmpty());
  }

  public void testCreation_oneElement() {
    Multiset<String> multiset = ImmutableMultiset.of("a");
    assertEquals(HashMultiset.create(asList("a")), multiset);
  }

  public void testCreation_twoElements() {
    Multiset<String> multiset = ImmutableMultiset.of("a", "b");
    assertEquals(HashMultiset.create(asList("a", "b")), multiset);
  }

  public void testCreation_threeElements() {
    Multiset<String> multiset = ImmutableMultiset.of("a", "b", "c");
    assertEquals(HashMultiset.create(asList("a", "b", "c")), multiset);
  }

  public void testCreation_fourElements() {
    Multiset<String> multiset = ImmutableMultiset.of("a", "b", "c", "d");
    assertEquals(HashMultiset.create(asList("a", "b", "c", "d")), multiset);
  }

  public void testCreation_fiveElements() {
    Multiset<String> multiset = ImmutableMultiset.of("a", "b", "c", "d", "e");
    assertEquals(HashMultiset.create(asList("a", "b", "c", "d", "e")),
        multiset);
  }

  public void testCreation_sixElements() {
    Multiset<String> multiset = ImmutableMultiset.of(
        "a", "b", "c", "d", "e", "f");
    assertEquals(HashMultiset.create(asList("a", "b", "c", "d", "e", "f")),
        multiset);
  }

  public void testCreation_sevenElements() {
    Multiset<String> multiset = ImmutableMultiset.of(
        "a", "b", "c", "d", "e", "f", "g");
    assertEquals(
        HashMultiset.create(asList("a", "b", "c", "d", "e", "f", "g")),
        multiset);
  }

  public void testCreation_emptyArray() {
    String[] array = new String[0];
    Multiset<String> multiset = ImmutableMultiset.copyOf(array);
    assertTrue(multiset.isEmpty());
  }

  public void testCreation_arrayOfOneElement() {
    String[] array = new String[] { "a" };
    Multiset<String> multiset = ImmutableMultiset.copyOf(array);
    assertEquals(HashMultiset.create(asList("a")), multiset);
  }

  public void testCreation_arrayOfArray() {
    String[] array = new String[] { "a" };
    Multiset<String[]> multiset = ImmutableMultiset.<String[]>of(array);
    Multiset<String[]> expected = HashMultiset.create();
    expected.add(array);
    assertEquals(expected, multiset);
  }

  public void testCreation_arrayContainingOnlyNull() {
    String[] array = new String[] { null };
    try {
      ImmutableMultiset.copyOf(array);
      fail();
    } catch (NullPointerException expected) {}
  }

  public void testCopyOf_collection_empty() {
    // "<String>" is required to work around a javac 1.5 bug.
    Collection<String> c = MinimalCollection.<String>of();
    Multiset<String> multiset = ImmutableMultiset.copyOf(c);
    assertTrue(multiset.isEmpty());
  }

  public void testCopyOf_collection_oneElement() {
    Collection<String> c = MinimalCollection.of("a");
    Multiset<String> multiset = ImmutableMultiset.copyOf(c);
    assertEquals(HashMultiset.create(asList("a")), multiset);
  }

  public void testCopyOf_collection_general() {
    Collection<String> c = MinimalCollection.of("a", "b", "a");
    Multiset<String> multiset = ImmutableMultiset.copyOf(c);
    assertEquals(HashMultiset.create(asList("a", "b", "a")), multiset);
  }

  public void testCopyOf_collectionContainingNull() {
    Collection<String> c = MinimalCollection.of("a", null, "b");
    try {
      ImmutableMultiset.copyOf(c);
      fail();
    } catch (NullPointerException expected) {}
  }

  public void testCopyOf_multiset_empty() {
    Multiset<String> c = HashMultiset.create();
    Multiset<String> multiset = ImmutableMultiset.copyOf(c);
    assertTrue(multiset.isEmpty());
  }

  public void testCopyOf_multiset_oneElement() {
    Multiset<String> c = HashMultiset.create(asList("a"));
    Multiset<String> multiset = ImmutableMultiset.copyOf(c);
    assertEquals(HashMultiset.create(asList("a")), multiset);
  }

  public void testCopyOf_multiset_general() {
    Multiset<String> c = HashMultiset.create(asList("a", "b", "a"));
    Multiset<String> multiset = ImmutableMultiset.copyOf(c);
    assertEquals(HashMultiset.create(asList("a", "b", "a")), multiset);
  }

  public void testCopyOf_multisetContainingNull() {
    Multiset<String> c = HashMultiset.create(asList("a", null, "b"));
    try {
      ImmutableMultiset.copyOf(c);
      fail();
    } catch (NullPointerException expected) {}
  }

  public void testCopyOf_iterator_empty() {
    Iterator<String> iterator = Iterators.emptyIterator();
    Multiset<String> multiset = ImmutableMultiset.copyOf(iterator);
    assertTrue(multiset.isEmpty());
  }

  public void testCopyOf_iterator_oneElement() {
    Iterator<String> iterator = Iterators.singletonIterator("a");
    Multiset<String> multiset = ImmutableMultiset.copyOf(iterator);
    assertEquals(HashMultiset.create(asList("a")), multiset);
  }

  public void testCopyOf_iterator_general() {
    Iterator<String> iterator = asList("a", "b", "a").iterator();
    Multiset<String> multiset = ImmutableMultiset.copyOf(iterator);
    assertEquals(HashMultiset.create(asList("a", "b", "a")), multiset);
  }

  public void testCopyOf_iteratorContainingNull() {
    Iterator<String> iterator = asList("a", null, "b").iterator();
    try {
      ImmutableMultiset.copyOf(iterator);
      fail();
    } catch (NullPointerException expected) {}
  }

  private static class CountingIterable implements Iterable<String> {
    int count = 0;
    @Override
    public Iterator<String> iterator() {
      count++;
      return asList("a", "b", "a").iterator();
    }
  }

  public void testCopyOf_plainIterable() {
    CountingIterable iterable = new CountingIterable();
    Multiset<String> multiset = ImmutableMultiset.copyOf(iterable);
    assertEquals(HashMultiset.create(asList("a", "b", "a")), multiset);
    assertEquals(1, iterable.count);
  }

  public void testCopyOf_shortcut_empty() {
    Collection<String> c = ImmutableMultiset.of();
    assertSame(c, ImmutableMultiset.copyOf(c));
  }

  public void testCopyOf_shortcut_singleton() {
    Collection<String> c = ImmutableMultiset.of("a");
    assertSame(c, ImmutableMultiset.copyOf(c));
  }

  public void testCopyOf_shortcut_immutableMultiset() {
    Collection<String> c = ImmutableMultiset.of("a", "b", "c");
    assertSame(c, ImmutableMultiset.copyOf(c));
  }

  public void testBuilderAdd() {
    ImmutableMultiset<String> multiset = new ImmutableMultiset.Builder<String>()
        .add("a")
        .add("b")
        .add("a")
        .add("c")
        .build();
    assertEquals(HashMultiset.create(asList("a", "b", "a", "c")), multiset);
  }

  public void testBuilderAddAll() {
    List<String> a = asList("a", "b");
    List<String> b = asList("c", "d");
    ImmutableMultiset<String> multiset = new ImmutableMultiset.Builder<String>()
        .addAll(a)
        .addAll(b)
        .build();
    assertEquals(HashMultiset.create(asList("a", "b", "c", "d")), multiset);
  }

  public void testBuilderAddAllMultiset() {
    Multiset<String> a = HashMultiset.create(asList("a", "b", "b"));
    Multiset<String> b = HashMultiset.create(asList("c", "b"));
    ImmutableMultiset<String> multiset = new ImmutableMultiset.Builder<String>()
        .addAll(a)
        .addAll(b)
        .build();
    assertEquals(
        HashMultiset.create(asList("a", "b", "b", "b", "c")), multiset);
  }

  public void testBuilderAddAllIterator() {
    Iterator<String> iterator = asList("a", "b", "a", "c").iterator();
    ImmutableMultiset<String> multiset = new ImmutableMultiset.Builder<String>()
        .addAll(iterator)
        .build();
    assertEquals(HashMultiset.create(asList("a", "b", "a", "c")), multiset);
  }

  public void testBuilderAddCopies() {
    ImmutableMultiset<String> multiset = new ImmutableMultiset.Builder<String>()
        .addCopies("a", 2)
        .addCopies("b", 3)
        .addCopies("c", 0)
        .build();
    assertEquals(
        HashMultiset.create(asList("a", "a", "b", "b", "b")), multiset);
  }

  public void testBuilderSetCount() {
    ImmutableMultiset<String> multiset = new ImmutableMultiset.Builder<String>()
        .add("a")
        .setCount("a", 2)
        .setCount("b", 3)
        .build();
    assertEquals(
        HashMultiset.create(asList("a", "a", "b", "b", "b")), multiset);
  }

  public void testBuilderAddHandlesNullsCorrectly() {
    ImmutableMultiset.Builder<String> builder = ImmutableMultiset.builder();
    try {
      builder.add((String) null);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {}
  }

  public void testBuilderAddAllHandlesNullsCorrectly() {
    ImmutableMultiset.Builder<String> builder = ImmutableMultiset.builder();
    try {
      builder.addAll((Collection<String>) null);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {}

    builder = ImmutableMultiset.builder();
    List<String> listWithNulls = asList("a", null, "b");
    try {
      builder.addAll(listWithNulls);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {}

    builder = ImmutableMultiset.builder();
    Multiset<String> multisetWithNull
        = LinkedHashMultiset.create(asList("a", null, "b"));
    try {
      builder.addAll(multisetWithNull);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {}
  }

  public void testBuilderAddCopiesHandlesNullsCorrectly() {
    ImmutableMultiset.Builder<String> builder = ImmutableMultiset.builder();
    try {
      builder.addCopies(null, 2);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {}
  }

  public void testBuilderAddCopiesIllegal() {
    ImmutableMultiset.Builder<String> builder = ImmutableMultiset.builder();
    try {
      builder.addCopies("a", -2);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {}
  }

  public void testBuilderSetCountHandlesNullsCorrectly() {
    ImmutableMultiset.Builder<String> builder = ImmutableMultiset.builder();
    try {
      builder.setCount(null, 2);
      fail("expected NullPointerException");
    } catch (NullPointerException expected) {}
  }

  public void testBuilderSetCountIllegal() {
    ImmutableMultiset.Builder<String> builder = ImmutableMultiset.builder();
    try {
      builder.setCount("a", -2);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {}
  }

  public void testEquals_immutableMultiset() {
    Collection<String> c = ImmutableMultiset.of("a", "b", "a");
    assertEquals(c, ImmutableMultiset.of("a", "b", "a"));
    assertEquals(c, ImmutableMultiset.of("a", "a", "b"));
    ASSERT.that(c).isNotEqualTo(ImmutableMultiset.of("a", "b"));
    ASSERT.that(c).isNotEqualTo(ImmutableMultiset.of("a", "b", "c", "d"));
  }

  public void testIterationOrder() {
    Collection<String> c = ImmutableMultiset.of("a", "b", "a");
    ASSERT.that(c).has().exactly("a", "a", "b").inOrder();
  }

  public void testMultisetWrites() {
    Multiset<String> multiset = ImmutableMultiset.of("a", "b", "a");
    UnmodifiableCollectionTests.assertMultisetIsUnmodifiable(multiset, "test");
  }

  public void testAsList() {
    ImmutableMultiset<String> multiset
        = ImmutableMultiset.of("a", "a", "b", "b", "b");
    ImmutableList<String> list = multiset.asList();
    assertEquals(ImmutableList.of("a", "a", "b", "b", "b"), list);
    assertEquals(2, list.indexOf("b"));
    assertEquals(4, list.lastIndexOf("b"));
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(ImmutableMultiset.of(), ImmutableMultiset.of())
        .addEqualityGroup(ImmutableMultiset.of(1), ImmutableMultiset.of(1))
        .addEqualityGroup(ImmutableMultiset.of(1, 1), ImmutableMultiset.of(1, 1))
        .addEqualityGroup(ImmutableMultiset.of(1, 2, 1), ImmutableMultiset.of(2, 1, 1))
        .testEquals();
  }
}
