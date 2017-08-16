/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.collect.testing.Helpers.orderEntriesByKey;

import com.google.common.annotations.GwtCompatible;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implementation helper for {@link TestMapGenerator} for use with enum maps.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
public abstract class TestEnumMapGenerator implements TestMapGenerator<AnEnum, String> {

  @Override
  public SampleElements<Entry<AnEnum, String>> samples() {
    return new SampleElements<>(
        Helpers.mapEntry(AnEnum.A, "January"),
        Helpers.mapEntry(AnEnum.B, "February"),
        Helpers.mapEntry(AnEnum.C, "March"),
        Helpers.mapEntry(AnEnum.D, "April"),
        Helpers.mapEntry(AnEnum.E, "May"));
  }

  @Override
  public final Map<AnEnum, String> create(Object... entries) {
    @SuppressWarnings("unchecked")
    Entry<AnEnum, String>[] array = new Entry[entries.length];
    int i = 0;
    for (Object o : entries) {
      @SuppressWarnings("unchecked")
      Entry<AnEnum, String> e = (Entry<AnEnum, String>) o;
      array[i++] = e;
    }
    return create(array);
  }

  protected abstract Map<AnEnum, String> create(Entry<AnEnum, String>[] entries);

  @Override
  @SuppressWarnings("unchecked")
  public final Entry<AnEnum, String>[] createArray(int length) {
    return new Entry[length];
  }

  @Override
  public final AnEnum[] createKeyArray(int length) {
    return new AnEnum[length];
  }

  @Override
  public final String[] createValueArray(int length) {
    return new String[length];
  }

  /** Returns the elements sorted in natural order. */
  @Override
  public Iterable<Entry<AnEnum, String>> order(List<Entry<AnEnum, String>> insertionOrder) {
    return orderEntriesByKey(insertionOrder);
  }
}
