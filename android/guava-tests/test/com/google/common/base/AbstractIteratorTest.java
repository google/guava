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

package com.google.common.base;

import static com.google.common.base.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.base.SneakyThrows.sneakyThrow;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.TestExceptions.SomeCheckedException;
import com.google.common.base.TestExceptions.SomeUncheckedException;
import com.google.common.testing.GcFinalization;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.NoSuchElementException;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit test for {@code AbstractIterator}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@NullUnmarked
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
              throw sneakyThrow(new SomeCheckedException());
            }
          }
        };

    // The first time, the sneakily-thrown exception comes out
    assertThrows(SomeCheckedException.class, iter::hasNext);
    // But the second time, AbstractIterator itself throws an ISE
    assertThrows(IllegalStateException.class, iter::hasNext);
  }

  public void testException() {
    SomeUncheckedException exception = new SomeUncheckedException();
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


  @GwtIncompatible // weak references
  @J2ktIncompatible
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

  // Technically we should test other reentrant scenarios (4 combinations of
  // hasNext/next), but we'll cop out for now, knowing that
  // next() both start by invoking hasNext() anyway.
}
