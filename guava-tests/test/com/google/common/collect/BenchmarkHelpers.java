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
import java.util.Set;
import java.util.TreeSet;

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

  public enum Value {
    INSTANCE;
  }
}
