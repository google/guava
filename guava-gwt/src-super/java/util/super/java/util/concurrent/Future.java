/*
 * Copyright (C) 2015 The Guava Authors
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

package java.util.concurrent;

/**
 * Emulation of Future. Since GWT environment is single threaded, attempting to block on the future
 * by calling {@link #get()} or {@link #get(long, TimeUnit)} when the future is not yet done is
 * considered illegal because it would lead to a deadlock. Future implementations must throw {@link
 * IllegalStateException} to avoid a deadlock.
 *
 * @param <V> value type returned by the future.
 */
public interface Future<V> {
   boolean cancel(boolean mayInterruptIfRunning);

   boolean isCancelled();

   boolean isDone();

  // Even though the 'get' methods below are blocking, they are the only built-in APIs to get the
  // result of the {@code Future}, hence they are not removed. The implementation must throw {@link
  // IllegalStateException} if the {@code Future} is not done yet (see the class javadoc).

   V get() throws InterruptedException, ExecutionException;

   V get(long timeout, TimeUnit unit)
       throws InterruptedException, ExecutionException, TimeoutException;
}
