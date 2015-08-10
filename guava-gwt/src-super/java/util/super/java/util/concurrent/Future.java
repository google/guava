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
 * by calling {@link #get()} or {@link #get(long, TimeUnit)} when the it is not yet done is
 * considered illegal because it would lead to a deadlock. Future implementations must throw
 * {@link IllegalStateException} to avoid a deadlock.
 */
public interface Future<V> {
   boolean cancel(boolean mayInterruptIfRunning);

   boolean isCancelled();

   boolean isDone();
    
   V get() throws InterruptedException, ExecutionException;

   V get(long timeout, TimeUnit unit)
       throws InterruptedException, ExecutionException, TimeoutException;
}
