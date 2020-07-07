/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Contains dummy collection implementations to convince GWT that part of serializing a collection
 * is serializing its elements.
 *
 * <p>Because of our use of final fields in our collections, GWT's normal heuristic for determining
 * which classes might be serialized fails. That heuristic is, roughly speaking, to look at each
 * parameter and return type of each RPC interface and to assume that implementations of those types
 * might be serialized. Those types have their own dependencies -- their fields -- which are
 * analyzed recursively and analogously.
 *
 * <p>For classes with final fields, GWT assumes that the class itself might be serialized but
 * doesn't assume the same about its final fields. To work around this, we provide dummy
 * implementations of our collections with their dependencies as non-final fields. Even though these
 * implementations are never instantiated, they are visible to GWT when it performs its
 * serialization analysis, and it assumes that their fields may be serialized.
 *
 * <p>Currently we provide dummy implementations of all the immutable collection classes necessary
 * to support declarations like {@code ImmutableMultiset<String>} in RPC interfaces. Support for
 * {@code ImmutableMultiset} in the interface is support for {@code Multiset}, so there is nothing
 * further to be done to support the new collection interfaces. It is not support, however, for an
 * RPC interface in terms of {@code HashMultiset}. It is still possible to send a {@code
 * HashMultiset} over GWT RPC; it is only the declaration of an interface in terms of {@code
 * HashMultiset} that we haven't tried to support. (We may wish to revisit this decision in the
 * future.)
 *
 * @author Chris Povirk
 */
@GwtCompatible
// None of these classes are instantiated, let alone serialized:
@SuppressWarnings("serial")
final class GwtSerializationDependencies {
  private GwtSerializationDependencies() {}

  static final class ImmutableListMultimapDependencies<K, V> extends ImmutableListMultimap<K, V> {
    K key;
    V value;

    ImmutableListMultimapDependencies() {
      super(null, 0);
    }
  }

  // ImmutableMap is covered by ImmutableSortedMap/ImmutableBiMap.

  // ImmutableMultimap is covered by ImmutableSetMultimap/ImmutableListMultimap.

  static final class ImmutableSetMultimapDependencies<K, V> extends ImmutableSetMultimap<K, V> {
    K key;
    V value;

    ImmutableSetMultimapDependencies() {
      super(null, 0, null);
    }
  }

  /*
   * We support an interface declared in terms of LinkedListMultimap because it
   * supports entry ordering not supported by other implementations.
   */
  static final class LinkedListMultimapDependencies<K, V> extends LinkedListMultimap<K, V> {
    K key;
    V value;

    LinkedListMultimapDependencies() {}
  }

  static final class HashBasedTableDependencies<R, C, V> extends HashBasedTable<R, C, V> {
    HashMap<R, HashMap<C, V>> data;

    HashBasedTableDependencies() {
      super(null, null);
    }
  }

  static final class TreeBasedTableDependencies<R, C, V> extends TreeBasedTable<R, C, V> {
    TreeMap<R, TreeMap<C, V>> data;

    TreeBasedTableDependencies() {
      super(null, null);
    }
  }

  /*
   * We don't normally need "implements Serializable," but we do here. That's
   * because ImmutableTable itself is not Serializable as of this writing. We
   * need for GWT to believe that this dummy class is serializable, or else it
   * won't generate serialization code for R, C, and V.
   */
  static final class ImmutableTableDependencies<R, C, V> extends SingletonImmutableTable<R, C, V>
      implements Serializable {
    R rowKey;
    C columnKey;
    V value;

    ImmutableTableDependencies() {
      super(null, null, null);
    }
  }

  static final class TreeMultimapDependencies<K, V> extends TreeMultimap<K, V> {
    Comparator<? super K> keyComparator;
    Comparator<? super V> valueComparator;
    K key;
    V value;

    TreeMultimapDependencies() {
      super(null, null);
    }
  }
}
