/*
 * Copyright (C) 2010 The Guava Authors
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

import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.IteratorTester;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Tests for {@link AbstractLinkedIterator}. */
@GwtCompatible(emulated = true)
public class AbstractLinkedIteratorTest extends TestCase {
  @GwtIncompatible("Too slow")
  public void testDoublerExhaustive() {
    new IteratorTester<Integer>(3, UNMODIFIABLE, ImmutableList.of(1, 2),
        IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return newDoubler(1, 2);
      }
    }.test();
  }

  public void testDoubler() {
    Iterable<Integer> doubled = new Iterable<Integer>() {
      @Override
      public Iterator<Integer> iterator() {
        return newDoubler(2, 32);
      }
    };
    ASSERT.that(doubled).hasContentsInOrder(2, 4, 8, 16, 32);
  }

  public void testEmpty() {
    Iterator<Object> empty = newEmpty();
    assertFalse(empty.hasNext());
    try {
      empty.next();
      fail();
    } catch (NoSuchElementException expected) {
    }
    try {
      empty.remove();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testBroken() {
    Iterator<Object> broken = newBroken();
    assertTrue(broken.hasNext());
    // We can't retrieve even the known first element:
    try {
      broken.next();
      fail();
    } catch (MyException expected) {
    }
    try {
      broken.next();
      fail();
    } catch (MyException expected) {
    }
  }

  private static Iterator<Integer> newDoubler(int first, final int last) {
    return new AbstractLinkedIterator<Integer>(first) {
      @Override
      protected Integer computeNext(Integer previous) {
        return (previous == last) ? null : previous * 2;
      }
    };
  }

  private static <T> Iterator<T> newEmpty() {
    return new AbstractLinkedIterator<T>(null) {
      @Override
      protected T computeNext(T previous) {
        throw new AssertionFailedError();
      }
    };
  }

  private static Iterator<Object> newBroken() {
    return new AbstractLinkedIterator<Object>("UNUSED") {
      @Override
      protected Object computeNext(Object previous) {
        throw new MyException();
      }
    };
  }

  private static class MyException extends RuntimeException {}
}
