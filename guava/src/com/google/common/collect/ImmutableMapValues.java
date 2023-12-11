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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;

/**
 * {@code values()} implementation for {@link ImmutableMap}.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
final class ImmutableMapValues<K, V> extends ImmutableCollection<V> {
  private final ImmutableMap<K, V> map;

  ImmutableMapValues(ImmutableMap<K, V> map) {
    this.map = map;
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public UnmodifiableIterator<V> iterator() {
    return new UnmodifiableIterator<V>() {
      final UnmodifiableIterator<Entry<K, V>> entryItr = map.entrySet().iterator();

      @Override
      public boolean hasNext() {
        return entryItr.hasNext();
      }

      @Override
      public V next() {
        return entryItr.next().getValue();
      }
    };
  }

  @Override
  public Spliterator<V> spliterator() {
    return CollectSpliterators.map(map.entrySet().spliterator(), Entry::getValue);
  }

  @Override
  public boolean contains(@CheckForNull Object object) {
    return object != null && Iterators.contains(iterator(), object);
  }

  @Override
  boolean isPartialView() {
    return true;
  }

  @Override
  public ImmutableList<V> asList() {
    final ImmutableList<Entry<K, V>> entryList = map.entrySet().asList();
    return new ImmutableAsList<V>() {
      @Override
      public V get(int index) {
        return entryList.get(index).getValue();
      }

      @Override
      ImmutableCollection<V> delegateCollection() {
        return ImmutableMapValues.this;
      }

      // redeclare to help optimizers with b/310253115
      @SuppressWarnings("RedundantOverride")
      @Override
      @J2ktIncompatible // serialization
      @GwtIncompatible // serialization
      Object writeReplace() {
        return super.writeReplace();
      }
    };
  }

  @GwtIncompatible // serialization
  @Override
  public void forEach(Consumer<? super V> action) {
    checkNotNull(action);
    map.forEach((k, v) -> action.accept(v));
  }

  // redeclare to help optimizers with b/310253115
  @SuppressWarnings("RedundantOverride")
  @Override
  @J2ktIncompatible // serialization
  @GwtIncompatible // serialization
  Object writeReplace() {
    return super.writeReplace();
  }

  // No longer used for new writes, but kept so that old data can still be read.
  @GwtIncompatible // serialization
  @J2ktIncompatible
  @SuppressWarnings("unused")
  private static class SerializedForm<V> implements Serializable {
    final ImmutableMap<?, V> map;

    SerializedForm(ImmutableMap<?, V> map) {
      this.map = map;
    }

    Object readResolve() {
      return map.values();
    }

    private static final long serialVersionUID = 0;
  }
}
