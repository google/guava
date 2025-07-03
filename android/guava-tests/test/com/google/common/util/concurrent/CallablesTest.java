/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.util.concurrent;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;
import static com.google.common.util.concurrent.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.TestExceptions.SomeCheckedException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit tests for {@link Callables}.
 *
 * @author Isaac Shum
 */
@GwtCompatible(emulated = true)
@NullUnmarked
public class CallablesTest extends TestCase {

  @J2ktIncompatible // TODO(b/324550390): Enable
  public void testReturning() throws Exception {
    assertNull(Callables.returning(null).call());

    Object value = new Object();
    Callable<Object> callable = Callables.returning(value);
    assertSame(value, callable.call());
    // Expect the same value on subsequent calls
    assertSame(value, callable.call());
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testAsAsyncCallable() throws Exception {
    String expected = "MyCallableString";
    Callable<String> callable =
        new Callable<String>() {
          @Override
          public String call() throws Exception {
            return expected;
          }
        };

    AsyncCallable<String> asyncCallable =
        Callables.asAsyncCallable(callable, newDirectExecutorService());

    ListenableFuture<String> future = asyncCallable.call();
    assertSame(expected, future.get());
  }

  @J2ktIncompatible
  @GwtIncompatible
  public void testAsAsyncCallable_exception() throws Exception {
    Exception expected = new IllegalArgumentException();
    Callable<String> callable =
        new Callable<String>() {
          @Override
          public String call() throws Exception {
            throw expected;
          }
        };

    AsyncCallable<String> asyncCallable =
        Callables.asAsyncCallable(callable, newDirectExecutorService());

    ListenableFuture<String> future = asyncCallable.call();
    ExecutionException e = assertThrows(ExecutionException.class, () -> future.get());
    assertThat(e).hasCauseThat().isSameInstanceAs(expected);
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testRenaming() throws Exception {
    String oldName = Thread.currentThread().getName();
    Supplier<String> newName = Suppliers.ofInstance("MyCrazyThreadName");
    Callable<@Nullable Void> callable =
        new Callable<@Nullable Void>() {
          @Override
          public @Nullable Void call() throws Exception {
            assertEquals(Thread.currentThread().getName(), newName.get());
            return null;
          }
        };
    Callables.threadRenaming(callable, newName).call();
    assertEquals(oldName, Thread.currentThread().getName());
  }

  @J2ktIncompatible
  @GwtIncompatible // threads
  public void testRenaming_exceptionalReturn() throws Exception {
    String oldName = Thread.currentThread().getName();
    Supplier<String> newName = Suppliers.ofInstance("MyCrazyThreadName");
    Callable<@Nullable Void> callable =
        new Callable<@Nullable Void>() {
          @Override
          public @Nullable Void call() throws Exception {
            assertEquals(Thread.currentThread().getName(), newName.get());
            throw new SomeCheckedException();
          }
        };
    assertThrows(
        SomeCheckedException.class, () -> Callables.threadRenaming(callable, newName).call());
    assertEquals(oldName, Thread.currentThread().getName());
  }
}
