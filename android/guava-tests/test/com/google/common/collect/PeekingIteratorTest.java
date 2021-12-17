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

import static com.google.common.collect.Iterators.peekingIterator;
import static com.google.common.collect.testing.IteratorFeature.MODIFIABLE;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static java.util.Collections.emptyList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.IteratorTester;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import junit.framework.TestCase;

/**
 * Unit test for {@link PeekingIterator}.
 *
 * @author Mick Killianey
 */
@SuppressWarnings("serial") // No serialization is used in this test
@GwtCompatible(emulated = true)
public class PeekingIteratorTest extends TestCase {

  /**
   * Version of {@link IteratorTester} that compares an iterator over a given collection of elements
   * (used as the reference iterator) against a {@code PeekingIterator} that *wraps* such an
   * iterator (used as the target iterator).
   *
   * <p>This IteratorTester makes copies of the master so that it can later verify that {@link
   * PeekingIterator#remove()} removes the same elements as the reference's iterator {@code
   * #remove()}.
   */
  private static class PeekingIteratorTester<T> extends IteratorTester<T> {
    private Iterable<T> master;
    private List<T> targetList;

    public PeekingIteratorTester(Collection<T> master) {
      super(master.size() + 3, MODIFIABLE, master, IteratorTester.KnownOrder.KNOWN_ORDER);
      this.master = master;
    }

    @Override
    protected Iterator<T> newTargetIterator() {
      // make copy from master to verify later
      targetList = Lists.newArrayList(master);
      Iterator<T> iterator = targetList.iterator();
      return Iterators.peekingIterator(iterator);
    }

    @Override
    protected void verify(List<T> elements) {
      // verify same objects were removed from reference and target
      assertEquals(elements, targetList);
    }
  }

  private <T> void actsLikeIteratorHelper(final List<T> list) {
    // Check with modifiable copies of the list
    new PeekingIteratorTester<T>(list).test();

    // Check with unmodifiable lists
    new IteratorTester<T>(
        list.size() * 2 + 2, UNMODIFIABLE, list, IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<T> newTargetIterator() {
        Iterator<T> iterator = Collections.unmodifiableList(list).iterator();
        return Iterators.peekingIterator(iterator);
      }
    }.test();
  }

  public void testPeekingIteratorBehavesLikeIteratorOnEmptyIterable() {
    actsLikeIteratorHelper(Collections.emptyList());
  }

  public void testPeekingIteratorBehavesLikeIteratorOnSingletonIterable() {
    actsLikeIteratorHelper(Collections.singletonList(new Object()));
  }

  // TODO(cpovirk): instead of skipping, use a smaller number of steps
  @GwtIncompatible // works but takes 5 minutes to run
  public void testPeekingIteratorBehavesLikeIteratorOnThreeElementIterable() {
    actsLikeIteratorHelper(Lists.newArrayList("A", "B", "C"));
  }

  @GwtIncompatible // works but takes 5 minutes to run
  public void testPeekingIteratorAcceptsNullElements() {
    actsLikeIteratorHelper(Lists.newArrayList(null, "A", null));
  }

  public void testPeekOnEmptyList() {
    List<?> list = Collections.emptyList();
    Iterator<?> iterator = list.iterator();
    PeekingIterator<?> peekingIterator = Iterators.peekingIterator(iterator);

    try {
      peekingIterator.peek();
      fail("Should throw NoSuchElementException if nothing to peek()");
    } catch (NoSuchElementException e) {
      /* expected */
    }
  }

