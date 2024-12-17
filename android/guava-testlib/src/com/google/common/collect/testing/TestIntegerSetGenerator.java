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
import com.google.common.collect.testing.SampleElements.Ints;
import java.util.List;
import java.util.Set;

/**
 * Create integer sets for collection tests.
 *
 * @author Gregory Kick
 */
@GwtCompatible
public abstract class TestIntegerSetGenerator implements TestSetGenerator<Integer> {
  @Override
  public SampleElements<Integer> samples() {
    return new Ints();
  }

  @Override
  public Set<Integer> create(Object... elements) {
    Integer[] array = new Integer[elements.length];
    int i = 0;
    for (Object e : elements) {
      array[i++] = (Integer) e;
    }
    return create(array);
  }

  protected abstract Set<Integer> create(Integer[] elements);

  @Override
  public Integer[] createArray(int length) {
    return new Integer[length];
  }

  /**
   * {@inheritDoc}
   *
   * <p>By default, returns the supplied elements in their given order; however, generators for
   * containers with a known order other than insertion order must override this method.
   *
   * <p>Note: This default implementation is overkill (but valid) for an unordered container. An
   * equally valid implementation for an unordered container is to throw an exception. The chosen
   * implementation, however, has the advantage of working for insertion-ordered containers, as
   * well.
   */
  @Override
  public List<Integer> order(List<Integer> insertionOrder) {
    return insertionOrder;
  }
}
