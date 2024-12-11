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

import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.TestExceptions.SomeCheckedException;
import com.google.common.collect.TestExceptions.SomeUncheckedException;
import com.google.common.testing.GcFinalization;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.NoSuchElementException;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@code AbstractIterator}.
 *
 * @author Kevin Bourrillion
 */
@SuppressWarnings("serial") // No serialization is used in this test
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public class AbstractIteratorTest extends TestCase {

  public void testDefaultBehaviorOfNextAndHasNext() {

    // This sample AbstractIterator returns 0 on the first call, 1 on the
    // second, then signals that it's reached the end of the data
    Iterator<Integer> iter =
        new AbstractIterator<Integer>() {
          private int rep;

          @Override
          public @Nullable Integer computeNext() {
            switch (rep++) {
              case 0:
                return 0;
              case 1:
                return 1;
              case 2:
                return endOfData();
              default:
                throw new AssertionError("Should not have been invoked again");
            }
          }
        };

    assertTrue(iter.hasNext());
    assertEquals(0, (int) iter.next());

    // verify idempotence of hasNext()
    assertTrue(iter.hasNext());
    assertTrue(iter.hasNext());
    assertTrue(iter.hasNext());
    assertEquals(1, (int) iter.next());

    assertFalse(iter.hasNext());

    // Make sure computeNext() doesn't get invoked again
    assertFalse(iter.hasNext());

    assertThrows(NoSuchElementException.class, iter::next);
  }

  public void testDefaultBehaviorOfPeek() {
    /*
     * This sample AbstractIterator returns 0 on the first call, 1 on the
     * second, then signals that it's reached the end of the data
     */
    AbstractIterator<Integer> iter =
        new AbstractIterator<Integer>() {
          private int rep;

          @Override
          public @Nullable Integer computeNext() {
            switch (rep++) {
              case 0:
                return 0;
              case 1:
                return 1;
              case 2:
                return endOfData();
              default:
                throw new AssertionError("Should not have been invoked again");
            }
          }
        };

    assertEquals(0, (int) iter.peek());
    assertEquals(0, (int) iter.peek());
    assertTrue(iter.hasNext());
    assertEquals(0, (int) iter.peek());
    assertEquals(0, (int) iter.next());

    assertEquals(1, (int) iter.peek());
    assertEquals(1, (int) iter.next());

    /*
     * We test peek() after various calls to make sure that one bad call doesn't interfere with its
     * ability to throw the correct exception in the future.
     */
    assertThrows(NoSuchElementException.class, iter::peek);
    assertThrows(NoSuchElementException.class, iter::peek);
    assertThrows(NoSuchElementException.class, iter::next);
    assertThrows(NoSuchElementException.class, iter::peek);
  }


  @J2ktIncompatible // weak references, details of GC
  @GwtIncompatible // weak references
  @AndroidIncompatible // depends on details of GC
  public void testFreesNextReference() {
    Iterator<Object> itr =
        new AbstractIterator<Object>() {
          @Override
          public Object computeNext() {
            return new Object();
          }
        };
    WeakReference<Object> ref = new WeakReference<>(itr.next());
    GcFinalization.awaitClear(ref);
  }

  public void testDefaultBehaviorOfPeekForEmptyIteration() {

    AbstractIterator<Integer> empty =
        new AbstractIterator<Integer>() {
          private boolean alreadyCalledEndOfData;

          @Override
          public @Nullable Integer computeNext() {
            if (alreadyCalledEndOfData) {
              fail("Should not have been invoked again");
            }
            alreadyCalledEndOfData = true;
            return endOfData();
          }
        };

    /*
     * We test multiple calls to peek() to make sure that one bad call doesn't interfere with its
     * ability to throw the correct exception in the future.
     */
    assertThrows(NoSuchElementException.class, empty::peek);
    assertThrows(NoSuchElementException.class, empty::peek);
  }

  public void testSneakyThrow() throws Exception {
    Iterator<Integer> iter =
        new AbstractIterator<Integer>() {
          boolean haveBeenCalled;

          @Override
          public Integer computeNext() {
            if (haveBeenCalled) {
              throw new AssertionError("Should not have been called again");
            } else {
              haveBeenCalled = true;
              sneakyThrow(new SomeCheckedException());
              throw new AssertionError(); // unreachable
            }
          }
        };

    // The first time, the sneakily-thrown exception comes out
    assertThrows(SomeCheckedException.class, iter::hasNext);
    // But the second time, AbstractIterator itself throws an ISE
    assertThrows(IllegalStateException.class, iter::hasNext);
  }

  public void testException() {
    final SomeUncheckedException exception = new SomeUncheckedException();
    Iterator<Integer> iter =
        new AbstractIterator<Integer>() {
          @Override
          public Integer computeNext() {
            throw exception;
          }
        };

    // It should pass through untouched
    SomeUncheckedException e = assertThrows(SomeUncheckedException.class, iter::hasNext);
    assertSame(exception, e);
  }

  public void testExceptionAfterEndOfData() {
    Iterator<Integer> iter =
        new AbstractIterator<Integer>() {
          @Override
          public Integer computeNext() {
            endOfData();
            throw new SomeUncheckedException();
          }
        };
    assertThrows(SomeUncheckedException.class, iter::hasNext);
  }

  @SuppressWarnings("DoNotCall")
  public void testCantRemove() {
    Iterator<Integer> iter =
        new AbstractIterator<Integer>() {
          boolean haveBeenCalled;

          @Override
          public Integer computeNext() {
            if (haveBeenCalled) {
              endOfData();
            }
            haveBeenCalled = true;
            return 0;
          }
        };

    assertEquals(0, (int) iter.next());

    assertThrows(UnsupportedOperationException.class, iter::remove);
  }

  public void testReentrantHasNext() {
    Iterator<Integer> iter =
        new AbstractIterator<Integer>() {
          @Override
          protected Integer computeNext() {
            boolean unused = hasNext();
            throw new AssertionError();
          }
        };
    assertThrows(IllegalStateException.class, iter::hasNext);
  }

  // Technically we should test other reentrant scenarios (9 combinations of
  // hasNext/next/peek), but we'll cop out for now, knowing that peek() and
  // next() both start by invoking hasNext() anyway.

  /** Throws an undeclared checked exception. */
  private static void sneakyThrow(Throwable t) {
    class SneakyThrower<T extends Throwable> {
      @SuppressWarnings("unchecked") // not really safe, but that's the point
      void throwIt(Throwable t) throws T {
        throw (T) t;
      }
    }
    new SneakyThrower<Error>().throwIt(t);
  }
}
