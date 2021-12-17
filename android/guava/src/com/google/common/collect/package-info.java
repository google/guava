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

/**
 * This package contains generic collection interfaces and implementations, and other utilities for
 * working with collections. It is a part of the open-source <a
 * href="http://github.com/google/guava">Guava</a> library.
 *
 * <h2>Collection Types</h2>
 *
 * <dl>
 *   <dt>{@link com.google.common.collect.BiMap}
 *   <dd>An extension of {@link java.util.Map} that guarantees the uniqueness of its values as well
 *       as that of its keys. This is sometimes called an "invertible map," since the restriction on
 *       values enables it to support an {@linkplain com.google.common.collect.BiMap#inverse inverse
 *       view} -- which is another instance of {@code BiMap}.
 *   <dt>{@link com.google.common.collect.Multiset}
 *   <dd>An extension of {@link java.util.Collection} that may contain duplicate values like a
 *       {@link java.util.List}, yet has order-independent equality like a {@link java.util.Set}.
 *       One typical use for a multiset is to represent a histogram.
 *   <dt>{@link com.google.common.collect.Multimap}
 *   <dd>A new type, which is similar to {@link java.util.Map}, but may contain multiple entries
 *       with the same key. Some behaviors of {@link com.google.common.collect.Multimap} are left
 *       unspecified and are provided only by the subtypes mentioned below.
 *   <dt>{@link com.google.common.collect.ListMultimap}
 *   <dd>An extension of {@link com.google.common.collect.Multimap} which permits duplicate entries,
 *       supports random access of values for a particular key, and has <i>partially order-dependent
 *       equality</i> as defined by {@link com.google.common.collect.ListMultimap#equals(Object)}.
 *       {@code ListMultimap} takes its name from the fact that the {@linkplain
 *       com.google.common.collect.ListMultimap#get collection of values} associated with a given
 *       key fulfills the {@link java.util.List} contract.
 *   <dt>{@link com.google.common.collect.SetMultimap}
 *   <dd>An extension of {@link com.google.common.collect.Multimap} which has order-independent
 *       equality and does not allow duplicate entries; that is, while a key may appear twice in a
 *       {@code SetMultimap}, each must map to a different value. {@code SetMultimap} takes its name
 *       from the fact that the {@linkplain com.google.common.collect.SetMultimap#get collection of
 *       values} associated with a given key fulfills the {@link java.util.Set} contract.
 *   <dt>{@link com.google.common.collect.SortedSetMultimap}
 *   <dd>An extension of {@link com.google.common.collect.SetMultimap} for which the {@linkplain
 *       com.google.common.collect.SortedSetMultimap#get collection values} associated with a given
 *       key is a {@link java.util.SortedSet}.
 *   <dt>{@link com.google.common.collect.Table}
 *   <dd>A new type, which is similar to {@link java.util.Map}, but which indexes its values by an
 *       ordered pair of keys, a row key and column key.
 *   <dt>{@link com.google.common.collect.ClassToInstanceMap}
 *   <dd>An extension of {@link java.util.Map} that associates a raw type with an instance of that
 *       type.
 * </dl>
 *
 * <h2>Collection Implementations</h2>
 *
 * <h3>of {@link java.util.List}</h3>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.ImmutableList}
 * </ul>
 *
 * <h3>of {@link java.util.Set}</h3>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.ImmutableSet}
 *   <li>{@link com.google.common.collect.ImmutableSortedSet}
 *   <li>{@link com.google.common.collect.ContiguousSet} (see {@code Range})
 * </ul>
 *
 * <h3>of {@link java.util.Map}</h3>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.ImmutableMap}
 *   <li>{@link com.google.common.collect.ImmutableSortedMap}
 *   <li>{@link com.google.common.collect.MapMaker}
 * </ul>
 *
 * <h3>of {@link com.google.common.collect.BiMap}</h3>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.ImmutableBiMap}
 *   <li>{@link com.google.common.collect.HashBiMap}
 *   <li>{@link com.google.common.collect.EnumBiMap}
 *   <li>{@link com.google.common.collect.EnumHashBiMap}
 * </ul>
 *
 * <h3>of {@link com.google.common.collect.Multiset}</h3>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.ImmutableMultiset}
 *   <li>{@link com.google.common.collect.ImmutableSortedMultiset}
 *   <li>{@link com.google.common.collect.HashMultiset}
 *   <li>{@link com.google.common.collect.LinkedHashMultiset}
 *   <li>{@link com.google.common.collect.TreeMultiset}
 *   <li>{@link com.google.common.collect.EnumMultiset}
 *   <li>{@link com.google.common.collect.ConcurrentHashMultiset}
 * </ul>
 *
 * <h3>of {@link com.google.common.collect.Multimap}</h3>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.ImmutableMultimap}
 *   <li>{@link com.google.common.collect.ImmutableListMultimap}
 *   <li>{@link com.google.common.collect.ImmutableSetMultimap}
 *   <li>{@link com.google.common.collect.ArrayListMultimap}
 *   <li>{@link com.google.common.collect.HashMultimap}
 *   <li>{@link com.google.common.collect.TreeMultimap}
 *   <li>{@link com.google.common.collect.LinkedHashMultimap}
 *   <li>{@link com.google.common.collect.LinkedListMultimap}
 * </ul>
 *
 * <h3>of {@link com.google.common.collect.Table}</h3>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.ImmutableTable}
 *   <li>{@link com.google.common.collect.ArrayTable}
 *   <li>{@link com.google.common.collect.HashBasedTable}
 *   <li>{@link com.google.common.collect.TreeBasedTable}
 * </ul>
 *
 * <h3>of {@link com.google.common.collect.ClassToInstanceMap}</h3>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.ImmutableClassToInstanceMap}
 *   <li>{@link com.google.common.collect.MutableClassToInstanceMap}
 * </ul>
 *
 * <h2>Classes of static utility methods</h2>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.Collections2}
 *   <li>{@link com.google.common.collect.Iterators}
 *   <li>{@link com.google.common.collect.Iterables}
 *   <li>{@link com.google.common.collect.Lists}
 *   <li>{@link com.google.common.collect.Maps}
 *   <li>{@link com.google.common.collect.Queues}
 *   <li>{@link com.google.common.collect.Sets}
 *   <li>{@link com.google.common.collect.Multisets}
 *   <li>{@link com.google.common.collect.Multimaps}
 *   <li>{@link com.google.common.collect.Tables}
 *   <li>{@link com.google.common.collect.ObjectArrays}
 * </ul>
 *
 * <h2>Comparison</h2>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.Ordering}
 *   <li>{@link com.google.common.collect.ComparisonChain}
 * </ul>
 *
 * <h2>Abstract implementations</h2>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.AbstractIterator}
 *   <li>{@link com.google.common.collect.AbstractSequentialIterator}
 *   <li>{@link com.google.common.collect.ImmutableCollection}
 *   <li>{@link com.google.common.collect.UnmodifiableIterator}
 *   <li>{@link com.google.common.collect.UnmodifiableListIterator}
 * </ul>
 *
 * <h2>Ranges</h2>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.Range}
 *   <li>{@link com.google.common.collect.RangeMap}
 *   <li>{@link com.google.common.collect.DiscreteDomain}
 *   <li>{@link com.google.common.collect.ContiguousSet}
 * </ul>
 *
 * <h2>Other</h2>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.Interner}, {@link com.google.common.collect.Interners}
 *   <li>{@link com.google.common.collect.MapDifference}, {@link
 *       com.google.common.collect.SortedMapDifference}
 *   <li>{@link com.google.common.collect.MinMaxPriorityQueue}
 *   <li>{@link com.google.common.collect.PeekingIterator}
 * </ul>
 *
 * <h2>Forwarding collections</h2>
 *
 * <ul>
 *   <li>{@link com.google.common.collect.ForwardingCollection}
 *   <li>{@link com.google.common.collect.ForwardingConcurrentMap}
 *   <li>{@link com.google.common.collect.ForwardingIterator}
 *   <li>{@link com.google.common.collect.ForwardingList}
 *   <li>{@link com.google.common.collect.ForwardingListIterator}
 *   <li>{@link com.google.common.collect.ForwardingListMultimap}
 *   <li>{@link com.google.common.collect.ForwardingMap}
 *   <li>{@link com.google.common.collect.ForwardingMapEntry}
 *   <li>{@link com.google.common.collect.ForwardingMultimap}
 *   <li>{@link com.google.common.collect.ForwardingMultiset}
 *   <li>{@link com.google.common.collect.ForwardingNavigableMap}
 *   <li>{@link com.google.common.collect.ForwardingNavigableSet}
 *   <li>{@link com.google.common.collect.ForwardingObject}
 *   <li>{@link com.google.common.collect.ForwardingQueue}
 *   <li>{@link com.google.common.collect.ForwardingSet}
 *   <li>{@link com.google.common.collect.ForwardingSetMultimap}
 *   <li>{@link com.google.common.collect.ForwardingSortedMap}
 *   <li>{@link com.google.common.collect.ForwardingSortedMultiset}
 *   <li>{@link com.google.common.collect.ForwardingSortedSet}
 *   <li>{@link com.google.common.collect.ForwardingSortedSetMultimap}
 *   <li>{@link com.google.common.collect.ForwardingTable}
 * </ul>
 */
@CheckReturnValue
@ParametersAreNonnullByDefault
package com.google.common.collect;

import com.google.errorprone.annotations.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
