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
import com.google.common.collect.testing.SampleElements.Strings;
import java.util.List;

/**
 * TODO: javadoc.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
public abstract class TestStringListGenerator implements TestListGenerator<String> {
  @Override
  public SampleElements<String> samples() {
    return new Strings();
  }

  @Override
  public List<String> create(Object... elements) {
    String[] array = new String[elements.length];
    int i = 0;
    for (Object e : elements) {
      array[i++] = (String) e;
    }
    return create(array);
  }

  /**
   * Creates a new collection containing the given elements; implement this method instead of {@link
   * #create(Object...)}.
   */
  protected abstract List<String> create(String[] elements);

  @Override
  public String[] createArray(int length) {
    return new String[length];
  }

  /** Returns the original element list, unchanged. */
  @Override
  public List<String> order(List<String> insertionOrder) {
    return insertionOrder;
  }
}
