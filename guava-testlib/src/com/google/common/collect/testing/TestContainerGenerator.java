/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtCompatible;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * To be implemented by test generators of things that can contain elements. Such things include
 * both {@link Collection} and {@link Map}; since there isn't an established collective noun that
 * encompasses both of these, 'container' is used.
 *
 * @author George van den Driessche
 */
@GwtCompatible
public interface TestContainerGenerator<T, E> {
  /** Returns the sample elements that this generate populates its container with. */
  SampleElements<E> samples();

  /**
   * Creates a new container containing the given elements. TODO: would be nice to figure out how to
   * use E... or E[] as a parameter type, but this doesn't seem to work because Java creates an
   * array of the erased type.
   */
  T create(Object... elements);

  /**
   * Helper method to create an array of the appropriate type used by this generator. The returned
   * array will contain only nulls.
   */
  E[] createArray(int length);

  /**
   * Returns the iteration ordering of elements, given the order in which they were added to the
   * container. This method may return the original list unchanged, the original list modified in
   * place, or a different list.
   *
   * <p>If the order is non-deterministic, as with {@link java.util.HashSet}, this method can return
   * its input unmodified. Provided that the test suite is built without {@link
   * com.google.common.collect.testing.features.CollectionFeature#KNOWN_ORDER}, the tests will look
   * only at the returned contents without regard for order.
   */
  Iterable<E> order(List<E> insertionOrder);
}
