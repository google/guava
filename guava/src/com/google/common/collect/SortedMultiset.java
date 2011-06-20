// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.collect;

import com.google.common.annotations.Beta;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * A {@link Multiset} which maintains the ordering of its elements, according to
 * either their natural order or an explicit {@link Comparator}. In all cases,
 * this implementation uses {@link Comparable#compareTo} or
 * {@link Comparator#compare} instead of {@link Object#equals} to determine
 * equivalence of instances.
 *
 * <p><b>Warning:</b> The comparison must be <i>consistent with equals</i> as
 * explained by the {@link Comparable} class specification. Otherwise, the
 * resulting multiset will violate the {@link Collection} contract, which it is
 * specified in terms of {@link Object#equals}.
 *
 * @author Louis Wasserman
 */
@Beta
interface SortedMultiset<E> extends Multiset<E> {
  /**
   * Returns the comparator that orders this multiset, or
   * {@link Ordering#natural()} if the natural ordering of the elements is used.
   */
  Comparator<? super E> comparator();

  /**
   * Returns the greatest element in this multiset that is strictly less than
   * the specified element, or {@code null} if there is no such element.
   */
  E lower(E e);

  /**
   * Returns the greatest element in this multiset that is less than or equal to
   * the specified element, or {@code null} if there is no such element.
   */
  E floor(E e);

  /**
   * Returns the lowest element in this multiset that is greater than or equal
   * to the specified element, or {@code null} if there is no such element.
   */
  E ceiling(E e);

  /**
   * Returns the lowest element in this multiset that is strictly greater than
   * the specified element, or {@code null} if there is no such element.
   */
  E higher(E e);

  /**
   * Returns the entry of the greatest element in this multiset that is strictly
   * less than the specified element, or {@code null} if there is no such
   * element.
   *
   * <p>Equivalent to {@code headMultiset(e, false).lastEntry()}.
   */
  Entry<E> lowerEntry(E e);

  /**
   * Returns the entry of the greatest element in this multiset that is less
   * than or equal to the specified element, or {@code null} if there is no such
   * element.
   * <p>Equivalent to {@code headMultiset(e, true).lastEntry()}.
   */
  Entry<E> floorEntry(E e);

  /**
   * Returns the entry of the lowest element in this multiset that is greater
   * than or equal to the specified element, or {@code null} if there is no such
   * element.
   * <p>Equivalent to {@code tailMultiset(e, true).firstEntry()}.
   */
  Entry<E> ceilingEntry(E e);

  /**
   * Returns the entry of the lowest element in this multiset that is strictly
   * greater than the specified element, or {@code null} if there is no such
   * element.
   * <p>Equivalent to {@code tailMultiset(e, false).firstEntry()}.
   */
  Entry<E> higherEntry(E e);

  /**
   * Returns the first element in this multiset. Equivalent to {@code
   * elementSet().first()}.
   *
   * @throws NoSuchElementException if this multiset is empty
   */
  E first();

  /**
   * Returns the last element in this multiset. Equivalent to {@code
   * elementSet().last()}.
   *
   * @throws NoSuchElementException if this multiset is empty
   */
  E last();

  /**
   * Returns the entry of the first element in this multiset, or {@code null} if
   * this multiset is empty.
   */
  Entry<E> firstEntry();

  /**
   * Returns the entry of the last element in this multiset, or {@code null} if
   * this multiset is empty.
   */
  Entry<E> lastEntry();

  /**
   * Returns and removes the entry associated with the lowest element in this
   * multiset, or returns {@code null} if this multiset is empty.
   */
  Entry<E> pollFirstEntry();

  /**
   * Returns and removes the entry associated with the greatest element in this
   * multiset, or returns {@code null} if this multiset is empty.
   */
  Entry<E> pollLastEntry();

  /**
   * Returns a {@link SortedSet} view of the distinct elements in this multiset.
   */
  @Override SortedSet<E> elementSet();

  /**
   * Returns a descending view of the distinct elements in this multiset.
   */
  SortedSet<E> descendingElementSet();

  /**
   * Returns an iterator over this multiset in descending order.
   */
  Iterator<E> descendingIterator();

  /**
   * Returns a descending view of this multiset. Modifications made to either
   * map will be reflected in the other.
   */
  SortedMultiset<E> descendingMultiset();

  /**
   * Equivalent to {@code headMultiset(toElement, false)}.
   */
  SortedMultiset<E> headMultiset(E toElement);

  /**
   * Returns a view of this multiset restricted to the elements less than (or
   * equal to, if {@code inclusive}) {@code toElement}. The returned multiset is
   * a view of this multiset, so changes to one will be reflected in the other.
   * The returned multiset supports all operations that this multiset supports.
   *
   * <p>The returned multiset will throw an {@link IllegalArgumentException} on
   * attempts to add elements outside its range.
   */
  SortedMultiset<E> headMultiset(E toElement, boolean inclusive);

  /**
   * Returns a view of this multiset restricted to the range between {@code
   * fromElement} and {@code toElement}. The returned multiset is a view of this
   * multiset, so changes to one will be reflected in the other. The returned
   * multiset supports all operations that this multiset supports.
   *
   * <p>The returned multiset will throw an {@link IllegalArgumentException} on
   * attempts to add elements outside its range.
   * 
   * @throws IllegalArgumentException if {@code fromElement} is greater than
   *         {@code toElement}
   */
  SortedMultiset<E> subMultiset(
      E fromElement, boolean fromInclusive, E toElement, boolean toInclusive);

  /**
   * Equivalent to {@code subMultiset(fromElement, true, toElement, false)}.
   */
  SortedMultiset<E> subMultiset(E fromElement, E toElement);

  /**
   * Equivalent to {@code tailMultiset(fromElement, true)}.
   */
  SortedMultiset<E> tailMultiset(E fromElement);

  /**
   * Returns a view of this multiset restricted to the elements greater than (or
   * equal to, if {@code inclusive}) {@code fromElement}. The returned multiset
   * is a view of this multiset, so changes to one will be reflected in the
   * other. The returned multiset supports all operations that this multiset
   * supports.
   *
   * <p>The returned multiset will throw an {@link IllegalArgumentException} on
   * attempts to add elements outside its range.
   */
  SortedMultiset<E> tailMultiset(E fromElement, boolean inclusive);
}
