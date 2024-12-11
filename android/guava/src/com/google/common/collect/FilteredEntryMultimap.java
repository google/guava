/*
 * Copyright (C) 2012 The Guava Authors
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
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps.ViewCachingAbstractMap;
import com.google.j2objc.annotations.WeakOuter;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of {@link Multimaps#filterEntries(Multimap, Predicate)}.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
class FilteredEntryMultimap<K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractMultimap<K, V> implements FilteredMultimap<K, V> {
  final Multimap<K, V> unfiltered;
  final Predicate<? super Entry<K, V>> predicate;

  FilteredEntryMultimap(Multimap<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
    this.unfiltered = checkNotNull(unfiltered);
    this.predicate = checkNotNull(predicate);
  }

  @Override
  public Multimap<K, V> unfiltered() {
    return unfiltered;
  }

  @Override
  public Predicate<? super Entry<K, V>> entryPredicate() {
    return predicate;
  }

  @Override
  public int size() {
    return entries().size();
  }

  private boolean satisfies(@ParametricNullness K key, @ParametricNullness V value) {
    return predicate.apply(immutableEntry(key, value));
  }

  final class ValuePredicate implements Predicate<V> {
    @ParametricNullness private final K key;

    ValuePredicate(@ParametricNullness K key) {
      this.key = key;
    }

    @Override
    public boolean apply(@ParametricNullness V value) {
      return satisfies(key, value);
    }
  }

  static <E extends @Nullable Object> Collection<E> filterCollection(
      Collection<E> collection, Predicate<? super E> predicate) {
    if (collection instanceof Set) {
      return Sets.filter((Set<E>) collection, predicate);
    } else {
      return Collections2.filter(collection, predicate);
    }
  }

  @Override
  public boolean containsKey(@CheckForNull Object key) {
    return asMap().get(key) != null;
  }

  @Override
  public Collection<V> removeAll(@CheckForNull Object key) {
    return MoreObjects.firstNonNull(asMap().remove(key), unmodifiableEmptyCollection());
  }

  Collection<V> unmodifiableEmptyCollection() {
    // These return false, rather than throwing a UOE, on remove calls.
    return (unfiltered instanceof SetMultimap)
        ? Collections.<V>emptySet()
        : Collections.<V>emptyList();
  }

  @Override
  public void clear() {
    entries().clear();
  }

  @Override
  public Collection<V> get(@ParametricNullness K key) {
    return filterCollection(unfiltered.get(key), new ValuePredicate(key));
  }

  @Override
  Collection<Entry<K, V>> createEntries() {
    return filterCollection(unfiltered.entries(), predicate);
  }

  @Override
  Collection<V> createValues() {
    return new FilteredMultimapValues<>(this);
  }

  @Override
  Iterator<Entry<K, V>> entryIterator() {
    throw new AssertionError("should never be called");
  }

  @Override
  Map<K, Collection<V>> createAsMap() {
    return new AsMap();
  }

  @Override
  Set<K> createKeySet() {
    return asMap().keySet();
  }

  boolean removeEntriesIf(Predicate<? super Entry<K, Collection<V>>> predicate) {
    Iterator<Entry<K, Collection<V>>> entryIterator = unfiltered.asMap().entrySet().iterator();
    boolean changed = false;
    while (entryIterator.hasNext()) {
      Entry<K, Collection<V>> entry = entryIterator.next();
      K key = entry.getKey();
      Collection<V> collection = filterCollection(entry.getValue(), new ValuePredicate(key));
      if (!collection.isEmpty()
          && predicate.apply(Maps.<K, Collection<V>>immutableEntry(key, collection))) {
        if (collection.size() == entry.getValue().size()) {
          entryIterator.remove();
        } else {
          collection.clear();
        }
        changed = true;
      }
    }
    return changed;
  }

  @WeakOuter
  class AsMap extends ViewCachingAbstractMap<K, Collection<V>> {
    @Override
    public boolean containsKey(@CheckForNull Object key) {
      return get(key) != null;
    }

    @Override
    public void clear() {
      FilteredEntryMultimap.this.clear();
    }

    @Override
    @CheckForNull
    public Collection<V> get(@CheckForNull Object key) {
      Collection<V> result = unfiltered.asMap().get(key);
      if (result == null) {
        return null;
      }
      @SuppressWarnings("unchecked") // key is equal to a K, if not a K itself
      K k = (K) key;
      result = filterCollection(result, new ValuePredicate(k));
      return result.isEmpty() ? null : result;
    }

    @Override
    @CheckForNull
    public Collection<V> remove(@CheckForNull Object key) {
      Collection<V> collection = unfiltered.asMap().get(key);
      if (collection == null) {
        return null;
      }
      @SuppressWarnings("unchecked") // it's definitely equal to a K
      K k = (K) key;
      List<V> result = Lists.newArrayList();
      Iterator<V> itr = collection.iterator();
      while (itr.hasNext()) {
        V v = itr.next();
        if (satisfies(k, v)) {
          itr.remove();
          result.add(v);
        }
      }
      if (result.isEmpty()) {
        return null;
      } else if (unfiltered instanceof SetMultimap) {
        return unmodifiableSet(Sets.newLinkedHashSet(result));
      } else {
        return unmodifiableList(result);
      }
    }

    @Override
    Set<K> createKeySet() {
      @WeakOuter
      class KeySetImpl extends Maps.KeySet<K, Collection<V>> {
        KeySetImpl() {
          super(AsMap.this);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
          return removeEntriesIf(Maps.<K>keyPredicateOnEntries(in(c)));
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          return removeEntriesIf(Maps.<K>keyPredicateOnEntries(not(in(c))));
        }

        @Override
        public boolean remove(@CheckForNull Object o) {
          return AsMap.this.remove(o) != null;
        }
      }
      return new KeySetImpl();
    }

    @Override
    Set<Entry<K, Collection<V>>> createEntrySet() {
      @WeakOuter
      class EntrySetImpl extends Maps.EntrySet<K, Collection<V>> {
        @Override
        Map<K, Collection<V>> map() {
          return AsMap.this;
        }

        @Override
        public Iterator<Entry<K, Collection<V>>> iterator() {
          return new AbstractIterator<Entry<K, Collection<V>>>() {
            final Iterator<Entry<K, Collection<V>>> backingIterator =
                unfiltered.asMap().entrySet().iterator();

            @Override
            @CheckForNull
            protected Entry<K, Collection<V>> computeNext() {
              while (backingIterator.hasNext()) {
                Entry<K, Collection<V>> entry = backingIterator.next();
                K key = entry.getKey();
                Collection<V> collection =
                    filterCollection(entry.getValue(), new ValuePredicate(key));
                if (!collection.isEmpty()) {
                  return immutableEntry(key, collection);
                }
              }
              return endOfData();
            }
          };
        }

        @Override
        public boolean removeAll(Collection<?> c) {
          return removeEntriesIf(in(c));
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          return removeEntriesIf(not(in(c)));
        }

        @Override
        public int size() {
          return Iterators.size(iterator());
        }
      }
      return new EntrySetImpl();
    }

    @Override
    Collection<Collection<V>> createValues() {
      @WeakOuter
      class ValuesImpl extends Maps.Values<K, Collection<V>> {
        ValuesImpl() {
          super(AsMap.this);
        }

        @Override
        public boolean remove(@CheckForNull Object o) {
          if (o instanceof Collection) {
            Collection<?> c = (Collection<?>) o;
            Iterator<Entry<K, Collection<V>>> entryIterator =
                unfiltered.asMap().entrySet().iterator();
            while (entryIterator.hasNext()) {
              Entry<K, Collection<V>> entry = entryIterator.next();
              K key = entry.getKey();
              Collection<V> collection =
                  filterCollection(entry.getValue(), new ValuePredicate(key));
              if (!collection.isEmpty() && c.equals(collection)) {
                if (collection.size() == entry.getValue().size()) {
                  entryIterator.remove();
                } else {
                  collection.clear();
                }
                return true;
              }
            }
          }
          return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
          return removeEntriesIf(Maps.<Collection<V>>valuePredicateOnEntries(in(c)));
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          return removeEntriesIf(Maps.<Collection<V>>valuePredicateOnEntries(not(in(c))));
        }
      }
      return new ValuesImpl();
    }
  }

  @Override
  Multiset<K> createKeys() {
    return new Keys();
  }

  @WeakOuter
  class Keys extends Multimaps.Keys<K, V> {
    Keys() {
      super(FilteredEntryMultimap.this);
    }

    @Override
    public int remove(@CheckForNull Object key, int occurrences) {
      checkNonnegative(occurrences, "occurrences");
      if (occurrences == 0) {
        return count(key);
      }
      Collection<V> collection = unfiltered.asMap().get(key);
      if (collection == null) {
        return 0;
      }
      @SuppressWarnings("unchecked") // key is equal to a K, if not a K itself
      K k = (K) key;
      int oldCount = 0;
      Iterator<V> itr = collection.iterator();
      while (itr.hasNext()) {
        V v = itr.next();
        if (satisfies(k, v)) {
          oldCount++;
          if (oldCount <= occurrences) {
            itr.remove();
          }
        }
      }
      return oldCount;
    }

    @Override
    public Set<Multiset.Entry<K>> entrySet() {
      return new Multisets.EntrySet<K>() {

        @Override
        Multiset<K> multiset() {
          return Keys.this;
        }

        @Override
        public Iterator<Multiset.Entry<K>> iterator() {
          return Keys.this.entryIterator();
        }

        @Override
        public int size() {
          return FilteredEntryMultimap.this.keySet().size();
        }

        private boolean removeEntriesIf(Predicate<? super Multiset.Entry<K>> predicate) {
          return FilteredEntryMultimap.this.removeEntriesIf(
              (Map.Entry<K, Collection<V>> entry) ->
                  predicate.apply(
                      Multisets.<K>immutableEntry(entry.getKey(), entry.getValue().size())));
        }

        @Override
        public boolean removeAll(Collection<?> c) {
          return removeEntriesIf(in(c));
        }

        @Override
        public boolean retainAll(Collection<?> c) {
          return removeEntriesIf(not(in(c)));
        }
      };
    }
  }
}
