/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * A mutable value of type {@code int}, for multisets to use in tracking counts of values.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class Count implements Serializable {
  private int value;

  Count(int value) {
    this.value = value;
  }

  public int get() {
    return value;
  }

  public void add(int delta) {
    value += delta;
  }

  public int addAndGet(int delta) {
    return value += delta;
  }

  public void set(int newValue) {
    value = newValue;
  }

  public int getAndSet(int newValue) {
    int result = value;
    value = newValue;
    return result;
  }

  @Override
  public int hashCode() {
    return value;
  }

  @Override
  public boolean equals(@NullableDecl Object obj) {
    return obj instanceof Count && ((Count) obj).value == value;
  }

  @Override
  public String toString() {
    return Integer.toString(value);
  }
}
