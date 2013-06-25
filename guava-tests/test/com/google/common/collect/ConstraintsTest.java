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

import static java.util.Arrays.asList;
import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;

/**
 * Tests for {@code Constraints}.
 *
 * @author Mike Bostock
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class ConstraintsTest extends TestCase {

  private static final String TEST_ELEMENT = "test";

  private static final class TestElementException
      extends IllegalArgumentException {
    private static final long serialVersionUID = 0;
  }

  private static final Constraint<String> TEST_CONSTRAINT
      = new Constraint<String>() {
          @Override
          public String checkElement(String element) {
            if (TEST_ELEMENT.equals(element)) {
              throw new TestElementException();
            }
            return element;
          }
        };

  public void testNotNull() {
    Constraint<? super String> constraint = Constraints.notNull();
    assertSame(TEST_ELEMENT, constraint.checkElement(TEST_ELEMENT));
    try {
      constraint.checkElement(null);
      fail("NullPointerException expected");
    } catch (NullPointerException expected) {}
    assertEquals("Not null", constraint.toString());
  }

  public void testConstrainedCollectionLegal() {
    Collection<String> collection = Lists.newArrayList("foo", "bar");
    Collection<String> constrained = Constraints.constrainedCollection(
        collection, TEST_CONSTRAINT);
    collection.add(TEST_ELEMENT);
    constrained.add("qux");
    constrained.addAll(asList("cat", "dog"));
    /* equals and hashCode aren't defined for Collection */
    ASSERT.that(collection).has()
        .exactly("foo", "bar", TEST_ELEMENT, "qux", "cat", "dog").inOrder();
    ASSERT.that(constrained).has()
        .exactly("foo", "bar", TEST_ELEMENT, "qux", "cat", "dog").inOrder();
  }

  public void testConstrainedCollectionIllegal() {
    Collection<String> collection = Lists.newArrayList("foo", "bar");
    Collection<String> constrained = Constraints.constrainedCollection(
        collection, TEST_CONSTRAINT);
    try {
      constrained.add(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.addAll(asList("baz", TEST_ELEMENT));
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    ASSERT.that(constrained).has().exactly("foo", "bar").inOrder();
    ASSERT.that(collection).has().exactly("foo", "bar").inOrder();
  }

  public void testConstrainedSetLegal() {
    Set<String> set = Sets.newLinkedHashSet(asList("foo", "bar"));
    Set<String> constrained = Constraints.constrainedSet(set, TEST_CONSTRAINT);
    set.add(TEST_ELEMENT);
    constrained.add("qux");
    constrained.addAll(asList("cat", "dog"));
    assertTrue(set.equals(constrained));
    assertTrue(constrained.equals(set));
    assertEquals(set.toString(), constrained.toString());
    assertEquals(set.hashCode(), constrained.hashCode());
    ASSERT.that(set).has().exactly("foo", "bar", TEST_ELEMENT, "qux", "cat", "dog").inOrder();
    ASSERT.that(constrained).has()
        .exactly("foo", "bar", TEST_ELEMENT, "qux", "cat", "dog").inOrder();
  }

  public void testConstrainedSetIllegal() {
    Set<String> set = Sets.newLinkedHashSet(asList("foo", "bar"));
    Set<String> constrained = Constraints.constrainedSet(set, TEST_CONSTRAINT);
    try {
      constrained.add(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.addAll(asList("baz", TEST_ELEMENT));
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    ASSERT.that(constrained).has().exactly("foo", "bar").inOrder();
    ASSERT.that(set).has().exactly("foo", "bar").inOrder();
  }

  public void testConstrainedSortedSetLegal() {
    SortedSet<String> sortedSet = Sets.newTreeSet(asList("foo", "bar"));
    SortedSet<String> constrained = Constraints.constrainedSortedSet(
        sortedSet, TEST_CONSTRAINT);
    sortedSet.add(TEST_ELEMENT);
    constrained.add("qux");
    constrained.addAll(asList("cat", "dog"));
    assertTrue(sortedSet.equals(constrained));
    assertTrue(constrained.equals(sortedSet));
    assertEquals(sortedSet.toString(), constrained.toString());
    assertEquals(sortedSet.hashCode(), constrained.hashCode());
    ASSERT.that(sortedSet).has().exactly("bar", "cat", "dog", "foo", "qux", TEST_ELEMENT).inOrder();
    ASSERT.that(constrained).has()
        .exactly("bar", "cat", "dog", "foo", "qux", TEST_ELEMENT).inOrder();
    assertNull(constrained.comparator());
    assertEquals("bar", constrained.first());
    assertEquals(TEST_ELEMENT, constrained.last());
  }

  public void testConstrainedSortedSetIllegal() {
    SortedSet<String> sortedSet = Sets.newTreeSet(asList("foo", "bar"));
    SortedSet<String> constrained = Constraints.constrainedSortedSet(
        sortedSet, TEST_CONSTRAINT);
    try {
      constrained.add(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.subSet("bar", "foo").add(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.headSet("bar").add(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.tailSet("foo").add(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.addAll(asList("baz", TEST_ELEMENT));
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    ASSERT.that(constrained).has().exactly("bar", "foo").inOrder();
    ASSERT.that(sortedSet).has().exactly("bar", "foo").inOrder();
  }

  public void testConstrainedListLegal() {
    List<String> list = Lists.newArrayList("foo", "bar");
    List<String> constrained = Constraints.constrainedList(
        list, TEST_CONSTRAINT);
    list.add(TEST_ELEMENT);
    constrained.add("qux");
    constrained.addAll(asList("cat", "dog"));
    constrained.add(1, "cow");
    constrained.addAll(4, asList("box", "fan"));
    constrained.set(2, "baz");
    assertTrue(list.equals(constrained));
    assertTrue(constrained.equals(list));
    assertEquals(list.toString(), constrained.toString());
    assertEquals(list.hashCode(), constrained.hashCode());
    ASSERT.that(list).has().exactly(
        "foo", "cow", "baz", TEST_ELEMENT, "box", "fan", "qux", "cat", "dog").inOrder();
    ASSERT.that(constrained).has().exactly(
        "foo", "cow", "baz", TEST_ELEMENT, "box", "fan", "qux", "cat", "dog").inOrder();
    ListIterator<String> iterator = constrained.listIterator();
    iterator.next();
    iterator.set("sun");
    constrained.listIterator(2).add("sky");
    ASSERT.that(list).has().exactly(
        "sun", "cow", "sky", "baz", TEST_ELEMENT, "box", "fan", "qux", "cat", "dog").inOrder();
    ASSERT.that(constrained).has().exactly(
        "sun", "cow", "sky", "baz", TEST_ELEMENT, "box", "fan", "qux", "cat", "dog").inOrder();
    assertTrue(constrained instanceof RandomAccess);
  }

  public void testConstrainedListRandomAccessFalse() {
    List<String> list = Lists.newLinkedList(asList("foo", "bar"));
    List<String> constrained = Constraints.constrainedList(
        list, TEST_CONSTRAINT);
    list.add(TEST_ELEMENT);
    constrained.add("qux");
    assertFalse(constrained instanceof RandomAccess);
  }

  public void testConstrainedListIllegal() {
    List<String> list = Lists.newArrayList("foo", "bar");
    List<String> constrained = Constraints.constrainedList(
        list, TEST_CONSTRAINT);
    try {
      constrained.add(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.listIterator().add(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.listIterator(1).add(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.listIterator().set(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.listIterator(1).set(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.subList(0, 1).add(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.add(1, TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.set(1, TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.addAll(asList("baz", TEST_ELEMENT));
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.addAll(1, asList("baz", TEST_ELEMENT));
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    ASSERT.that(constrained).has().exactly("foo", "bar").inOrder();
    ASSERT.that(list).has().exactly("foo", "bar").inOrder();
  }

  public void testConstrainedMultisetLegal() {
    Multiset<String> multiset = HashMultiset.create(asList("foo", "bar"));
    Multiset<String> constrained = Constraints.constrainedMultiset(
        multiset, TEST_CONSTRAINT);
    multiset.add(TEST_ELEMENT);
    constrained.add("qux");
    constrained.addAll(asList("cat", "dog"));
    constrained.add("cow", 2);
    assertTrue(multiset.equals(constrained));
    assertTrue(constrained.equals(multiset));
    assertEquals(multiset.toString(), constrained.toString());
    assertEquals(multiset.hashCode(), constrained.hashCode());
    ASSERT.that(multiset).has().exactly(
        "foo", "bar", TEST_ELEMENT, "qux", "cat", "dog", "cow", "cow");
    ASSERT.that(constrained).has().exactly(
        "foo", "bar", TEST_ELEMENT, "qux", "cat", "dog", "cow", "cow");
    assertEquals(1, constrained.count("foo"));
    assertEquals(1, constrained.remove("foo", 3));
    assertEquals(2, constrained.setCount("cow", 0));
    ASSERT.that(multiset).has().exactly("bar", TEST_ELEMENT, "qux", "cat", "dog");
    ASSERT.that(constrained).has().exactly("bar", TEST_ELEMENT, "qux", "cat", "dog");
  }

  public void testConstrainedMultisetIllegal() {
    Multiset<String> multiset = HashMultiset.create(asList("foo", "bar"));
    Multiset<String> constrained = Constraints.constrainedMultiset(
        multiset, TEST_CONSTRAINT);
    try {
      constrained.add(TEST_ELEMENT);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.add(TEST_ELEMENT, 2);
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    try {
      constrained.addAll(asList("baz", TEST_ELEMENT));
      fail("TestElementException expected");
    } catch (TestElementException expected) {}
    ASSERT.that(constrained).has().exactly("foo", "bar");
    ASSERT.that(multiset).has().exactly("foo", "bar");
  }

  public void testNefariousAddAll() {
    List<String> list = Lists.newArrayList("foo", "bar");
    List<String> constrained = Constraints.constrainedList(
        list, TEST_CONSTRAINT);
    Collection<String> onceIterable = onceIterableCollection("baz");
    constrained.addAll(onceIterable);
    ASSERT.that(constrained).has().exactly("foo", "bar", "baz").inOrder();
    ASSERT.that(list).has().exactly("foo", "bar", "baz").inOrder();
  }

  /**
   * Returns a "nefarious" collection, which permits only one call to
   * iterator(). This verifies that the constrained collection uses a defensive
   * copy instead of potentially checking the elements in one snapshot and
   * adding the elements from another.
   *
   * @param element the element to be contained in the collection
   */
  static <E> Collection<E> onceIterableCollection(final E element) {
    return new AbstractCollection<E>() {
      boolean iteratorCalled;
      @Override public int size() {
        /*
         * We could make the collection empty, but that seems more likely to
         * trigger special cases (so maybe we should test both empty and
         * nonempty...).
         */
        return 1;
      }
      @Override public Iterator<E> iterator() {
        assertFalse("Expected only one call to iterator()", iteratorCalled);
        iteratorCalled = true;
        return Collections.singleton(element).iterator();
      }
    };
  }

  @GwtIncompatible("SerializableTester")
  public void testSerialization() {
    // TODO: Test serialization of constrained collections.
    assertSame(Constraints.notNull(),
        SerializableTester.reserialize(Constraints.notNull()));
  }
}
