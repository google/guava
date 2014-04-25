/*
 * Copyright (C) 2014 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.VmOptions;
import com.google.common.collect.ImmutableList;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * A benchmark for {@link Futures#combine}
 */
@VmOptions({"-Xms12g", "-Xmx12g", "-d64"})
public class FuturesCombineBenchmark {

  enum Impl {
    OLD {
      @Override <V> ListenableFuture<V> combine(final Callable<V> combiner, Executor executor,
          Iterable<? extends ListenableFuture<?>> futures) {
        ListenableFuture<?> trigger = Futures.successfulAsList(futures);
        checkNotNull(combiner);
        checkNotNull(trigger);
        return Futures.transform(trigger, new AsyncFunction<Object, V>() {
          @Override public ListenableFuture<V> apply(Object arg) throws Exception {
            try {
              return Futures.immediateFuture(combiner.call());
            } catch (CancellationException e) {
              return Futures.immediateCancelledFuture();
            } catch (ExecutionException e) {
              return Futures.immediateFailedFuture(e.getCause()); // OK to rethrow on Error
            }
          }
        }, executor);
      }
    },
    NEW {
      @Override
      <V> ListenableFuture<V> combine(Callable<V> combiner, final Executor executor,
          Iterable<? extends ListenableFuture<?>> futures) {
        return Futures.combine(combiner, executor, futures);
      }
    };

    abstract <V> ListenableFuture<V> combine(
        Callable<V> combiner, Executor executor,
        Iterable<? extends ListenableFuture<?>> futures);
  }

  private static final Executor INLINE_EXECUTOR = new Executor() {
    @Override public void execute(Runnable command) {
      command.run();
    }
  };

  @Param Impl impl;
  @Param({"1", "5", "10"}) int numInputs;

  @Benchmark int timeDoneSuccesfulFutures(int reps) throws Exception {
    ImmutableList.Builder<ListenableFuture<?>> futuresBuilder = ImmutableList.builder();
    for (int i = 0; i < numInputs; i++) {
      futuresBuilder.add(Futures.immediateFuture(i));
    }
    ImmutableList<ListenableFuture<?>> futures = futuresBuilder.build();
    Impl impl = this.impl;
    Callable<Integer> callable = Callables.returning(12);
    int sum = 0;
    for (int i = 0; i < reps; i++) {
      sum += impl.combine(callable, INLINE_EXECUTOR, futures).get();
    }
    return sum;
  }
  
  @Benchmark int timeDoneFailedFutures(int reps) throws Exception {
    ImmutableList.Builder<ListenableFuture<?>> futuresBuilder = ImmutableList.builder();
    for (int i = 0; i < numInputs; i++) {
      futuresBuilder.add(Futures.immediateFailedFuture(new Exception("boom")));
    }
    ImmutableList<ListenableFuture<?>> futures = futuresBuilder.build();
    Impl impl = this.impl;
    Callable<Integer> callable = Callables.returning(12);
    int sum = 0;
    for (int i = 0; i < reps; i++) {
      sum += impl.combine(callable, INLINE_EXECUTOR, futures).get();
    }
    return sum;
  }
  
  @Benchmark int timeSuccesfulFutures(int reps) throws Exception {
    Impl impl = this.impl;
    Callable<Integer> callable = Callables.returning(12);
    int sum = 0;
    for (int i = 0; i < reps; i++) {
      ImmutableList<SettableFuture<Integer>> futures = getSettableFutureList();
      ListenableFuture<Integer> combined = impl.combine(callable, INLINE_EXECUTOR, futures);
      for (SettableFuture<Integer> future : futures) {
        future.set(i);
      }
      sum += combined.get();
    }
    return sum;
  }

  @Benchmark int timeFailedFutures(int reps) throws Exception {
    Impl impl = this.impl;
    Callable<Integer> callable = Callables.returning(12);
    int sum = 0;
    Exception throwable = new Exception("boom");
    for (int i = 0; i < reps; i++) {
      ImmutableList<SettableFuture<Integer>> futures = getSettableFutureList();
      ListenableFuture<Integer> combined = impl.combine(callable, INLINE_EXECUTOR, futures);
      for (SettableFuture<Integer> future : futures) {
        future.setException(throwable);
      }
      sum += combined.get();
    }
    return sum;
  }

  private ImmutableList<SettableFuture<Integer>> getSettableFutureList() {
    ImmutableList.Builder<SettableFuture<Integer>> futuresBuilder = ImmutableList.builder();
    for (int i = 0; i < numInputs; i++) {
      futuresBuilder.add(SettableFuture.<Integer>create());
    }
    return futuresBuilder.build();
  }
}
