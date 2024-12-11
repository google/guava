/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.collect.testing.google;

import static java.util.Collections.sort;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multiset;
import com.google.common.collect.testing.AnEnum;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SampleElements.Enums;
import java.util.List;

/**
 * An abstract {@code TestMultisetGenerator} for generating multisets containing enum values.
 *
 * @author Jared Levy
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public abstract class TestEnumMultisetGenerator implements TestMultisetGenerator<AnEnum> {
  @Override
  public SampleElements<AnEnum> samples() {
    return new Enums();
  }

  @Override
  public Multiset<AnEnum> create(Object... elements) {
    AnEnum[] array = new AnEnum[elements.length];
    int i = 0;
    for (Object e : elements) {
      array[i++] = (AnEnum) e;
    }
    return create(array);
  }

  protected abstract Multiset<AnEnum> create(AnEnum[] elements);

  @Override
  public AnEnum[] createArray(int length) {
    return new AnEnum[length];
  }

  /** Sorts the enums according to their natural ordering. */
  @Override
  public List<AnEnum> order(List<AnEnum> insertionOrder) {
    sort(insertionOrder);
    return insertionOrder;
  }
}
