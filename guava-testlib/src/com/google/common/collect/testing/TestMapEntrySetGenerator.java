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

package com.google.common.collect.testing;

import static java.lang.System.arraycopy;

import com.google.common.annotations.GwtCompatible;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Creates map entries using sample keys and sample values.
 *
 * @author Jesse Wilson
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public abstract class TestMapEntrySetGenerator<
        K extends @Nullable Object, V extends @Nullable Object>
    implements TestSetGenerator<Map.Entry<K, V>> {
  private final SampleElements<K> keys;
  private final SampleElements<V> values;

  protected TestMapEntrySetGenerator(SampleElements<K> keys, SampleElements<V> values) {
    this.keys = keys;
    this.values = values;
  }

  @Override
  public SampleElements<Entry<K, V>> samples() {
    return SampleElements.mapEntries(keys, values);
  }

  @Override
  public Set<Entry<K, V>> create(Object... elements) {
    Entry<K, V>[] entries = createArray(elements.length);
    arraycopy(elements, 0, entries, 0, elements.length);
    return createFromEntries(entries);
  }

  public abstract Set<Entry<K, V>> createFromEntries(Entry<K, V>[] entries);

  @Override
  @SuppressWarnings("unchecked") // generic arrays make typesafety sad
  public Entry<K, V>[] createArray(int length) {
    return (Entry<K, V>[]) new Entry<?, ?>[length];
  }

  /** Returns the original element list, unchanged. */
  @Override
  public List<Entry<K, V>> order(List<Entry<K, V>> insertionOrder) {
    return insertionOrder;
  }
}
