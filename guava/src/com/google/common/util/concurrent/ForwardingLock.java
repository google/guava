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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/** Forwarding wrapper around a {@code Lock}. */
@ElementTypesAreNonnullByDefault
abstract class ForwardingLock implements Lock {
  abstract Lock delegate();

  @Override
  public void lock() {
    delegate().lock();
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    delegate().lockInterruptibly();
  }

  @Override
  public boolean tryLock() {
    return delegate().tryLock();
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    return delegate().tryLock(time, unit);
  }

  @Override
  public void unlock() {
    delegate().unlock();
  }

  @Override
  public Condition newCondition() {
    return delegate().newCondition();
  }
}
