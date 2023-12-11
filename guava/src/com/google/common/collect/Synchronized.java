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
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.j2objc.annotations.RetainedWith;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Synchronized collection views. The returned synchronized collection views are serializable if the
 * backing collection and the mutex are serializable.
 *
 * <p>If {@code null} is passed as the {@code mutex} parameter to any of this class's top-level
 * methods or inner class constructors, the created object uses itself as the synchronization mutex.
 *
 * <p>This class should be used by other collection classes only.
 *
 * @author Mike Bostock
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
/*
 * I have decided not to bother adding @ParametricNullness annotations in this class. Adding them is
 * a lot of busy work, and the annotation matters only when the APIs to be annotated are visible to
 * Kotlin code. In this class, nothing is publicly visible (nor exposed indirectly through a
 * publicly visible subclass), and I doubt any of our current or future Kotlin extensions for the
 * package will refer to the class. Plus, @ParametricNullness is only a temporary workaround,
 * anyway, so we just need to get by without the annotations here until Kotlin better understands
 * our other nullness annotations.
 */
final class Synchronized {
  private Synchronized() {}

  static class SynchronizedObject implements Serializable {
    final Object delegate;
    final Object mutex;

    SynchronizedObject(Object delegate, @CheckForNull Object mutex) {
      this.delegate = checkNotNull(delegate);
      this.mutex = (mutex == null) ? this : mutex;
    }

    Object delegate() {
      return delegate;
    }

    // No equals and hashCode; see ForwardingObject for details.

    @Override
    public String toString() {
      synchronized (mutex) {
        return delegate.toString();
      }
    }

    // Serialization invokes writeObject only when it's private.
    // The SynchronizedObject subclasses don't need a writeObject method since
    // they don't contain any non-transient member variables, while the
    // following writeObject() handles the SynchronizedObject members.

    @GwtIncompatible // java.io.ObjectOutputStream
    @J2ktIncompatible
    private void writeObject(ObjectOutputStream stream) throws IOException {
      synchronized (mutex) {
        stream.defaultWriteObject();
      }
    }

    @GwtIncompatible // not needed in emulated source
    @J2ktIncompatible
    private static final long serialVersionUID = 0;
  }

  private static <E extends @Nullable Object> Collection<E> collection(
      Collection<E> collection, @CheckForNull Object mutex) {
    return new SynchronizedCollection<E>(collection, mutex);
  }

