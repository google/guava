/*
 * Copyright (C) 2012 The Guava Authors
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

import junit.framework.TestCase;

/**
 * Tests for {@code ImmutableCollection}.
 *
 * @author Louis Wasserman
 */
public class ImmutableCollectionTest extends TestCase {
  public void testCapacityExpansion() {
    assertEquals(1, ImmutableCollection.Builder.expandedCapacity(0, 1));
    assertEquals(2, ImmutableCollection.Builder.expandedCapacity(0, 2));
    assertEquals(2, ImmutableCollection.Builder.expandedCapacity(1, 2));
    assertEquals(
        Integer.MAX_VALUE, ImmutableCollection.Builder.expandedCapacity(0, Integer.MAX_VALUE));
    assertEquals(
        Integer.MAX_VALUE, ImmutableCollection.Builder.expandedCapacity(1, Integer.MAX_VALUE));
    assertEquals(
        Integer.MAX_VALUE,
        ImmutableCollection.Builder.expandedCapacity(Integer.MAX_VALUE - 1, Integer.MAX_VALUE));

    assertEquals(13, ImmutableCollection.Builder.expandedCapacity(8, 9));
  }
}
