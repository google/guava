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
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * A constraint that an element must satisfy in order to be added to a
 * collection. For example, {@link Constraints#notNull()}, which prevents a
 * collection from including any null elements, could be implemented like this:
 * <pre>   {@code
 *
 *   public Object checkElement(Object element) {
 *     if (element == null) {
 *       throw new NullPointerException();
 *     }
 *     return element;
 *   }}</pre>
 *
 * <p>In order to be effective, constraints should be deterministic; that is,
 * they should not depend on state that can change (such as external state,
 * random variables, and time) and should only depend on the value of the
 * passed-in element. A non-deterministic constraint cannot reliably enforce
 * that all the collection's elements meet the constraint, since the constraint
 * is only enforced when elements are added.
 *
 * @author Mike Bostock
 */
@GwtCompatible
interface Constraint<E> {
  /**
   * Throws a suitable {@code RuntimeException} if the specified element is
   * illegal. Typically this is either a {@link NullPointerException}, an
   * {@link IllegalArgumentException}, or a {@link ClassCastException}, though
   * an application-specific exception class may be used if appropriate.
   *
   * @param element the element to check
   * @return the provided element
   */
  @CanIgnoreReturnValue
  E checkElement(E element);

  /**
   * Returns a brief human readable description of this constraint, such as
   * "Not null" or "Positive number".
   */
  @Override
  String toString();
}
