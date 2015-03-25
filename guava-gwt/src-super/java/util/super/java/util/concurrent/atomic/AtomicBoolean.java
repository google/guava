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
 */  /**
 * Atomically sets the value to the given updated value
 * if the current value {@code ==} the expected value.
 *
 * <p>May <a href="package-summary.html#Spurious">fail spuriously</a>
 * and does not provide ordering guarantees, so is only rarely an
 * appropriate alternative to {@code compareAndSet}.
 *
 * @param expect the expected value
 * @param update the new value
 * @return true if successful.
 */

package java.util.concurrent.atomic;

/**
 * GWT emulation of AtomicBoolean.
 */
public class AtomicBoolean implements java.io.Serializable {
  private boolean value;

  public AtomicBoolean(boolean initialValue) {
    value = initialValue;
  }

  public AtomicBoolean() {
  }

  public final boolean get() {
    return value;
  }

  public final boolean compareAndSet(boolean expect, boolean update) {
    if (get() == expect) {
      set(update);
      return true;
    }

    return false;
  }

  public boolean weakCompareAndSet(boolean expect, boolean update) {
    return compareAndSet(expect, update);
  }

  public final void set(boolean newValue) {
    value = newValue;
  }

  public final void lazySet(boolean newValue) {
    set(newValue);
  }

  public final boolean getAndSet(boolean newValue) {
    boolean current = get();
    set(newValue);
    return current;
  }

  public String toString() {
    return Boolean.toString(get());
  }
}
