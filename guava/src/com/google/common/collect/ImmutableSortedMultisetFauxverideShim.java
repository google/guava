/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

/**
 * "Overrides" the {@link ImmutableMultiset} static methods that lack
 * {@link ImmutableSortedMultiset} equivalents with deprecated, exception-throwing versions. This
 * prevents accidents like the following:
 *
 * <pre>   {@code
 *
 *   List<Object> objects = ...;
 *   // Sort them:
 *   Set<Object> sorted = ImmutableSortedMultiset.copyOf(objects);
 *   // BAD CODE! The returned multiset is actually an unsorted ImmutableMultiset!}</pre>
 *
 * <p>While we could put the overrides in {@link ImmutableSortedMultiset} itself, it seems clearer
 * to separate these "do not call" methods from those intended for normal use.
 *
 * @author Louis Wasserman
 */
abstract class ImmutableSortedMultisetFauxverideShim<E> extends ImmutableMultiset<E> {
  /**
   * Not supported. Use {@link ImmutableSortedMultiset#naturalOrder}, which offers better
   * type-safety, instead. This method exists only to hide {@link ImmutableMultiset#builder} from
   * consumers of {@code ImmutableSortedMultiset}.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Use {@link ImmutableSortedMultiset#naturalOrder}, which offers better type-safety.
   */
  @Deprecated
  public static <E> ImmutableSortedMultiset.Builder<E> builder() {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a multiset that may contain a non-{@code
   * Comparable} element.</b> Proper calls will resolve to the version in {@code
   * ImmutableSortedMultiset}, not this dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass a parameter of type {@code Comparable} to use
   *             {@link ImmutableSortedMultiset#of(Comparable)}.</b>
   */
  @Deprecated
  public static <E> ImmutableSortedMultiset<E> of(E element) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a multiset that may contain a non-{@code
   * Comparable} element.</b> Proper calls will resolve to the version in {@code
   * ImmutableSortedMultiset}, not this dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass the parameters of type {@code Comparable} to use
   *             {@link ImmutableSortedMultiset#of(Comparable, Comparable)}.</b>
   */
  @Deprecated
  public static <E> ImmutableSortedMultiset<E> of(E e1, E e2) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a multiset that may contain a non-{@code
   * Comparable} element.</b> Proper calls will resolve to the version in {@code
   * ImmutableSortedMultiset}, not this dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass the parameters of type {@code Comparable} to use
   *             {@link ImmutableSortedMultiset#of(Comparable, Comparable, Comparable)}.</b>
   */
  @Deprecated
  public static <E> ImmutableSortedMultiset<E> of(E e1, E e2, E e3) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a multiset that may contain a non-{@code
   * Comparable} element.</b> Proper calls will resolve to the version in {@code
   * ImmutableSortedMultiset}, not this dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass the parameters of type {@code Comparable} to use {@link
   *             ImmutableSortedMultiset#of(Comparable, Comparable, Comparable, Comparable)}. </b>
   */
  @Deprecated
  public static <E> ImmutableSortedMultiset<E> of(E e1, E e2, E e3, E e4) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a multiset that may contain a non-{@code
   * Comparable} element.</b> Proper calls will resolve to the version in {@code
   * ImmutableSortedMultiset}, not this dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass the parameters of type {@code Comparable} to use {@link
   *             ImmutableSortedMultiset#of(Comparable, Comparable, Comparable, Comparable,
   *             Comparable)} . </b>
   */
  @Deprecated
  public static <E> ImmutableSortedMultiset<E> of(E e1, E e2, E e3, E e4, E e5) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a multiset that may contain a non-{@code
   * Comparable} element.</b> Proper calls will resolve to the version in {@code
   * ImmutableSortedMultiset}, not this dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass the parameters of type {@code Comparable} to use {@link
   *             ImmutableSortedMultiset#of(Comparable, Comparable, Comparable, Comparable,
   *             Comparable, Comparable, Comparable...)} . </b>
   */
  @Deprecated
  public static <E> ImmutableSortedMultiset<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E... remaining) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. <b>You are attempting to create a multiset that may contain non-{@code
   * Comparable} elements.</b> Proper calls will resolve to the version in {@code
   * ImmutableSortedMultiset}, not this dummy version.
   *
   * @throws UnsupportedOperationException always
   * @deprecated <b>Pass parameters of type {@code Comparable} to use
   *             {@link ImmutableSortedMultiset#copyOf(Comparable[])}.</b>
   */
  @Deprecated
  public static <E> ImmutableSortedMultiset<E> copyOf(E[] elements) {
    throw new UnsupportedOperationException();
  }

  /*
   * We would like to include an unsupported "<E> copyOf(Iterable<E>)" here, providing only the
   * properly typed "<E extends Comparable<E>> copyOf(Iterable<E>)" in ImmutableSortedMultiset (and
   * likewise for the Iterator equivalent). However, due to a change in Sun's interpretation of the
   * JLS (as described at http://bugs.sun.com/view_bug.do?bug_id=6182950), the OpenJDK 7 compiler
   * available as of this writing rejects our attempts. To maintain compatibility with that version
   * and with any other compilers that interpret the JLS similarly, there is no definition of
   * copyOf() here, and the definition in ImmutableSortedMultiset matches that in
   * ImmutableMultiset.
   *
   * The result is that ImmutableSortedMultiset.copyOf() may be called on non-Comparable elements.
   * We have not discovered a better solution. In retrospect, the static factory methods should
   * have gone in a separate class so that ImmutableSortedMultiset wouldn't "inherit"
   * too-permissive factory methods from ImmutableMultiset.
   */
}
