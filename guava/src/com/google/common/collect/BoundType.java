/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

/**
 * Indicates whether an endpoint of some range is contained in the range itself ("closed") or not
 * ("open"). If a range is unbounded on a side, it is neither open nor closed on that side; the
 * bound simply does not exist.
 *
 * @since 10.0
 */
@GwtCompatible
public enum BoundType {
  /**
   * The endpoint value <i>is not</i> considered part of the set ("exclusive").
   */
  OPEN {
    @Override
    BoundType flip() {
      return CLOSED;
    }
  },
  /**
   * The endpoint value <i>is</i> considered part of the set ("inclusive").
   */
  CLOSED {
    @Override
    BoundType flip() {
      return OPEN;
    }
  };

  /**
   * Returns the bound type corresponding to a boolean value for inclusivity.
   */
  static BoundType forBoolean(boolean inclusive) {
    return inclusive ? CLOSED : OPEN;
  }

  abstract BoundType flip();
}
