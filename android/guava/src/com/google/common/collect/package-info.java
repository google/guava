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
 * Collection interfaces and implementations, and other utilities for collections. This package is a
 * part of the open-source <a href="https://github.com/google/guava">Guava</a> library.
 *
 * <p>The classes in this package include:
 *
 * <h2>Immutable collections</h2>
 *
 * These are collections whose contents will never change. They also offer a few additional
 * guarantees (see {@link ImmutableCollection} for details). Implementations are available for both
 * the JDK collection types and the Guava collection types (listed below).
 *
 * <h2>Collection types</h2>
 *
 * <dl>
 *   <dt>{@link Multimap}
 *   <dd>A new type, which is similar to {@link java.util.Map}, but may contain multiple entries
 *       with the same key. Some behaviors of {@link Multimap} are left unspecified and are provided
 *       only by the subtypes mentioned below.
 *   <dt>{@link ListMultimap}
 *   <dd>An extension of {@link Multimap} which permits duplicate entries, supports random access of
 *       values for a particular key, and has <i>partially order-dependent equality</i> as defined
 *       by {@link ListMultimap#equals(Object)}. {@code ListMultimap} takes its name from the fact
 *       that the {@linkplain ListMultimap#get collection of values} associated with a given key
 *       fulfills the {@link java.util.List} contract.
 *   <dt>{@link SetMultimap}
 *   <dd>An extension of {@link Multimap} which has order-independent equality and does not allow
 *       duplicate entries; that is, while a key may appear twice in a {@code SetMultimap}, each
 *       must map to a different value. {@code SetMultimap} takes its name from the fact that the
 *       {@linkplain SetMultimap#get collection of values} associated with a given key fulfills the
 *       {@link java.util.Set} contract.
 *   <dt>{@link SortedSetMultimap}
 *   <dd>An extension of {@link SetMultimap} for which the {@linkplain SortedSetMultimap#get
 *       collection values} associated with a given key is a {@link java.util.SortedSet}.
 *   <dt>{@link BiMap}
 *   <dd>An extension of {@link java.util.Map} that guarantees the uniqueness of its values as well
 *       as that of its keys. This is sometimes called an "invertible map," since the restriction on
 *       values enables it to support an {@linkplain BiMap#inverse inverse view} -- which is another
 *       instance of {@code BiMap}.
 *   <dt>{@link Table}
 *   <dd>A new type, which is similar to {@link java.util.Map}, but which indexes its values by an
 *       ordered pair of keys, a row key and column key.
 *   <dt>{@link Multiset}
 *   <dd>An extension of {@link java.util.Collection} that may contain duplicate values like a
 *       {@link java.util.List}, yet has order-independent equality like a {@link java.util.Set}.
 *       One typical use for a multiset is to represent a histogram.
 *   <dt>{@link ClassToInstanceMap}
 *   <dd>An extension of {@link java.util.Map} that associates a raw type with an instance of that
 *       type.
 * </dl>
 *
 * <h2>Ranges</h2>
 *
 * <ul>
 *   <li>{@link Range}
 *   <li>{@link RangeMap}
 *   <li>{@link RangeSet}
 *   <li>{@link DiscreteDomain}
 *   <li>{@link ContiguousSet}
 * </ul>
 *
 * <h2>Classes of static utility methods</h2>
 *
 * <ul>
 *   <li>{@link Collections2}
 *   <li>{@link Comparators}
 *   <li>{@link Iterables}
 *   <li>{@link Iterators}
 *   <li>{@link Lists}
 *   <li>{@link Maps}
 *   <li>{@link MoreCollectors}
 *   <li>{@link Multimaps}
 *   <li>{@link Multisets}
 *   <li>{@link ObjectArrays}
 *   <li>{@link Queues}
 *   <li>{@link Sets}
 *   <li>{@link Streams}
 *   <li>{@link Tables}
 * </ul>
 *
 * <h2>Abstract implementations</h2>
 *
 * <ul>
 *   <li>{@link AbstractIterator}
 *   <li>{@link AbstractSequentialIterator}
 *   <li>{@link UnmodifiableIterator}
 *   <li>{@link UnmodifiableListIterator}
 * </ul>
 *
 * <h2>Forwarding collections</h2>
 *
 * We provide implementations of collections that forward all method calls to a delegate collection
 * by default. Subclasses can override one or more methods to implement the decorator pattern. For
 * an example, see {@link ForwardingCollection}.
 *
 * <h2>Other</h2>
 *
 * <ul>
 *   <li>{@link EvictingQueue}
 *   <li>{@link Interner}, {@link Interners}
 *   <li>{@link MapMaker}
 *   <li>{@link MinMaxPriorityQueue}
 *   <li>{@link PeekingIterator}
 * </ul>
 */
@CheckReturnValue
@ParametersAreNonnullByDefault
package com.google.common.collect;

import com.google.errorprone.annotations.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
