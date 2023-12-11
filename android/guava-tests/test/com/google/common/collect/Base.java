/*
 * Copyright (C) 2007 The Guava Authors
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
import org.checkerframework.checker.nullness.qual.Nullable;

/** Simple base class to verify that we handle generics correctly. */
@GwtCompatible
class Base implements Comparable<Base>, Serializable {
  private final String s;

  public Base(String s) {
    this.s = s;
  }

  @Override
  public int hashCode() { // delegate to 's'
    return s.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == null) {
      return false;
    } else if (other instanceof Base) {
      return s.equals(((Base) other).s);
    } else {
      return false;
    }
  }

  @Override
  public int compareTo(Base o) {
    return s.compareTo(o.s);
  }

  private static final long serialVersionUID = 0;
}
