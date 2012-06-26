/*
 * Copyright (C) 2011 The Guava Authors
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

package java.util.concurrent.atomic;

/**
 * GWT emulated version of {@link AtomicLong}.  It's a thin wrapper
 * around the primitive {@code long}.
 *
 * @author Jige Yu
 */
public class AtomicLong extends Number implements java.io.Serializable {

  private long value;

  public AtomicLong(long initialValue) {
    this.value = initialValue;
  }

  public AtomicLong() {
  }

  public final long get() {
    return value;
  }

  public final void set(long newValue) {
    value = newValue;
  }

  public final void lazySet(long newValue) {
    set(newValue);
  }

  public final long getAndSet(long newValue) {
    long current = value;
    value = newValue;
    return current;
  }

  public final boolean compareAndSet(long expect, long update) {
    if (value == expect) {
      value = update;
      return true;
    } else {
      return false;
    }
  }

  public final long getAndIncrement() {
    return value++;
  }

  public final long getAndDecrement() {
    return value--;
  }

  public final long getAndAdd(long delta) {
    long current = value;
    value += delta;
    return current;
  }

  public final long incrementAndGet() {
    return ++value;
  }

  public final long decrementAndGet() {
    return --value;
  }

  public final long addAndGet(long delta) {
    value += delta;
    return value;
  }

  @Override public String toString() {
    return Long.toString(value);
  }

  public int intValue() {
    return (int) value;
  }

  public long longValue() {
    return value;
  }

  public float floatValue() {
    return (float) value;
  }

  public double doubleValue() {
    return (double) value;
  }
}
