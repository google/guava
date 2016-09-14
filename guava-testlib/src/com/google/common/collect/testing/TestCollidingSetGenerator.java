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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.SampleElements.Colliders;
import java.util.List;

/**
 * A generator using sample elements whose hash codes all collide badly.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
public abstract class TestCollidingSetGenerator implements TestSetGenerator<Object> {
  @Override
  public SampleElements<Object> samples() {
    return new Colliders();
  }

  @Override
  public Object[] createArray(int length) {
    return new Object[length];
  }

  /** Returns the original element list, unchanged. */
  @Override
  public List<Object> order(List<Object> insertionOrder) {
    return insertionOrder;
  }
}
