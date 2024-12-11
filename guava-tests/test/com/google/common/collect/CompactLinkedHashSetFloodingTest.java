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

import static java.lang.Math.log;

import com.google.common.annotations.GwtIncompatible;
import java.util.Set;

@GwtIncompatible
public class CompactLinkedHashSetFloodingTest extends AbstractHashFloodingTest<Set<Object>> {
  public CompactLinkedHashSetFloodingTest() {
    super(
        ImmutableList.of(Construction.setFromElements(CompactLinkedHashSet::create)),
        n -> n * log(n),
        ImmutableList.of(QueryOp.SET_CONTAINS));
  }
}
