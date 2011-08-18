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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * An immutable {@code SortedMultiset} that stores its elements in a sorted array. Some instances
 * are ordered by an explicit comparator, while others follow the natural sort ordering of their
 * elements. Either way, null elements are not supported.
 *
 * <p>Unlike {@link Multisets#unmodifiableSortedMultiset}, which is a <i>view</i> of a separate
 * collection that can still change, an instance of {@code ImmutableSortedMultiset} contains its
 * own private data and will <i>never</i> change. This class is convenient for {@code public static
 * final} multisets ("constant multisets") and also lets you easily make a "defensive copy" of a
 * set provided to your class by a caller.
 *
 * <p>The multisets returned by the {@link #headMultiset}, {@link #tailMultiset}, and
 * {@link #subMultiset} methods share the same array as the original multiset, preventing that
 * array from being garbage collected. If this is a concern, the data may be copied into a
 * correctly-sized array by calling {@link #copyOfSorted}.
 *
 * <p><b>Note on element equivalence:</b> The {@link #contains(Object)},
 * {@link #containsAll(Collection)}, and {@link #equals(Object)} implementations must check whether
 * a provided object is equivalent to an element in the collection. Unlike most collections, an
 * {@code ImmutableSortedMultiset} doesn't use {@link Object#equals} to determine if two elements
 * are equivalent. Instead, with an explicit comparator, the following relation determines whether
 * elements {@code x} and {@code y} are equivalent:
 *
 * <pre>   {@code
 *
 *   {(x, y) | comparator.compare(x, y) == 0}}</pre>
 *
 * With natural ordering of elements, the following relation determines whether two elements are
 * equivalent:
 *
 * <pre>   {@code
 *
 *   {(x, y) | x.compareTo(y) == 0}}</pre>
 *
 *  <b>Warning:</b> Like most multisets, an {@code ImmutableSortedMultiset} will not function
 * correctly if an element is modified after being placed in the multiset. For this reason, and to
 * avoid general confusion, it is strongly recommended to place only immutable objects into this
 * collection.
 *
 * <p><b>Note:</b> Although this class is not final, it cannot be subclassed as it has no public or
 * protected constructors. Thus, instances of this type are guaranteed to be immutable.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible("hasn't been tested yet")
abstract class ImmutableSortedMultiset<E> extends ImmutableSortedMultisetFauxverideShim<E>
    implements SortedMultiset<E> {
  // TODO(user): GWT compatibility

  private static final Comparator<Comparable> NATURAL_ORDER = Ordering.natural();

  private static final ImmutableSortedMultiset<Comparable> NATURAL_EMPTY_MULTISET =
      new EmptyImmutableSortedMultiset<Comparable>(NATURAL_ORDER);

  /**
   * Returns the empty immutable sorted multiset.
   */
  @SuppressWarnings("unchecked")
  public static <E> ImmutableSortedMultiset<E> of() {
    return (ImmutableSortedMultiset) NATURAL_EMPTY_MULTISET;
  }

  /**
   * Returns an immutable sorted multiset containing a single element.
   */
  public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(E element) {
    return RegularImmutableSortedMultiset.createFromSorted(
        NATURAL_ORDER, ImmutableList.of(Multisets.immutableEntry(checkNotNull(element), 1)));
  }

  /**
   * Returns an immutable sorted multiset containing the given elements sorted by their natural
   * ordering.
   *
   * @throws NullPointerException if any element is null
   */
  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(E e1, E e2) {
    return copyOf(Ordering.natural(), Arrays.asList(e1, e2));
  }

  /**
   * Returns an immutable sorted multiset containing the given elements sorted by their natural
   * ordering.
   *
   * @throws NullPointerException if any element is null
   */
  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(E e1, E e2, E e3) {
    return copyOf(Ordering.natural(), Arrays.asList(e1, e2, e3));
  }

  /**
   * Returns an immutable sorted multiset containing the given elements sorted by their natural
   * ordering.
   *
   * @throws NullPointerException if any element is null
   */
  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(
      E e1, E e2, E e3, E e4) {
    return copyOf(Ordering.natural(), Arrays.asList(e1, e2, e3, e4));
  }

  /**
   * Returns an immutable sorted multiset containing the given elements sorted by their natural
   * ordering.
   *
   * @throws NullPointerException if any element is null
   */
  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(
      E e1, E e2, E e3, E e4, E e5) {
    return copyOf(Ordering.natural(), Arrays.asList(e1, e2, e3, e4, e5));
  }

  /**
   * Returns an immutable sorted multiset containing the given elements sorted by their natural
   * ordering.
   *
   * @throws NullPointerException if any element is null
   */
  @SuppressWarnings("unchecked")
  public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(
      E e1,
      E e2,
      E e3,
      E e4,
      E e5,
      E e6,
      E... remaining) {
    int size = remaining.length + 6;
    List<E> all = new ArrayList<E>(size);
    Collections.addAll(all, e1, e2, e3, e4, e5, e6);
    Collections.addAll(all, remaining);
    return copyOf(Ordering.natural(), all);
  }

  /**
   * Returns an immutable sorted multiset containing the given elements sorted by their natural
   * ordering.
   *
   * @throws NullPointerException if any of {@code elements} is null
   */
  public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> copyOf(E[] elements) {
    return copyOf(Ordering.natural(), Arrays.asList(elements));
  }

  /**
   * Returns an immutable sorted multiset containing the given elements sorted by their natural
   * ordering. To create a copy of a {@code SortedMultiset} that preserves the
   * comparator, call {@link #copyOfSorted} instead. This method iterates over {@code elements} at
   * most once.
   *
   * <p>Note that if {@code s} is a {@code multiset<String>}, then {@code
   * ImmutableSortedMultiset.copyOf(s)} returns an {@code ImmutableSortedMultiset<String>}
   * containing each of the strings in {@code s}, while {@code ImmutableSortedMultiset.of(s)}
   * returns an {@code ImmutableSortedMultiset<multiset<String>>} containing one element (the given
   * multiset itself).
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * <p>This method is not type-safe, as it may be called on elements that are not mutually
   * comparable.
   *
   * @throws ClassCastException if the elements are not mutually comparable
   * @throws NullPointerException if any of {@code elements} is null
   */
  public static <E> ImmutableSortedMultiset<E> copyOf(Iterable<? extends E> elements) {
    // Hack around E not being a subtype of Comparable.
    // Unsafe, see ImmutableSortedMultisetFauxverideShim.
    @SuppressWarnings("unchecked")
    Ordering<E> naturalOrder = (Ordering<E>) Ordering.<Comparable>natural();
    return copyOf(naturalOrder, elements);
  }

  /**
   * Returns an immutable sorted multiset containing the given elements sorted by their natural
   * ordering.
   *
   * <p>This method is not type-safe, as it may be called on elements that are not mutually
   * comparable.
   *
   * @throws ClassCastException if the elements are not mutually comparable
   * @throws NullPointerException if any of {@code elements} is null
   */
  public static <E> ImmutableSortedMultiset<E> copyOf(Iterator<? extends E> elements) {
    // Hack around E not being a subtype of Comparable.
    // Unsafe, see ImmutableSortedMultisetFauxverideShim.
    @SuppressWarnings("unchecked")
    Ordering<E> naturalOrder = (Ordering<E>) Ordering.<Comparable>natural();
    return copyOfInternal(naturalOrder, elements);
  }

  /**
   * Returns an immutable sorted multiset containing the given elements sorted by the given {@code
   * Comparator}.
   *
   * @throws NullPointerException if {@code comparator} or any of {@code elements} is null
   */
  public static <E> ImmutableSortedMultiset<E> copyOf(
      Comparator<? super E> comparator, Iterator<? extends E> elements) {
    checkNotNull(comparator);
    return copyOfInternal(comparator, elements);
  }

  /**
   * Returns an immutable sorted multiset containing the given elements sorted by the given {@code
   * Comparator}. This method iterates over {@code elements} at most once.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * @throws NullPointerException if {@code comparator} or any of {@code elements} is null
   */
  public static <E> ImmutableSortedMultiset<E> copyOf(
      Comparator<? super E> comparator, Iterable<? extends E> elements) {
    checkNotNull(comparator);
    return copyOfInternal(comparator, elements);
  }

  /**
   * Returns an immutable sorted multiset containing the elements of a sorted multiset, sorted by
   * the same {@code Comparator}. That behavior differs from {@link #copyOf(Iterable)}, which
   * always uses the natural ordering of the elements.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
   * safe to do so. The exact circumstances under which a copy will or will not be performed are
   * undocumented and subject to change.
   *
   * <p>This method is safe to use even when {@code sortedMultiset} is a synchronized or concurrent
   * collection that is currently being modified by another thread.
   *
   * @throws NullPointerException if {@code sortedMultiset} or any of its elements is null
   */
  @SuppressWarnings("unchecked")
  public static <E> ImmutableSortedMultiset<E> copyOfSorted(SortedMultiset<E> sortedMultiset) {
    Comparator<? super E> comparator = sortedMultiset.comparator();
    if (comparator == null) {
      comparator = (Comparator<? super E>) NATURAL_ORDER;
    }
    return copyOfInternal(comparator, sortedMultiset);
  }

  @SuppressWarnings("unchecked")
  private static <E> ImmutableSortedMultiset<E> copyOfInternal(
      Comparator<? super E> comparator, Iterable<? extends E> iterable) {
    if (SortedIterables.hasSameComparator(comparator, iterable)
        && iterable instanceof ImmutableSortedMultiset<?>) {
      ImmutableSortedMultiset<E> multiset = (ImmutableSortedMultiset<E>) iterable;
      if (!multiset.isPartialView()) {
        return (ImmutableSortedMultiset<E>) iterable;
      }
    }
    ImmutableList<Entry<E>> entries =
        (ImmutableList) ImmutableList.copyOf(SortedIterables.sortedCounts(comparator, iterable));
    if (entries.isEmpty()) {
      return emptyMultiset(comparator);
    }
    verifyEntries(entries);
    return RegularImmutableSortedMultiset.createFromSorted(comparator, entries);
  }

  private static <E> ImmutableSortedMultiset<E> copyOfInternal(
      Comparator<? super E> comparator, Iterator<? extends E> iterator) {
    @SuppressWarnings("unchecked") // We can safely cast from IL<Entry<? extends E>> to IL<Entry<E>>
    ImmutableList<Entry<E>> entries =
        (ImmutableList) ImmutableList.copyOf(SortedIterables.sortedCounts(comparator, iterator));
    if (entries.isEmpty()) {
      return emptyMultiset(comparator);
    }
    verifyEntries(entries);
    return RegularImmutableSortedMultiset.createFromSorted(comparator, entries);
  }

  private static <E> void verifyEntries(Collection<Entry<E>> entries) {
    for (Entry<E> entry : entries) {
      checkNotNull(entry.getElement());
    }
  }

  @SuppressWarnings("unchecked")
  static <E> ImmutableSortedMultiset<E> emptyMultiset(Comparator<? super E> comparator) {
    if (NATURAL_ORDER.equals(comparator)) {
      return (ImmutableSortedMultiset) NATURAL_EMPTY_MULTISET;
    }
    return new EmptyImmutableSortedMultiset<E>(comparator);
  }

  private final transient Comparator<? super E> comparator;

  ImmutableSortedMultiset(Comparator<? super E> comparator) {
    this.comparator = checkNotNull(comparator);
  }

  @Override
  public Comparator<? super E> comparator() {
    return comparator;
  }

  // Pretend the comparator can compare anything. If it turns out it can't
  // compare two elements, it'll throw a CCE. Only methods that are specified to
  // throw CCE should call this.
  @SuppressWarnings("unchecked")
  Comparator<Object> unsafeComparator() {
    return (Comparator<Object>) comparator;
  }

  private transient Comparator<? super E> reverseComparator;

  Comparator<? super E> reverseComparator() {
    Comparator<? super E> result = reverseComparator;
    if (result == null) {
      return reverseComparator = Ordering.from(comparator).<E>reverse();
    }
    return result;
  }

  private transient ImmutableSortedSet<E> elementSet;

  @Override
  public ImmutableSortedSet<E> elementSet() {
    ImmutableSortedSet<E> result = elementSet;
    if (result == null) {
      return elementSet = createElementSet();
    }
    return result;
  }

  abstract ImmutableSortedSet<E> createElementSet();

  abstract ImmutableSortedSet<E> createDescendingElementSet();

  transient ImmutableSortedMultiset<E> descendingMultiset;

  @Override
  public ImmutableSortedMultiset<E> descendingMultiset() {
    ImmutableSortedMultiset<E> result = descendingMultiset;
    if (result == null) {
      return descendingMultiset = new DescendingImmutableSortedMultiset<E>(this);
    }
    return result;
  }

  abstract UnmodifiableIterator<Entry<E>> descendingEntryIterator();

  /**
   * {@inheritDoc}
   *
   * <p>This implementation is guaranteed to throw an {@link UnsupportedOperationException}.
   */
  @Override
  public final Entry<E> pollFirstEntry() {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation is guaranteed to throw an {@link UnsupportedOperationException}.
   */
  @Override
  public Entry<E> pollLastEntry() {
    throw new UnsupportedOperationException();
  }

  @Override
  public abstract ImmutableSortedMultiset<E> headMultiset(E upperBound, BoundType boundType);

  @Override
  public ImmutableSortedMultiset<E> subMultiset(
      E lowerBound, BoundType lowerBoundType, E upperBound, BoundType upperBoundType) {
    return tailMultiset(lowerBound, lowerBoundType).headMultiset(upperBound, upperBoundType);
  }

  @Override
  public abstract ImmutableSortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType);

  /**
   * Returns a builder that creates immutable sorted multisets with an explicit comparator. If the
   * comparator has a more general type than the set being generated, such as creating a {@code
   * SortedMultiset<Integer>} with a {@code Comparator<Number>}, use the {@link Builder}
   * constructor instead.
   *
   * @throws NullPointerException if {@code comparator} is null
   */
  public static <E> Builder<E> orderedBy(Comparator<E> comparator) {
    return new Builder<E>(comparator);
  }

  /**
   * Returns a builder that creates immutable sorted multisets whose elements are ordered by the
   * reverse of their natural ordering.
   *
   * <p>Note: the type parameter {@code E} extends {@code Comparable<E>} rather than {@code
   * Comparable<? super E>} as a workaround for javac <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6468354">bug 6468354</a>.
   */
  public static <E extends Comparable<E>> Builder<E> reverseOrder() {
    return new Builder<E>(Ordering.natural().reverse());
  }

  /**
   * Returns a builder that creates immutable sorted multisets whose elements are ordered by their
   * natural ordering. The sorted multisets use {@link Ordering#natural()} as the comparator. This
   * method provides more type-safety than {@link #builder}, as it can be called only for classes
   * that implement {@link Comparable}.
   *
   * <p>Note: the type parameter {@code E} extends {@code Comparable<E>} rather than {@code
   * Comparable<? super E>} as a workaround for javac <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6468354">bug 6468354</a>.
   */
  public static <E extends Comparable<E>> Builder<E> naturalOrder() {
    return new Builder<E>(Ordering.natural());
  }

  /**
   * A builder for creating immutable multiset instances, especially {@code public static final}
   * multisets ("constant multisets"). Example:
   *
   * <pre> {@code
   *
   *   public static final ImmutableSortedMultiset<Bean> BEANS =
   *       new ImmutableSortedMultiset.Builder<Bean>()
   *           .addCopies(Bean.COCOA, 4)
   *           .addCopies(Bean.GARDEN, 6)
   *           .addCopies(Bean.RED, 8)
   *           .addCopies(Bean.BLACK_EYED, 10)
   *           .build();}</pre>
   *
   * Builder instances can be reused; it is safe to call {@link #build} multiple times to build
   * multiple multisets in series.
   */
  public static class Builder<E> extends ImmutableMultiset.Builder<E> {
    private final Comparator<? super E> comparator;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder generated by
     * {@link ImmutableSortedMultiset#orderedBy(Comparator)}.
     */
    public Builder(Comparator<? super E> comparator) {
      super(TreeMultiset.<E>create(comparator));
      this.comparator = checkNotNull(comparator);
    }

    /**
     * Adds {@code element} to the {@code ImmutableSortedMultiset}.
     *
     * @param element the element to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code element} is null
     */
    @Override
    public Builder<E> add(E element) {
      super.add(element);
      return this;
    }

    /**
     * Adds a number of occurrences of an element to this {@code ImmutableSortedMultiset}.
     *
     * @param element the element to add
     * @param occurrences the number of occurrences of the element to add. May be zero, in which
     *        case no change will be made.
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code element} is null
     * @throws IllegalArgumentException if {@code occurrences} is negative, or if this operation
     *         would result in more than {@link Integer#MAX_VALUE} occurrences of the element
     */
    @Override
    public Builder<E> addCopies(E element, int occurrences) {
      super.addCopies(element, occurrences);
      return this;
    }

    /**
     * Adds or removes the necessary occurrences of an element such that the element attains the
     * desired count.
     *
     * @param element the element to add or remove occurrences of
     * @param count the desired count of the element in this multiset
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code element} is null
     * @throws IllegalArgumentException if {@code count} is negative
     */
    @Override
    public Builder<E> setCount(E element, int count) {
      super.setCount(element, count);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSortedMultiset}.
     *
     * @param elements the elements to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a null element
     */
    @Override
    public Builder<E> add(E... elements) {
      super.add(elements);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSortedMultiset}.
     *
     * @param elements the {@code Iterable} to add to the {@code ImmutableSortedMultiset}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a null element
     */
    @Override
    public Builder<E> addAll(Iterable<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSortedMultiset}.
     *
     * @param elements the elements to add to the {@code ImmutableSortedMultiset}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a null element
     */
    @Override
    public Builder<E> addAll(Iterator<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    /**
     * Returns a newly-created {@code ImmutableSortedMultiset} based on the contents of the {@code
     * Builder}.
     */
    @Override
    public ImmutableSortedMultiset<E> build() {
      return copyOf(comparator, contents);
    }
  }

  private static final class SerializedForm implements Serializable {
    Comparator comparator;
    Object[] elements;
    int[] counts;

    SerializedForm(SortedMultiset<?> multiset) {
      this.comparator = multiset.comparator();
      int n = multiset.entrySet().size();
      elements = new Object[n];
      counts = new int[n];
      int i = 0;
      for (Entry<?> entry : multiset.entrySet()) {
        elements[i] = entry.getElement();
        counts[i] = entry.getCount();
        i++;
      }
    }

    @SuppressWarnings("unchecked")
    Object readResolve() {
      int n = elements.length;
      Builder<Object> builder = orderedBy(comparator);
      for (int i = 0; i < n; i++) {
        builder.addCopies(elements[i], counts[i]);
      }
      return builder.build();
    }
  }

  @Override
  Object writeReplace() {
    return new SerializedForm(this);
  }
}
