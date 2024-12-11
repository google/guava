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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedWith;
import com.google.j2objc.annotations.Weak;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link SetMultimap} whose contents will never change, with many other important properties
 * detailed at {@link ImmutableCollection}.
 *
 * <p><b>Warning:</b> As in all {@link SetMultimap}s, do not modify either a key <i>or a value</i>
 * of a {@code ImmutableSetMultimap} in a way that affects its {@link Object#equals} behavior.
 * Undefined behavior and bugs will result.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained">immutable collections</a>.
 *
 * @author Mike Ward
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
@ElementTypesAreNonnullByDefault
public class ImmutableSetMultimap<K, V> extends ImmutableMultimap<K, V>
    implements SetMultimap<K, V> {
  /**
   * Returns a {@link Collector} that accumulates elements into an {@code ImmutableSetMultimap}
   * whose keys and values are the result of applying the provided mapping functions to the input
   * elements.
   *
   * <p>For streams with defined encounter order (as defined in the Ordering section of the {@link
   * java.util.stream} Javadoc), that order is preserved, but entries are <a
   * href="ImmutableMultimap.html#iteration">grouped by key</a>.
   *
   * <p>Example:
   *
   * <pre>{@code
   * static final Multimap<Character, String> FIRST_LETTER_MULTIMAP =
   *     Stream.of("banana", "apple", "carrot", "asparagus", "cherry")
   *         .collect(toImmutableSetMultimap(str -> str.charAt(0), str -> str.substring(1)));
   *
   * // is equivalent to
   *
   * static final Multimap<Character, String> FIRST_LETTER_MULTIMAP =
   *     new ImmutableSetMultimap.Builder<Character, String>()
   *         .put('b', "anana")
   *         .putAll('a', "pple", "sparagus")
   *         .putAll('c', "arrot", "herry")
   *         .build();
   * }</pre>
   *
   * @since 21.0
   */
  public static <T extends @Nullable Object, K, V>
      Collector<T, ?, ImmutableSetMultimap<K, V>> toImmutableSetMultimap(
          Function<? super T, ? extends K> keyFunction,
          Function<? super T, ? extends V> valueFunction) {
    return CollectCollectors.toImmutableSetMultimap(keyFunction, valueFunction);
  }

  /**
   * Returns a {@code Collector} accumulating entries into an {@code ImmutableSetMultimap}. Each
   * input element is mapped to a key and a stream of values, each of which are put into the
   * resulting {@code Multimap}, in the encounter order of the stream and the encounter order of the
   * streams of values.
   *
   * <p>Example:
   *
   * <pre>{@code
   * static final ImmutableSetMultimap<Character, Character> FIRST_LETTER_MULTIMAP =
   *     Stream.of("banana", "apple", "carrot", "asparagus", "cherry")
   *         .collect(
   *             flatteningToImmutableSetMultimap(
   *                  str -> str.charAt(0),
   *                  str -> str.substring(1).chars().mapToObj(c -> (char) c));
   *
   * // is equivalent to
   *
   * static final ImmutableSetMultimap<Character, Character> FIRST_LETTER_MULTIMAP =
   *     ImmutableSetMultimap.<Character, Character>builder()
   *         .putAll('b', Arrays.asList('a', 'n', 'a', 'n', 'a'))
   *         .putAll('a', Arrays.asList('p', 'p', 'l', 'e'))
   *         .putAll('c', Arrays.asList('a', 'r', 'r', 'o', 't'))
   *         .putAll('a', Arrays.asList('s', 'p', 'a', 'r', 'a', 'g', 'u', 's'))
   *         .putAll('c', Arrays.asList('h', 'e', 'r', 'r', 'y'))
   *         .build();
   *
   * // after deduplication, the resulting multimap is equivalent to
   *
   * static final ImmutableSetMultimap<Character, Character> FIRST_LETTER_MULTIMAP =
   *     ImmutableSetMultimap.<Character, Character>builder()
   *         .putAll('b', Arrays.asList('a', 'n'))
   *         .putAll('a', Arrays.asList('p', 'l', 'e', 's', 'a', 'r', 'g', 'u'))
   *         .putAll('c', Arrays.asList('a', 'r', 'o', 't', 'h', 'e', 'y'))
   *         .build();
   * }
   * }</pre>
   *
   * @since 21.0
   */
  public static <T extends @Nullable Object, K, V>
      Collector<T, ?, ImmutableSetMultimap<K, V>> flatteningToImmutableSetMultimap(
          Function<? super T, ? extends K> keyFunction,
          Function<? super T, ? extends Stream<? extends V>> valuesFunction) {
    return CollectCollectors.flatteningToImmutableSetMultimap(keyFunction, valuesFunction);
  }

  /**
   * Returns the empty multimap.
   *
   * <p><b>Performance note:</b> the instance returned is a singleton.
   */
  // Casting is safe because the multimap will never hold any elements.
  @SuppressWarnings("unchecked")
  public static <K, V> ImmutableSetMultimap<K, V> of() {
    return (ImmutableSetMultimap<K, V>) EmptyImmutableSetMultimap.INSTANCE;
  }

  /** Returns an immutable multimap containing a single entry. */
  public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1) {
    ImmutableSetMultimap.Builder<K, V> builder = ImmutableSetMultimap.builder();
    builder.put(k1, v1);
    return builder.build();
  }

  /**
   * Returns an immutable multimap containing the given entries, in order. Repeated occurrences of
   * an entry (according to {@link Object#equals}) after the first are ignored.
   */
  public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1, K k2, V v2) {
    ImmutableSetMultimap.Builder<K, V> builder = ImmutableSetMultimap.builder();
    builder.put(k1, v1);
    builder.put(k2, v2);
    return builder.build();
  }

  /**
   * Returns an immutable multimap containing the given entries, in order. Repeated occurrences of
   * an entry (according to {@link Object#equals}) after the first are ignored.
   */
  public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
    ImmutableSetMultimap.Builder<K, V> builder = ImmutableSetMultimap.builder();
    builder.put(k1, v1);
    builder.put(k2, v2);
    builder.put(k3, v3);
    return builder.build();
  }

  /**
   * Returns an immutable multimap containing the given entries, in order. Repeated occurrences of
   * an entry (according to {@link Object#equals}) after the first are ignored.
   */
  public static <K, V> ImmutableSetMultimap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    ImmutableSetMultimap.Builder<K, V> builder = ImmutableSetMultimap.builder();
    builder.put(k1, v1);
    builder.put(k2, v2);
    builder.put(k3, v3);
    builder.put(k4, v4);
    return builder.build();
  }

  /**
   * Returns an immutable multimap containing the given entries, in order. Repeated occurrences of
   * an entry (according to {@link Object#equals}) after the first are ignored.
   */
  public static <K, V> ImmutableSetMultimap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    ImmutableSetMultimap.Builder<K, V> builder = ImmutableSetMultimap.builder();
    builder.put(k1, v1);
    builder.put(k2, v2);
    builder.put(k3, v3);
    builder.put(k4, v4);
    builder.put(k5, v5);
    return builder.build();
  }

  // looking for of() with > 5 entries? Use the builder instead.

  /** Returns a new {@link Builder}. */
  public static <K, V> Builder<K, V> builder() {
    return new Builder<>();
  }

  /**
   * Returns a new builder with a hint for how many distinct keys are expected to be added. The
   * generated builder is equivalent to that returned by {@link #builder}, but may perform better if
   * {@code expectedKeys} is a good estimate.
   *
   * @throws IllegalArgumentException if {@code expectedKeys} is negative
   * @since 33.3.0
   */
  public static <K, V> Builder<K, V> builderWithExpectedKeys(int expectedKeys) {
    checkNonnegative(expectedKeys, "expectedKeys");
    return new Builder<>(expectedKeys);
  }

  /**
   * A builder for creating immutable {@code SetMultimap} instances, especially {@code public static
   * final} multimaps ("constant multimaps"). Example:
   *
   * <pre>{@code
   * static final Multimap<String, Integer> STRING_TO_INTEGER_MULTIMAP =
   *     new ImmutableSetMultimap.Builder<String, Integer>()
   *         .put("one", 1)
   *         .putAll("several", 1, 2, 3)
   *         .putAll("many", 1, 2, 3, 4, 5)
   *         .build();
   * }</pre>
   *
   * <p>Builder instances can be reused; it is safe to call {@link #build} multiple times to build
   * multiple multimaps in series. Each multimap contains the key-value mappings in the previously
   * created multimaps.
   *
   * @since 2.0
   */
  public static final class Builder<K, V> extends ImmutableMultimap.Builder<K, V> {
    /**
     * Creates a new builder. The returned builder is equivalent to the builder generated by {@link
     * ImmutableSetMultimap#builder}.
     */
    public Builder() {
      super();
    }

    Builder(int expectedKeys) {
      super(expectedKeys);
    }

    @Override
    ImmutableCollection.Builder<V> newValueCollectionBuilderWithExpectedSize(int expectedSize) {
      return (valueComparator == null)
          ? ImmutableSet.builderWithExpectedSize(expectedSize)
          : new ImmutableSortedSet.Builder<V>(valueComparator, expectedSize);
    }

    @Override
    int expectedValueCollectionSize(int defaultExpectedValues, Iterable<?> values) {
      // Only trust the size of `values` if it is a Set and therefore probably already deduplicated.
      if (values instanceof Set<?>) {
        Set<?> collection = (Set<?>) values;
        return max(defaultExpectedValues, collection.size());
      } else {
        return defaultExpectedValues;
      }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Note that {@code expectedValuesPerKey} is taken to mean the expected number of
     * <i>distinct</i> values per key.
     *
     * @since 33.3.0
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> expectedValuesPerKey(int expectedValuesPerKey) {
      super.expectedValuesPerKey(expectedValuesPerKey);
      return this;
    }

    /** Adds a key-value mapping to the built multimap if it is not already present. */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> put(K key, V value) {
      super.put(key, value);
      return this;
    }

    /**
     * Adds an entry to the built multimap if it is not already present.
     *
     * @since 11.0
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      super.put(entry);
      return this;
    }

    /**
     * {@inheritDoc}
     *
     * @since 19.0
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      super.putAll(entries);
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> putAll(K key, Iterable<? extends V> values) {
      super.putAll(key, values);
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> putAll(K key, V... values) {
      return putAll(key, asList(values));
    }

    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> putAll(Multimap<? extends K, ? extends V> multimap) {
      for (Entry<? extends K, ? extends Collection<? extends V>> entry :
          multimap.asMap().entrySet()) {
        putAll(entry.getKey(), entry.getValue());
      }
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    Builder<K, V> combine(ImmutableMultimap.Builder<K, V> other) {
      super.combine(other);
      return this;
    }

    /**
     * {@inheritDoc}
     *
     * @since 8.0
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> orderKeysBy(Comparator<? super K> keyComparator) {
      super.orderKeysBy(keyComparator);
      return this;
    }

    /**
     * Specifies the ordering of the generated multimap's values for each key.
     *
     * <p>If this method is called, the sets returned by the {@code get()} method of the generated
     * multimap and its {@link Multimap#asMap()} view are {@link ImmutableSortedSet} instances.
     * However, serialization does not preserve that property, though it does maintain the key and
     * value ordering.
     *
     * @since 8.0
     */
    // TODO: Make serialization behavior consistent.
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> orderValuesBy(Comparator<? super V> valueComparator) {
      super.orderValuesBy(valueComparator);
      return this;
    }

    /** Returns a newly-created immutable set multimap. */
    @Override
    public ImmutableSetMultimap<K, V> build() {
      if (builderMap == null) {
        return ImmutableSetMultimap.of();
      }
      Collection<Map.Entry<K, ImmutableCollection.Builder<V>>> mapEntries = builderMap.entrySet();
      if (keyComparator != null) {
        mapEntries = Ordering.from(keyComparator).<K>onKeys().immutableSortedCopy(mapEntries);
      }
      return fromMapBuilderEntries(mapEntries, valueComparator);
    }
  }

  /**
   * Returns an immutable set multimap containing the same mappings as {@code multimap}. The
   * generated multimap's key and value orderings correspond to the iteration ordering of the {@code
   * multimap.asMap()} view. Repeated occurrences of an entry in the multimap after the first are
   * ignored.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * @throws NullPointerException if any key or value in {@code multimap} is null
   */
  public static <K, V> ImmutableSetMultimap<K, V> copyOf(
      Multimap<? extends K, ? extends V> multimap) {
    return copyOf(multimap, null);
  }

  private static <K, V> ImmutableSetMultimap<K, V> copyOf(
      Multimap<? extends K, ? extends V> multimap,
      @CheckForNull Comparator<? super V> valueComparator) {
    checkNotNull(multimap); // eager for GWT
    if (multimap.isEmpty() && valueComparator == null) {
      return of();
    }

    if (multimap instanceof ImmutableSetMultimap) {
      @SuppressWarnings("unchecked") // safe since multimap is not writable
      ImmutableSetMultimap<K, V> kvMultimap = (ImmutableSetMultimap<K, V>) multimap;
      if (!kvMultimap.isPartialView()) {
        return kvMultimap;
      }
    }

    return fromMapEntries(multimap.asMap().entrySet(), valueComparator);
  }

  /**
   * Returns an immutable multimap containing the specified entries. The returned multimap iterates
   * over keys in the order they were first encountered in the input, and the values for each key
   * are iterated in the order they were encountered. If two values for the same key are {@linkplain
   * Object#equals equal}, the first value encountered is used.
   *
   * @throws NullPointerException if any key, value, or entry is null
   * @since 19.0
   */
  public static <K, V> ImmutableSetMultimap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    return new Builder<K, V>().putAll(entries).build();
  }

  /** Creates an ImmutableSetMultimap from an asMap.entrySet. */
  static <K, V> ImmutableSetMultimap<K, V> fromMapEntries(
      Collection<? extends Map.Entry<? extends K, ? extends Collection<? extends V>>> mapEntries,
      @CheckForNull Comparator<? super V> valueComparator) {
    if (mapEntries.isEmpty()) {
      return of();
    }
    ImmutableMap.Builder<K, ImmutableSet<V>> builder =
        new ImmutableMap.Builder<>(mapEntries.size());
    int size = 0;

    for (Entry<? extends K, ? extends Collection<? extends V>> entry : mapEntries) {
      K key = entry.getKey();
      Collection<? extends V> values = entry.getValue();
      ImmutableSet<V> set = valueSet(valueComparator, values);
      if (!set.isEmpty()) {
        builder.put(key, set);
        size += set.size();
      }
    }

    return new ImmutableSetMultimap<>(builder.buildOrThrow(), size, valueComparator);
  }

  /** Creates an ImmutableSetMultimap from a map to builders. */
  static <K, V> ImmutableSetMultimap<K, V> fromMapBuilderEntries(
      Collection<? extends Map.Entry<K, ImmutableCollection.Builder<V>>> mapEntries,
      @CheckForNull Comparator<? super V> valueComparator) {
    if (mapEntries.isEmpty()) {
      return of();
    }
    ImmutableMap.Builder<K, ImmutableSet<V>> builder =
        new ImmutableMap.Builder<>(mapEntries.size());
    int size = 0;

    for (Entry<K, ImmutableCollection.Builder<V>> entry : mapEntries) {
      K key = entry.getKey();
      ImmutableSet.Builder<? extends V> values = (ImmutableSet.Builder<V>) entry.getValue();
      // If orderValuesBy got called at the very end, we may need to do the ImmutableSet to
      // ImmutableSortedSet copy for each of these.
      ImmutableSet<V> set = valueSet(valueComparator, values.build());
      if (!set.isEmpty()) {
        builder.put(key, set);
        size += set.size();
      }
    }

    return new ImmutableSetMultimap<>(builder.buildOrThrow(), size, valueComparator);
  }

  /**
   * Returned by get() when a missing key is provided. Also holds the comparator, if any, used for
   * values.
   */
  private final transient ImmutableSet<V> emptySet;

  ImmutableSetMultimap(
      ImmutableMap<K, ImmutableSet<V>> map,
      int size,
      @CheckForNull Comparator<? super V> valueComparator) {
    super(map, size);
    this.emptySet = emptySet(valueComparator);
  }

  // views

  /**
   * Returns an immutable set of the values for the given key. If no mappings in the multimap have
   * the provided key, an empty immutable set is returned. The values are in the same order as the
   * parameters used to build this multimap.
   */
  @Override
  public ImmutableSet<V> get(K key) {
    // This cast is safe as its type is known in constructor.
    ImmutableSet<V> set = (ImmutableSet<V>) map.get(key);
    return MoreObjects.firstNonNull(set, emptySet);
  }

  @LazyInit @RetainedWith @CheckForNull private transient ImmutableSetMultimap<V, K> inverse;

  /**
   * {@inheritDoc}
   *
   * <p>Because an inverse of a set multimap cannot contain multiple pairs with the same key and
   * value, this method returns an {@code ImmutableSetMultimap} rather than the {@code
   * ImmutableMultimap} specified in the {@code ImmutableMultimap} class.
   */
  @Override
  public ImmutableSetMultimap<V, K> inverse() {
    ImmutableSetMultimap<V, K> result = inverse;
    return (result == null) ? (inverse = invert()) : result;
  }

  private ImmutableSetMultimap<V, K> invert() {
    Builder<V, K> builder = builder();
    for (Entry<K, V> entry : entries()) {
      builder.put(entry.getValue(), entry.getKey());
    }
    ImmutableSetMultimap<V, K> invertedMultimap = builder.build();
    invertedMultimap.inverse = this;
    return invertedMultimap;
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  @DoNotCall("Always throws UnsupportedOperationException")
  public final ImmutableSet<V> removeAll(@CheckForNull Object key) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the multimap unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  @DoNotCall("Always throws UnsupportedOperationException")
  public final ImmutableSet<V> replaceValues(K key, Iterable<? extends V> values) {
    throw new UnsupportedOperationException();
  }

  @LazyInit @RetainedWith @CheckForNull private transient ImmutableSet<Entry<K, V>> entries;

  /**
   * Returns an immutable collection of all key-value pairs in the multimap. Its iterator traverses
   * the values for the first key, the values for the second key, and so on.
   */
  @Override
  public ImmutableSet<Entry<K, V>> entries() {
    ImmutableSet<Entry<K, V>> result = entries;
    return result == null ? (entries = new EntrySet<>(this)) : result;
  }

  private static final class EntrySet<K, V> extends ImmutableSet<Entry<K, V>> {
    @Weak private final transient ImmutableSetMultimap<K, V> multimap;

    EntrySet(ImmutableSetMultimap<K, V> multimap) {
      this.multimap = multimap;
    }

    @Override
    public boolean contains(@CheckForNull Object object) {
      if (object instanceof Entry) {
        Entry<?, ?> entry = (Entry<?, ?>) object;
        return multimap.containsEntry(entry.getKey(), entry.getValue());
      }
      return false;
    }

    @Override
    public int size() {
      return multimap.size();
    }

    @Override
    public UnmodifiableIterator<Entry<K, V>> iterator() {
      return multimap.entryIterator();
    }

    @Override
    boolean isPartialView() {
      return false;
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
    }
  }

  private static <V> ImmutableSet<V> valueSet(
      @CheckForNull Comparator<? super V> valueComparator, Collection<? extends V> values) {
    return (valueComparator == null)
        ? ImmutableSet.copyOf(values)
        : ImmutableSortedSet.copyOf(valueComparator, values);
  }

  private static <V> ImmutableSet<V> emptySet(@CheckForNull Comparator<? super V> valueComparator) {
    return (valueComparator == null)
        ? ImmutableSet.<V>of()
        : ImmutableSortedSet.<V>emptySet(valueComparator);
  }

  private static <V> ImmutableSet.Builder<V> valuesBuilder(
      @CheckForNull Comparator<? super V> valueComparator) {
    return (valueComparator == null)
        ? new ImmutableSet.Builder<V>()
        : new ImmutableSortedSet.Builder<V>(valueComparator);
  }

  /**
   * @serialData number of distinct keys, and then for each distinct key: the key, the number of
   *     values for that key, and the key's values
   */
  @GwtIncompatible // java.io.ObjectOutputStream
  @J2ktIncompatible
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(valueComparator());
    Serialization.writeMultimap(this, stream);
  }

  @CheckForNull
  Comparator<? super V> valueComparator() {
    return emptySet instanceof ImmutableSortedSet
        ? ((ImmutableSortedSet<V>) emptySet).comparator()
        : null;
  }

  @GwtIncompatible // java serialization
  @J2ktIncompatible
  private static final class SetFieldSettersHolder {
    static final Serialization.FieldSetter<? super ImmutableSetMultimap<?, ?>>
        EMPTY_SET_FIELD_SETTER =
            Serialization.getFieldSetter(ImmutableSetMultimap.class, "emptySet");
  }

  @GwtIncompatible // java.io.ObjectInputStream
  @J2ktIncompatible
  // Serialization type safety is at the caller's mercy.
  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    Comparator<Object> valueComparator = (Comparator<Object>) stream.readObject();
    int keyCount = stream.readInt();
    if (keyCount < 0) {
      throw new InvalidObjectException("Invalid key count " + keyCount);
    }
    ImmutableMap.Builder<Object, ImmutableSet<Object>> builder = ImmutableMap.builder();
    int tmpSize = 0;

    for (int i = 0; i < keyCount; i++) {
      Object key = requireNonNull(stream.readObject());
      int valueCount = stream.readInt();
      if (valueCount <= 0) {
        throw new InvalidObjectException("Invalid value count " + valueCount);
      }

      ImmutableSet.Builder<Object> valuesBuilder = valuesBuilder(valueComparator);
      for (int j = 0; j < valueCount; j++) {
        valuesBuilder.add(requireNonNull(stream.readObject()));
      }
      ImmutableSet<Object> valueSet = valuesBuilder.build();
      if (valueSet.size() != valueCount) {
        throw new InvalidObjectException("Duplicate key-value pairs exist for key " + key);
      }
      builder.put(key, valueSet);
      tmpSize += valueCount;
    }

    ImmutableMap<Object, ImmutableSet<Object>> tmpMap;
    try {
      tmpMap = builder.buildOrThrow();
    } catch (IllegalArgumentException e) {
      throw (InvalidObjectException) new InvalidObjectException(e.getMessage()).initCause(e);
    }

    FieldSettersHolder.MAP_FIELD_SETTER.set(this, tmpMap);
    FieldSettersHolder.SIZE_FIELD_SETTER.set(this, tmpSize);
    SetFieldSettersHolder.EMPTY_SET_FIELD_SETTER.set(this, emptySet(valueComparator));
  }

  @GwtIncompatible // not needed in emulated source.
  @J2ktIncompatible
  private static final long serialVersionUID = 0;
}
