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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Arrays;

/**
 * A class that implements {@code Comparable} without generics, such as those found in libraries
 * that support Java 1.4 and before. Our library needs to do the bare minimum to accommodate such
 * types, though their use may still require an explicit type parameter and/or warning suppression.
 *
 * @author Kevin Bourrillion
 */
@SuppressWarnings("ComparableType")
@GwtCompatible
class LegacyComparable implements Comparable, Serializable {
  static final LegacyComparable X = new LegacyComparable("x");
  static final LegacyComparable Y = new LegacyComparable("y");
  static final LegacyComparable Z = new LegacyComparable("z");

  static final Iterable<LegacyComparable> VALUES_FORWARD = Arrays.asList(X, Y, Z);
  static final Iterable<LegacyComparable> VALUES_BACKWARD = Arrays.asList(Z, Y, X);

  private final String value;

  LegacyComparable(String value) {
    this.value = value;
  }

  @Override
  public int compareTo(Object object) {
    // This method is spec'd to throw CCE if object is of the wrong type
    LegacyComparable that = (LegacyComparable) object;
    return this.value.compareTo(that.value);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof LegacyComparable) {
      LegacyComparable that = (LegacyComparable) object;
      return this.value.equals(that.value);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  private static final long serialVersionUID = 0;
}
