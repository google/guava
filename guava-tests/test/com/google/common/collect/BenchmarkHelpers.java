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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper classes for various benchmarks.
 *
 * @author Christopher Swenson
 */
final class BenchmarkHelpers {
  /**
   * So far, this is the best way to test various implementations of {@link Set} subclasses.
   */
  public enum SetImpl {
    Hash {
      @Override <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return new HashSet<E>(contents);
      }
    },
    LinkedHash {
      @Override <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return new LinkedHashSet<E>(contents);
      }
    },
    Tree {
      @Override <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return new TreeSet<E>(contents);
      }
    },
    Unmodifiable {
      @Override <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return Collections.unmodifiableSet(new HashSet<E>(contents));
      }
    },
    Synchronized {
      @Override <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return Collections.synchronizedSet(new HashSet<E>(contents));
      }
    },
    Immutable {
      @Override <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return ImmutableSet.copyOf(contents);
      }
    },
    ImmutableSorted {
      @Override <E extends Comparable<E>> Set<E> create(Collection<E> contents) {
        return ImmutableSortedSet.copyOf(contents);
      }
    },
    ;

    abstract <E extends Comparable<E>> Set<E> create(Collection<E> contents);
  }

  public enum ListMultimapImpl {
    ArrayList {
      @Override
      <K, V> ListMultimap<K, V> create(Multimap<K, V> contents) {
        return ArrayListMultimap.create(contents);
      }
    },
    LinkedList {
      @Override
      <K, V> ListMultimap<K, V> create(Multimap<K, V> contents) {
        return LinkedListMultimap.create(contents);
      }
    },
    ImmutableList {
      @Override
      <K, V> ListMultimap<K, V> create(Multimap<K, V> contents) {
        return ImmutableListMultimap.copyOf(contents);
      }
    };

    abstract <K, V> ListMultimap<K, V> create(Multimap<K, V> contents);
  }

  public enum SetMultimapImpl {
    Hash {
      @Override
      <K extends Comparable<K>, V extends Comparable<V>> SetMultimap<K, V> create(
          Multimap<K, V> contents) {
        return HashMultimap.create(contents);
      }
    },
    LinkedHash {
      @Override
      <K extends Comparable<K>, V extends Comparable<V>> SetMultimap<K, V> create(
          Multimap<K, V> contents) {
        return LinkedHashMultimap.create(contents);
      }
    },
    Tree {
      @Override
      <K extends Comparable<K>, V extends Comparable<V>> SetMultimap<K, V> create(
          Multimap<K, V> contents) {
        return TreeMultimap.create(contents);
      }
    },
    ImmutableSet {
      @Override
      <K extends Comparable<K>, V extends Comparable<V>> SetMultimap<K, V> create(
          Multimap<K, V> contents) {
        return ImmutableSetMultimap.copyOf(contents);
      }
    };

    abstract <K extends Comparable<K>, V extends Comparable<V>> SetMultimap<K, V> create(
        Multimap<K, V> contents);
  }

  public enum MapImpl {
    Hash {
      @Override
      <K, V> Map<K, V> create(Map<K, V> map) {
        return Maps.newHashMap(map);
      }
    },
    LinkedHash {
      @Override
      <K, V> Map<K, V> create(Map<K, V> map) {
        return Maps.newLinkedHashMap(map);
      }
    },
    ConcurrentHash {
      @Override
      <K, V> Map<K, V> create(Map<K, V> map) {
        return new ConcurrentHashMap<K, V>(map);
      }
    },
    Immutable {
      @Override
      <K, V> Map<K, V> create(Map<K, V> map) {
        return ImmutableMap.copyOf(map);
      }
    };

    abstract <K, V> Map<K, V> create(Map<K, V> map);
  }

  enum SortedMapImpl {
    Tree {
      @Override
      <K extends Comparable<K>, V> SortedMap<K, V> create(Map<K, V> map) {
        SortedMap<K, V> result = Maps.newTreeMap();
        result.putAll(map);
        return result;
      }
    },
    ImmutableSorted {
      @Override
      <K extends Comparable<K>, V> SortedMap<K, V> create(Map<K, V> map) {
        return ImmutableSortedMap.copyOf(map);
      }
    };

    abstract <K extends Comparable<K>, V> SortedMap<K, V> create(Map<K, V> map);
  }

  enum BiMapImpl {
    Hash{
      @Override
      <K, V> BiMap<K, V> create(BiMap<K, V> map) {
        return HashBiMap.create(map);
      }
    },
    Immutable {
      @Override
      <K, V> BiMap<K, V> create(BiMap<K, V> map) {
        return ImmutableBiMap.copyOf(map);
      }
    };

    abstract <K, V> BiMap<K, V> create(BiMap<K, V> map);
  }

  enum MultisetImpl {
    Hash {
      @Override
      <E> Multiset<E> create(Multiset<E> contents) {
        return HashMultiset.create(contents);
      }
    },
    LinkedHash {
      @Override
      <E> Multiset<E> create(Multiset<E> contents) {
        return LinkedHashMultiset.create(contents);
      }
    },
    ConcurrentHash {
      @Override
      <E> Multiset<E> create(Multiset<E> contents) {
        return ConcurrentHashMultiset.create(contents);
      }
    },
    Immutable {
      @Override
      <E> Multiset<E> create(Multiset<E> contents) {
        return ImmutableMultiset.copyOf(contents);
      }
    };

    abstract <E> Multiset<E> create(Multiset<E> contents);
  }

  enum SortedMultisetImpl {
    Tree {
      @Override
      <E extends Comparable<E>> SortedMultiset<E> create(Multiset<E> contents) {
        return TreeMultiset.create(contents);
      }
    },
    ImmutableSorted {
      @Override
      <E extends Comparable<E>> SortedMultiset<E> create(Multiset<E> contents) {
        return ImmutableSortedMultiset.copyOf(contents);
      }
    };

    abstract <E extends Comparable<E>> SortedMultiset<E> create(Multiset<E> contents);
  }

  enum TableImpl {
    HashBased {
      @Override
      <R extends Comparable<R>, C extends Comparable<C>, V> Table<R, C, V> create(
          Table<R, C, V> contents) {
        return HashBasedTable.create(contents);
      }
    },
    TreeBased {
      @Override
      <R extends Comparable<R>, C extends Comparable<C>, V> Table<R, C, V> create(
          Table<R, C, V> contents) {
        Table<R, C, V> table = TreeBasedTable.create();
        table.putAll(contents);
        return table;
      }
    },
    Array {
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
    Immutable {
      @Override
      <R extends Comparable<R>, C extends Comparable<C>, V> Table<R, C, V> create(
          Table<R, C, V> contents) {
        return ImmutableTable.copyOf(contents);
      }
    };

    abstract <R extends Comparable<R>, C extends Comparable<C>, V>
        Table<R, C, V> create(Table<R, C, V> contents);
  }

  public enum Value {
    INSTANCE;
  }

  public enum ListSizeDistribution {
    UNIFORM_0_TO_2(0, 2), UNIFORM_0_TO_9(0, 9), ALWAYS_0(0, 0), ALWAYS_10(10, 10);

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
