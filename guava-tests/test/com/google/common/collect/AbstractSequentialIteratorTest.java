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

import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.TestExceptions.SomeUncheckedException;
import com.google.common.collect.testing.IteratorTester;
import java.util.Iterator;
import java.util.NoSuchElementException;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Tests for {@link AbstractSequentialIterator}. */
@GwtCompatible(emulated = true)
@NullMarked
public class AbstractSequentialIteratorTest extends TestCase {
  @GwtIncompatible // Too slow
  public void testDoublerExhaustive() {
    new IteratorTester<Integer>(
        3, UNMODIFIABLE, ImmutableList.of(1, 2), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        return newDoubler(1, 2);
      }
    }.test();
  }

  public void testDoubler() {
    Iterable<Integer> doubled =
        new Iterable<Integer>() {
          @Override
          public Iterator<Integer> iterator() {
            return newDoubler(2, 32);
          }
        };
    assertThat(doubled).containsExactly(2, 4, 8, 16, 32).inOrder();
  }

  public void testSampleCode() {
    Iterable<Integer> actual =
        new Iterable<Integer>() {
          @Override
          public Iterator<Integer> iterator() {
            Iterator<Integer> powersOfTwo =
                new AbstractSequentialIterator<Integer>(1) {
                  @Override
                  protected @Nullable Integer computeNext(Integer previous) {
                    return (previous == 1 << 30) ? null : previous * 2;
                  }
                };
            return powersOfTwo;
          }
        };
    assertThat(actual)
        .containsExactly(
            1,
            2,
            4,
            8,
            16,
            32,
            64,
            128,
            256,
            512,
            1024,
            2048,
            4096,
            8192,
            16384,
            32768,
            65536,
            131072,
            262144,
            524288,
            1048576,
            2097152,
            4194304,
            8388608,
            16777216,
            33554432,
            67108864,
            134217728,
            268435456,
            536870912,
            1073741824)
        .inOrder();
  }

  @SuppressWarnings("DoNotCall")
  public void testEmpty() {
    Iterator<Object> empty = new EmptyAbstractSequentialIterator<>();
    assertFalse(empty.hasNext());
    assertThrows(NoSuchElementException.class, empty::next);
    assertThrows(UnsupportedOperationException.class, empty::remove);
  }

  public void testBroken() {
    Iterator<Object> broken = new BrokenAbstractSequentialIterator();
    assertTrue(broken.hasNext());
    // We can't retrieve even the known first element:
    assertThrows(SomeUncheckedException.class, broken::next);
    assertThrows(SomeUncheckedException.class, broken::next);
  }

  private static Iterator<Integer> newDoubler(int first, int last) {
    return new AbstractSequentialIterator<Integer>(first) {
      @Override
      protected @Nullable Integer computeNext(Integer previous) {
        return (previous == last) ? null : previous * 2;
      }
    };
  }

  private static class EmptyAbstractSequentialIterator<T> extends AbstractSequentialIterator<T> {

    EmptyAbstractSequentialIterator() {
      super(null);
    }

    @Override
    protected T computeNext(T previous) {
      throw new AssertionFailedError();
    }
  }

  private static class BrokenAbstractSequentialIterator extends AbstractSequentialIterator<Object> {

    BrokenAbstractSequentialIterator() {
      super("UNUSED");
    }

    @Override
    protected Object computeNext(Object previous) {
      throw new SomeUncheckedException();
    }
  }
}
