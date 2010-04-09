/*
 * Copyright (C) 2009 Google Inc.
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@code Future} whose {@code get} calls cannot be interrupted. If a thread
 * is interrupted during such a call, the call continues to block until the
 * result is available or the timeout elapses, and only then re-interrupts the
 * thread. Obtain an instance of this type using {@link
 * Futures#makeUninterruptible(Future)}.
 *
 * @author Kevin Bourrillion
 * @since 2009.09.15 <b>tentative</b>
 */
public interface UninterruptibleFuture<V> extends Future<V> {
  /*@Override*/ V get() throws ExecutionException;

  /*@Override*/ V get(long timeout, TimeUnit unit)
      throws ExecutionException, TimeoutException;
}
