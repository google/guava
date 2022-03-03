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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Equivalence;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Helper classes for various benchmarks.
 *
 * @author Christopher Swenson
 */
final class BenchmarkHelpers {
  /** So far, this is the best way to test various implementations of {@link Set} subclasses. */
  public interface CollectionsImplEnum {
    <E extends Comparable<E>> Collection<E> create(Collection<E> contents);

    String name();
  }

  public interface MapsImplEnum {
    <K extends Comparable<K>, V> Map<K, V> create(Map<K, V> contents);

    String name();
  }

  public interface InternerImplEnum {
    <E> Interner<E> create(Collection<E> contents);

    String name();
  }

  public enum SetImpl implements CollectionsImplEnum {
    HashSetImpl {
      @Override
      public <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return new HashSet<E>(contents);
      }
    },
    LinkedHashSetImpl {
      @Override
      public <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return new LinkedHashSet<E>(contents);
      }
    },
    TreeSetImpl {
      @Override
      public <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return new TreeSet<E>(contents);
      }
    },
    UnmodifiableSetImpl {
      @Override
      public <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return Collections.unmodifiableSet(new HashSet<E>(contents));
      }
    },
    SynchronizedSetImpl {
      @Override
      public <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return Collections.synchronizedSet(new HashSet<E>(contents));
      }
    },
    ImmutableSetImpl {
      @Override
      public <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return ImmutableSet.copyOf(contents);
      }
    },
    ImmutableSortedSetImpl {
      @Override
      public <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return ImmutableSortedSet.copyOf(contents);
      }
    },
    ContiguousSetImpl {
      @Override
      public <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return ContiguousSet.copyOf(contents);
      }
    },
    ;
  }

  public enum ListMultimapImpl {
    ArrayListMultimapImpl {
      @Override
      <K, V> ListMultimap<K, V> create(Multimap<K, V> contents) {
        return ArrayListMultimap.create(contents);
      }
    },
    LinkedListMultimapImpl {
      @Override
      <K, V> ListMultimap<K, V> create(Multimap<K, V> contents) {
        return LinkedListMultimap.create(contents);
      }
    },
    ImmutableListMultimapImpl {
      @Override
      <K, V> ListMultimap<K, V> create(Multimap<K, V> contents) {
        return ImmutableListMultimap.copyOf(contents);
      }
    };

    abstract <K, V> ListMultimap<K, V> create(Multimap<K, V> contents);
  }

  public enum RangeSetImpl {
    TreeRangeSetImpl {
      @Override
      <K extends Comparable<K>> RangeSet<K> create(RangeSet<K> contents) {
        return TreeRangeSet.create(contents);
      }
    },
    ImmutableRangeSetImpl {
      @Override
      <K extends Comparable<K>> RangeSet<K> create(RangeSet<K> contents) {
        return ImmutableRangeSet.copyOf(contents);
      }
    };

    abstract <K extends Comparable<K>> RangeSet<K> create(RangeSet<K> contents);
  }

  public enum SetMultimapImpl {
    HashMultimapImpl {
      @Override
      <K extends Comparable<K>, V extends Comparable<V>> SetMultimap<K, V> create(
          Multimap<K, V> contents) {
        return HashMultimap.create(contents);
      }
    },
    LinkedHashMultimapImpl {
      @Override
      <K extends Comparable<K>, V extends Comparable<V>> SetMultimap<K, V> create(
          Multimap<K, V> contents) {
        return LinkedHashMultimap.create(contents);
      }
    },
    TreeMultimapImpl {
      @Override
      <K extends Comparable<K>, V extends Comparable<V>> SetMultimap<K, V> create(
          Multimap<K, V> contents) {
        return TreeMultimap.create(contents);
      }
    },
    ImmutableSetMultimapImpl {
      @Override
      <K extends Comparable<K>, V extends Comparable<V>> SetMultimap<K, V> create(
          Multimap<K, V> contents) {
        return ImmutableSetMultimap.copyOf(contents);
      }
    };

    abstract <K extends Comparable<K>, V extends Comparable<V>> SetMultimap<K, V> create(
        Multimap<K, V> contents);
  }

  public enum MapImpl implements MapsImplEnum {
    HashMapImpl {
      @Override
      public <K extends Comparable<K>, V> Map<K, V> create(Map<K, V> map) {
        return Maps.newHashMap(map);
      }
    },
    LinkedHashMapImpl {
      @Override
      public <K extends Comparable<K>, V> Map<K, V> create(Map<K, V> map) {
        return Maps.newLinkedHashMap(map);
      }
    },
    ConcurrentHashMapImpl {
      @Override
      public <K extends Comparable<K>, V> Map<K, V> create(Map<K, V> map) {
        return new ConcurrentHashMap<>(map);
      }
    },
    ImmutableMapImpl {
      @Override
      public <K extends Comparable<K>, V> Map<K, V> create(Map<K, V> map) {
        return ImmutableMap.copyOf(map);
      }
    },
    MapMakerStrongKeysStrongValues {
      @Override
      public <K extends Comparable<K>, V> Map<K, V> create(Map<K, V> map) {
        // We use a "custom" equivalence to force MapMaker to make a MapMakerInternalMap.
        ConcurrentMap<K, V> newMap = new MapMaker().keyEquivalence(Equivalence.equals()).makeMap();
        checkState(newMap instanceof MapMakerInternalMap);
        newMap.putAll(map);
        return newMap;
      }
    },
    MapMakerStrongKeysWeakValues {
      @Override
      public <K extends Comparable<K>, V> Map<K, V> create(Map<K, V> map) {
        ConcurrentMap<K, V> newMap = new MapMaker().weakValues().makeMap();
        checkState(newMap instanceof MapMakerInternalMap);
        newMap.putAll(map);
        return newMap;
      }
    },
    MapMakerWeakKeysStrongValues {
      @Override
      public <K extends Comparable<K>, V> Map<K, V> create(Map<K, V> map) {
        ConcurrentMap<K, V> newMap = new MapMaker().weakKeys().makeMap();
        checkState(newMap instanceof MapMakerInternalMap);
        newMap.putAll(map);
        return newMap;
      }
    },
    MapMakerWeakKeysWeakValues {
      @Override
      public <K extends Comparable<K>, V> Map<K, V> create(Map<K, V> map) {
        ConcurrentMap<K, V> newMap = new MapMaker().weakKeys().weakValues().makeMap();
        checkState(newMap instanceof MapMakerInternalMap);
        newMap.putAll(map);
        return newMap;
      }
    };
  }

  enum SortedMapImpl implements MapsImplEnum {
    TreeMapImpl {
      @Override
      public <K extends Comparable<K>, V> SortedMap<K, V> create(Map<K, V> map) {
        SortedMap<K, V> result = Maps.newTreeMap();
        result.putAll(map);
        return result;
      }
    },
    ConcurrentSkipListImpl {
      @Override
      public <K extends Comparable<K>, V> SortedMap<K, V> create(Map<K, V> map) {
        return new ConcurrentSkipListMap<>(map);
      }
    },
    ImmutableSortedMapImpl {
      @Override
      public <K extends Comparable<K>, V> SortedMap<K, V> create(Map<K, V> map) {
        return ImmutableSortedMap.copyOf(map);
      }
    };
  }

  enum BiMapImpl implements MapsImplEnum {
    HashBiMapImpl {
      @Override
      public <K extends Comparable<K>, V> BiMap<K, V> create(Map<K, V> map) {
        return HashBiMap.create(map);
      }
    },
    ImmutableBiMapImpl {
      @Override
      public <K extends Comparable<K>, V> BiMap<K, V> create(Map<K, V> map) {
        return ImmutableBiMap.copyOf(map);
      }
    };

    @Override
    public abstract <K extends Comparable<K>, V> BiMap<K, V> create(Map<K, V> map);
  }

  enum MultisetImpl implements CollectionsImplEnum {
    HashMultisetImpl {
      @Override
      public <E extends Comparable<E>> Multiset<E> create(Collection<E> contents) {
        return HashMultiset.create(contents);
      }
    },
    LinkedHashMultisetImpl {
      @Override
      public <E extends Comparable<E>> Multiset<E> create(Collection<E> contents) {
        return LinkedHashMultiset.create(contents);
      }
    },
    ConcurrentHashMultisetImpl {
      @Override
      public <E extends Comparable<E>> Multiset<E> create(Collection<E> contents) {
        return ConcurrentHashMultiset.create(contents);
      }
    },
    ImmutableMultisetImpl {
      @Override
      public <E extends Comparable<E>> Multiset<E> create(Collection<E> contents) {
        return ImmutableMultiset.copyOf(contents);
      }
    };
  }

  enum SortedMultisetImpl implements CollectionsImplEnum {
    TreeMultisetImpl {
      @Override
      public <E extends Comparable<E>> SortedMultiset<E> create(Collection<E> contents) {
        return TreeMultiset.create(contents);
      }
    },
    ImmutableSortedMultisetImpl {
      @Override
      public <E extends Comparable<E>> SortedMultiset<E> create(Collection<E> contents) {
        return ImmutableSortedMultiset.copyOf(contents);
      }
    };
  }

  enum QueueImpl implements CollectionsImplEnum {
    MinMaxPriorityQueueImpl {
      @Override
      public <E extends Comparable<E>> Queue<E> create(Collection<E> contents) {
        return MinMaxPriorityQueue.create(contents);
      }
    };
  }

  enum TableImpl {
    HashBasedTableImpl {
      @Override
      <R extends Comparable<R>, C extends Comparable<C>, V> Table<R, C, V> create(
          Table<R, C, V> contents) {
        return HashBasedTable.create(contents);
      }
    },
    TreeBasedTableImpl {
      @Override
      <R extends Comparable<R>, C extends Comparable<C>, V> Table<R, C, V> create(
          Table<R, C, V> contents) {
        Table<R, C, V> table = TreeBasedTable.create();
        table.putAll(contents);
        return table;
      }
    },
    ArrayTableImpl {
      @Override
      <R extends Comparable<R>, C extends Comparable<C>, V> Table<R, C, V> create(
          Table<R, C, V> contents) {
        if (contents.isEmpty()) {
          return ImmutableTable.of();
        } else {
          return ArrayTable.create(contents);
        }
      }
    },
    ImmutableTableImpl {
      @Override
      <R extends Comparable<R>, C extends Comparable<C>, V> Table<R, C, V> create(
          Table<R, C, V> contents) {
        return ImmutableTable.copyOf(contents);
      }
    };

    abstract <R extends Comparable<R>, C extends Comparable<C>, V> Table<R, C, V> create(
        Table<R, C, V> contents);
  }

  public enum InternerImpl implements InternerImplEnum {
    WeakInternerImpl {
      @Override
      public <E> Interner<E> create(Collection<E> contents) {
        Interner<E> interner = Interners.newWeakInterner();
        for (E e : contents) {
          E unused = interner.intern(e);
        }
        return interner;
      }
    },
    StrongInternerImpl {
      @Override
      public <E> Interner<E> create(Collection<E> contents) {
        Interner<E> interner = Interners.newStrongInterner();
        for (E e : contents) {
          E unused = interner.intern(e);
        }
        return interner;
      }
    };
  }

  public enum Value {
    INSTANCE;
  }

  public enum ListSizeDistribution {
    UNIFORM_0_TO_2(0, 2),
    UNIFORM_0_TO_9(0, 9),
    ALWAYS_0(0, 0),
    ALWAYS_10(10, 10);

    final int min;
    final int max;

    private ListSizeDistribution(int min, int max) {
      this.min = min;
      this.max = max;
    }

    public int chooseSize(Random random) {
      return random.nextInt(max - min + 1) + min;
    }
  }
}