  @VisibleForTesting
  static class SynchronizedCollection<E extends @Nullable Object> extends SynchronizedObject
      implements Collection<E> {
    private SynchronizedCollection(Collection<E> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @SuppressWarnings("unchecked")
    @Override
    Collection<E> delegate() {
      return (Collection<E>) super.delegate();
    }

    @Override
    public boolean add(E e) {
      synchronized (mutex) {
        return delegate().add(e);
      }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
      synchronized (mutex) {
        return delegate().addAll(c);
      }
    }

    @Override
    public void clear() {
      synchronized (mutex) {
        delegate().clear();
      }
    }

    @Override
    public boolean contains(@CheckForNull Object o) {
      synchronized (mutex) {
        return delegate().contains(o);
      }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      synchronized (mutex) {
        return delegate().containsAll(c);
      }
    }

    @Override
    public boolean isEmpty() {
      synchronized (mutex) {
        return delegate().isEmpty();
      }
    }

    @Override
    public Iterator<E> iterator() {
      return delegate().iterator(); // manually synchronized
    }

    @Override
    public Spliterator<E> spliterator() {
      synchronized (mutex) {
        return delegate().spliterator();
      }
    }

    @Override
    public Stream<E> stream() {
      synchronized (mutex) {
        return delegate().stream();
      }
    }

    @Override
    public Stream<E> parallelStream() {
      synchronized (mutex) {
        return delegate().parallelStream();
      }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
      synchronized (mutex) {
        delegate().forEach(action);
      }
    }

    @Override
    public boolean remove(@CheckForNull Object o) {
      synchronized (mutex) {
        return delegate().remove(o);
      }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      synchronized (mutex) {
        return delegate().removeAll(c);
      }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      synchronized (mutex) {
        return delegate().retainAll(c);
      }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
      synchronized (mutex) {
        return delegate().removeIf(filter);
      }
    }

    @Override
    public int size() {
      synchronized (mutex) {
        return delegate().size();
      }
    }

    @Override
    public @Nullable Object[] toArray() {
      synchronized (mutex) {
        return delegate().toArray();
      }
    }

    @Override
    @SuppressWarnings("nullness") // b/192354773 in our checker affects toArray declarations
    public <T extends @Nullable Object> T[] toArray(T[] a) {
      synchronized (mutex) {
        return delegate().toArray(a);
      }
    }

    private static final long serialVersionUID = 0;
  }

  @VisibleForTesting
  static <E extends @Nullable Object> Set<E> set(Set<E> set, @CheckForNull Object mutex) {
    return new SynchronizedSet<E>(set, mutex);
  }

  static class SynchronizedSet<E extends @Nullable Object> extends SynchronizedCollection<E>
      implements Set<E> {

    SynchronizedSet(Set<E> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    Set<E> delegate() {
      return (Set<E>) super.delegate();
    }

    @Override
    public boolean equals(@CheckForNull Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(o);
      }
    }

    @Override
    public int hashCode() {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    private static final long serialVersionUID = 0;
  }

  private static <E extends @Nullable Object> SortedSet<E> sortedSet(
      SortedSet<E> set, @CheckForNull Object mutex) {
    return new SynchronizedSortedSet<E>(set, mutex);
  }

  static class SynchronizedSortedSet<E extends @Nullable Object> extends SynchronizedSet<E>
      implements SortedSet<E> {
    SynchronizedSortedSet(SortedSet<E> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    SortedSet<E> delegate() {
      return (SortedSet<E>) super.delegate();
    }

    @Override
    @CheckForNull
    public Comparator<? super E> comparator() {
      synchronized (mutex) {
        return delegate().comparator();
      }
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
      synchronized (mutex) {
        return sortedSet(delegate().subSet(fromElement, toElement), mutex);
      }
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
      synchronized (mutex) {
        return sortedSet(delegate().headSet(toElement), mutex);
      }
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
      synchronized (mutex) {
        return sortedSet(delegate().tailSet(fromElement), mutex);
      }
    }

    @Override
    public E first() {
      synchronized (mutex) {
        return delegate().first();
      }
    }

    @Override
    public E last() {
      synchronized (mutex) {
        return delegate().last();
      }
    }

    private static final long serialVersionUID = 0;
  }

  private static <E extends @Nullable Object> List<E> list(
      List<E> list, @CheckForNull Object mutex) {
    return (list instanceof RandomAccess)
        ? new SynchronizedRandomAccessList<E>(list, mutex)
        : new SynchronizedList<E>(list, mutex);
  }

  static class SynchronizedList<E extends @Nullable Object> extends SynchronizedCollection<E>
      implements List<E> {
    SynchronizedList(List<E> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    List<E> delegate() {
      return (List<E>) super.delegate();
    }

    @Override
    public void add(int index, E element) {
      synchronized (mutex) {
        delegate().add(index, element);
      }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
      synchronized (mutex) {
        return delegate().addAll(index, c);
      }
    }

    @Override
    public E get(int index) {
      synchronized (mutex) {
        return delegate().get(index);
      }
    }

    @Override
    public int indexOf(@CheckForNull Object o) {
      synchronized (mutex) {
        return delegate().indexOf(o);
      }
    }

    @Override
    public int lastIndexOf(@CheckForNull Object o) {
      synchronized (mutex) {
        return delegate().lastIndexOf(o);
      }
    }

    @Override
    public ListIterator<E> listIterator() {
      return delegate().listIterator(); // manually synchronized
    }

    @Override
    public ListIterator<E> listIterator(int index) {
      return delegate().listIterator(index); // manually synchronized
    }

    @Override
    public E remove(int index) {
      synchronized (mutex) {
        return delegate().remove(index);
      }
    }

    @Override
    public E set(int index, E element) {
      synchronized (mutex) {
        return delegate().set(index, element);
      }
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
      synchronized (mutex) {
        delegate().replaceAll(operator);
      }
    }

    @Override
    public void sort(@Nullable Comparator<? super E> c) {
      synchronized (mutex) {
        delegate().sort(c);
      }
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
      synchronized (mutex) {
        return list(delegate().subList(fromIndex, toIndex), mutex);
      }
    }

    @Override
    public boolean equals(@CheckForNull Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(o);
      }
    }

    @Override
    public int hashCode() {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    private static final long serialVersionUID = 0;
  }

  static final class SynchronizedRandomAccessList<E extends @Nullable Object>
      extends SynchronizedList<E> implements RandomAccess {
    SynchronizedRandomAccessList(List<E> list, @CheckForNull Object mutex) {
      super(list, mutex);
    }

    private static final long serialVersionUID = 0;
  }

  static <E extends @Nullable Object> Multiset<E> multiset(
      Multiset<E> multiset, @CheckForNull Object mutex) {
    if (multiset instanceof SynchronizedMultiset || multiset instanceof ImmutableMultiset) {
      return multiset;
    }
    return new SynchronizedMultiset<E>(multiset, mutex);
  }

  static final class SynchronizedMultiset<E extends @Nullable Object>
      extends SynchronizedCollection<E> implements Multiset<E> {
    @CheckForNull transient Set<E> elementSet;
    @CheckForNull transient Set<Multiset.Entry<E>> entrySet;

    SynchronizedMultiset(Multiset<E> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    Multiset<E> delegate() {
      return (Multiset<E>) super.delegate();
    }

    @Override
    public int count(@CheckForNull Object o) {
      synchronized (mutex) {
        return delegate().count(o);
      }
    }

    @Override
    public int add(@ParametricNullness E e, int n) {
      synchronized (mutex) {
        return delegate().add(e, n);
      }
    }

    @Override
    public int remove(@CheckForNull Object o, int n) {
      synchronized (mutex) {
        return delegate().remove(o, n);
      }
    }

    @Override
    public int setCount(@ParametricNullness E element, int count) {
      synchronized (mutex) {
        return delegate().setCount(element, count);
      }
    }

    @Override
    public boolean setCount(@ParametricNullness E element, int oldCount, int newCount) {
      synchronized (mutex) {
        return delegate().setCount(element, oldCount, newCount);
      }
    }

    @Override
    public Set<E> elementSet() {
      synchronized (mutex) {
        if (elementSet == null) {
          elementSet = typePreservingSet(delegate().elementSet(), mutex);
        }
        return elementSet;
      }
    }

    @Override
    public Set<Multiset.Entry<E>> entrySet() {
      synchronized (mutex) {
        if (entrySet == null) {
          entrySet = typePreservingSet(delegate().entrySet(), mutex);
        }
        return entrySet;
      }
    }

    @Override
    public boolean equals(@CheckForNull Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(o);
      }
    }

    @Override
    public int hashCode() {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    private static final long serialVersionUID = 0;
  }

  static <K extends @Nullable Object, V extends @Nullable Object> Multimap<K, V> multimap(
      Multimap<K, V> multimap, @CheckForNull Object mutex) {
    if (multimap instanceof SynchronizedMultimap || multimap instanceof BaseImmutableMultimap) {
      return multimap;
    }
    return new SynchronizedMultimap<>(multimap, mutex);
  }

  static class SynchronizedMultimap<K extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedObject implements Multimap<K, V> {
    @CheckForNull transient Set<K> keySet;
    @CheckForNull transient Collection<V> valuesCollection;
    @CheckForNull transient Collection<Map.Entry<K, V>> entries;
    @CheckForNull transient Map<K, Collection<V>> asMap;
    @CheckForNull transient Multiset<K> keys;

    @SuppressWarnings("unchecked")
    @Override
    Multimap<K, V> delegate() {
      return (Multimap<K, V>) super.delegate();
    }

    SynchronizedMultimap(Multimap<K, V> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    public int size() {
      synchronized (mutex) {
        return delegate().size();
      }
    }

    @Override
    public boolean isEmpty() {
      synchronized (mutex) {
        return delegate().isEmpty();
      }
    }

    @Override
    public boolean containsKey(@CheckForNull Object key) {
      synchronized (mutex) {
        return delegate().containsKey(key);
      }
    }

    @Override
    public boolean containsValue(@CheckForNull Object value) {
      synchronized (mutex) {
        return delegate().containsValue(value);
      }
    }

    @Override
    public boolean containsEntry(@CheckForNull Object key, @CheckForNull Object value) {
      synchronized (mutex) {
        return delegate().containsEntry(key, value);
      }
    }

    @Override
    public Collection<V> get(@ParametricNullness K key) {
      synchronized (mutex) {
        return typePreservingCollection(delegate().get(key), mutex);
      }
    }

    @Override
    public boolean put(@ParametricNullness K key, @ParametricNullness V value) {
      synchronized (mutex) {
        return delegate().put(key, value);
      }
    }

    @Override
    public boolean putAll(@ParametricNullness K key, Iterable<? extends V> values) {
      synchronized (mutex) {
        return delegate().putAll(key, values);
      }
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
      synchronized (mutex) {
        return delegate().putAll(multimap);
      }
    }

    @Override
    public Collection<V> replaceValues(@ParametricNullness K key, Iterable<? extends V> values) {
      synchronized (mutex) {
        return delegate().replaceValues(key, values); // copy not synchronized
      }
    }

    @Override
    public boolean remove(@CheckForNull Object key, @CheckForNull Object value) {
      synchronized (mutex) {
        return delegate().remove(key, value);
      }
    }

    @Override
    public Collection<V> removeAll(@CheckForNull Object key) {
      synchronized (mutex) {
        return delegate().removeAll(key); // copy not synchronized
      }
    }

    @Override
    public void clear() {
      synchronized (mutex) {
        delegate().clear();
      }
    }

    @Override
    public Set<K> keySet() {
      synchronized (mutex) {
        if (keySet == null) {
          keySet = typePreservingSet(delegate().keySet(), mutex);
        }
        return keySet;
      }
    }

    @Override
    public Collection<V> values() {
      synchronized (mutex) {
        if (valuesCollection == null) {
          valuesCollection = collection(delegate().values(), mutex);
        }
        return valuesCollection;
      }
    }

    @Override
    public Collection<Map.Entry<K, V>> entries() {
      synchronized (mutex) {
        if (entries == null) {
          entries = typePreservingCollection(delegate().entries(), mutex);
        }
        return entries;
      }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
      synchronized (mutex) {
        delegate().forEach(action);
      }
    }

    @Override
    public Map<K, Collection<V>> asMap() {
      synchronized (mutex) {
        if (asMap == null) {
          asMap = new SynchronizedAsMap<>(delegate().asMap(), mutex);
        }
        return asMap;
      }
    }

    @Override
    public Multiset<K> keys() {
      synchronized (mutex) {
        if (keys == null) {
          keys = multiset(delegate().keys(), mutex);
        }
        return keys;
      }
    }

    @Override
    public boolean equals(@CheckForNull Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(o);
      }
    }

    @Override
    public int hashCode() {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    private static final long serialVersionUID = 0;
  }

  static <K extends @Nullable Object, V extends @Nullable Object> ListMultimap<K, V> listMultimap(
      ListMultimap<K, V> multimap, @CheckForNull Object mutex) {
    if (multimap instanceof SynchronizedListMultimap || multimap instanceof BaseImmutableMultimap) {
      return multimap;
    }
    return new SynchronizedListMultimap<>(multimap, mutex);
  }

  static final class SynchronizedListMultimap<
          K extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedMultimap<K, V> implements ListMultimap<K, V> {
    SynchronizedListMultimap(ListMultimap<K, V> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    ListMultimap<K, V> delegate() {
      return (ListMultimap<K, V>) super.delegate();
    }

    @Override
    public List<V> get(K key) {
      synchronized (mutex) {
        return list(delegate().get(key), mutex);
      }
    }

    @Override
    public List<V> removeAll(@CheckForNull Object key) {
      synchronized (mutex) {
        return delegate().removeAll(key); // copy not synchronized
      }
    }

    @Override
    public List<V> replaceValues(K key, Iterable<? extends V> values) {
      synchronized (mutex) {
        return delegate().replaceValues(key, values); // copy not synchronized
      }
    }

    private static final long serialVersionUID = 0;
  }

  static <K extends @Nullable Object, V extends @Nullable Object> SetMultimap<K, V> setMultimap(
      SetMultimap<K, V> multimap, @CheckForNull Object mutex) {
    if (multimap instanceof SynchronizedSetMultimap || multimap instanceof BaseImmutableMultimap) {
      return multimap;
    }
    return new SynchronizedSetMultimap<>(multimap, mutex);
  }

  static class SynchronizedSetMultimap<K extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedMultimap<K, V> implements SetMultimap<K, V> {
    @CheckForNull transient Set<Map.Entry<K, V>> entrySet;

    SynchronizedSetMultimap(SetMultimap<K, V> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    SetMultimap<K, V> delegate() {
      return (SetMultimap<K, V>) super.delegate();
    }

    @Override
    public Set<V> get(K key) {
      synchronized (mutex) {
        return set(delegate().get(key), mutex);
      }
    }

    @Override
    public Set<V> removeAll(@CheckForNull Object key) {
      synchronized (mutex) {
        return delegate().removeAll(key); // copy not synchronized
      }
    }

    @Override
    public Set<V> replaceValues(K key, Iterable<? extends V> values) {
      synchronized (mutex) {
        return delegate().replaceValues(key, values); // copy not synchronized
      }
    }

    @Override
    public Set<Map.Entry<K, V>> entries() {
      synchronized (mutex) {
        if (entrySet == null) {
          entrySet = set(delegate().entries(), mutex);
        }
        return entrySet;
      }
    }

    private static final long serialVersionUID = 0;
  }

  static <K extends @Nullable Object, V extends @Nullable Object>
      SortedSetMultimap<K, V> sortedSetMultimap(
          SortedSetMultimap<K, V> multimap, @CheckForNull Object mutex) {
    if (multimap instanceof SynchronizedSortedSetMultimap) {
      return multimap;
    }
    return new SynchronizedSortedSetMultimap<>(multimap, mutex);
  }

  static final class SynchronizedSortedSetMultimap<
          K extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedSetMultimap<K, V> implements SortedSetMultimap<K, V> {
    SynchronizedSortedSetMultimap(SortedSetMultimap<K, V> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    SortedSetMultimap<K, V> delegate() {
      return (SortedSetMultimap<K, V>) super.delegate();
    }

    @Override
    public SortedSet<V> get(K key) {
      synchronized (mutex) {
        return sortedSet(delegate().get(key), mutex);
      }
    }

    @Override
    public SortedSet<V> removeAll(@CheckForNull Object key) {
      synchronized (mutex) {
        return delegate().removeAll(key); // copy not synchronized
      }
    }

    @Override
    public SortedSet<V> replaceValues(K key, Iterable<? extends V> values) {
      synchronized (mutex) {
        return delegate().replaceValues(key, values); // copy not synchronized
      }
    }

    @Override
    @CheckForNull
    public Comparator<? super V> valueComparator() {
      synchronized (mutex) {
        return delegate().valueComparator();
      }
    }

    private static final long serialVersionUID = 0;
  }

  private static <E extends @Nullable Object> Collection<E> typePreservingCollection(
      Collection<E> collection, @CheckForNull Object mutex) {
    if (collection instanceof SortedSet) {
      return sortedSet((SortedSet<E>) collection, mutex);
    }
    if (collection instanceof Set) {
      return set((Set<E>) collection, mutex);
    }
    if (collection instanceof List) {
      return list((List<E>) collection, mutex);
    }
    return collection(collection, mutex);
  }

  private static <E extends @Nullable Object> Set<E> typePreservingSet(
      Set<E> set, @CheckForNull Object mutex) {
    if (set instanceof SortedSet) {
      return sortedSet((SortedSet<E>) set, mutex);
    } else {
      return set(set, mutex);
    }
  }

  static final class SynchronizedAsMapEntries<
          K extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedSet<Map.Entry<K, Collection<V>>> {
    SynchronizedAsMapEntries(
        Set<Map.Entry<K, Collection<V>>> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    public Iterator<Map.Entry<K, Collection<V>>> iterator() {
      // Must be manually synchronized.
      return new TransformedIterator<Map.Entry<K, Collection<V>>, Map.Entry<K, Collection<V>>>(
          super.iterator()) {
        @Override
        Map.Entry<K, Collection<V>> transform(final Map.Entry<K, Collection<V>> entry) {
          return new ForwardingMapEntry<K, Collection<V>>() {
            @Override
            protected Map.Entry<K, Collection<V>> delegate() {
              return entry;
            }

            @Override
            public Collection<V> getValue() {
              return typePreservingCollection(entry.getValue(), mutex);
            }
          };
        }
      };
    }

    // See Collections.CheckedMap.CheckedEntrySet for details on attacks.

    @Override
    public @Nullable Object[] toArray() {
      synchronized (mutex) {
        /*
         * toArrayImpl returns `@Nullable Object[]` rather than `Object[]` but only because it can
         * be used with collections that may contain null. This collection never contains nulls, so
         * we could return `Object[]`. But this class is private and J2KT cannot change return types
         * in overrides, so we declare `@Nullable Object[]` as the return type.
         */
        return ObjectArrays.toArrayImpl(delegate());
      }
    }

    @Override
    @SuppressWarnings("nullness") // b/192354773 in our checker affects toArray declarations
    public <T extends @Nullable Object> T[] toArray(T[] array) {
      synchronized (mutex) {
        return ObjectArrays.toArrayImpl(delegate(), array);
      }
    }

    @Override
    public boolean contains(@CheckForNull Object o) {
      synchronized (mutex) {
        return Maps.containsEntryImpl(delegate(), o);
      }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      synchronized (mutex) {
        return Collections2.containsAllImpl(delegate(), c);
      }
    }

    @Override
    public boolean equals(@CheckForNull Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return Sets.equalsImpl(delegate(), o);
      }
    }

    @Override
    public boolean remove(@CheckForNull Object o) {
      synchronized (mutex) {
        return Maps.removeEntryImpl(delegate(), o);
      }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      synchronized (mutex) {
        return Iterators.removeAll(delegate().iterator(), c);
      }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      synchronized (mutex) {
        return Iterators.retainAll(delegate().iterator(), c);
      }
    }

    private static final long serialVersionUID = 0;
  }

  @VisibleForTesting
  static <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> map(
      Map<K, V> map, @CheckForNull Object mutex) {
    return new SynchronizedMap<>(map, mutex);
  }

  static class SynchronizedMap<K extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedObject implements Map<K, V> {
    @CheckForNull transient Set<K> keySet;
    @CheckForNull transient Collection<V> values;
    @CheckForNull transient Set<Map.Entry<K, V>> entrySet;

    SynchronizedMap(Map<K, V> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @SuppressWarnings("unchecked")
    @Override
    Map<K, V> delegate() {
      return (Map<K, V>) super.delegate();
    }

    @Override
    public void clear() {
      synchronized (mutex) {
        delegate().clear();
      }
    }

    @Override
    public boolean containsKey(@CheckForNull Object key) {
      synchronized (mutex) {
        return delegate().containsKey(key);
      }
    }

    @Override
    public boolean containsValue(@CheckForNull Object value) {
      synchronized (mutex) {
        return delegate().containsValue(value);
      }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
      synchronized (mutex) {
        if (entrySet == null) {
          entrySet = set(delegate().entrySet(), mutex);
        }
        return entrySet;
      }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
      synchronized (mutex) {
        delegate().forEach(action);
      }
    }

    @Override
    @CheckForNull
    public V get(@CheckForNull Object key) {
      synchronized (mutex) {
        return delegate().get(key);
      }
    }

    @Override
    @CheckForNull
    public V getOrDefault(@CheckForNull Object key, @CheckForNull V defaultValue) {
      synchronized (mutex) {
        return delegate().getOrDefault(key, defaultValue);
      }
    }

    @Override
    public boolean isEmpty() {
      synchronized (mutex) {
        return delegate().isEmpty();
      }
    }

    @Override
    public Set<K> keySet() {
      synchronized (mutex) {
        if (keySet == null) {
          keySet = set(delegate().keySet(), mutex);
        }
        return keySet;
      }
    }

    @Override
    @CheckForNull
    public V put(K key, V value) {
      synchronized (mutex) {
        return delegate().put(key, value);
      }
    }

    @Override
    @CheckForNull
    public V putIfAbsent(K key, V value) {
      synchronized (mutex) {
        return delegate().putIfAbsent(key, value);
      }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
      synchronized (mutex) {
        return delegate().replace(key, oldValue, newValue);
      }
    }

    @Override
    @CheckForNull
    public V replace(K key, V value) {
      synchronized (mutex) {
        return delegate().replace(key, value);
      }
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
      synchronized (mutex) {
        return delegate().computeIfAbsent(key, mappingFunction);
      }
    }

    /*
     * TODO(cpovirk): Uncomment the @NonNull annotations below once our JDK stubs and J2KT
     * emulations include them.
     */
    @Override
    @CheckForNull
    public V computeIfPresent(
        K key,
        BiFunction<? super K, ? super /*@NonNull*/ V, ? extends @Nullable V> remappingFunction) {
      synchronized (mutex) {
        return delegate().computeIfPresent(key, remappingFunction);
      }
    }

    @Override
    @CheckForNull
    public V compute(
        K key,
        BiFunction<? super K, ? super @Nullable V, ? extends @Nullable V> remappingFunction) {
      synchronized (mutex) {
        return delegate().compute(key, remappingFunction);
      }
    }

    @Override
    @CheckForNull
    public V merge(
        K key,
        /*@NonNull*/ V value,
        BiFunction<? super /*@NonNull*/ V, ? super /*@NonNull*/ V, ? extends @Nullable V>
            remappingFunction) {
      synchronized (mutex) {
        return delegate().merge(key, value, remappingFunction);
      }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
      synchronized (mutex) {
        delegate().putAll(map);
      }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
      synchronized (mutex) {
        delegate().replaceAll(function);
      }
    }

    @Override
    @CheckForNull
    public V remove(@CheckForNull Object key) {
      synchronized (mutex) {
        return delegate().remove(key);
      }
    }

    @Override
    public boolean remove(@CheckForNull Object key, @CheckForNull Object value) {
      synchronized (mutex) {
        return delegate().remove(key, value);
      }
    }

    @Override
    public int size() {
      synchronized (mutex) {
        return delegate().size();
      }
    }

    @Override
    public Collection<V> values() {
      synchronized (mutex) {
        if (values == null) {
          values = collection(delegate().values(), mutex);
        }
        return values;
      }
    }

    @Override
    public boolean equals(@CheckForNull Object o) {
      if (o == this) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(o);
      }
    }

    @Override
    public int hashCode() {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    private static final long serialVersionUID = 0;
  }

  static <K extends @Nullable Object, V extends @Nullable Object> SortedMap<K, V> sortedMap(
      SortedMap<K, V> sortedMap, @CheckForNull Object mutex) {
    return new SynchronizedSortedMap<>(sortedMap, mutex);
  }

  static class SynchronizedSortedMap<K extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedMap<K, V> implements SortedMap<K, V> {

    SynchronizedSortedMap(SortedMap<K, V> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    SortedMap<K, V> delegate() {
      return (SortedMap<K, V>) super.delegate();
    }

    @Override
    @CheckForNull
    public Comparator<? super K> comparator() {
      synchronized (mutex) {
        return delegate().comparator();
      }
    }

    @Override
    public K firstKey() {
      synchronized (mutex) {
        return delegate().firstKey();
      }
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      synchronized (mutex) {
        return sortedMap(delegate().headMap(toKey), mutex);
      }
    }

    @Override
    public K lastKey() {
      synchronized (mutex) {
        return delegate().lastKey();
      }
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      synchronized (mutex) {
        return sortedMap(delegate().subMap(fromKey, toKey), mutex);
      }
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      synchronized (mutex) {
        return sortedMap(delegate().tailMap(fromKey), mutex);
      }
    }

    private static final long serialVersionUID = 0;
  }

  static <K extends @Nullable Object, V extends @Nullable Object> BiMap<K, V> biMap(
      BiMap<K, V> bimap, @CheckForNull Object mutex) {
    if (bimap instanceof SynchronizedBiMap || bimap instanceof ImmutableBiMap) {
      return bimap;
    }
    return new SynchronizedBiMap<>(bimap, mutex, null);
  }

  static final class SynchronizedBiMap<K extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedMap<K, V> implements BiMap<K, V>, Serializable {
    @CheckForNull private transient Set<V> valueSet;
    @RetainedWith @CheckForNull private transient BiMap<V, K> inverse;

    private SynchronizedBiMap(
        BiMap<K, V> delegate, @CheckForNull Object mutex, @CheckForNull BiMap<V, K> inverse) {
      super(delegate, mutex);
      this.inverse = inverse;
    }

    @Override
    BiMap<K, V> delegate() {
      return (BiMap<K, V>) super.delegate();
    }

    @Override
    public Set<V> values() {
      synchronized (mutex) {
        if (valueSet == null) {
          valueSet = set(delegate().values(), mutex);
        }
        return valueSet;
      }
    }

    @Override
    @CheckForNull
    public V forcePut(@ParametricNullness K key, @ParametricNullness V value) {
      synchronized (mutex) {
        return delegate().forcePut(key, value);
      }
    }

    @Override
    public BiMap<V, K> inverse() {
      synchronized (mutex) {
        if (inverse == null) {
          inverse = new SynchronizedBiMap<>(delegate().inverse(), mutex, this);
        }
        return inverse;
      }
    }

    private static final long serialVersionUID = 0;
  }

  static final class SynchronizedAsMap<K extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedMap<K, Collection<V>> {
    @CheckForNull transient Set<Map.Entry<K, Collection<V>>> asMapEntrySet;
    @CheckForNull transient Collection<Collection<V>> asMapValues;

    SynchronizedAsMap(Map<K, Collection<V>> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    @CheckForNull
    public Collection<V> get(@CheckForNull Object key) {
      synchronized (mutex) {
        Collection<V> collection = super.get(key);
        return (collection == null) ? null : typePreservingCollection(collection, mutex);
      }
    }

    @Override
    public Set<Map.Entry<K, Collection<V>>> entrySet() {
      synchronized (mutex) {
        if (asMapEntrySet == null) {
          asMapEntrySet = new SynchronizedAsMapEntries<>(delegate().entrySet(), mutex);
        }
        return asMapEntrySet;
      }
    }

    @Override
    public Collection<Collection<V>> values() {
      synchronized (mutex) {
        if (asMapValues == null) {
          asMapValues = new SynchronizedAsMapValues<V>(delegate().values(), mutex);
        }
        return asMapValues;
      }
    }

    @Override
    public boolean containsValue(@CheckForNull Object o) {
      // values() and its contains() method are both synchronized.
      return values().contains(o);
    }

    private static final long serialVersionUID = 0;
  }

  static final class SynchronizedAsMapValues<V extends @Nullable Object>
      extends SynchronizedCollection<Collection<V>> {
    SynchronizedAsMapValues(Collection<Collection<V>> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    public Iterator<Collection<V>> iterator() {
      // Must be manually synchronized.
      return new TransformedIterator<Collection<V>, Collection<V>>(super.iterator()) {
        @Override
        Collection<V> transform(Collection<V> from) {
          return typePreservingCollection(from, mutex);
        }
      };
    }

    private static final long serialVersionUID = 0;
  }

  @GwtIncompatible // NavigableSet
  @VisibleForTesting
  static final class SynchronizedNavigableSet<E extends @Nullable Object>
      extends SynchronizedSortedSet<E> implements NavigableSet<E> {
    SynchronizedNavigableSet(NavigableSet<E> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    NavigableSet<E> delegate() {
      return (NavigableSet<E>) super.delegate();
    }

    @Override
    @CheckForNull
    public E ceiling(E e) {
      synchronized (mutex) {
        return delegate().ceiling(e);
      }
    }

    @Override
    public Iterator<E> descendingIterator() {
      return delegate().descendingIterator(); // manually synchronized
    }

    @CheckForNull transient NavigableSet<E> descendingSet;

    @Override
    public NavigableSet<E> descendingSet() {
      synchronized (mutex) {
        if (descendingSet == null) {
          NavigableSet<E> dS = Synchronized.navigableSet(delegate().descendingSet(), mutex);
          descendingSet = dS;
          return dS;
        }
        return descendingSet;
      }
    }

    @Override
    @CheckForNull
    public E floor(E e) {
      synchronized (mutex) {
        return delegate().floor(e);
      }
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
      synchronized (mutex) {
        return Synchronized.navigableSet(delegate().headSet(toElement, inclusive), mutex);
      }
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
      return headSet(toElement, false);
    }

    @Override
    @CheckForNull
    public E higher(E e) {
      synchronized (mutex) {
        return delegate().higher(e);
      }
    }

    @Override
    @CheckForNull
    public E lower(E e) {
      synchronized (mutex) {
        return delegate().lower(e);
      }
    }

    @Override
    @CheckForNull
    public E pollFirst() {
      synchronized (mutex) {
        return delegate().pollFirst();
      }
    }

    @Override
    @CheckForNull
    public E pollLast() {
      synchronized (mutex) {
        return delegate().pollLast();
      }
    }

    @Override
    public NavigableSet<E> subSet(
        E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
      synchronized (mutex) {
        return Synchronized.navigableSet(
            delegate().subSet(fromElement, fromInclusive, toElement, toInclusive), mutex);
      }
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
      return subSet(fromElement, true, toElement, false);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      synchronized (mutex) {
        return Synchronized.navigableSet(delegate().tailSet(fromElement, inclusive), mutex);
      }
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
      return tailSet(fromElement, true);
    }

    private static final long serialVersionUID = 0;
  }

  @GwtIncompatible // NavigableSet
  static <E extends @Nullable Object> NavigableSet<E> navigableSet(
      NavigableSet<E> navigableSet, @CheckForNull Object mutex) {
    return new SynchronizedNavigableSet<E>(navigableSet, mutex);
  }

  @GwtIncompatible // NavigableSet
  static <E extends @Nullable Object> NavigableSet<E> navigableSet(NavigableSet<E> navigableSet) {
    return navigableSet(navigableSet, null);
  }

  @GwtIncompatible // NavigableMap
  static <K extends @Nullable Object, V extends @Nullable Object> NavigableMap<K, V> navigableMap(
      NavigableMap<K, V> navigableMap) {
    return navigableMap(navigableMap, null);
  }

  @GwtIncompatible // NavigableMap
  static <K extends @Nullable Object, V extends @Nullable Object> NavigableMap<K, V> navigableMap(
      NavigableMap<K, V> navigableMap, @CheckForNull Object mutex) {
    return new SynchronizedNavigableMap<>(navigableMap, mutex);
  }

  @GwtIncompatible // NavigableMap
  @VisibleForTesting
  static final class SynchronizedNavigableMap<
          K extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedSortedMap<K, V> implements NavigableMap<K, V> {

    SynchronizedNavigableMap(NavigableMap<K, V> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    NavigableMap<K, V> delegate() {
      return (NavigableMap<K, V>) super.delegate();
    }

    @Override
    @CheckForNull
    public Map.Entry<K, V> ceilingEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().ceilingEntry(key), mutex);
      }
    }

    @Override
    @CheckForNull
    public K ceilingKey(K key) {
      synchronized (mutex) {
        return delegate().ceilingKey(key);
      }
    }

    @CheckForNull transient NavigableSet<K> descendingKeySet;

    @Override
    public NavigableSet<K> descendingKeySet() {
      synchronized (mutex) {
        if (descendingKeySet == null) {
          return descendingKeySet = Synchronized.navigableSet(delegate().descendingKeySet(), mutex);
        }
        return descendingKeySet;
      }
    }

    @CheckForNull transient NavigableMap<K, V> descendingMap;

    @Override
    public NavigableMap<K, V> descendingMap() {
      synchronized (mutex) {
        if (descendingMap == null) {
          return descendingMap = navigableMap(delegate().descendingMap(), mutex);
        }
        return descendingMap;
      }
    }

    @Override
    @CheckForNull
    public Map.Entry<K, V> firstEntry() {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().firstEntry(), mutex);
      }
    }

    @Override
    @CheckForNull
    public Map.Entry<K, V> floorEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().floorEntry(key), mutex);
      }
    }

    @Override
    @CheckForNull
    public K floorKey(K key) {
      synchronized (mutex) {
        return delegate().floorKey(key);
      }
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      synchronized (mutex) {
        return navigableMap(delegate().headMap(toKey, inclusive), mutex);
      }
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
    }

    @Override
    @CheckForNull
    public Map.Entry<K, V> higherEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().higherEntry(key), mutex);
      }
    }

    @Override
    @CheckForNull
    public K higherKey(K key) {
      synchronized (mutex) {
        return delegate().higherKey(key);
      }
    }

    @Override
    @CheckForNull
    public Map.Entry<K, V> lastEntry() {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().lastEntry(), mutex);
      }
    }

    @Override
    @CheckForNull
    public Map.Entry<K, V> lowerEntry(K key) {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().lowerEntry(key), mutex);
      }
    }

    @Override
    @CheckForNull
    public K lowerKey(K key) {
      synchronized (mutex) {
        return delegate().lowerKey(key);
      }
    }

    @Override
    public Set<K> keySet() {
      return navigableKeySet();
    }

    @CheckForNull transient NavigableSet<K> navigableKeySet;

    @Override
    public NavigableSet<K> navigableKeySet() {
      synchronized (mutex) {
        if (navigableKeySet == null) {
          return navigableKeySet = Synchronized.navigableSet(delegate().navigableKeySet(), mutex);
        }
        return navigableKeySet;
      }
    }

    @Override
    @CheckForNull
    public Map.Entry<K, V> pollFirstEntry() {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().pollFirstEntry(), mutex);
      }
    }

    @Override
    @CheckForNull
    public Map.Entry<K, V> pollLastEntry() {
      synchronized (mutex) {
        return nullableSynchronizedEntry(delegate().pollLastEntry(), mutex);
      }
    }

    @Override
    public NavigableMap<K, V> subMap(
        K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      synchronized (mutex) {
        return navigableMap(delegate().subMap(fromKey, fromInclusive, toKey, toInclusive), mutex);
      }
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      synchronized (mutex) {
        return navigableMap(delegate().tailMap(fromKey, inclusive), mutex);
      }
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
    }

    private static final long serialVersionUID = 0;
  }

  @GwtIncompatible // works but is needed only for NavigableMap
  @CheckForNull
  private static <K extends @Nullable Object, V extends @Nullable Object>
      Map.Entry<K, V> nullableSynchronizedEntry(
          @CheckForNull Map.Entry<K, V> entry, @CheckForNull Object mutex) {
    if (entry == null) {
      return null;
    }
    return new SynchronizedEntry<>(entry, mutex);
  }

  @GwtIncompatible // works but is needed only for NavigableMap
  static final class SynchronizedEntry<K extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedObject implements Map.Entry<K, V> {

    SynchronizedEntry(Map.Entry<K, V> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @SuppressWarnings("unchecked") // guaranteed by the constructor
    @Override
    Map.Entry<K, V> delegate() {
      return (Map.Entry<K, V>) super.delegate();
    }

    @Override
    public boolean equals(@CheckForNull Object obj) {
      synchronized (mutex) {
        return delegate().equals(obj);
      }
    }

    @Override
    public int hashCode() {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    @Override
    public K getKey() {
      synchronized (mutex) {
        return delegate().getKey();
      }
    }

    @Override
    public V getValue() {
      synchronized (mutex) {
        return delegate().getValue();
      }
    }

    @Override
    public V setValue(V value) {
      synchronized (mutex) {
        return delegate().setValue(value);
      }
    }

    private static final long serialVersionUID = 0;
  }

  static <E extends @Nullable Object> Queue<E> queue(Queue<E> queue, @CheckForNull Object mutex) {
    return (queue instanceof SynchronizedQueue) ? queue : new SynchronizedQueue<E>(queue, mutex);
  }

  static class SynchronizedQueue<E extends @Nullable Object> extends SynchronizedCollection<E>
      implements Queue<E> {

    SynchronizedQueue(Queue<E> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    Queue<E> delegate() {
      return (Queue<E>) super.delegate();
    }

    @Override
    public E element() {
      synchronized (mutex) {
        return delegate().element();
      }
    }

    @Override
    public boolean offer(E e) {
      synchronized (mutex) {
        return delegate().offer(e);
      }
    }

    @Override
    @CheckForNull
    public E peek() {
      synchronized (mutex) {
        return delegate().peek();
      }
    }

    @Override
    @CheckForNull
    public E poll() {
      synchronized (mutex) {
        return delegate().poll();
      }
    }

    @Override
    public E remove() {
      synchronized (mutex) {
        return delegate().remove();
      }
    }

    private static final long serialVersionUID = 0;
  }

  static <E extends @Nullable Object> Deque<E> deque(Deque<E> deque, @CheckForNull Object mutex) {
    return new SynchronizedDeque<E>(deque, mutex);
  }

  static final class SynchronizedDeque<E extends @Nullable Object> extends SynchronizedQueue<E>
      implements Deque<E> {

    SynchronizedDeque(Deque<E> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @Override
    Deque<E> delegate() {
      return (Deque<E>) super.delegate();
    }

    @Override
    public void addFirst(E e) {
      synchronized (mutex) {
        delegate().addFirst(e);
      }
    }

    @Override
    public void addLast(E e) {
      synchronized (mutex) {
        delegate().addLast(e);
      }
    }

    @Override
    public boolean offerFirst(E e) {
      synchronized (mutex) {
        return delegate().offerFirst(e);
      }
    }

    @Override
    public boolean offerLast(E e) {
      synchronized (mutex) {
        return delegate().offerLast(e);
      }
    }

    @Override
    public E removeFirst() {
      synchronized (mutex) {
        return delegate().removeFirst();
      }
    }

    @Override
    public E removeLast() {
      synchronized (mutex) {
        return delegate().removeLast();
      }
    }

    @Override
    @CheckForNull
    public E pollFirst() {
      synchronized (mutex) {
        return delegate().pollFirst();
      }
    }

    @Override
    @CheckForNull
    public E pollLast() {
      synchronized (mutex) {
        return delegate().pollLast();
      }
    }

    @Override
    public E getFirst() {
      synchronized (mutex) {
        return delegate().getFirst();
      }
    }

    @Override
    public E getLast() {
      synchronized (mutex) {
        return delegate().getLast();
      }
    }

    @Override
    @CheckForNull
    public E peekFirst() {
      synchronized (mutex) {
        return delegate().peekFirst();
      }
    }

    @Override
    @CheckForNull
    public E peekLast() {
      synchronized (mutex) {
        return delegate().peekLast();
      }
    }

    @Override
    public boolean removeFirstOccurrence(@CheckForNull Object o) {
      synchronized (mutex) {
        return delegate().removeFirstOccurrence(o);
      }
    }

    @Override
    public boolean removeLastOccurrence(@CheckForNull Object o) {
      synchronized (mutex) {
        return delegate().removeLastOccurrence(o);
      }
    }

    @Override
    public void push(E e) {
      synchronized (mutex) {
        delegate().push(e);
      }
    }

    @Override
    public E pop() {
      synchronized (mutex) {
        return delegate().pop();
      }
    }

    @Override
    public Iterator<E> descendingIterator() {
      synchronized (mutex) {
        return delegate().descendingIterator();
      }
    }

    private static final long serialVersionUID = 0;
  }

  static <R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      Table<R, C, V> table(Table<R, C, V> table, @CheckForNull Object mutex) {
    return new SynchronizedTable<>(table, mutex);
  }

  static final class SynchronizedTable<
          R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      extends SynchronizedObject implements Table<R, C, V> {

    SynchronizedTable(Table<R, C, V> delegate, @CheckForNull Object mutex) {
      super(delegate, mutex);
    }

    @SuppressWarnings("unchecked")
    @Override
    Table<R, C, V> delegate() {
      return (Table<R, C, V>) super.delegate();
    }

    @Override
    public boolean contains(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
      synchronized (mutex) {
        return delegate().contains(rowKey, columnKey);
      }
    }

    @Override
    public boolean containsRow(@CheckForNull Object rowKey) {
      synchronized (mutex) {
        return delegate().containsRow(rowKey);
      }
    }

    @Override
    public boolean containsColumn(@CheckForNull Object columnKey) {
      synchronized (mutex) {
        return delegate().containsColumn(columnKey);
      }
    }

    @Override
    public boolean containsValue(@CheckForNull Object value) {
      synchronized (mutex) {
        return delegate().containsValue(value);
      }
    }

    @Override
    @CheckForNull
    public V get(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
      synchronized (mutex) {
        return delegate().get(rowKey, columnKey);
      }
    }

    @Override
    public boolean isEmpty() {
      synchronized (mutex) {
        return delegate().isEmpty();
      }
    }

    @Override
    public int size() {
      synchronized (mutex) {
        return delegate().size();
      }
    }

    @Override
    public void clear() {
      synchronized (mutex) {
        delegate().clear();
      }
    }

    @Override
    @CheckForNull
    public V put(
        @ParametricNullness R rowKey,
        @ParametricNullness C columnKey,
        @ParametricNullness V value) {
      synchronized (mutex) {
        return delegate().put(rowKey, columnKey, value);
      }
    }

    @Override
    public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
      synchronized (mutex) {
        delegate().putAll(table);
      }
    }

    @Override
    @CheckForNull
    public V remove(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
      synchronized (mutex) {
        return delegate().remove(rowKey, columnKey);
      }
    }

    @Override
    public Map<C, V> row(@ParametricNullness R rowKey) {
      synchronized (mutex) {
        return map(delegate().row(rowKey), mutex);
      }
    }

    @Override
    public Map<R, V> column(@ParametricNullness C columnKey) {
      synchronized (mutex) {
        return map(delegate().column(columnKey), mutex);
      }
    }

    @Override
    public Set<Cell<R, C, V>> cellSet() {
      synchronized (mutex) {
        return set(delegate().cellSet(), mutex);
      }
    }

    @Override
    public Set<R> rowKeySet() {
      synchronized (mutex) {
        return set(delegate().rowKeySet(), mutex);
      }
    }

    @Override
    public Set<C> columnKeySet() {
      synchronized (mutex) {
        return set(delegate().columnKeySet(), mutex);
      }
    }

    @Override
    public Collection<V> values() {
      synchronized (mutex) {
        return collection(delegate().values(), mutex);
      }
    }

    @Override
    public Map<R, Map<C, V>> rowMap() {
      synchronized (mutex) {
        return map(
            Maps.transformValues(
                delegate().rowMap(),
                new com.google.common.base.Function<Map<C, V>, Map<C, V>>() {
                  @Override
                  public Map<C, V> apply(Map<C, V> t) {
                    return map(t, mutex);
                  }
                }),
            mutex);
      }
    }

    @Override
    public Map<C, Map<R, V>> columnMap() {
      synchronized (mutex) {
        return map(
            Maps.transformValues(
                delegate().columnMap(),
                new com.google.common.base.Function<Map<R, V>, Map<R, V>>() {
                  @Override
                  public Map<R, V> apply(Map<R, V> t) {
                    return map(t, mutex);
                  }
                }),
            mutex);
      }
    }

    @Override
    public int hashCode() {
      synchronized (mutex) {
        return delegate().hashCode();
      }
    }

    @Override
    public boolean equals(@CheckForNull Object obj) {
      if (this == obj) {
        return true;
      }
      synchronized (mutex) {
        return delegate().equals(obj);
      }
    }
  }
}
