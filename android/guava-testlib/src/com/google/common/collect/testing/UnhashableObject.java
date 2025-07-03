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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtCompatible;
import org.jspecify.annotations.Nullable;

/**
 * An unhashable object to be used in testing as values in our collections.
 *
 * @author Regina O'Dell
 */
@GwtCompatible
public class UnhashableObject implements Comparable<UnhashableObject> {
  private final int value;

  public UnhashableObject(int value) {
    this.value = value;
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof UnhashableObject) {
      UnhashableObject that = (UnhashableObject) object;
      return this.value == that.value;
    }
    return false;
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  // needed because otherwise Object.toString() calls hashCode()
  @Override
  public String toString() {
    return "DontHashMe" + value;
  }

  @Override
  public int compareTo(UnhashableObject o) {
    return Integer.compare(this.value, o.value);
  }
}