  public void testPeekDoesntChangeIteration() {
    List<?> list = Lists.newArrayList("A", "B", "C");
    Iterator<?> iterator = list.iterator();
    PeekingIterator<?> peekingIterator = Iterators.peekingIterator(iterator);

    assertEquals("Should be able to peek() at first element", "A", peekingIterator.peek());
    assertEquals(
        "Should be able to peek() first element multiple times", "A", peekingIterator.peek());
    assertEquals(
        "next() should still return first element after peeking", "A", peekingIterator.next());

    assertEquals("Should be able to peek() at middle element", "B", peekingIterator.peek());
    assertEquals(
        "Should be able to peek() middle element multiple times", "B", peekingIterator.peek());
    assertEquals(
        "next() should still return middle element after peeking", "B", peekingIterator.next());

    assertEquals("Should be able to peek() at last element", "C", peekingIterator.peek());
    assertEquals(
        "Should be able to peek() last element multiple times", "C", peekingIterator.peek());
    assertEquals(
        "next() should still return last element after peeking", "C", peekingIterator.next());

    try {
      peekingIterator.peek();
      fail("Should throw exception if no next to peek()");
    } catch (NoSuchElementException e) {
      /* expected */
    }
    try {
      peekingIterator.peek();
      fail("Should continue to throw exception if no next to peek()");
    } catch (NoSuchElementException e) {
      /* expected */
    }
    try {
      peekingIterator.next();
      fail("next() should still throw exception after the end of iteration");
    } catch (NoSuchElementException e) {
      /* expected */
    }
  }

  public void testCantRemoveAfterPeek() {
    List<String> list = Lists.newArrayList("A", "B", "C");
    Iterator<String> iterator = list.iterator();
    PeekingIterator<?> peekingIterator = Iterators.peekingIterator(iterator);

    assertEquals("A", peekingIterator.next());
    assertEquals("B", peekingIterator.peek());

    /* Should complain on attempt to remove() after peek(). */
    try {
      peekingIterator.remove();
      fail("remove() should throw IllegalStateException after a peek()");
    } catch (IllegalStateException e) {
      /* expected */
    }

    assertEquals(
        "After remove() throws exception, peek should still be ok", "B", peekingIterator.peek());

    /* Should recover to be able to remove() after next(). */
    assertEquals("B", peekingIterator.next());
    peekingIterator.remove();
    assertEquals("Should have removed an element", 2, list.size());
    assertFalse("Second element should be gone", list.contains("B"));
  }

  static class ThrowsAtEndException extends RuntimeException {
    /* nothing */
  }

  /**
   * This Iterator claims to have more elements than the underlying iterable, but when you try to
   * fetch the extra elements, it throws an unchecked exception.
   */
  static class ThrowsAtEndIterator<E> implements Iterator<E> {
    Iterator<E> iterator;

    public ThrowsAtEndIterator(Iterable<E> iterable) {
      this.iterator = iterable.iterator();
    }

    @Override
    public boolean hasNext() {
      return true; // pretend that you have more...
    }

    @Override
    public E next() {
      // ...but throw an unchecked exception when you ask for it.
      if (!iterator.hasNext()) {
        throw new ThrowsAtEndException();
      }
      return iterator.next();
    }

    @Override
    public void remove() {
      iterator.remove();
    }
  }

  public void testPeekingIteratorDoesntAdvancePrematurely() throws Exception {
    /*
     * This test will catch problems where the underlying iterator
     * throws a RuntimeException when retrieving the nth element.
     *
     * If the PeekingIterator is caching elements too aggressively,
     * it may throw the exception on the (n-1)th element (oops!).
     */

    /* Checks the case where the first element throws an exception. */

    List<Integer> list = emptyList();
    Iterator<Integer> iterator = peekingIterator(new ThrowsAtEndIterator<Integer>(list));
    assertNextThrows(iterator);

    /* Checks the case where a later element throws an exception. */

    list = Lists.newArrayList(1, 2);
    iterator = peekingIterator(new ThrowsAtEndIterator<Integer>(list));
    assertTrue(iterator.hasNext());
    iterator.next();
    assertTrue(iterator.hasNext());
    iterator.next();
    assertNextThrows(iterator);
  }

  private void assertNextThrows(Iterator<?> iterator) {
    try {
      iterator.next();
      fail();
    } catch (ThrowsAtEndException expected) {
    }
  }
}
