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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.primitives.Primitives;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A mutable class-to-instance map backed by an arbitrary user-provided map. See also {@link
 * ImmutableClassToInstanceMap}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#classtoinstancemap"> {@code
 * ClassToInstanceMap}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtIncompatible
@SuppressWarnings("serial") // using writeReplace instead of standard serialization
public final class MutableClassToInstanceMap<B extends @NonNull Object>
    extends ForwardingMap<Class<? extends B>, @Nullable B>
    implements ClassToInstanceMap<B>, Serializable {

  /**
   * Returns a new {@code MutableClassToInstanceMap} instance backed by a {@link HashMap} using the
   * default initial capacity and load factor.
   */
  public static <B extends @NonNull Object> MutableClassToInstanceMap<B> create() {
    return new MutableClassToInstanceMap<B>(new HashMap<Class<? extends B>, @Nullable B>());
  }

  /**
   * Returns a new {@code MutableClassToInstanceMap} instance backed by a given empty {@code
   * backingMap}. The caller surrenders control of the backing map, and thus should not allow any
   * direct references to it to remain accessible.
   */
  public static <B extends @NonNull Object> MutableClassToInstanceMap<B> create(
      Map<Class<? extends B>, @Nullable B> backingMap) {
    return new MutableClassToInstanceMap<B>(backingMap);
  }

  private final Map<Class<? extends B>, @Nullable B> delegate;

  private MutableClassToInstanceMap(Map<Class<? extends B>, @Nullable B> delegate) {
    this.delegate = checkNotNull(delegate);
  }

  @Override
  protected Map<Class<? extends B>, @Nullable B> delegate() {
    return delegate;
  }

  /**
   * Wraps the {@code setValue} implementation of an {@code Entry} to enforce the class constraint.
   */
  private static <B extends @NonNull Object> Entry<Class<? extends B>, @Nullable B> checkedEntry(
      Entry<Class<? extends B>, @Nullable B> entry) {
    return new ForwardingMapEntry<Class<? extends B>, @Nullable B>() {
      @Override
      protected Entry<Class<? extends B>, @Nullable B> delegate() {
        return entry;
      }

      @Override
      public @Nullable B setValue(@Nullable B value) {
        return super.setValue(cast(getKey(), value));
      }
    };
  }

  @Override
  // The warning is probably related to https://github.com/typetools/checker-framework/issues/3027
  @SuppressWarnings("override.return.invalid")
  public Set<Entry<Class<? extends B>, @Nullable B>> entrySet() {
    return new CheckedEntrySet();
  }

  // Not an anonymous class to avoid https://github.com/typetools/checker-framework/issues/3021
  private final class CheckedEntrySet
      extends ForwardingSet<Entry<Class<? extends B>, @Nullable B>> {
    @Override
    protected Set<Entry<Class<? extends B>, @Nullable B>> delegate() {
      return MutableClassToInstanceMap.this.delegate().entrySet();
    }

    @Override
    public Spliterator<Entry<Class<? extends B>, @Nullable B>> spliterator() {
      return CollectSpliterators
          .<Entry<Class<? extends B>, @Nullable B>, Entry<Class<? extends B>, @Nullable B>>map(
              delegate().spliterator(), MutableClassToInstanceMap::checkedEntry);
    }

    @Override
    public Iterator<Entry<Class<? extends B>, @Nullable B>> iterator() {
      return new TransformedIterator<
          Entry<Class<? extends B>, @Nullable B>, Entry<Class<? extends B>, @Nullable B>>(
          delegate().iterator()) {
        @Override
        Entry<Class<? extends B>, @Nullable B> transform(
            Entry<Class<? extends B>, @Nullable B> from) {
          return MutableClassToInstanceMap.<B>checkedEntry(from);
        }
      };
    }

    @Override
@SuppressWarnings("nullness")
    public Object[] toArray() {
      return standardToArray();
    }

    @Override
@SuppressWarnings("nullness")
    public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }
  }

  @Override
  @CanIgnoreReturnValue
  // The warning is probably related to https://github.com/typetools/checker-framework/issues/3027
  @SuppressWarnings("override.param.invalid")
  public @Nullable B put(Class<? extends B> key, @Nullable B value) {
    return super.put(key, cast(key, value));
  }

  @Override
  // The warning is probably related to https://github.com/typetools/checker-framework/issues/3027
  @SuppressWarnings("override.param.invalid")
  public void putAll(Map<? extends Class<? extends B>, ? extends @Nullable B> map) {
    Map<Class<? extends B>, @Nullable B> copy = new LinkedHashMap<>(map);
    for (Entry<? extends Class<? extends B>, @Nullable B> entry : copy.entrySet()) {
      cast(entry.getKey(), entry.getValue());
    }
    super.putAll(copy);
  }

  @CanIgnoreReturnValue
  @Override
  public <T extends B> @Nullable T putInstance(Class<T> type, @Nullable T value) {
    return cast(type, put(type, value));
  }

  @Override
  public <T extends B> @Nullable T getInstance(Class<T> type) {
    return cast(type, get(type));
  }

  @CanIgnoreReturnValue
  private static <T extends @NonNull Object> @Nullable T cast(
      Class<T> type, @Nullable Object value) {
    return Primitives.wrap(type).cast(value);
  }

  private Object writeReplace() {
    return new SerializedForm(delegate());
  }

  /** Serialized form of the map, to avoid serializing the constraint. */
  private static final class SerializedForm<B extends @NonNull Object> implements Serializable {
    private final Map<Class<? extends B>, @Nullable B> backingMap;

    SerializedForm(Map<Class<? extends B>, @Nullable B> backingMap) {
      this.backingMap = backingMap;
    }

    Object readResolve() {
      return MutableClassToInstanceMap.<B>create(backingMap);
    }

    private static final long serialVersionUID = 0;
  }
}
