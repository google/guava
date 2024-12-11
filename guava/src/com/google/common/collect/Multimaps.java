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
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.Weak;
import com.google.j2objc.annotations.WeakOuter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides static methods acting on or generating a {@code Multimap}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#multimaps">{@code
 * Multimaps}</a>.
 *
 * @author Jared Levy
 * @author Robert Konigsberg
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public final class Multimaps {
  private Multimaps() {}

  /**
   * Returns a {@code Collector} accumulating entries into a {@code Multimap} generated from the
   * specified supplier. The keys and values of the entries are the result of applying the provided
   * mapping functions to the input elements, accumulated in the encounter order of the stream.
   *
   * <p>Example:
   *
   * <pre>{@code
   * static final ListMultimap<Character, String> FIRST_LETTER_MULTIMAP =
   *     Stream.of("banana", "apple", "carrot", "asparagus", "cherry")
   *         .collect(
   *             toMultimap(
   *                  str -> str.charAt(0),
   *                  str -> str.substring(1),
   *                  MultimapBuilder.treeKeys().arrayListValues()::build));
   *
   * // is equivalent to
   *
   * static final ListMultimap<Character, String> FIRST_LETTER_MULTIMAP;
   *
   * static {
   *     FIRST_LETTER_MULTIMAP = MultimapBuilder.treeKeys().arrayListValues().build();
   *     FIRST_LETTER_MULTIMAP.put('b', "anana");
   *     FIRST_LETTER_MULTIMAP.put('a', "pple");
   *     FIRST_LETTER_MULTIMAP.put('a', "sparagus");
   *     FIRST_LETTER_MULTIMAP.put('c', "arrot");
   *     FIRST_LETTER_MULTIMAP.put('c', "herry");
   * }
   * }</pre>
   *
   * <p>To collect to an {@link ImmutableMultimap}, use either {@link
   * ImmutableSetMultimap#toImmutableSetMultimap} or {@link
   * ImmutableListMultimap#toImmutableListMultimap}.
   *
   * @since 21.0
   */
  public static <
          T extends @Nullable Object,
          K extends @Nullable Object,
          V extends @Nullable Object,
          M extends Multimap<K, V>>
      Collector<T, ?, M> toMultimap(
          java.util.function.Function<? super T, ? extends K> keyFunction,
          java.util.function.Function<? super T, ? extends V> valueFunction,
          java.util.function.Supplier<M> multimapSupplier) {
    return CollectCollectors.<T, K, V, M>toMultimap(keyFunction, valueFunction, multimapSupplier);
  }

  /**
   * Returns a {@code Collector} accumulating entries into a {@code Multimap} generated from the
   * specified supplier. Each input element is mapped to a key and a stream of values, each of which
   * are put into the resulting {@code Multimap}, in the encounter order of the stream and the
   * encounter order of the streams of values.
   *
   * <p>Example:
   *
   * <pre>{@code
   * static final ListMultimap<Character, Character> FIRST_LETTER_MULTIMAP =
   *     Stream.of("banana", "apple", "carrot", "asparagus", "cherry")
   *         .collect(
   *             flatteningToMultimap(
   *                  str -> str.charAt(0),
   *                  str -> str.substring(1).chars().mapToObj(c -> (char) c),
   *                  MultimapBuilder.linkedHashKeys().arrayListValues()::build));
   *
   * // is equivalent to
   *
   * static final ListMultimap<Character, Character> FIRST_LETTER_MULTIMAP;
   *
   * static {
   *     FIRST_LETTER_MULTIMAP = MultimapBuilder.linkedHashKeys().arrayListValues().build();
   *     FIRST_LETTER_MULTIMAP.putAll('b', Arrays.asList('a', 'n', 'a', 'n', 'a'));
   *     FIRST_LETTER_MULTIMAP.putAll('a', Arrays.asList('p', 'p', 'l', 'e'));
   *     FIRST_LETTER_MULTIMAP.putAll('c', Arrays.asList('a', 'r', 'r', 'o', 't'));
   *     FIRST_LETTER_MULTIMAP.putAll('a', Arrays.asList('s', 'p', 'a', 'r', 'a', 'g', 'u', 's'));
   *     FIRST_LETTER_MULTIMAP.putAll('c', Arrays.asList('h', 'e', 'r', 'r', 'y'));
   * }
   * }</pre>
   *
   * @since 21.0
   */
  public static <
          T extends @Nullable Object,
          K extends @Nullable Object,
          V extends @Nullable Object,
          M extends Multimap<K, V>>
      Collector<T, ?, M> flatteningToMultimap(
          java.util.function.Function<? super T, ? extends K> keyFunction,
          java.util.function.Function<? super T, ? extends Stream<? extends V>> valueFunction,
          java.util.function.Supplier<M> multimapSupplier) {
    return CollectCollectors.<T, K, V, M>flatteningToMultimap(
        keyFunction, valueFunction, multimapSupplier);
  }

  /**
   * Creates a new {@code Multimap} backed by {@code map}, whose internal value collections are
   * generated by {@code factory}.
   *
   * <p><b>Warning: do not use</b> this method when the collections returned by {@code factory}
   * implement either {@link List} or {@code Set}! Use the more specific method {@link
   * #newListMultimap}, {@link #newSetMultimap} or {@link #newSortedSetMultimap} instead, to avoid
   * very surprising behavior from {@link Multimap#equals}.
   *
   * <p>The {@code factory}-generated and {@code map} classes determine the multimap iteration
   * order. They also specify the behavior of the {@code equals}, {@code hashCode}, and {@code
   * toString} methods for the multimap and its returned views. However, the multimap's {@code get}
   * method returns instances of a different class than {@code factory.get()} does.
   *
   * <p>The multimap is serializable if {@code map}, {@code factory}, the collections generated by
   * {@code factory}, and the multimap contents are all serializable.
   *
   * <p>The multimap is not threadsafe when any concurrent operations update the multimap, even if
   * {@code map} and the instances generated by {@code factory} are. Concurrent read operations will
   * work correctly. To allow concurrent update operations, wrap the multimap with a call to {@link
   * #synchronizedMultimap}.
   *
   * <p>Call this method only when the simpler methods {@link ArrayListMultimap#create()}, {@link
   * HashMultimap#create()}, {@link LinkedHashMultimap#create()}, {@link
   * LinkedListMultimap#create()}, {@link TreeMultimap#create()}, and {@link
   * TreeMultimap#create(Comparator, Comparator)} won't suffice.
   *
   * <p>Note: the multimap assumes complete ownership over of {@code map} and the collections
   * returned by {@code factory}. Those objects should not be manually updated and they should not
   * use soft, weak, or phantom references.
   *
   * @param map place to store the mapping from each key to its corresponding values
   * @param factory supplier of new, empty collections that will each hold all values for a given
   *     key
   * @throws IllegalArgumentException if {@code map} is not empty
   */
  public static <K extends @Nullable Object, V extends @Nullable Object> Multimap<K, V> newMultimap(
      Map<K, Collection<V>> map, final Supplier<? extends Collection<V>> factory) {
    return new CustomMultimap<>(map, factory);
  }

  private static class CustomMultimap<K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractMapBasedMultimap<K, V> {
    transient Supplier<? extends Collection<V>> factory;

    CustomMultimap(Map<K, Collection<V>> map, Supplier<? extends Collection<V>> factory) {
      super(map);
      this.factory = checkNotNull(factory);
    }

    @Override
    Set<K> createKeySet() {
      return createMaybeNavigableKeySet();
    }

    @Override
    Map<K, Collection<V>> createAsMap() {
      return createMaybeNavigableAsMap();
    }

    @Override
    protected Collection<V> createCollection() {
      return factory.get();
    }

    @Override
    <E extends @Nullable Object> Collection<E> unmodifiableCollectionSubclass(
        Collection<E> collection) {
      if (collection instanceof NavigableSet) {
        return Sets.unmodifiableNavigableSet((NavigableSet<E>) collection);
      } else if (collection instanceof SortedSet) {
        return Collections.unmodifiableSortedSet((SortedSet<E>) collection);
      } else if (collection instanceof Set) {
        return Collections.unmodifiableSet((Set<E>) collection);
      } else if (collection instanceof List) {
        return Collections.unmodifiableList((List<E>) collection);
      } else {
        return Collections.unmodifiableCollection(collection);
      }
    }

    @Override
    Collection<V> wrapCollection(@ParametricNullness K key, Collection<V> collection) {
      if (collection instanceof List) {
        return wrapList(key, (List<V>) collection, null);
      } else if (collection instanceof NavigableSet) {
        return new WrappedNavigableSet(key, (NavigableSet<V>) collection, null);
      } else if (collection instanceof SortedSet) {
        return new WrappedSortedSet(key, (SortedSet<V>) collection, null);
      } else if (collection instanceof Set) {
        return new WrappedSet(key, (Set<V>) collection);
      } else {
        return new WrappedCollection(key, collection, null);
      }
    }

    // can't use Serialization writeMultimap and populateMultimap methods since
    // there's no way to generate the empty backing map.

    /**
     * @serialData the factory and the backing map
     */
    @GwtIncompatible // java.io.ObjectOutputStream
    @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(factory);
      stream.writeObject(backingMap());
    }

    @GwtIncompatible // java.io.ObjectInputStream
    @J2ktIncompatible
    @SuppressWarnings("unchecked") // reading data stored by writeObject
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      factory = (Supplier<? extends Collection<V>>) requireNonNull(stream.readObject());
      Map<K, Collection<V>> map = (Map<K, Collection<V>>) requireNonNull(stream.readObject());
      setMap(map);
    }

    @GwtIncompatible // java serialization not supported
    @J2ktIncompatible
    private static final long serialVersionUID = 0;
  }

  /**
   * Creates a new {@code ListMultimap} that uses the provided map and factory. It can generate a
   * multimap based on arbitrary {@link Map} and {@link List} classes.
   *
   * <p>The {@code factory}-generated and {@code map} classes determine the multimap iteration
   * order. They also specify the behavior of the {@code equals}, {@code hashCode}, and {@code
   * toString} methods for the multimap and its returned views. The multimap's {@code get}, {@code
   * removeAll}, and {@code replaceValues} methods return {@code RandomAccess} lists if the factory
   * does. However, the multimap's {@code get} method returns instances of a different class than
   * does {@code factory.get()}.
   *
   * <p>The multimap is serializable if {@code map}, {@code factory}, the lists generated by {@code
   * factory}, and the multimap contents are all serializable.
   *
   * <p>The multimap is not threadsafe when any concurrent operations update the multimap, even if
   * {@code map} and the instances generated by {@code factory} are. Concurrent read operations will
   * work correctly. To allow concurrent update operations, wrap the multimap with a call to {@link
   * #synchronizedListMultimap}.
   *
   * <p>Call this method only when the simpler methods {@link ArrayListMultimap#create()} and {@link
   * LinkedListMultimap#create()} won't suffice.
   *
   * <p>Note: the multimap assumes complete ownership over of {@code map} and the lists returned by
   * {@code factory}. Those objects should not be manually updated, they should be empty when
   * provided, and they should not use soft, weak, or phantom references.
   *
   * @param map place to store the mapping from each key to its corresponding values
   * @param factory supplier of new, empty lists that will each hold all values for a given key
   * @throws IllegalArgumentException if {@code map} is not empty
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      ListMultimap<K, V> newListMultimap(
          Map<K, Collection<V>> map, final Supplier<? extends List<V>> factory) {
    return new CustomListMultimap<>(map, factory);
  }

  private static class CustomListMultimap<K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractListMultimap<K, V> {
    transient Supplier<? extends List<V>> factory;

    CustomListMultimap(Map<K, Collection<V>> map, Supplier<? extends List<V>> factory) {
      super(map);
      this.factory = checkNotNull(factory);
    }

    @Override
    Set<K> createKeySet() {
      return createMaybeNavigableKeySet();
    }

    @Override
    Map<K, Collection<V>> createAsMap() {
      return createMaybeNavigableAsMap();
    }

    @Override
    protected List<V> createCollection() {
      return factory.get();
    }

    /**
     * @serialData the factory and the backing map
     */
    @GwtIncompatible // java.io.ObjectOutputStream
    @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(factory);
      stream.writeObject(backingMap());
    }

    @GwtIncompatible // java.io.ObjectInputStream
    @J2ktIncompatible
    @SuppressWarnings("unchecked") // reading data stored by writeObject
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      factory = (Supplier<? extends List<V>>) requireNonNull(stream.readObject());
      Map<K, Collection<V>> map = (Map<K, Collection<V>>) requireNonNull(stream.readObject());
      setMap(map);
    }

    @GwtIncompatible // java serialization not supported
    @J2ktIncompatible
    private static final long serialVersionUID = 0;
  }

  /**
   * Creates a new {@code SetMultimap} that uses the provided map and factory. It can generate a
   * multimap based on arbitrary {@link Map} and {@link Set} classes.
   *
   * <p>The {@code factory}-generated and {@code map} classes determine the multimap iteration
   * order. They also specify the behavior of the {@code equals}, {@code hashCode}, and {@code
   * toString} methods for the multimap and its returned views. However, the multimap's {@code get}
   * method returns instances of a different class than {@code factory.get()} does.
   *
   * <p>The multimap is serializable if {@code map}, {@code factory}, the sets generated by {@code
   * factory}, and the multimap contents are all serializable.
   *
   * <p>The multimap is not threadsafe when any concurrent operations update the multimap, even if
   * {@code map} and the instances generated by {@code factory} are. Concurrent read operations will
   * work correctly. To allow concurrent update operations, wrap the multimap with a call to {@link
   * #synchronizedSetMultimap}.
   *
   * <p>Call this method only when the simpler methods {@link HashMultimap#create()}, {@link
   * LinkedHashMultimap#create()}, {@link TreeMultimap#create()}, and {@link
   * TreeMultimap#create(Comparator, Comparator)} won't suffice.
   *
   * <p>Note: the multimap assumes complete ownership over of {@code map} and the sets returned by
   * {@code factory}. Those objects should not be manually updated and they should not use soft,
   * weak, or phantom references.
   *
   * @param map place to store the mapping from each key to its corresponding values
   * @param factory supplier of new, empty sets that will each hold all values for a given key
   * @throws IllegalArgumentException if {@code map} is not empty
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      SetMultimap<K, V> newSetMultimap(
          Map<K, Collection<V>> map, final Supplier<? extends Set<V>> factory) {
    return new CustomSetMultimap<>(map, factory);
  }

  private static class CustomSetMultimap<K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractSetMultimap<K, V> {
    transient Supplier<? extends Set<V>> factory;

    CustomSetMultimap(Map<K, Collection<V>> map, Supplier<? extends Set<V>> factory) {
      super(map);
      this.factory = checkNotNull(factory);
    }

    @Override
    Set<K> createKeySet() {
      return createMaybeNavigableKeySet();
    }

    @Override
    Map<K, Collection<V>> createAsMap() {
      return createMaybeNavigableAsMap();
    }

    @Override
    protected Set<V> createCollection() {
      return factory.get();
    }

    @Override
    <E extends @Nullable Object> Collection<E> unmodifiableCollectionSubclass(
        Collection<E> collection) {
      if (collection instanceof NavigableSet) {
        return Sets.unmodifiableNavigableSet((NavigableSet<E>) collection);
      } else if (collection instanceof SortedSet) {
        return Collections.unmodifiableSortedSet((SortedSet<E>) collection);
      } else {
        return Collections.unmodifiableSet((Set<E>) collection);
      }
    }

    @Override
    Collection<V> wrapCollection(@ParametricNullness K key, Collection<V> collection) {
      if (collection instanceof NavigableSet) {
        return new WrappedNavigableSet(key, (NavigableSet<V>) collection, null);
      } else if (collection instanceof SortedSet) {
        return new WrappedSortedSet(key, (SortedSet<V>) collection, null);
      } else {
        return new WrappedSet(key, (Set<V>) collection);
      }
    }

    /**
     * @serialData the factory and the backing map
     */
    @GwtIncompatible // java.io.ObjectOutputStream
    @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(factory);
      stream.writeObject(backingMap());
    }

    @GwtIncompatible // java.io.ObjectInputStream
    @J2ktIncompatible
    @SuppressWarnings("unchecked") // reading data stored by writeObject
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      factory = (Supplier<? extends Set<V>>) requireNonNull(stream.readObject());
      Map<K, Collection<V>> map = (Map<K, Collection<V>>) requireNonNull(stream.readObject());
      setMap(map);
    }

    @GwtIncompatible // not needed in emulated source
    @J2ktIncompatible
    private static final long serialVersionUID = 0;
  }

  /**
   * Creates a new {@code SortedSetMultimap} that uses the provided map and factory. It can generate
   * a multimap based on arbitrary {@link Map} and {@link SortedSet} classes.
   *
   * <p>The {@code factory}-generated and {@code map} classes determine the multimap iteration
   * order. They also specify the behavior of the {@code equals}, {@code hashCode}, and {@code
   * toString} methods for the multimap and its returned views. However, the multimap's {@code get}
   * method returns instances of a different class than {@code factory.get()} does.
   *
   * <p>The multimap is serializable if {@code map}, {@code factory}, the sets generated by {@code
   * factory}, and the multimap contents are all serializable.
   *
   * <p>The multimap is not threadsafe when any concurrent operations update the multimap, even if
   * {@code map} and the instances generated by {@code factory} are. Concurrent read operations will
   * work correctly. To allow concurrent update operations, wrap the multimap with a call to {@link
   * #synchronizedSortedSetMultimap}.
   *
   * <p>Call this method only when the simpler methods {@link TreeMultimap#create()} and {@link
   * TreeMultimap#create(Comparator, Comparator)} won't suffice.
   *
   * <p>Note: the multimap assumes complete ownership over of {@code map} and the sets returned by
   * {@code factory}. Those objects should not be manually updated and they should not use soft,
   * weak, or phantom references.
   *
   * @param map place to store the mapping from each key to its corresponding values
   * @param factory supplier of new, empty sorted sets that will each hold all values for a given
   *     key
   * @throws IllegalArgumentException if {@code map} is not empty
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      SortedSetMultimap<K, V> newSortedSetMultimap(
          Map<K, Collection<V>> map, final Supplier<? extends SortedSet<V>> factory) {
    return new CustomSortedSetMultimap<>(map, factory);
  }

  private static class CustomSortedSetMultimap<
          K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractSortedSetMultimap<K, V> {
    transient Supplier<? extends SortedSet<V>> factory;
    @CheckForNull transient Comparator<? super V> valueComparator;

    CustomSortedSetMultimap(Map<K, Collection<V>> map, Supplier<? extends SortedSet<V>> factory) {
      super(map);
      this.factory = checkNotNull(factory);
      valueComparator = factory.get().comparator();
    }

    @Override
    Set<K> createKeySet() {
      return createMaybeNavigableKeySet();
    }

    @Override
    Map<K, Collection<V>> createAsMap() {
      return createMaybeNavigableAsMap();
    }

    @Override
    protected SortedSet<V> createCollection() {
      return factory.get();
    }

    @Override
    @CheckForNull
    public Comparator<? super V> valueComparator() {
      return valueComparator;
    }

    /**
     * @serialData the factory and the backing map
     */
    @GwtIncompatible // java.io.ObjectOutputStream
    @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(factory);
      stream.writeObject(backingMap());
    }

    @GwtIncompatible // java.io.ObjectInputStream
    @J2ktIncompatible
    @SuppressWarnings("unchecked") // reading data stored by writeObject
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      factory = (Supplier<? extends SortedSet<V>>) requireNonNull(stream.readObject());
      valueComparator = factory.get().comparator();
      Map<K, Collection<V>> map = (Map<K, Collection<V>>) requireNonNull(stream.readObject());
      setMap(map);
    }

    @GwtIncompatible // not needed in emulated source
    @J2ktIncompatible
    private static final long serialVersionUID = 0;
  }

  /**
   * Copies each key-value mapping in {@code source} into {@code dest}, with its key and value
   * reversed.
   *
   * <p>If {@code source} is an {@link ImmutableMultimap}, consider using {@link
   * ImmutableMultimap#inverse} instead.
   *
   * @param source any multimap
   * @param dest the multimap to copy into; usually empty
   * @return {@code dest}
   */
  @CanIgnoreReturnValue
  public static <K extends @Nullable Object, V extends @Nullable Object, M extends Multimap<K, V>>
      M invertFrom(Multimap<? extends V, ? extends K> source, M dest) {
    checkNotNull(dest);
    for (Map.Entry<? extends V, ? extends K> entry : source.entries()) {
      dest.put(entry.getValue(), entry.getKey());
    }
    return dest;
  }

  /**
   * Returns a synchronized (thread-safe) multimap backed by the specified multimap. In order to
   * guarantee serial access, it is critical that <b>all</b> access to the backing multimap is
   * accomplished through the returned multimap.
   *
   * <p>It is imperative that the user manually synchronize on the returned multimap when accessing
   * any of its collection views:
   *
   * <pre>{@code
   * Multimap<K, V> multimap = Multimaps.synchronizedMultimap(
   *     HashMultimap.<K, V>create());
   * ...
   * Collection<V> values = multimap.get(key);  // Needn't be in synchronized block
   * ...
   * synchronized (multimap) {  // Synchronizing on multimap, not values!
   *   Iterator<V> i = values.iterator(); // Must be in synchronized block
   *   while (i.hasNext()) {
   *     foo(i.next());
   *   }
   * }
   * }</pre>
   *
   * <p>Failure to follow this advice may result in non-deterministic behavior.
   *
   * <p>Note that the generated multimap's {@link Multimap#removeAll} and {@link
   * Multimap#replaceValues} methods return collections that aren't synchronized.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param multimap the multimap to be wrapped in a synchronized view
   * @return a synchronized view of the specified multimap
   */
  @J2ktIncompatible // Synchronized
  public static <K extends @Nullable Object, V extends @Nullable Object>
      Multimap<K, V> synchronizedMultimap(Multimap<K, V> multimap) {
    return Synchronized.multimap(multimap, null);
  }

  /**
   * Returns an unmodifiable view of the specified multimap. Query operations on the returned
   * multimap "read through" to the specified multimap, and attempts to modify the returned
   * multimap, either directly or through the multimap's views, result in an {@code
   * UnsupportedOperationException}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param delegate the multimap for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified multimap
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      Multimap<K, V> unmodifiableMultimap(Multimap<K, V> delegate) {
    if (delegate instanceof UnmodifiableMultimap || delegate instanceof ImmutableMultimap) {
      return delegate;
    }
    return new UnmodifiableMultimap<>(delegate);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated
  public static <K, V> Multimap<K, V> unmodifiableMultimap(ImmutableMultimap<K, V> delegate) {
    return checkNotNull(delegate);
  }

  private static class UnmodifiableMultimap<K extends @Nullable Object, V extends @Nullable Object>
      extends ForwardingMultimap<K, V> implements Serializable {
    final Multimap<K, V> delegate;
    @LazyInit @CheckForNull transient Collection<Entry<K, V>> entries;
    @LazyInit @CheckForNull transient Multiset<K> keys;
    @LazyInit @CheckForNull transient Set<K> keySet;
    @LazyInit @CheckForNull transient Collection<V> values;
    @LazyInit @CheckForNull transient Map<K, Collection<V>> map;

    UnmodifiableMultimap(final Multimap<K, V> delegate) {
      this.delegate = checkNotNull(delegate);
    }

    @Override
    protected Multimap<K, V> delegate() {
      return delegate;
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<K, Collection<V>> asMap() {
      Map<K, Collection<V>> result = map;
      if (result == null) {
        result =
            map =
                Collections.unmodifiableMap(
                    Maps.transformValues(
                        delegate.asMap(), collection -> unmodifiableValueCollection(collection)));
      }
      return result;
    }

    @Override
    public Collection<Entry<K, V>> entries() {
      Collection<Entry<K, V>> result = entries;
      if (result == null) {
        entries = result = unmodifiableEntries(delegate.entries());
      }
      return result;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> consumer) {
      delegate.forEach(checkNotNull(consumer));
    }

    @Override
    public Collection<V> get(@ParametricNullness K key) {
      return unmodifiableValueCollection(delegate.get(key));
    }

    @Override
    public Multiset<K> keys() {
      Multiset<K> result = keys;
      if (result == null) {
        keys = result = Multisets.unmodifiableMultiset(delegate.keys());
      }
      return result;
    }

    @Override
    public Set<K> keySet() {
      Set<K> result = keySet;
      if (result == null) {
        keySet = result = Collections.unmodifiableSet(delegate.keySet());
      }
      return result;
    }

    @Override
    public boolean put(@ParametricNullness K key, @ParametricNullness V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(@CheckForNull Object key, @CheckForNull Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> removeAll(@CheckForNull Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
      Collection<V> result = values;
      if (result == null) {
        values = result = Collections.unmodifiableCollection(delegate.values());
      }
      return result;
    }

    private static final long serialVersionUID = 0;
  }

  private static class UnmodifiableListMultimap<
          K extends @Nullable Object, V extends @Nullable Object>
      extends UnmodifiableMultimap<K, V> implements ListMultimap<K, V> {
    UnmodifiableListMultimap(ListMultimap<K, V> delegate) {
      super(delegate);
    }

    @Override
    public ListMultimap<K, V> delegate() {
      return (ListMultimap<K, V>) super.delegate();
    }

    @Override
    public List<V> get(@ParametricNullness K key) {
      return Collections.unmodifiableList(delegate().get(key));
    }

    @Override
    public List<V> removeAll(@CheckForNull Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    private static final long serialVersionUID = 0;
  }

  private static class UnmodifiableSetMultimap<
          K extends @Nullable Object, V extends @Nullable Object>
      extends UnmodifiableMultimap<K, V> implements SetMultimap<K, V> {
    UnmodifiableSetMultimap(SetMultimap<K, V> delegate) {
      super(delegate);
    }

    @Override
    public SetMultimap<K, V> delegate() {
      return (SetMultimap<K, V>) super.delegate();
    }

    @Override
    public Set<V> get(@ParametricNullness K key) {
      /*
       * Note that this doesn't return a SortedSet when delegate is a
       * SortedSetMultiset, unlike (SortedSet<V>) super.get().
       */
      return Collections.unmodifiableSet(delegate().get(key));
    }

    @Override
    public Set<Map.Entry<K, V>> entries() {
      return Maps.unmodifiableEntrySet(delegate().entries());
    }

    @Override
    public Set<V> removeAll(@CheckForNull Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    private static final long serialVersionUID = 0;
  }

  private static class UnmodifiableSortedSetMultimap<
          K extends @Nullable Object, V extends @Nullable Object>
      extends UnmodifiableSetMultimap<K, V> implements SortedSetMultimap<K, V> {
    UnmodifiableSortedSetMultimap(SortedSetMultimap<K, V> delegate) {
      super(delegate);
    }

    @Override
    public SortedSetMultimap<K, V> delegate() {
      return (SortedSetMultimap<K, V>) super.delegate();
    }

    @Override
    public SortedSet<V> get(@ParametricNullness K key) {
      return Collections.unmodifiableSortedSet(delegate().get(key));
    }

    @Override
    public SortedSet<V> removeAll(@CheckForNull Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SortedSet<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    @CheckForNull
    public Comparator<? super V> valueComparator() {
      return delegate().valueComparator();
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a synchronized (thread-safe) {@code SetMultimap} backed by the specified multimap.
   *
   * <p>You must follow the warnings described in {@link #synchronizedMultimap}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param multimap the multimap to be wrapped
   * @return a synchronized view of the specified multimap
   */
  @J2ktIncompatible // Synchronized
  public static <K extends @Nullable Object, V extends @Nullable Object>
      SetMultimap<K, V> synchronizedSetMultimap(SetMultimap<K, V> multimap) {
    return Synchronized.setMultimap(multimap, null);
  }

  /**
   * Returns an unmodifiable view of the specified {@code SetMultimap}. Query operations on the
   * returned multimap "read through" to the specified multimap, and attempts to modify the returned
   * multimap, either directly or through the multimap's views, result in an {@code
   * UnsupportedOperationException}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param delegate the multimap for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified multimap
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      SetMultimap<K, V> unmodifiableSetMultimap(SetMultimap<K, V> delegate) {
    if (delegate instanceof UnmodifiableSetMultimap || delegate instanceof ImmutableSetMultimap) {
      return delegate;
    }
    return new UnmodifiableSetMultimap<>(delegate);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated
  public static <K, V> SetMultimap<K, V> unmodifiableSetMultimap(
      ImmutableSetMultimap<K, V> delegate) {
    return checkNotNull(delegate);
  }

  /**
   * Returns a synchronized (thread-safe) {@code SortedSetMultimap} backed by the specified
   * multimap.
   *
   * <p>You must follow the warnings described in {@link #synchronizedMultimap}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param multimap the multimap to be wrapped
   * @return a synchronized view of the specified multimap
   */
  @J2ktIncompatible // Synchronized
  public static <K extends @Nullable Object, V extends @Nullable Object>
      SortedSetMultimap<K, V> synchronizedSortedSetMultimap(SortedSetMultimap<K, V> multimap) {
    return Synchronized.sortedSetMultimap(multimap, null);
  }

  /**
   * Returns an unmodifiable view of the specified {@code SortedSetMultimap}. Query operations on
   * the returned multimap "read through" to the specified multimap, and attempts to modify the
   * returned multimap, either directly or through the multimap's views, result in an {@code
   * UnsupportedOperationException}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param delegate the multimap for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified multimap
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      SortedSetMultimap<K, V> unmodifiableSortedSetMultimap(SortedSetMultimap<K, V> delegate) {
    if (delegate instanceof UnmodifiableSortedSetMultimap) {
      return delegate;
    }
    return new UnmodifiableSortedSetMultimap<>(delegate);
  }

  /**
   * Returns a synchronized (thread-safe) {@code ListMultimap} backed by the specified multimap.
   *
   * <p>You must follow the warnings described in {@link #synchronizedMultimap}.
   *
   * @param multimap the multimap to be wrapped
   * @return a synchronized view of the specified multimap
   */
  @J2ktIncompatible // Synchronized
  public static <K extends @Nullable Object, V extends @Nullable Object>
      ListMultimap<K, V> synchronizedListMultimap(ListMultimap<K, V> multimap) {
    return Synchronized.listMultimap(multimap, null);
  }

  /**
   * Returns an unmodifiable view of the specified {@code ListMultimap}. Query operations on the
   * returned multimap "read through" to the specified multimap, and attempts to modify the returned
   * multimap, either directly or through the multimap's views, result in an {@code
   * UnsupportedOperationException}.
   *
   * <p>The returned multimap will be serializable if the specified multimap is serializable.
   *
   * @param delegate the multimap for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified multimap
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      ListMultimap<K, V> unmodifiableListMultimap(ListMultimap<K, V> delegate) {
    if (delegate instanceof UnmodifiableListMultimap || delegate instanceof ImmutableListMultimap) {
      return delegate;
    }
    return new UnmodifiableListMultimap<>(delegate);
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated
  public static <K, V> ListMultimap<K, V> unmodifiableListMultimap(
      ImmutableListMultimap<K, V> delegate) {
    return checkNotNull(delegate);
  }

  /**
   * Returns an unmodifiable view of the specified collection, preserving the interface for
   * instances of {@code SortedSet}, {@code Set}, {@code List} and {@code Collection}, in that order
   * of preference.
   *
   * @param collection the collection for which to return an unmodifiable view
   * @return an unmodifiable view of the collection
   */
  private static <V extends @Nullable Object> Collection<V> unmodifiableValueCollection(
      Collection<V> collection) {
    if (collection instanceof SortedSet) {
      return Collections.unmodifiableSortedSet((SortedSet<V>) collection);
    } else if (collection instanceof Set) {
      return Collections.unmodifiableSet((Set<V>) collection);
    } else if (collection instanceof List) {
      return Collections.unmodifiableList((List<V>) collection);
    }
    return Collections.unmodifiableCollection(collection);
  }

  /**
   * Returns an unmodifiable view of the specified collection of entries. The {@link Entry#setValue}
   * operation throws an {@link UnsupportedOperationException}. If the specified collection is a
   * {@code Set}, the returned collection is also a {@code Set}.
   *
   * @param entries the entries for which to return an unmodifiable view
   * @return an unmodifiable view of the entries
   */
  private static <K extends @Nullable Object, V extends @Nullable Object>
      Collection<Entry<K, V>> unmodifiableEntries(Collection<Entry<K, V>> entries) {
    if (entries instanceof Set) {
      return Maps.unmodifiableEntrySet((Set<Entry<K, V>>) entries);
    }
    return new Maps.UnmodifiableEntries<>(Collections.unmodifiableCollection(entries));
  }

  /**
   * Returns {@link ListMultimap#asMap multimap.asMap()}, with its type corrected from {@code Map<K,
   * Collection<V>>} to {@code Map<K, List<V>>}.
   *
   * @since 15.0
   */
  @SuppressWarnings("unchecked")
  // safe by specification of ListMultimap.asMap()
  public static <K extends @Nullable Object, V extends @Nullable Object> Map<K, List<V>> asMap(
      ListMultimap<K, V> multimap) {
    return (Map<K, List<V>>) (Map<K, ?>) multimap.asMap();
  }

  /**
   * Returns {@link SetMultimap#asMap multimap.asMap()}, with its type corrected from {@code Map<K,
   * Collection<V>>} to {@code Map<K, Set<V>>}.
   *
   * @since 15.0
   */
  @SuppressWarnings("unchecked")
  // safe by specification of SetMultimap.asMap()
  public static <K extends @Nullable Object, V extends @Nullable Object> Map<K, Set<V>> asMap(
      SetMultimap<K, V> multimap) {
    return (Map<K, Set<V>>) (Map<K, ?>) multimap.asMap();
  }

  /**
   * Returns {@link SortedSetMultimap#asMap multimap.asMap()}, with its type corrected from {@code
   * Map<K, Collection<V>>} to {@code Map<K, SortedSet<V>>}.
   *
   * @since 15.0
   */
  @SuppressWarnings("unchecked")
  // safe by specification of SortedSetMultimap.asMap()
  public static <K extends @Nullable Object, V extends @Nullable Object> Map<K, SortedSet<V>> asMap(
      SortedSetMultimap<K, V> multimap) {
    return (Map<K, SortedSet<V>>) (Map<K, ?>) multimap.asMap();
  }

  /**
   * Returns {@link Multimap#asMap multimap.asMap()}. This is provided for parity with the other
   * more strongly-typed {@code asMap()} implementations.
   *
   * @since 15.0
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      Map<K, Collection<V>> asMap(Multimap<K, V> multimap) {
    return multimap.asMap();
  }

  /**
   * Returns a multimap view of the specified map. The multimap is backed by the map, so changes to
   * the map are reflected in the multimap, and vice versa. If the map is modified while an
   * iteration over one of the multimap's collection views is in progress (except through the
   * iterator's own {@code remove} operation, or through the {@code setValue} operation on a map
   * entry returned by the iterator), the results of the iteration are undefined.
   *
   * <p>The multimap supports mapping removal, which removes the corresponding mapping from the map.
   * It does not support any operations which might add mappings, such as {@code put}, {@code
   * putAll} or {@code replaceValues}.
   *
   * <p>The returned multimap will be serializable if the specified map is serializable.
   *
   * @param map the backing map for the returned multimap view
   */
  public static <K extends @Nullable Object, V extends @Nullable Object> SetMultimap<K, V> forMap(
      Map<K, V> map) {
    return new MapMultimap<>(map);
  }

  /** @see Multimaps#forMap */
  private static class MapMultimap<K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractMultimap<K, V> implements SetMultimap<K, V>, Serializable {
    final Map<K, V> map;

    MapMultimap(Map<K, V> map) {
      this.map = checkNotNull(map);
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    public boolean containsKey(@CheckForNull Object key) {
      return map.containsKey(key);
    }

    @Override
    public boolean containsValue(@CheckForNull Object value) {
      return map.containsValue(value);
    }

    @Override
    public boolean containsEntry(@CheckForNull Object key, @CheckForNull Object value) {
      return map.entrySet().contains(Maps.immutableEntry(key, value));
    }

    @Override
    public Set<V> get(@ParametricNullness final K key) {
      return new Sets.ImprovedAbstractSet<V>() {
        @Override
        public Iterator<V> iterator() {
          return new Iterator<V>() {
            int i;

            @Override
            public boolean hasNext() {
              return (i == 0) && map.containsKey(key);
            }

            @Override
            @ParametricNullness
            public V next() {
              if (!hasNext()) {
                throw new NoSuchElementException();
              }
              i++;
              /*
               * The cast is safe because of the containsKey check in hasNext(). (That means it's
               * unsafe under concurrent modification, but all bets are off then, anyway.)
               */
              return uncheckedCastNullableTToT(map.get(key));
            }

            @Override
            public void remove() {
              checkRemove(i == 1);
              i = -1;
              map.remove(key);
            }
          };
        }

        @Override
        public int size() {
          return map.containsKey(key) ? 1 : 0;
        }
      };
    }

    @Override
    public boolean put(@ParametricNullness K key, @ParametricNullness V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(@CheckForNull Object key, @CheckForNull Object value) {
      return map.entrySet().remove(Maps.immutableEntry(key, value));
    }

    @Override
    public Set<V> removeAll(@CheckForNull Object key) {
      Set<V> values = new HashSet<>(2);
      if (!map.containsKey(key)) {
        return values;
      }
      values.add(map.remove(key));
      return values;
    }

    @Override
    public void clear() {
      map.clear();
    }

    @Override
    Set<K> createKeySet() {
      return map.keySet();
    }

    @Override
    Collection<V> createValues() {
      return map.values();
    }

    @Override
    public Set<Entry<K, V>> entries() {
      return map.entrySet();
    }

    @Override
    Collection<Entry<K, V>> createEntries() {
      throw new AssertionError("unreachable");
    }

    @Override
    Multiset<K> createKeys() {
      return new Multimaps.Keys<K, V>(this);
    }

    @Override
    Iterator<Entry<K, V>> entryIterator() {
      return map.entrySet().iterator();
    }

    @Override
    Map<K, Collection<V>> createAsMap() {
      return new AsMap<>(this);
    }

    @Override
    public int hashCode() {
      return map.hashCode();
    }

    private static final long serialVersionUID = 7845222491160860175L;
  }

  /**
   * Returns a view of a multimap where each value is transformed by a function. All other
   * properties of the multimap, such as iteration order, are left intact. For example, the code:
   *
   * <pre>{@code
   * Multimap<String, Integer> multimap =
   *     ImmutableSetMultimap.of("a", 2, "b", -3, "b", -3, "a", 4, "c", 6);
   * Function<Integer, String> square = new Function<Integer, String>() {
   *     public String apply(Integer in) {
   *       return Integer.toString(in * in);
   *     }
   * };
   * Multimap<String, String> transformed =
   *     Multimaps.transformValues(multimap, square);
   *   System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {a=[4, 16], b=[9, 9], c=[36]}}.
   *
   * <p>Changes in the underlying multimap are reflected in this view. Conversely, this view
   * supports removal operations, and these are reflected in the underlying multimap.
   *
   * <p>It's acceptable for the underlying multimap to contain null keys, and even null values
   * provided that the function is capable of accepting null input. The transformed multimap might
   * contain null values, if the function sometimes gives a null result.
   *
   * <p>The returned multimap is not thread-safe or serializable, even if the underlying multimap
   * is. The {@code equals} and {@code hashCode} methods of the returned multimap are meaningless,
   * since there is not a definition of {@code equals} or {@code hashCode} for general collections,
   * and {@code get()} will return a general {@code Collection} as opposed to a {@code List} or a
   * {@code Set}.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary for the returned
   * multimap to be a view, but it means that the function will be applied many times for bulk
   * operations like {@link Multimap#containsValue} and {@code Multimap.toString()}. For this to
   * perform well, {@code function} should be fast. To avoid lazy evaluation when the returned
   * multimap doesn't need to be a view, copy the returned multimap into a new multimap of your
   * choosing.
   *
   * @since 7.0
   */
  public static <
          K extends @Nullable Object, V1 extends @Nullable Object, V2 extends @Nullable Object>
      Multimap<K, V2> transformValues(
          Multimap<K, V1> fromMultimap, final Function<? super V1, V2> function) {
    checkNotNull(function);
    EntryTransformer<K, V1, V2> transformer = Maps.asEntryTransformer(function);
    return transformEntries(fromMultimap, transformer);
  }

  /**
   * Returns a view of a {@code ListMultimap} where each value is transformed by a function. All
   * other properties of the multimap, such as iteration order, are left intact. For example, the
   * code:
   *
   * <pre>{@code
   * ListMultimap<String, Integer> multimap
   *      = ImmutableListMultimap.of("a", 4, "a", 16, "b", 9);
   * Function<Integer, Double> sqrt =
   *     new Function<Integer, Double>() {
   *       public Double apply(Integer in) {
   *         return Math.sqrt((int) in);
   *       }
   *     };
   * ListMultimap<String, Double> transformed = Multimaps.transformValues(map,
   *     sqrt);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {a=[2.0, 4.0], b=[3.0]}}.
   *
   * <p>Changes in the underlying multimap are reflected in this view. Conversely, this view
   * supports removal operations, and these are reflected in the underlying multimap.
   *
   * <p>It's acceptable for the underlying multimap to contain null keys, and even null values
   * provided that the function is capable of accepting null input. The transformed multimap might
   * contain null values, if the function sometimes gives a null result.
   *
   * <p>The returned multimap is not thread-safe or serializable, even if the underlying multimap
   * is.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary for the returned
   * multimap to be a view, but it means that the function will be applied many times for bulk
   * operations like {@link Multimap#containsValue} and {@code Multimap.toString()}. For this to
   * perform well, {@code function} should be fast. To avoid lazy evaluation when the returned
   * multimap doesn't need to be a view, copy the returned multimap into a new multimap of your
   * choosing.
   *
   * @since 7.0
   */
  public static <
          K extends @Nullable Object, V1 extends @Nullable Object, V2 extends @Nullable Object>
      ListMultimap<K, V2> transformValues(
          ListMultimap<K, V1> fromMultimap, final Function<? super V1, V2> function) {
    checkNotNull(function);
    EntryTransformer<K, V1, V2> transformer = Maps.asEntryTransformer(function);
    return transformEntries(fromMultimap, transformer);
  }

  /**
   * Returns a view of a multimap whose values are derived from the original multimap's entries. In
   * contrast to {@link #transformValues}, this method's entry-transformation logic may depend on
   * the key as well as the value.
   *
   * <p>All other properties of the transformed multimap, such as iteration order, are left intact.
   * For example, the code:
   *
   * <pre>{@code
   * SetMultimap<String, Integer> multimap =
   *     ImmutableSetMultimap.of("a", 1, "a", 4, "b", -6);
   * EntryTransformer<String, Integer, String> transformer =
   *     new EntryTransformer<String, Integer, String>() {
   *       public String transformEntry(String key, Integer value) {
   *          return (value >= 0) ? key : "no" + key;
   *       }
   *     };
   * Multimap<String, String> transformed =
   *     Multimaps.transformEntries(multimap, transformer);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {a=[a, a], b=[nob]}}.
   *
   * <p>Changes in the underlying multimap are reflected in this view. Conversely, this view
   * supports removal operations, and these are reflected in the underlying multimap.
   *
   * <p>It's acceptable for the underlying multimap to contain null keys and null values provided
   * that the transformer is capable of accepting null inputs. The transformed multimap might
   * contain null values if the transformer sometimes gives a null result.
   *
   * <p>The returned multimap is not thread-safe or serializable, even if the underlying multimap
   * is. The {@code equals} and {@code hashCode} methods of the returned multimap are meaningless,
   * since there is not a definition of {@code equals} or {@code hashCode} for general collections,
   * and {@code get()} will return a general {@code Collection} as opposed to a {@code List} or a
   * {@code Set}.
   *
   * <p>The transformer is applied lazily, invoked when needed. This is necessary for the returned
   * multimap to be a view, but it means that the transformer will be applied many times for bulk
   * operations like {@link Multimap#containsValue} and {@link Object#toString}. For this to perform
   * well, {@code transformer} should be fast. To avoid lazy evaluation when the returned multimap
   * doesn't need to be a view, copy the returned multimap into a new multimap of your choosing.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of {@code
   * EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also of
   * type {@code K}. Using an {@code EntryTransformer} key type for which this may not hold, such as
   * {@code ArrayList}, may risk a {@code ClassCastException} when calling methods on the
   * transformed multimap.
   *
   * @since 7.0
   */
  public static <
          K extends @Nullable Object, V1 extends @Nullable Object, V2 extends @Nullable Object>
      Multimap<K, V2> transformEntries(
          Multimap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
    return new TransformedEntriesMultimap<>(fromMap, transformer);
  }

  /**
   * Returns a view of a {@code ListMultimap} whose values are derived from the original multimap's
   * entries. In contrast to {@link #transformValues(ListMultimap, Function)}, this method's
   * entry-transformation logic may depend on the key as well as the value.
   *
   * <p>All other properties of the transformed multimap, such as iteration order, are left intact.
   * For example, the code:
   *
   * <pre>{@code
   * Multimap<String, Integer> multimap =
   *     ImmutableMultimap.of("a", 1, "a", 4, "b", 6);
   * EntryTransformer<String, Integer, String> transformer =
   *     new EntryTransformer<String, Integer, String>() {
   *       public String transformEntry(String key, Integer value) {
   *         return key + value;
   *       }
   *     };
   * Multimap<String, String> transformed =
   *     Multimaps.transformEntries(multimap, transformer);
   * System.out.println(transformed);
   * }</pre>
   *
   * ... prints {@code {"a"=["a1", "a4"], "b"=["b6"]}}.
   *
   * <p>Changes in the underlying multimap are reflected in this view. Conversely, this view
   * supports removal operations, and these are reflected in the underlying multimap.
   *
   * <p>It's acceptable for the underlying multimap to contain null keys and null values provided
   * that the transformer is capable of accepting null inputs. The transformed multimap might
   * contain null values if the transformer sometimes gives a null result.
   *
   * <p>The returned multimap is not thread-safe or serializable, even if the underlying multimap
   * is.
   *
   * <p>The transformer is applied lazily, invoked when needed. This is necessary for the returned
   * multimap to be a view, but it means that the transformer will be applied many times for bulk
   * operations like {@link Multimap#containsValue} and {@link Object#toString}. For this to perform
   * well, {@code transformer} should be fast. To avoid lazy evaluation when the returned multimap
   * doesn't need to be a view, copy the returned multimap into a new multimap of your choosing.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of {@code
   * EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also of
   * type {@code K}. Using an {@code EntryTransformer} key type for which this may not hold, such as
   * {@code ArrayList}, may risk a {@code ClassCastException} when calling methods on the
   * transformed multimap.
   *
   * @since 7.0
   */
  public static <
          K extends @Nullable Object, V1 extends @Nullable Object, V2 extends @Nullable Object>
      ListMultimap<K, V2> transformEntries(
          ListMultimap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
    return new TransformedEntriesListMultimap<>(fromMap, transformer);
  }

  private static class TransformedEntriesMultimap<
          K extends @Nullable Object, V1 extends @Nullable Object, V2 extends @Nullable Object>
      extends AbstractMultimap<K, V2> {
    final Multimap<K, V1> fromMultimap;
    final EntryTransformer<? super K, ? super V1, V2> transformer;

    TransformedEntriesMultimap(
        Multimap<K, V1> fromMultimap,
        final EntryTransformer<? super K, ? super V1, V2> transformer) {
      this.fromMultimap = checkNotNull(fromMultimap);
      this.transformer = checkNotNull(transformer);
    }

    Collection<V2> transform(@ParametricNullness K key, Collection<V1> values) {
      Function<? super V1, V2> function = Maps.asValueToValueFunction(transformer, key);
      if (values instanceof List) {
        return Lists.transform((List<V1>) values, function);
      } else {
        return Collections2.transform(values, function);
      }
    }

    @Override
    Map<K, Collection<V2>> createAsMap() {
      return Maps.transformEntries(fromMultimap.asMap(), (key, value) -> transform(key, value));
    }

    @Override
    public void clear() {
      fromMultimap.clear();
    }

    @Override
    public boolean containsKey(@CheckForNull Object key) {
      return fromMultimap.containsKey(key);
    }

    @Override
    Collection<Entry<K, V2>> createEntries() {
      return new Entries();
    }

    @Override
    Iterator<Entry<K, V2>> entryIterator() {
      return Iterators.transform(
          fromMultimap.entries().iterator(), Maps.<K, V1, V2>asEntryToEntryFunction(transformer));
    }

    @Override
    public Collection<V2> get(@ParametricNullness final K key) {
      return transform(key, fromMultimap.get(key));
    }

    @Override
    public boolean isEmpty() {
      return fromMultimap.isEmpty();
    }

    @Override
    Set<K> createKeySet() {
      return fromMultimap.keySet();
    }

    @Override
    Multiset<K> createKeys() {
      return fromMultimap.keys();
    }

    @Override
    public boolean put(@ParametricNullness K key, @ParametricNullness V2 value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(@ParametricNullness K key, Iterable<? extends V2> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V2> multimap) {
      throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(@CheckForNull Object key, @CheckForNull Object value) {
      return get((K) key).remove(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<V2> removeAll(@CheckForNull Object key) {
      return transform((K) key, fromMultimap.removeAll(key));
    }

    @Override
    public Collection<V2> replaceValues(@ParametricNullness K key, Iterable<? extends V2> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
      return fromMultimap.size();
    }

    @Override
    Collection<V2> createValues() {
      return Collections2.transform(
          fromMultimap.entries(), Maps.<K, V1, V2>asEntryToValueFunction(transformer));
    }
  }

  private static final class TransformedEntriesListMultimap<
          K extends @Nullable Object, V1 extends @Nullable Object, V2 extends @Nullable Object>
      extends TransformedEntriesMultimap<K, V1, V2> implements ListMultimap<K, V2> {

    TransformedEntriesListMultimap(
        ListMultimap<K, V1> fromMultimap, EntryTransformer<? super K, ? super V1, V2> transformer) {
      super(fromMultimap, transformer);
    }

    @Override
    List<V2> transform(@ParametricNullness K key, Collection<V1> values) {
      return Lists.transform((List<V1>) values, Maps.asValueToValueFunction(transformer, key));
    }

    @Override
    public List<V2> get(@ParametricNullness K key) {
      return transform(key, fromMultimap.get(key));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<V2> removeAll(@CheckForNull Object key) {
      return transform((K) key, fromMultimap.removeAll(key));
    }

    @Override
    public List<V2> replaceValues(@ParametricNullness K key, Iterable<? extends V2> values) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Creates an index {@code ImmutableListMultimap} that contains the results of applying a
   * specified function to each item in an {@code Iterable} of values. Each value will be stored as
   * a value in the resulting multimap, yielding a multimap with the same size as the input
   * iterable. The key used to store that value in the multimap will be the result of calling the
   * function on that value. The resulting multimap is created as an immutable snapshot. In the
   * returned multimap, keys appear in the order they are first encountered, and the values
   * corresponding to each key appear in the same order as they are encountered.
   *
   * <p>For example,
   *
   * <pre>{@code
   * List<String> badGuys =
   *     Arrays.asList("Inky", "Blinky", "Pinky", "Pinky", "Clyde");
   * Function<String, Integer> stringLengthFunction = ...;
   * Multimap<Integer, String> index =
   *     Multimaps.index(badGuys, stringLengthFunction);
   * System.out.println(index);
   * }</pre>
   *
   * <p>prints
   *
   * <pre>{@code
   * {4=[Inky], 6=[Blinky], 5=[Pinky, Pinky, Clyde]}
   * }</pre>
   *
   * <p>The returned multimap is serializable if its keys and values are all serializable.
   *
   * @param values the values to use when constructing the {@code ImmutableListMultimap}
   * @param keyFunction the function used to produce the key for each value
   * @return {@code ImmutableListMultimap} mapping the result of evaluating the function {@code
   *     keyFunction} on each value in the input collection to that value
   * @throws NullPointerException if any element of {@code values} is {@code null}, or if {@code
   *     keyFunction} produces {@code null} for any key
   */
  public static <K, V> ImmutableListMultimap<K, V> index(
      Iterable<V> values, Function<? super V, K> keyFunction) {
    return index(values.iterator(), keyFunction);
  }

  /**
   * Creates an index {@code ImmutableListMultimap} that contains the results of applying a
   * specified function to each item in an {@code Iterator} of values. Each value will be stored as
   * a value in the resulting multimap, yielding a multimap with the same size as the input
   * iterator. The key used to store that value in the multimap will be the result of calling the
   * function on that value. The resulting multimap is created as an immutable snapshot. In the
   * returned multimap, keys appear in the order they are first encountered, and the values
   * corresponding to each key appear in the same order as they are encountered.
   *
   * <p>For example,
   *
   * <pre>{@code
   * List<String> badGuys =
   *     Arrays.asList("Inky", "Blinky", "Pinky", "Pinky", "Clyde");
   * Function<String, Integer> stringLengthFunction = ...;
   * Multimap<Integer, String> index =
   *     Multimaps.index(badGuys.iterator(), stringLengthFunction);
   * System.out.println(index);
   * }</pre>
   *
   * <p>prints
   *
   * <pre>{@code
   * {4=[Inky], 6=[Blinky], 5=[Pinky, Pinky, Clyde]}
   * }</pre>
   *
   * <p>The returned multimap is serializable if its keys and values are all serializable.
   *
   * @param values the values to use when constructing the {@code ImmutableListMultimap}
   * @param keyFunction the function used to produce the key for each value
   * @return {@code ImmutableListMultimap} mapping the result of evaluating the function {@code
   *     keyFunction} on each value in the input collection to that value
   * @throws NullPointerException if any element of {@code values} is {@code null}, or if {@code
   *     keyFunction} produces {@code null} for any key
   * @since 10.0
   */
  public static <K, V> ImmutableListMultimap<K, V> index(
      Iterator<V> values, Function<? super V, K> keyFunction) {
    checkNotNull(keyFunction);
    ImmutableListMultimap.Builder<K, V> builder = ImmutableListMultimap.builder();
    while (values.hasNext()) {
      V value = values.next();
      checkNotNull(value, values);
      builder.put(keyFunction.apply(value), value);
    }
    return builder.build();
  }

  static class Keys<K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractMultiset<K> {
    @Weak final Multimap<K, V> multimap;

    Keys(Multimap<K, V> multimap) {
      this.multimap = multimap;
    }

    @Override
    Iterator<Multiset.Entry<K>> entryIterator() {
      return new TransformedIterator<Map.Entry<K, Collection<V>>, Multiset.Entry<K>>(
          multimap.asMap().entrySet().iterator()) {
        @Override
        Multiset.Entry<K> transform(final Map.Entry<K, Collection<V>> backingEntry) {
          return new Multisets.AbstractEntry<K>() {
            @Override
            @ParametricNullness
            public K getElement() {
              return backingEntry.getKey();
            }

            @Override
            public int getCount() {
              return backingEntry.getValue().size();
            }
          };
        }
      };
    }

    @Override
    public Spliterator<K> spliterator() {
      return CollectSpliterators.map(multimap.entries().spliterator(), Map.Entry::getKey);
    }

    @Override
    public void forEach(Consumer<? super K> consumer) {
      checkNotNull(consumer);
      multimap.entries().forEach(entry -> consumer.accept(entry.getKey()));
    }

    @Override
    int distinctElements() {
      return multimap.asMap().size();
    }

    @Override
    public int size() {
      return multimap.size();
    }

    @Override
    public boolean contains(@CheckForNull Object element) {
      return multimap.containsKey(element);
    }

    @Override
    public Iterator<K> iterator() {
      return Maps.keyIterator(multimap.entries().iterator());
    }

    @Override
    public int count(@CheckForNull Object element) {
      Collection<V> values = Maps.safeGet(multimap.asMap(), element);
      return (values == null) ? 0 : values.size();
    }

    @Override
    public int remove(@CheckForNull Object element, int occurrences) {
      checkNonnegative(occurrences, "occurrences");
      if (occurrences == 0) {
        return count(element);
      }

      Collection<V> values = Maps.safeGet(multimap.asMap(), element);

      if (values == null) {
        return 0;
      }

      int oldCount = values.size();
      if (occurrences >= oldCount) {
        values.clear();
      } else {
        Iterator<V> iterator = values.iterator();
        for (int i = 0; i < occurrences; i++) {
          iterator.next();
          iterator.remove();
        }
      }
      return oldCount;
    }

    @Override
    public void clear() {
      multimap.clear();
    }

    @Override
    public Set<K> elementSet() {
      return multimap.keySet();
    }

    @Override
    Iterator<K> elementIterator() {
      throw new AssertionError("should never be called");
    }
  }

  /** A skeleton implementation of {@link Multimap#entries()}. */
  abstract static class Entries<K extends @Nullable Object, V extends @Nullable Object>
      extends AbstractCollection<Map.Entry<K, V>> {
    abstract Multimap<K, V> multimap();

    @Override
    public int size() {
      return multimap().size();
    }

    @Override
    public boolean contains(@CheckForNull Object o) {
      if (o instanceof Map.Entry) {
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        return multimap().containsEntry(entry.getKey(), entry.getValue());
      }
      return false;
    }

    @Override
    public boolean remove(@CheckForNull Object o) {
      if (o instanceof Map.Entry) {
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        return multimap().remove(entry.getKey(), entry.getValue());
      }
      return false;
    }

    @Override
    public void clear() {
      multimap().clear();
    }
  }

  /** A skeleton implementation of {@link Multimap#asMap()}. */
  static final class AsMap<K extends @Nullable Object, V extends @Nullable Object>
      extends Maps.ViewCachingAbstractMap<K, Collection<V>> {
    @Weak private final Multimap<K, V> multimap;

    AsMap(Multimap<K, V> multimap) {
      this.multimap = checkNotNull(multimap);
    }

    @Override
    public int size() {
      return multimap.keySet().size();
    }

    @Override
    protected Set<Entry<K, Collection<V>>> createEntrySet() {
      return new EntrySet();
    }

    void removeValuesForKey(@CheckForNull Object key) {
      multimap.keySet().remove(key);
    }

    @WeakOuter
    class EntrySet extends Maps.EntrySet<K, Collection<V>> {
      @Override
      Map<K, Collection<V>> map() {
        return AsMap.this;
      }

      @Override
      public Iterator<Entry<K, Collection<V>>> iterator() {
        return Maps.asMapEntryIterator(multimap.keySet(), key -> multimap.get(key));
      }

      @Override
      public boolean remove(@CheckForNull Object o) {
        if (!contains(o)) {
          return false;
        }
        // requireNonNull is safe because of the contains check.
        Map.Entry<?, ?> entry = requireNonNull((Map.Entry<?, ?>) o);
        removeValuesForKey(entry.getKey());
        return true;
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    @CheckForNull
    public Collection<V> get(@CheckForNull Object key) {
      return containsKey(key) ? multimap.get((K) key) : null;
    }

    @Override
    @CheckForNull
    public Collection<V> remove(@CheckForNull Object key) {
      return containsKey(key) ? multimap.removeAll(key) : null;
    }

    @Override
    public Set<K> keySet() {
      return multimap.keySet();
    }

    @Override
    public boolean isEmpty() {
      return multimap.isEmpty();
    }

    @Override
    public boolean containsKey(@CheckForNull Object key) {
      return multimap.containsKey(key);
    }

    @Override
    public void clear() {
      multimap.clear();
    }
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} whose keys satisfy a
   * predicate. The returned multimap is a live view of {@code unfiltered}; changes to one affect
   * the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a key that doesn't
   * satisfy the predicate, the multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose keys satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code keyPredicate} must be <i>consistent with equals</i>, as documented at
   * {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 11.0
   */
  public static <K extends @Nullable Object, V extends @Nullable Object> Multimap<K, V> filterKeys(
      Multimap<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    if (unfiltered instanceof SetMultimap) {
      return filterKeys((SetMultimap<K, V>) unfiltered, keyPredicate);
    } else if (unfiltered instanceof ListMultimap) {
      return filterKeys((ListMultimap<K, V>) unfiltered, keyPredicate);
    } else if (unfiltered instanceof FilteredKeyMultimap) {
      FilteredKeyMultimap<K, V> prev = (FilteredKeyMultimap<K, V>) unfiltered;
      return new FilteredKeyMultimap<>(
          prev.unfiltered, Predicates.<K>and(prev.keyPredicate, keyPredicate));
    } else if (unfiltered instanceof FilteredMultimap) {
      FilteredMultimap<K, V> prev = (FilteredMultimap<K, V>) unfiltered;
      return filterFiltered(prev, Maps.<K>keyPredicateOnEntries(keyPredicate));
    } else {
      return new FilteredKeyMultimap<>(unfiltered, keyPredicate);
    }
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} whose keys satisfy a
   * predicate. The returned multimap is a live view of {@code unfiltered}; changes to one affect
   * the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a key that doesn't
   * satisfy the predicate, the multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose keys satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code keyPredicate} must be <i>consistent with equals</i>, as documented at
   * {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 14.0
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      SetMultimap<K, V> filterKeys(
          SetMultimap<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    if (unfiltered instanceof FilteredKeySetMultimap) {
      FilteredKeySetMultimap<K, V> prev = (FilteredKeySetMultimap<K, V>) unfiltered;
      return new FilteredKeySetMultimap<>(
          prev.unfiltered(), Predicates.<K>and(prev.keyPredicate, keyPredicate));
    } else if (unfiltered instanceof FilteredSetMultimap) {
      FilteredSetMultimap<K, V> prev = (FilteredSetMultimap<K, V>) unfiltered;
      return filterFiltered(prev, Maps.<K>keyPredicateOnEntries(keyPredicate));
    } else {
      return new FilteredKeySetMultimap<>(unfiltered, keyPredicate);
    }
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} whose keys satisfy a
   * predicate. The returned multimap is a live view of {@code unfiltered}; changes to one affect
   * the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a key that doesn't
   * satisfy the predicate, the multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose keys satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code keyPredicate} must be <i>consistent with equals</i>, as documented at
   * {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 14.0
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      ListMultimap<K, V> filterKeys(
          ListMultimap<K, V> unfiltered, final Predicate<? super K> keyPredicate) {
    if (unfiltered instanceof FilteredKeyListMultimap) {
      FilteredKeyListMultimap<K, V> prev = (FilteredKeyListMultimap<K, V>) unfiltered;
      return new FilteredKeyListMultimap<>(
          prev.unfiltered(), Predicates.<K>and(prev.keyPredicate, keyPredicate));
    } else {
      return new FilteredKeyListMultimap<>(unfiltered, keyPredicate);
    }
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} whose values satisfy a
   * predicate. The returned multimap is a live view of {@code unfiltered}; changes to one affect
   * the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a value that doesn't
   * satisfy the predicate, the multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose value satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code valuePredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 11.0
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      Multimap<K, V> filterValues(
          Multimap<K, V> unfiltered, final Predicate<? super V> valuePredicate) {
    return filterEntries(unfiltered, Maps.<V>valuePredicateOnEntries(valuePredicate));
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} whose values satisfy a
   * predicate. The returned multimap is a live view of {@code unfiltered}; changes to one affect
   * the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a value that doesn't
   * satisfy the predicate, the multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose value satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code valuePredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}. Do not provide a predicate such as {@code
   * Predicates.instanceOf(ArrayList.class)}, which is inconsistent with equals.
   *
   * @since 14.0
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      SetMultimap<K, V> filterValues(
          SetMultimap<K, V> unfiltered, final Predicate<? super V> valuePredicate) {
    return filterEntries(unfiltered, Maps.<V>valuePredicateOnEntries(valuePredicate));
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} that satisfy a predicate. The
   * returned multimap is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a key/value pair that
   * doesn't satisfy the predicate, multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose keys satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}.
   *
   * @since 11.0
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      Multimap<K, V> filterEntries(
          Multimap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
    checkNotNull(entryPredicate);
    if (unfiltered instanceof SetMultimap) {
      return filterEntries((SetMultimap<K, V>) unfiltered, entryPredicate);
    }
    return (unfiltered instanceof FilteredMultimap)
        ? filterFiltered((FilteredMultimap<K, V>) unfiltered, entryPredicate)
        : new FilteredEntryMultimap<K, V>(checkNotNull(unfiltered), entryPredicate);
  }

  /**
   * Returns a multimap containing the mappings in {@code unfiltered} that satisfy a predicate. The
   * returned multimap is a live view of {@code unfiltered}; changes to one affect the other.
   *
   * <p>The resulting multimap's views have iterators that don't support {@code remove()}, but all
   * other methods are supported by the multimap and its views. When adding a key/value pair that
   * doesn't satisfy the predicate, multimap's {@code put()}, {@code putAll()}, and {@code
   * replaceValues()} methods throw an {@link IllegalArgumentException}.
   *
   * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered
   * multimap or its views, only mappings whose keys satisfy the filter will be removed from the
   * underlying multimap.
   *
   * <p>The returned multimap isn't threadsafe or serializable, even if {@code unfiltered} is.
   *
   * <p>Many of the filtered multimap's methods, such as {@code size()}, iterate across every
   * key/value mapping in the underlying multimap and determine which satisfy the filter. When a
   * live view is <i>not</i> needed, it may be faster to copy the filtered multimap and use the
   * copy.
   *
   * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with equals</i>, as documented
   * at {@link Predicate#apply}.
   *
   * @since 14.0
   */
  public static <K extends @Nullable Object, V extends @Nullable Object>
      SetMultimap<K, V> filterEntries(
          SetMultimap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
    checkNotNull(entryPredicate);
    return (unfiltered instanceof FilteredSetMultimap)
        ? filterFiltered((FilteredSetMultimap<K, V>) unfiltered, entryPredicate)
        : new FilteredEntrySetMultimap<K, V>(checkNotNull(unfiltered), entryPredicate);
  }

  /**
   * Support removal operations when filtering a filtered multimap. Since a filtered multimap has
   * iterators that don't support remove, passing one to the FilteredEntryMultimap constructor would
   * lead to a multimap whose removal operations would fail. This method combines the predicates to
   * avoid that problem.
   */
  private static <K extends @Nullable Object, V extends @Nullable Object>
      Multimap<K, V> filterFiltered(
          FilteredMultimap<K, V> multimap, Predicate<? super Entry<K, V>> entryPredicate) {
    Predicate<Entry<K, V>> predicate =
        Predicates.<Entry<K, V>>and(multimap.entryPredicate(), entryPredicate);
    return new FilteredEntryMultimap<>(multimap.unfiltered(), predicate);
  }

  /**
   * Support removal operations when filtering a filtered multimap. Since a filtered multimap has
   * iterators that don't support remove, passing one to the FilteredEntryMultimap constructor would
   * lead to a multimap whose removal operations would fail. This method combines the predicates to
   * avoid that problem.
   */
  private static <K extends @Nullable Object, V extends @Nullable Object>
      SetMultimap<K, V> filterFiltered(
          FilteredSetMultimap<K, V> multimap, Predicate<? super Entry<K, V>> entryPredicate) {
    Predicate<Entry<K, V>> predicate =
        Predicates.<Entry<K, V>>and(multimap.entryPredicate(), entryPredicate);
    return new FilteredEntrySetMultimap<>(multimap.unfiltered(), predicate);
  }

  static boolean equalsImpl(Multimap<?, ?> multimap, @CheckForNull Object object) {
    if (object == multimap) {
      return true;
    }
    if (object instanceof Multimap) {
      Multimap<?, ?> that = (Multimap<?, ?>) object;
      return multimap.asMap().equals(that.asMap());
    }
    return false;
  }

  // TODO(jlevy): Create methods that filter a SortedSetMultimap.
}
