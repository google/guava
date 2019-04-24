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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * A {@code BiMap} backed by an {@code EnumMap} instance for keys-to-values, and a {@code HashMap}
 * instance for values-to-keys. Null keys are not permitted, but null values are. An {@code
 * EnumHashBiMap} and its inverse are both serializable.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#bimap"> {@code BiMap}</a>.
 *
 * @author Mike Bostock
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class EnumHashBiMap<K extends Enum<K>, V> extends AbstractBiMap<K, V> {
  private transient Class<K> keyType;

  /**
   * Returns a new, empty {@code EnumHashBiMap} using the specified key type.
   *
   * @param keyType the key type
   */
  public static <K extends Enum<K>, V> EnumHashBiMap<K, V> create(Class<K> keyType) {
    return new EnumHashBiMap<>(keyType);
  }

  /**
   * Constructs a new bimap with the same mappings as the specified map. If the specified map is an
   * {@code EnumHashBiMap} or an {@link EnumBiMap}, the new bimap has the same key type as the input
   * bimap. Otherwise, the specified map must contain at least one mapping, in order to determine
   * the key type.
   *
   * @param map the map whose mappings are to be placed in this map
   * @throws IllegalArgumentException if map is not an {@code EnumBiMap} or an {@code EnumHashBiMap}
   *     instance and contains no mappings
   */
  public static <K extends Enum<K>, V> EnumHashBiMap<K, V> create(Map<K, ? extends V> map) {
    EnumHashBiMap<K, V> bimap = create(EnumBiMap.inferKeyType(map));
    bimap.putAll(map);
    return bimap;
  }

  private EnumHashBiMap(Class<K> keyType) {
    super(
        new EnumMap<K, V>(keyType),
        Maps.<V, K>newHashMapWithExpectedSize(keyType.getEnumConstants().length));
    this.keyType = keyType;
  }

  // Overriding these 3 methods to show that values may be null (but not keys)

  @Override
  K checkKey(K key) {
    return checkNotNull(key);
  }

  @CanIgnoreReturnValue
  @Override
  public V put(K key, @NullableDecl V value) {
    return super.put(key, value);
  }

  @CanIgnoreReturnValue
  @Override
  public V forcePut(K key, @NullableDecl V value) {
    return super.forcePut(key, value);
  }

  /** Returns the associated key type. */
  public Class<K> keyType() {
    return keyType;
  }

  /**
   * @serialData the key class, number of entries, first key, first value, second key, second value,
   *     and so on.
   */
  @GwtIncompatible // java.io.ObjectOutputStream
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(keyType);
    Serialization.writeMap(this, stream);
  }

  @SuppressWarnings("unchecked") // reading field populated by writeObject
  @GwtIncompatible // java.io.ObjectInputStream
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    keyType = (Class<K>) stream.readObject();
    setDelegates(
        new EnumMap<K, V>(keyType), new HashMap<V, K>(keyType.getEnumConstants().length * 3 / 2));
    Serialization.populateMap(this, stream);
  }

  @GwtIncompatible // only needed in emulated source.
  private static final long serialVersionUID = 0;
}
