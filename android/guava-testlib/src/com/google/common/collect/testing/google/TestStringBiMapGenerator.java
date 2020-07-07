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

package com.google.common.collect.testing.google;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.BiMap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.SampleElements;
import java.util.List;
import java.util.Map.Entry;

/**
 * Implementation helper for {@link TestBiMapGenerator} for use with bimaps of strings.
 *
 * @author Chris Povirk
 * @author Jared Levy
 * @author George van den Driessche
 * @author Louis Wasserman
 */
@GwtCompatible
public abstract class TestStringBiMapGenerator implements TestBiMapGenerator<String, String> {

  @Override
  public SampleElements<Entry<String, String>> samples() {
    return new SampleElements<>(
        Helpers.mapEntry("one", "January"),
        Helpers.mapEntry("two", "February"),
        Helpers.mapEntry("three", "March"),
        Helpers.mapEntry("four", "April"),
        Helpers.mapEntry("five", "May"));
  }

  @Override
  public final BiMap<String, String> create(Object... entries) {
    @SuppressWarnings("unchecked")
    Entry<String, String>[] array = new Entry[entries.length];
    int i = 0;
    for (Object o : entries) {
      @SuppressWarnings("unchecked")
      Entry<String, String> e = (Entry<String, String>) o;
      array[i++] = e;
    }
    return create(array);
  }

  protected abstract BiMap<String, String> create(Entry<String, String>[] entries);

  @Override
  @SuppressWarnings("unchecked")
  public final Entry<String, String>[] createArray(int length) {
    return new Entry[length];
  }

  @Override
  public final String[] createKeyArray(int length) {
    return new String[length];
  }

  @Override
  public final String[] createValueArray(int length) {
    return new String[length];
  }

  /** Returns the original element list, unchanged. */
  @Override
  public Iterable<Entry<String, String>> order(List<Entry<String, String>> insertionOrder) {
    return insertionOrder;
  }
}
