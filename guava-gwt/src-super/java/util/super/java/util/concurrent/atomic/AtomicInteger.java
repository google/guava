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

package java.util.concurrent.atomic;

/**
 * GWT emulated version of {@link AtomicInteger}.  It's a thin wrapper
 * around the primitive {@code int}.
 *
 * @author Hayward Chan
 */
public class AtomicInteger extends Number implements java.io.Serializable {

  private int value;

  public AtomicInteger(int initialValue) {
    value = initialValue;
  }

  public AtomicInteger() {
  }

  public final int get() {
    return value;
  }

  public final void set(int newValue) {
    value = newValue;
  }

  public final void lazySet(int newValue) {
    set(newValue);
  }

  public final int getAndSet(int newValue) {
    int current = value;
    value = newValue;
    return current;
  }

  public final boolean compareAndSet(int expect, int update) {
    if (value == expect) {
      value = update;
      return true;
    } else {
      return false;
    }
  }

  public final int getAndIncrement() {
    return value++;
  }

  public final int getAndDecrement() {
    return value--;
  }

  public final int getAndAdd(int delta) {
    int current = value;
    value += delta;
    return current;
  }

  public final int incrementAndGet() {
    return ++value;
  }

  public final int decrementAndGet() {
    return --value;
  }

  public final int addAndGet(int delta) {
    value += delta;
    return value;
  }

  @Override public String toString() {
    return Integer.toString(value);
  }

  public int intValue() {
    return value;
  }

  public long longValue() {
    return (long) value;
  }

  public float floatValue() {
    return (float) value;
  }

  public double doubleValue() {
    return (double) value;
  }
}
