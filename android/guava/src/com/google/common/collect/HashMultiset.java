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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Multiset implementation that uses hashing for key and entry access.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
@ElementTypesAreNonnullByDefault
public final class HashMultiset<E extends @Nullable Object> extends AbstractMapBasedMultiset<E> {

  /** Creates a new, empty {@code HashMultiset} using the default initial capacity. */
  public static <E extends @Nullable Object> HashMultiset<E> create() {
    return create(ObjectCountHashMap.DEFAULT_SIZE);
  }

  /**
   * Creates a new, empty {@code HashMultiset} with the specified expected number of distinct
   * elements.
   *
   * @param distinctElements the expected number of distinct elements
   * @throws IllegalArgumentException if {@code distinctElements} is negative
   */
  public static <E extends @Nullable Object> HashMultiset<E> create(int distinctElements) {
    return new HashMultiset<>(distinctElements);
  }

  /**
   * Creates a new {@code HashMultiset} containing the specified elements.
   *
   * <p>This implementation is highly efficient when {@code elements} is itself a {@link Multiset}.
   *
   * @param elements the elements that the multiset should contain
   */
  public static <E extends @Nullable Object> HashMultiset<E> create(
      Iterable<? extends E> elements) {
    HashMultiset<E> multiset = create(Multisets.inferDistinctElements(elements));
    Iterables.addAll(multiset, elements);
    return multiset;
  }

  HashMultiset(int distinctElements) {
    super(distinctElements);
  }

  @Override
  ObjectCountHashMap<E> newBackingMap(int distinctElements) {
    return new ObjectCountHashMap<>(distinctElements);
  }

  @GwtIncompatible // Not needed in emulated source.
  @J2ktIncompatible
  private static final long serialVersionUID = 0;
}
