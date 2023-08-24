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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Platform.getDeclaringClassOrObjectForJ2cl;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * A {@code BiMap} backed by two {@code EnumMap} instances. Null keys and values are not permitted.
 * An {@code EnumBiMap} and its inverse are both serializable.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#bimap">{@code BiMap}</a>.
 *
 * @author Mike Bostock
 * @since 2.0
 */
@GwtCompatible(emulated = true)
@J2ktIncompatible
@ElementTypesAreNonnullByDefault
public final class EnumBiMap<K extends Enum<K>, V extends Enum<V>> extends AbstractBiMap<K, V> {
  /*
   * J2CL's EnumMap does not need the Class instance, so we can use Object.class instead. (Or we
   * could use null, but that messes with our nullness checking, including under J2KT. We could
   * probably work around it by changing how we annotate the J2CL EnumMap, but that's probably more
   * trouble than just using Object.class.)
   *
   * Then we declare the getters for these fields as @GwtIncompatible so that no one can try to use
   * them under J2CL—or, as an unfortunate side effect, under GWT. We do still give the fields
   * themselves their proper values under GWT, since GWT's EnumMap does need the Class instance.
   *
   * Note that sometimes these fields *do* have correct values under J2CL: They will if the caller
   * calls `create(Foo.class)`, rather than `create(map)`. That's fine; we just shouldn't rely on
   * it.
   */
  transient Class<K> keyTypeOrObjectUnderJ2cl;
  transient Class<V> valueTypeOrObjectUnderJ2cl;

  /**
   * Returns a new, empty {@code EnumBiMap} using the specified key and value types.
   *
   * @param keyType the key type
   * @param valueType the value type
   */
  public static <K extends Enum<K>, V extends Enum<V>> EnumBiMap<K, V> create(
      Class<K> keyType, Class<V> valueType) {
    return new EnumBiMap<>(keyType, valueType);
  }

  /**
   * Returns a new bimap with the same mappings as the specified map. If the specified map is an
   * {@code EnumBiMap}, the new bimap has the same types as the provided map. Otherwise, the
   * specified map must contain at least one mapping, in order to determine the key and value types.
   *
   * @param map the map whose mappings are to be placed in this map
   * @throws IllegalArgumentException if map is not an {@code EnumBiMap} instance and contains no
   *     mappings
   */
  public static <K extends Enum<K>, V extends Enum<V>> EnumBiMap<K, V> create(Map<K, V> map) {
    EnumBiMap<K, V> bimap =
        create(inferKeyTypeOrObjectUnderJ2cl(map), inferValueTypeOrObjectUnderJ2cl(map));
    bimap.putAll(map);
    return bimap;
  }

  private EnumBiMap(Class<K> keyTypeOrObjectUnderJ2cl, Class<V> valueTypeOrObjectUnderJ2cl) {
    super(
        new EnumMap<K, V>(keyTypeOrObjectUnderJ2cl), new EnumMap<V, K>(valueTypeOrObjectUnderJ2cl));
    this.keyTypeOrObjectUnderJ2cl = keyTypeOrObjectUnderJ2cl;
    this.valueTypeOrObjectUnderJ2cl = valueTypeOrObjectUnderJ2cl;
  }

  static <K extends Enum<K>> Class<K> inferKeyTypeOrObjectUnderJ2cl(Map<K, ?> map) {
    if (map instanceof EnumBiMap) {
      return ((EnumBiMap<K, ?>) map).keyTypeOrObjectUnderJ2cl;
    }
    if (map instanceof EnumHashBiMap) {
      return ((EnumHashBiMap<K, ?>) map).keyTypeOrObjectUnderJ2cl;
    }
    checkArgument(!map.isEmpty());
    return getDeclaringClassOrObjectForJ2cl(map.keySet().iterator().next());
  }

  private static <V extends Enum<V>> Class<V> inferValueTypeOrObjectUnderJ2cl(Map<?, V> map) {
    if (map instanceof EnumBiMap) {
      return ((EnumBiMap<?, V>) map).valueTypeOrObjectUnderJ2cl;
    }
    checkArgument(!map.isEmpty());
    return getDeclaringClassOrObjectForJ2cl(map.values().iterator().next());
  }

  /** Returns the associated key type. */
  @GwtIncompatible
  public Class<K> keyType() {
    return keyTypeOrObjectUnderJ2cl;
  }

  /** Returns the associated value type. */
  @GwtIncompatible
  public Class<V> valueType() {
    return valueTypeOrObjectUnderJ2cl;
  }

  @Override
  K checkKey(K key) {
    return checkNotNull(key);
  }

  @Override
  V checkValue(V value) {
    return checkNotNull(value);
  }

  /**
   * @serialData the key class, value class, number of entries, first key, first value, second key,
   *     second value, and so on.
   */
  @GwtIncompatible // java.io.ObjectOutputStream
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(keyTypeOrObjectUnderJ2cl);
    stream.writeObject(valueTypeOrObjectUnderJ2cl);
    Serialization.writeMap(this, stream);
  }

  @SuppressWarnings("unchecked") // reading fields populated by writeObject
  @GwtIncompatible // java.io.ObjectInputStream
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    keyTypeOrObjectUnderJ2cl = (Class<K>) requireNonNull(stream.readObject());
    valueTypeOrObjectUnderJ2cl = (Class<V>) requireNonNull(stream.readObject());
    setDelegates(
        new EnumMap<K, V>(keyTypeOrObjectUnderJ2cl), new EnumMap<V, K>(valueTypeOrObjectUnderJ2cl));
    Serialization.populateMap(this, stream);
  }

  @GwtIncompatible // not needed in emulated source.
  private static final long serialVersionUID = 0;
}
