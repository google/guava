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
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.primitives.Primitives;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A mutable class-to-instance map backed by an arbitrary user-provided map. See also {@link
 * ImmutableClassToInstanceMap}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#classtoinstancemap">{@code
 * ClassToInstanceMap}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
@J2ktIncompatible
@GwtIncompatible
@SuppressWarnings("serial") // using writeReplace instead of standard serialization
@ElementTypesAreNonnullByDefault
public final class MutableClassToInstanceMap<B extends @Nullable Object>
    extends ForwardingMap<Class<? extends @NonNull B>, B>
    implements ClassToInstanceMap<B>, Serializable {

  /**
   * Returns a new {@code MutableClassToInstanceMap} instance backed by a {@link HashMap} using the
   * default initial capacity and load factor.
   */
  public static <B extends @Nullable Object> MutableClassToInstanceMap<B> create() {
    return new MutableClassToInstanceMap<B>(new HashMap<Class<? extends @NonNull B>, B>());
  }

  /**
   * Returns a new {@code MutableClassToInstanceMap} instance backed by a given empty {@code
   * backingMap}. The caller surrenders control of the backing map, and thus should not allow any
   * direct references to it to remain accessible.
   */
  public static <B extends @Nullable Object> MutableClassToInstanceMap<B> create(
      Map<Class<? extends @NonNull B>, B> backingMap) {
    return new MutableClassToInstanceMap<B>(backingMap);
  }

  private final Map<Class<? extends @NonNull B>, B> delegate;

  private MutableClassToInstanceMap(Map<Class<? extends @NonNull B>, B> delegate) {
    this.delegate = checkNotNull(delegate);
  }

  @Override
  protected Map<Class<? extends @NonNull B>, B> delegate() {
    return delegate;
  }

  /**
   * Wraps the {@code setValue} implementation of an {@code Entry} to enforce the class constraint.
   */
  private static <B extends @Nullable Object> Entry<Class<? extends @NonNull B>, B> checkedEntry(
      final Entry<Class<? extends @NonNull B>, B> entry) {
    return new ForwardingMapEntry<Class<? extends @NonNull B>, B>() {
      @Override
      protected Entry<Class<? extends @NonNull B>, B> delegate() {
        return entry;
      }

      @Override
      @ParametricNullness
      public B setValue(@ParametricNullness B value) {
        cast(getKey(), value);
        return super.setValue(value);
      }
    };
  }

  @Override
  public Set<Entry<Class<? extends @NonNull B>, B>> entrySet() {
    return new ForwardingSet<Entry<Class<? extends @NonNull B>, B>>() {

      @Override
      protected Set<Entry<Class<? extends @NonNull B>, B>> delegate() {
        return MutableClassToInstanceMap.this.delegate().entrySet();
      }

      @Override
      public Spliterator<Entry<Class<? extends @NonNull B>, B>> spliterator() {
        return CollectSpliterators.map(
            delegate().spliterator(), MutableClassToInstanceMap::checkedEntry);
      }

      @Override
      public Iterator<Entry<Class<? extends @NonNull B>, B>> iterator() {
        return new TransformedIterator<
            Entry<Class<? extends @NonNull B>, B>, Entry<Class<? extends @NonNull B>, B>>(
            delegate().iterator()) {
          @Override
          Entry<Class<? extends @NonNull B>, B> transform(
              Entry<Class<? extends @NonNull B>, B> from) {
            return checkedEntry(from);
          }
        };
      }

      @Override
      public Object[] toArray() {
        /*
         * standardToArray returns `@Nullable Object[]` rather than `Object[]` but only because it
         * can be used with collections that may contain null. This collection is a collection of
         * non-null Entry objects (Entry objects that might contain null values but are not
         * themselves null), so we can treat it as a plain `Object[]`.
         */
        @SuppressWarnings("nullness")
        Object[] result = standardToArray();
        return result;
      }

      @Override
      @SuppressWarnings("nullness") // b/192354773 in our checker affects toArray declarations
      public <T extends @Nullable Object> T[] toArray(T[] array) {
        return standardToArray(array);
      }
    };
  }

  @Override
  @CanIgnoreReturnValue
  @CheckForNull
  public B put(Class<? extends @NonNull B> key, @ParametricNullness B value) {
    cast(key, value);
    return super.put(key, value);
  }

  @Override
  public void putAll(Map<? extends Class<? extends @NonNull B>, ? extends B> map) {
    Map<Class<? extends @NonNull B>, B> copy = new LinkedHashMap<>(map);
    for (Entry<? extends Class<? extends @NonNull B>, B> entry : copy.entrySet()) {
      cast(entry.getKey(), entry.getValue());
    }
    super.putAll(copy);
  }

  @CanIgnoreReturnValue
  @Override
  @CheckForNull
  public <T extends B> T putInstance(Class<@NonNull T> type, @ParametricNullness T value) {
    return cast(type, put(type, value));
  }

  @Override
  @CheckForNull
  public <T extends @NonNull B> T getInstance(Class<T> type) {
    return cast(type, get(type));
  }

  @CanIgnoreReturnValue
  @CheckForNull
  private static <T> T cast(Class<T> type, @CheckForNull Object value) {
    return Primitives.wrap(type).cast(value);
  }

  private Object writeReplace() {
    return new SerializedForm(delegate());
  }

  private void readObject(ObjectInputStream stream) throws InvalidObjectException {
    throw new InvalidObjectException("Use SerializedForm");
  }

  /** Serialized form of the map, to avoid serializing the constraint. */
  private static final class SerializedForm<B extends @Nullable Object> implements Serializable {
    private final Map<Class<? extends @NonNull B>, B> backingMap;

    SerializedForm(Map<Class<? extends @NonNull B>, B> backingMap) {
      this.backingMap = backingMap;
    }

    Object readResolve() {
      return create(backingMap);
    }

    private static final long serialVersionUID = 0;
  }
}
