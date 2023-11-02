/*
 * Copyright (C) 2016 The Guava Authors
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

import static com.google.common.base.Verify.verify;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase;

/**
 * Tests for {@link FluentFuture}. The tests cover only the basics for the API. The actual logic is
 * tested in {@link FuturesTest}.
 */
@GwtCompatible(emulated = true)
public class FluentFutureTest extends TestCase {
  public void testFromFluentFuture() {
    FluentFuture<String> f = FluentFuture.from(SettableFuture.<String>create());
    assertThat(FluentFuture.from(f)).isSameInstanceAs(f);
  }

  public void testFromFluentFuturePassingAsNonFluent() {
    ListenableFuture<String> f = FluentFuture.from(SettableFuture.<String>create());
    assertThat(FluentFuture.from(f)).isSameInstanceAs(f);
  }

  public void testFromNonFluentFuture() throws Exception {
    ListenableFuture<String> f =
        new SimpleForwardingListenableFuture<String>(immediateFuture("a")) {};
    verify(!(f instanceof FluentFuture));
    assertThat(FluentFuture.from(f).get()).isEqualTo("a");
    // TODO(cpovirk): Test forwarding more extensively.
  }

  public void testAddCallback() {
    FluentFuture<String> f = FluentFuture.from(immediateFuture("a"));
    final boolean[] called = new boolean[1];
    f.addCallback(
        new FutureCallback<String>() {
          @Override
          public void onSuccess(String result) {
            called[0] = true;
          }

          @Override
          public void onFailure(Throwable t) {}
        },
        directExecutor());
    assertThat(called[0]).isTrue();
  }

  public void testCatching() throws Exception {
    FluentFuture<?> f =
        FluentFuture.from(immediateFailedFuture(new RuntimeException()))
            .catching(
                Throwable.class,
                new Function<Throwable, Class<?>>() {
                  @Override
                  public Class<?> apply(Throwable input) {
                    return input.getClass();
                  }
                },
                directExecutor());
    assertThat(f.get()).isEqualTo(RuntimeException.class);
  }

  public void testCatchingAsync() throws Exception {
    FluentFuture<?> f =
        FluentFuture.from(immediateFailedFuture(new RuntimeException()))
            .catchingAsync(
                Throwable.class,
                new AsyncFunction<Throwable, Class<?>>() {
                  @Override
                  public ListenableFuture<Class<?>> apply(Throwable input) {
                    return Futures.<Class<?>>immediateFuture(input.getClass());
                  }
                },
                directExecutor());
    assertThat(f.get()).isEqualTo(RuntimeException.class);
  }

  public void testTransform() throws Exception {
    FluentFuture<Integer> f =
        FluentFuture.from(immediateFuture(1))
            .transform(
                new Function<Integer, Integer>() {
                  @Override
                  public Integer apply(Integer input) {
                    return input + 1;
                  }
                },
                directExecutor());
    assertThat(f.get()).isEqualTo(2);
  }

  public void testTransformAsync() throws Exception {
    FluentFuture<Integer> f =
        FluentFuture.from(immediateFuture(1))
            .transformAsync(
                new AsyncFunction<Integer, Integer>() {
                  @Override
                  public ListenableFuture<Integer> apply(Integer input) {
                    return immediateFuture(input + 1);
                  }
                },
                directExecutor());
    assertThat(f.get()).isEqualTo(2);
  }

  @GwtIncompatible // withTimeout
  public void testWithTimeout() throws Exception {
    ScheduledExecutorService executor = newScheduledThreadPool(1);
    try {
      FluentFuture<?> f =
          FluentFuture.from(SettableFuture.create()).withTimeout(0, SECONDS, executor);
      ExecutionException e = assertThrows(ExecutionException.class, () -> f.get());
      assertThat(e).hasCauseThat().isInstanceOf(TimeoutException.class);
    } finally {
      executor.shutdown();
    }
  }
}
