/*
 * Copyright (C) 2017 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.util.concurrent;

import com.google.common.annotations.J2ktIncompatible;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/** Forwarding wrapper around a {@code Condition}. */
@J2ktIncompatible
@ElementTypesAreNonnullByDefault
abstract class ForwardingCondition implements Condition {
  abstract Condition delegate();

  @Override
  public void await() throws InterruptedException {
    delegate().await();
  }

  @Override
  public boolean await(long time, TimeUnit unit) throws InterruptedException {
    return delegate().await(time, unit);
  }

  @Override
  public void awaitUninterruptibly() {
    delegate().awaitUninterruptibly();
  }

  @Override
  public long awaitNanos(long nanosTimeout) throws InterruptedException {
    return delegate().awaitNanos(nanosTimeout);
  }

  @Override
  public boolean awaitUntil(Date deadline) throws InterruptedException {
    return delegate().awaitUntil(deadline);
  }

  @Override
  public void signal() {
    delegate().signal();
  }

  @Override
  public void signalAll() {
    delegate().signalAll();
  }
}
