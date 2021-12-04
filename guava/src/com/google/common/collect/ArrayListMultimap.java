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

import static com.google.common.collect.CollectPreconditions.checkNonnegative;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of {@code Multimap} that uses an {@code ArrayList} to store the values for a given
 * key. A {@link HashMap} associates each key with an {@link ArrayList} of values.
 *
 * <p>When iterating through the collections supplied by this class, the ordering of values for a
 * given key agrees with the order in which the values were added.
 *
 * <p>This multimap allows duplicate key-value pairs. After adding a new key-value pair equal to an
 * existing key-value pair, the {@code ArrayListMultimap} will contain entries for both the new
 * value and the old value.
 *
 * <p>Keys and values may be null. All optional multimap methods are supported, and all returned
 * views are modifiable.
 *
 * <p>The lists returned by {@link #get}, {@link #removeAll}, and {@link #replaceValues} all
 * implement {@link java.util.RandomAccess}.
 *
 * <p>This class is not threadsafe when any concurrent operations update the multimap. Concurrent
 * read operations will work correctly. To allow concurrent update operations, wrap your multimap
 * with a call to {@link Multimaps#synchronizedListMultimap}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#multimap"> {@code
 * Multimap}</a>.
 *
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
@ElementTypesAreNonnullByDefault
public final class ArrayListMultimap<K extends @Nullable Object, V extends @Nullable Object>
    extends ArrayListMultimapGwtSerializationDependencies<K, V> {
  // Default from ArrayList
  private static final int DEFAULT_VALUES_PER_KEY = 3;

  @VisibleForTesting transient int expectedValuesPerKey;

  /**
   * Creates a new, empty {@code ArrayListMultimap} with the default initial capacities.
   *
   * <p>This method will soon be deprecated in favor of {@code
   * MultimapBuilder.hashKeys().arrayListValues().build()}.
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      ArrayListMultimap<K, V> create() {
    return new ArrayListMultimap<>();
  }

  /**
   * Constructs an empty {@code ArrayListMultimap} with enough capacity to hold the specified
   * numbers of keys and values without resizing.
   *
   * <p>This method will soon be deprecated in favor of {@code
   * MultimapBuilder.hashKeys(expectedKeys).arrayListValues(expectedValuesPerKey).build()}.
   *
   * @param expectedKeys the expected number of distinct keys
   * @param expectedValuesPerKey the expected average number of values per key
   * @throws IllegalArgumentException if {@code expectedKeys} or {@code expectedValuesPerKey} is
   *     negative
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      ArrayListMultimap<K, V> create(int expectedKeys, int expectedValuesPerKey) {
    return new ArrayListMultimap<>(expectedKeys, expectedValuesPerKey);
  }

  /**
   * Constructs an {@code ArrayListMultimap} with the same mappings as the specified multimap.
   *
   * <p>This method will soon be deprecated in favor of {@code
   * MultimapBuilder.hashKeys().arrayListValues().build(multimap)}.
   *
   * @param multimap the multimap whose contents are copied to this multimap
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      ArrayListMultimap<K, V> create(Multimap<? extends K, ? extends V> multimap) {
    return new ArrayListMultimap<>(multimap);
  }

  private ArrayListMultimap() {
    this(12, DEFAULT_VALUES_PER_KEY);
  }

  private ArrayListMultimap(int expectedKeys, int expectedValuesPerKey) {
    super(Platform.<K, Collection<V>>newHashMapWithExpectedSize(expectedKeys));
    checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey");
    this.expectedValuesPerKey = expectedValuesPerKey;
  }

  private ArrayListMultimap(Multimap<? extends K, ? extends V> multimap) {
    this(
        multimap.keySet().size(),
        (multimap instanceof ArrayListMultimap)
            ? ((ArrayListMultimap<?, ?>) multimap).expectedValuesPerKey
            : DEFAULT_VALUES_PER_KEY);
    putAll(multimap);
  }

  /**
   * Creates a new, empty {@code ArrayList} to hold the collection of values for an arbitrary key.
   */
  @Override
  List<V> createCollection() {
    return new ArrayList<V>(expectedValuesPerKey);
  }

  /**
   * Reduces the memory used by this {@code ArrayListMultimap}, if feasible.
   *
   * @deprecated For a {@link ListMultimap} that automatically trims to size, use {@link
   *     ImmutableListMultimap}. If you need a mutable collection, remove the {@code trimToSize}
   *     call, or switch to a {@code HashMap<K, ArrayList<V>>}.
   */
  @Deprecated
  public void trimToSize() {
    for (Collection<V> collection : backingMap().values()) {
      ArrayList<V> arrayList = (ArrayList<V>) collection;
      arrayList.trimToSize();
    }
  }

  /**
   * @serialData expectedValuesPerKey, number of distinct keys, and then for each distinct key: the
   *     key, number of values for that key, and the key's values
   */
  @GwtIncompatible // java.io.ObjectOutputStream
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    Serialization.writeMultimap(this, stream);
  }

  @GwtIncompatible // java.io.ObjectOutputStream
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    expectedValuesPerKey = DEFAULT_VALUES_PER_KEY;
    int distinctKeys = Serialization.readCount(stream);
    Map<K, Collection<V>> map = Maps.newHashMap();
    setMap(map);
    Serialization.populateMultimap(this, stream, distinctKeys);
  }

  @GwtIncompatible // Not needed in emulated source.
  private static final long serialVersionUID = 0;
}
