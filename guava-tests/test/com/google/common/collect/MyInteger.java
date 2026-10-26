/*
 * Copyright (C) 2026 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.io.Serializable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A trivial class {@code int} wrapper that is not a <a
 * href="https://docs.oracle.com/en/java/javase/27/docs/api/java.base/java/lang/doc-files/ValueBased.html">value-based
 * class</a> like {@link Integer}. That allows us to have different equivalent instances.
 */
@NullMarked
@GwtCompatible
final class MyInteger extends Number implements Comparable<MyInteger>, Serializable {
  private final int value;

  MyInteger(int value) {
    this.value = value;
  }

  @Override
  public int intValue() {
    return value;
  }

  @Override
  public long longValue() {
    return value;
  }

  @Override
  public float floatValue() {
    return value;
  }

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public int compareTo(MyInteger other) {
    return Integer.compare(value, other.value);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof MyInteger) {
      return value == ((MyInteger) obj).value;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
}
