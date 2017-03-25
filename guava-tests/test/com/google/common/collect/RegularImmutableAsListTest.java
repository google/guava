/*
 * Copyright (C) 2013 The Guava Authors
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
import junit.framework.TestCase;

/**
 * Tests for {@link RegularImmutableAsList}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class RegularImmutableAsListTest extends TestCase {
  /**
   * RegularImmutableAsList should assume its input is null-free without checking, because it only
   * gets invoked from other immutable collections.
   */
  public void testDoesntCheckForNull() {
    ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3);
    new RegularImmutableAsList<Integer>(set, new Object[] {null, null, null});
    // shouldn't throw!
  }
}
