/*
 * Copyright (C) 2012 The Guava Authors
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

import com.google.common.annotations.GwtIncompatible;
import java.util.Map;

@GwtIncompatible
public class CompactLinkedHashMapFloodingTest
    extends AbstractHashFloodingTest<Map<Object, Object>> {
  public CompactLinkedHashMapFloodingTest() {
    super(
        ImmutableList.of(Construction.mapFromKeys(CompactLinkedHashMap::create)),
        n -> n * Math.log(n),
        ImmutableList.of(QueryOp.MAP_GET));
  }
}
