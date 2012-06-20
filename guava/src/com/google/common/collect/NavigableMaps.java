/*
 * Copyright (C) 2010 The Guava Authors
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
import static com.google.common.collect.NavigableSets.synchronizedNavigableSet;
import static com.google.common.collect.NavigableSets.unmodifiableNavigableSet;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.collect.Maps.TransformedEntriesSortedMap;
import com.google.common.collect.Synchronized.SynchronizedObject;
import com.google.common.collect.Synchronized.SynchronizedSortedMap;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to {@link NavigableMap} instances.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible(value = "JDK 5 incompatibility")
final class NavigableMaps {

  private NavigableMaps() {}

  /**
   * Returns a view of a navigable map where each value is transformed by a
   * function. All other properties of the map, such as iteration order, are
   * left intact.  For example, the code: <pre>   {@code
   *
   *   NavigableMap<String, Integer> map = Maps.newTreeMap();
   *   map.put("a", 4);
   *   map.put("b", 9);
   *   Function<Integer, Double> sqrt =
   *       new Function<Integer, Double>() {
   *         public Double apply(Integer in) {
   *           return Math.sqrt((int) in);
   *         }
   *       };
   *   NavigableMap<String, Double> transformed =
   *        Maps.transformNavigableValues(map, sqrt);
   *   System.out.println(transformed);}</pre>
   *
   * ... prints {@code {a=2.0, b=3.0}}.
   *
   * Changes in the underlying map are reflected in this view.
   * Conversely, this view supports removal operations, and these are reflected
   * in the underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys, and even
   * null values provided that the function is capable of accepting null input.
   * The transformed map might contain null values, if the function sometimes
   * gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the
   * underlying map is.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary
   * for the returned map to be a view, but it means that the function will be
   * applied many times for bulk operations like {@link Map#containsValue} and
   * {@code Map.toString()}. For this to perform well, {@code function} should
   * be fast. To avoid lazy evaluation when the returned map doesn't need to be
   * a view, copy the returned map into a new map of your choosing.
   *
   * @deprecated Use {@link Maps#transformValues(NavigableMap, Function}.
   */
  @Deprecated
  public static <K, V1, V2> NavigableMap<K, V2> transformValues(
      NavigableMap<K, V1> fromMap, final Function<? super V1, V2> function) {
    checkNotNull(function);
    EntryTransformer<K, V1, V2> transformer =
        new EntryTransformer<K, V1, V2>() {
          @Override
          public V2 transformEntry(K key, V1 value) {
            return function.apply(value);
          }
        };
    return transformEntries(fromMap, transformer);
  }

  /**
   * Returns a view of a navigable map whose values are derived from the
   * original navigable map's entries. In contrast to {@link
   * #transformValues}, this method's entry-transformation logic may
   * depend on the key as well as the value.
   *
   * <p>All other properties of the transformed map, such as iteration order,
   * are left intact. For example, the code: <pre>   {@code
   *
   *   NavigableMap<String, Boolean> options = Maps.newTreeMap();
   *   options.put("verbose", false);
   *   options.put("sort", true);
   *   EntryTransformer<String, Boolean, String> flagPrefixer =
   *       new EntryTransformer<String, Boolean, String>() {
   *         public String transformEntry(String key, Boolean value) {
   *           return value ? key : ("yes" + key);
   *         }
   *       };
   *   NavigableMap<String, String> transformed =
   *       LabsMaps.transformNavigableEntries(options, flagPrefixer);
   *   System.out.println(transformed);}</pre>
   *
   * ... prints {@code {sort=yessort, verbose=verbose}}.
   *
   * <p>Changes in the underlying map are reflected in this view.
   * Conversely, this view supports removal operations, and these are reflected
   * in the underlying map.
   *
   * <p>It's acceptable for the underlying map to contain null keys and null
   * values provided that the transformer is capable of accepting null inputs.
   * The transformed map might contain null values if the transformer sometimes
   * gives a null result.
   *
   * <p>The returned map is not thread-safe or serializable, even if the
   * underlying map is.
   *
   * <p>The transformer is applied lazily, invoked when needed. This is
   * necessary for the returned map to be a view, but it means that the
   * transformer will be applied many times for bulk operations like {@link
   * Map#containsValue} and {@link Object#toString}. For this to perform well,
   * {@code transformer} should be fast. To avoid lazy evaluation when the
   * returned map doesn't need to be a view, copy the returned map into a new
   * map of your choosing.
   *
   * <p><b>Warning:</b> This method assumes that for any instance {@code k} of
   * {@code EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies
   * that {@code k2} is also of type {@code K}. Using an {@code
   * EntryTransformer} key type for which this may not hold, such as {@code
   * ArrayList}, may risk a {@code ClassCastException} when calling methods on
   * the transformed map.
   *
   * @deprecated Use {@link Maps#transformEntries(NavigableMap,
   * EntryTransformer}.
   */
  @Deprecated
  public static <K, V1, V2> NavigableMap<K, V2> transformEntries(
      final NavigableMap<K, V1> fromMap,
      EntryTransformer<? super K, ? super V1, V2> transformer) {
    return new TransformedEntriesNavigableMap<K, V1, V2>(fromMap, transformer);
  }

  private static class TransformedEntriesNavigableMap<K, V1, V2>
      extends TransformedEntriesSortedMap<K, V1, V2> implements
      NavigableMap<K, V2> {

    TransformedEntriesNavigableMap(NavigableMap<K, V1> fromMap,
        EntryTransformer<? super K, ? super V1, V2> transformer) {
      super(fromMap, transformer);
    }

    @Override public Entry<K, V2> ceilingEntry(K key) {
      return transformEntry(fromMap().ceilingEntry(key));
    }

    @Override public K ceilingKey(K key) {
      return fromMap().ceilingKey(key);
    }

    @Override public NavigableSet<K> descendingKeySet() {
      return fromMap().descendingKeySet();
    }

    @Override public NavigableMap<K, V2> descendingMap() {
      return transformEntries(fromMap().descendingMap(), transformer);
    }

    @Override public Entry<K, V2> firstEntry() {
      return transformEntry(fromMap().firstEntry());
    }
    @Override public Entry<K, V2> floorEntry(K key) {
      return transformEntry(fromMap().floorEntry(key));
    }

    @Override public K floorKey(K key) {
      return fromMap().floorKey(key);
    }

    @Override public NavigableMap<K, V2> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override public NavigableMap<K, V2> headMap(K toKey, boolean inclusive) {
      return transformEntries(
          fromMap().headMap(toKey, inclusive), transformer);
    }

    @Override public Entry<K, V2> higherEntry(K key) {
      return transformEntry(fromMap().higherEntry(key));
    }

    @Override public K higherKey(K key) {
      return fromMap().higherKey(key);
    }

    @Override public Entry<K, V2> lastEntry() {
      return transformEntry(fromMap().lastEntry());
    }

    @Override public Entry<K, V2> lowerEntry(K key) {
      return transformEntry(fromMap().lowerEntry(key));
    }

    @Override public K lowerKey(K key) {
      return fromMap().lowerKey(key);
    }

    @Override public NavigableSet<K> navigableKeySet() {
      return fromMap().navigableKeySet();
    }

    @Override public Entry<K, V2> pollFirstEntry() {
      return transformEntry(fromMap().pollFirstEntry());
    }

    @Override public Entry<K, V2> pollLastEntry() {
      return transformEntry(fromMap().pollLastEntry());
    }

    @Override public NavigableMap<K, V2> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return transformEntries(
          fromMap().subMap(fromKey, fromInclusive, toKey, toInclusive),
          transformer);
    }

    @Override public NavigableMap<K, V2> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override public NavigableMap<K, V2> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }

    @Override public NavigableMap<K, V2> tailMap(K fromKey, boolean inclusive) {
      return transformEntries(
          fromMap().tailMap(fromKey, inclusive), transformer);
    }

    private Entry<K, V2> transformEntry(Entry<K, V1> entry) {
      if (entry == null) {
        return null;
      }
      K key = entry.getKey();
      V2 v2 = transformer.transformEntry(key, entry.getValue());
      return Maps.immutableEntry(key, v2);
    }

    @Override protected NavigableMap<K, V1> fromMap() {
      return (NavigableMap<K, V1>) super.fromMap();
    }

  }

  /**
   * Returns an unmodifiable view of the specified navigable map. This method
   * allows modules to provide users with "read-only" access to internal
   * navigable maps. Query operations on the returned navigable map "read
   * through" to the specified navigable map. Attempts to modify the returned
   * navigable map, whether direct, via its collection views, or via its
   * {@code descendingMap}, {@code subMap}, {@code headMap}, or
   * {@code tailMap} views, result in an
   * {@code UnsupportedOperationException}.
   *
   * <p>The returned navigable map will be serializable if the specified
   * navigable map is serializable.
   *
   * @param navigableMap the navigable map for which an unmodifiable view is to
   *        be returned.
   * @return an unmodifiable view of the specified navigable map.
   * @deprecated Use {@link Maps#unmodifiableNavigableMap}.
   */
  @Deprecated
  public static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(
      NavigableMap<K, V> navigableMap) {
    return new UnmodifiableNavigableMap<K, V>(navigableMap);
  }

  private static <K, V> Entry<K, V> nullableUnmodifiableEntry(
      @Nullable Entry<K, V> entry) {
    if (entry == null) {
      return null;
    }
    return Maps.unmodifiableEntry(entry);
  }

  private static class UnmodifiableNavigableMap<K, V>
      extends ForwardingSortedMap<K, V>
      implements NavigableMap<K, V>, Serializable {

    private final NavigableMap<K, V> delegate;
    private final SortedMap<K, V> unmodifiableSortedMap;
    transient NavigableMap<K, V> descendingMap;
    transient NavigableSet<K> navigableKeySet;
    transient NavigableSet<K> descendingKeySet;

    protected UnmodifiableNavigableMap(NavigableMap<K, V> delegate) {
      this.delegate = checkNotNull(delegate);
      this.unmodifiableSortedMap = Collections.unmodifiableSortedMap(delegate);
    }

    @Override protected SortedMap<K, V> delegate() {
      return unmodifiableSortedMap;
    }

    @Override public Entry<K, V> ceilingEntry(K key) {
      return nullableUnmodifiableEntry(delegate.ceilingEntry(key));
    }

    @Override public K ceilingKey(K key) {
      return delegate.ceilingKey(key);
    }

    @Override public NavigableSet<K> descendingKeySet() {
      if (descendingKeySet == null) {
        NavigableSet<K> dKS =
            unmodifiableNavigableSet(delegate.descendingKeySet());
        descendingKeySet = dKS;
        return dKS;
      }
      return descendingKeySet;
    }

    @Override public NavigableMap<K, V> descendingMap() {
      if (descendingMap == null) {
        NavigableMap<K, V> dM =
            unmodifiableNavigableMap(delegate.descendingMap());
        descendingMap = dM;
        return dM;
      }
      return descendingMap;
    }

    @Override public Entry<K, V> firstEntry() {
      return nullableUnmodifiableEntry(delegate.firstEntry());
    }

    @Override public Entry<K, V> floorEntry(K key) {
      return nullableUnmodifiableEntry(delegate.floorEntry(key));
    }

    @Override public K floorKey(K key) {
      return delegate.floorKey(key);
    }

    @Override public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return unmodifiableNavigableMap(delegate.headMap(toKey, inclusive));
    }

    @Override public Entry<K, V> higherEntry(K key) {
      return nullableUnmodifiableEntry(delegate.higherEntry(key));
    }

    @Override public K higherKey(K key) {
      return delegate.higherKey(key);
    }

    @Override public Entry<K, V> lastEntry() {
      return nullableUnmodifiableEntry(delegate.lastEntry());
    }

    @Override public Entry<K, V> lowerEntry(K key) {
      return nullableUnmodifiableEntry(delegate.lowerEntry(key));
    }

    @Override public K lowerKey(K key) {
      return delegate.lowerKey(key);
    }

    @Override public Set<K> keySet() {
      return navigableKeySet();
    }

    @Override public NavigableSet<K> navigableKeySet() {
      if (navigableKeySet == null) {
        NavigableSet<K> nKS =
            unmodifiableNavigableSet(delegate.navigableKeySet());
        navigableKeySet = nKS;
        return nKS;
      }
      return navigableKeySet;
    }

    @Override public Entry<K, V> pollFirstEntry() {
      throw new UnsupportedOperationException();
    }

    @Override public Entry<K, V> pollLastEntry() {
      throw new UnsupportedOperationException();
    }

    @Override public NavigableMap<K, V> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return unmodifiableNavigableMap(delegate.subMap(
          fromKey, fromInclusive, toKey, toInclusive));
    }

    @Override public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return unmodifiableNavigableMap(delegate.tailMap(fromKey, inclusive));
    }

    private static final long serialVersionUID = 0;
  }

  private static <K, V> Entry<K, V> nullableSynchronizedEntry(
      @Nullable Entry<K, V> entry, @Nullable Object mutex) {
    if (entry == null) {
      return null;
    }
    return new SynchronizedEntry<K, V>(entry, mutex);
  }

  private static class SynchronizedEntry<K, V> extends SynchronizedObject
      implements Entry<K, V> {

    SynchronizedEntry(Entry<K, V> delegate, @Nullable Object mutex) {
      super(delegate, mutex);
    }

    @SuppressWarnings("unchecked") // guaranteed by the constructor
    @Override Entry<K, V> delegate() {
      return (Entry<K, V>) super.delegate();
    }

    @Override public boolean equals(Object obj) {
      synchronized (mutex) {
        return delegate().equals(obj);
      }
    }

    @Override public int hashCode() {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    @Override public K getKey() {
      synchronized (mutex) {
        return delegate().getKey();
      }
    }

    @Override public V getValue() {
      synchronized (mutex) {
        return delegate().getValue();
      }
    }

    @Override public V setValue(V value) {
      synchronized (mutex) {
        return delegate().setValue(value);
      }
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a synchronized (thread-safe) navigable map backed by the specified
   * navigable map.  In order to guarantee serial access, it is critical that
   * <b>all</b> access to the backing navigable map is accomplished
   * through the returned navigable map (or its views).
   *
   * <p>It is imperative that the user manually synchronize on the returned
   * navigable map when iterating over any of its collection views, or the
   * collections views of any of its {@code descendingMap}, {@code subMap},
   * {@code headMap} or {@code tailMap} views. <pre>   {@code
   *
   *   NavigableMap<K, V> map = synchronizedNavigableMap(new TreeMap<K, V>());
   *
   *   // Needn't be in synchronized block
   *   NavigableSet<K> set = map.navigableKeySet();
   *
   *   synchronized (map) { // Synchronizing on map, not set!
   *     Iterator<K> it = set.iterator(); // Must be in synchronized block
   *     while (it.hasNext()){
   *       foo(it.next());
   *     }
   *   }}</pre>
   *
   * or: <pre>   {@code
   *
   *   NavigableMap<K, V> map = synchronizedNavigableMap(new TreeMap<K, V>());
   *   NavigableMap<K, V> map2 = map.subMap(foo, false, bar, true);
   *
   *   // Needn't be in synchronized block
   *   NavigableSet<K> set2 = map2.descendingKeySet();
   *
   *   synchronized (map) { // Synchronizing on map, not map2 or set2!
   *     Iterator<K> it = set2.iterator(); // Must be in synchronized block
   *     while (it.hasNext()){
   *       foo(it.next());
   *     }
   *   }}</pre>
   *
   * Failure to follow this advice may result in non-deterministic behavior.
   *
   * <p>The returned navigable map will be serializable if the specified
   * navigable map is serializable.
   *
   * @param navigableMap the navigable map to be "wrapped" in a synchronized
   *    navigable map.
   * @return a synchronized view of the specified navigable map.
   * @deprecated Use {@link Maps#synchronizedNavigableMap}.
   */
  @Deprecated
  public static <K, V> NavigableMap<K, V> synchronizedNavigableMap(
      NavigableMap<K, V> navigableMap) {
    return synchronizedNavigableMap(navigableMap, null);
  }

  static <K, V> NavigableMap<K, V> synchronizedNavigableMap(
      NavigableMap<K, V> navigableMap, @Nullable Object mutex) {
    return new SynchronizedNavigableMap<K, V>(navigableMap, mutex);
  }

  @VisibleForTesting static class SynchronizedNavigableMap<K, V>
      extends SynchronizedSortedMap<K, V> implements NavigableMap<K, V> {

    SynchronizedNavigableMap(
        NavigableMap<K, V> delegate, @Nullable Object mutex) {
      super(delegate, mutex);
    }

    @Override NavigableMap<K, V> delegate() {
      return (NavigableMap<K, V>) super.delegate();
    }

    @Override public Entry<K, V> ceilingEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().ceilingEntry(key), mutex);
      }
    }

    @Override public K ceilingKey(K key) {
      synchronized (mutex) {
        return delegate().ceilingKey(key);
      }
    }

    transient NavigableSet<K> descendingKeySet;

    @Override public NavigableSet<K> descendingKeySet() {
      synchronized (mutex) {
        if (descendingKeySet == null) {
          return descendingKeySet =
              synchronizedNavigableSet(delegate().descendingKeySet(), mutex);
        }
        return descendingKeySet;
      }
    }

    transient NavigableMap<K, V> descendingMap;

    @Override public NavigableMap<K, V> descendingMap() {
      synchronized (mutex) {
        if (descendingMap == null) {
          return descendingMap =
              synchronizedNavigableMap(delegate().descendingMap(), mutex);
        }
        return descendingMap;
      }
    }

    @Override public Entry<K, V> firstEntry() {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().firstEntry(), mutex);
      }
    }

    @Override public Entry<K, V> floorEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().floorEntry(key), mutex);
      }
    }

    @Override public K floorKey(K key) {
      synchronized (mutex) {
        return delegate().floorKey(key);
      }
    }

    @Override public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      synchronized (mutex) {
        return synchronizedNavigableMap(
            delegate().headMap(toKey, inclusive), mutex);
      }
    }

    @Override public Entry<K, V> higherEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().higherEntry(key), mutex);
      }
    }

    @Override public K higherKey(K key) {
      synchronized (mutex) {
        return delegate().higherKey(key);
      }
    }

    @Override public Entry<K, V> lastEntry() {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().lastEntry(), mutex);
      }
    }

    @Override public Entry<K, V> lowerEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().lowerEntry(key), mutex);
      }
    }

    @Override public K lowerKey(K key) {
      synchronized (mutex) {
        return delegate().lowerKey(key);
      }
    }

    @Override public Set<K> keySet() {
      return navigableKeySet();
    }

    transient NavigableSet<K> navigableKeySet;

    @Override public NavigableSet<K> navigableKeySet() {
      synchronized (mutex) {
        if (navigableKeySet == null) {
          return navigableKeySet =
              synchronizedNavigableSet(delegate().navigableKeySet(), mutex);
        }
        return navigableKeySet;
      }
    }

    @Override public Entry<K, V> pollFirstEntry() {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().pollFirstEntry(), mutex);
      }
    }

    @Override public Entry<K, V> pollLastEntry() {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().pollLastEntry(), mutex);
      }
    }

    @Override public NavigableMap<K, V> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      synchronized (mutex) {
        return synchronizedNavigableMap(
            delegate().subMap(fromKey, fromInclusive, toKey, toInclusive),
            mutex);
      }
    }

    @Override public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      synchronized (mutex) {
        return synchronizedNavigableMap(
            delegate().tailMap(fromKey, inclusive), mutex);
      }
    }

    @Override public SortedMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override public SortedMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }

    private static final long serialVersionUID = 0;
  }
}
