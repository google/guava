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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Multiset.Entry;
import com.google.common.primitives.Ints;

/**
 * Provides static utility methods for creating and working with {@link
 * Multiset} instances.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible
public final class Multisets {
  private Multisets() {}

  /**
   * Returns an unmodifiable view of the specified multiset. Query operations on
   * the returned multiset "read through" to the specified multiset, and
   * attempts to modify the returned multiset result in an
   * {@link UnsupportedOperationException}.
   *
   * <p>The returned multiset will be serializable if the specified multiset is
   * serializable.
   *
   * @param multiset the multiset for which an unmodifiable view is to be
   *     generated
   * @return an unmodifiable view of the multiset
   */
  public static <E> Multiset<E> unmodifiableMultiset(
      Multiset<? extends E> multiset) {
    if (multiset instanceof UnmodifiableMultiset ||
        multiset instanceof ImmutableMultiset) {
      // Since it's unmodifiable, the covariant cast is safe
      @SuppressWarnings("unchecked")
      Multiset<E> result = (Multiset<E>) multiset;
      return result;
    }
    return new UnmodifiableMultiset<E>(checkNotNull(multiset));
  }

  /**
   * Simply returns its argument.
   *
   * @deprecated no need to use this
   * @since 10.0
   */
  @Deprecated public static <E> Multiset<E> unmodifiableMultiset(
      ImmutableMultiset<E> multiset) {
    return checkNotNull(multiset);
  }

  static class UnmodifiableMultiset<E>
      extends ForwardingMultiset<E> implements Serializable {
    final Multiset<? extends E> delegate;

    UnmodifiableMultiset(Multiset<? extends E> delegate) {
      this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    @Override protected Multiset<E> delegate() {
      // This is safe because all non-covariant methods are overriden
      return (Multiset<E>) delegate;
    }

    transient Set<E> elementSet;

    Set<E> createElementSet() {
      return Collections.<E>unmodifiableSet(delegate.elementSet());
    }

    @Override
    public Set<E> elementSet() {
      Set<E> es = elementSet;
      return (es == null) ? elementSet = createElementSet() : es;
    }

    transient Set<Multiset.Entry<E>> entrySet;

    @SuppressWarnings("unchecked")
    @Override public Set<Multiset.Entry<E>> entrySet() {
      Set<Multiset.Entry<E>> es = entrySet;
      return (es == null)
          // Safe because the returned set is made unmodifiable and Entry
          // itself is readonly
          ? entrySet = (Set) Collections.unmodifiableSet(delegate.entrySet())
          : es;
    }

    @SuppressWarnings("unchecked")
    @Override public Iterator<E> iterator() {
      // Safe because the returned Iterator is made unmodifiable
      return (Iterator<E>) Iterators.unmodifiableIterator(delegate.iterator());
    }

    @Override public boolean add(E element) {
      throw new UnsupportedOperationException();
    }

    @Override public int add(E element, int occurences) {
      throw new UnsupportedOperationException();
    }

    @Override public boolean addAll(Collection<? extends E> elementsToAdd) {
      throw new UnsupportedOperationException();
    }

    @Override public boolean remove(Object element) {
      throw new UnsupportedOperationException();
    }

    @Override public int remove(Object element, int occurrences) {
      throw new UnsupportedOperationException();
    }

    @Override public boolean removeAll(Collection<?> elementsToRemove) {
      throw new UnsupportedOperationException();
    }

    @Override public boolean retainAll(Collection<?> elementsToRetain) {
      throw new UnsupportedOperationException();
    }

    @Override public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override public int setCount(E element, int count) {
      throw new UnsupportedOperationException();
    }

    @Override public boolean setCount(E element, int oldCount, int newCount) {
      throw new UnsupportedOperationException();
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns an unmodifiable view of the specified sorted multiset. Query
   * operations on the returned multiset "read through" to the specified
   * multiset, and attempts to modify the returned multiset result in an {@link
   * UnsupportedOperationException}.
   *
   * <p>The returned multiset will be serializable if the specified multiset is
   * serializable.
   *
   * @param sortedMultiset the sorted multiset for which an unmodifiable view is
   *     to be generated
   * @return an unmodifiable view of the multiset
   * @since 11.0
   */
  @Beta
  public static <E> SortedMultiset<E> unmodifiableSortedMultiset(
      SortedMultiset<E> sortedMultiset) {
    return new UnmodifiableSortedMultiset<E>(checkNotNull(sortedMultiset));
  }

  private static final class UnmodifiableSortedMultiset<E>
      extends UnmodifiableMultiset<E> implements SortedMultiset<E> {
    private UnmodifiableSortedMultiset(SortedMultiset<E> delegate) {
      super(delegate);
    }

    @Override
    protected SortedMultiset<E> delegate() {
      return (SortedMultiset<E>) super.delegate();
    }

    @Override
    public Comparator<? super E> comparator() {
      return delegate().comparator();
    }

    @Override
    SortedSet<E> createElementSet() {
      return Collections.unmodifiableSortedSet(delegate().elementSet());
    }

    @Override
    public SortedSet<E> elementSet() {
      return (SortedSet<E>) super.elementSet();
    }

    private transient UnmodifiableSortedMultiset<E> descendingMultiset;

    @Override
    public SortedMultiset<E> descendingMultiset() {
      UnmodifiableSortedMultiset<E> result = descendingMultiset;
      if (result == null) {
        result = new UnmodifiableSortedMultiset<E>(
            delegate().descendingMultiset());
        result.descendingMultiset = this;
        return descendingMultiset = result;
      }
      return result;
    }

    @Override
    public Entry<E> firstEntry() {
      return delegate().firstEntry();
    }

    @Override
    public Entry<E> lastEntry() {
      return delegate().lastEntry();
    }

    @Override
    public Entry<E> pollFirstEntry() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Entry<E> pollLastEntry() {
      throw new UnsupportedOperationException();
    }

    @Override
    public SortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
      return unmodifiableSortedMultiset(
          delegate().headMultiset(upperBound, boundType));
    }

    @Override
    public SortedMultiset<E> subMultiset(
        E lowerBound, BoundType lowerBoundType,
        E upperBound, BoundType upperBoundType) {
      return unmodifiableSortedMultiset(delegate().subMultiset(
          lowerBound, lowerBoundType, upperBound, upperBoundType));
    }

    @Override
    public SortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
      return unmodifiableSortedMultiset(
          delegate().tailMultiset(lowerBound, boundType));
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns an immutable multiset entry with the specified element and count.
   * The entry will be serializable if {@code e} is.
   *
   * @param e the element to be associated with the returned entry
   * @param n the count to be associated with the returned entry
   * @throws IllegalArgumentException if {@code n} is negative
   */
  public static <E> Multiset.Entry<E> immutableEntry(@Nullable E e, int n) {
    return new ImmutableEntry<E>(e, n);
  }

  static final class ImmutableEntry<E> extends AbstractEntry<E> implements
      Serializable {
    @Nullable final E element;
    final int count;

    ImmutableEntry(@Nullable E element, int count) {
      this.element = element;
      this.count = count;
      checkArgument(count >= 0);
    }

    @Override
    @Nullable public E getElement() {
      return element;
    }

    @Override
    public int getCount() {
      return count;
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a multiset view of the specified set. The multiset is backed by the
   * set, so changes to the set are reflected in the multiset, and vice versa.
   * If the set is modified while an iteration over the multiset is in progress
   * (except through the iterator's own {@code remove} operation) the results of
   * the iteration are undefined.
   *
   * <p>The multiset supports element removal, which removes the corresponding
   * element from the set. It does not support the {@code add} or {@code addAll}
   * operations, nor does it support the use of {@code setCount} to add
   * elements.
   *
   * <p>The returned multiset will be serializable if the specified set is
   * serializable. The multiset is threadsafe if the set is threadsafe.
   *
   * @param set the backing set for the returned multiset view
   */
  static <E> Multiset<E> forSet(Set<E> set) {
    return new SetMultiset<E>(set);
  }

  /** @see Multisets#forSet */
  private static class SetMultiset<E> extends ForwardingCollection<E>
      implements Multiset<E>, Serializable {
    final Set<E> delegate;

    SetMultiset(Set<E> set) {
      delegate = checkNotNull(set);
    }

    @Override protected Set<E> delegate() {
      return delegate;
    }

    @Override
    public int count(Object element) {
      return delegate.contains(element) ? 1 : 0;
    }

    @Override
    public int add(E element, int occurrences) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int remove(Object element, int occurrences) {
      if (occurrences == 0) {
        return count(element);
      }
      checkArgument(occurrences > 0);
      return delegate.remove(element) ? 1 : 0;
    }

    transient Set<E> elementSet;

    @Override
    public Set<E> elementSet() {
      Set<E> es = elementSet;
      return (es == null) ? elementSet = new ElementSet() : es;
    }

    transient Set<Entry<E>> entrySet;

    @Override public Set<Entry<E>> entrySet() {
      Set<Entry<E>> es = entrySet;
      if (es == null) {
        es = entrySet = new EntrySet<E>() {
          @Override Multiset<E> multiset() {
            return SetMultiset.this;
          }

          @Override public Iterator<Entry<E>> iterator() {
            return Iterators.transform(delegate.iterator(),
                new Function<E, Entry<E>>() {
                  @Override public Entry<E> apply(E elem) {
                    return immutableEntry(elem, 1);
                  }
                });
          }

          @Override public int size() {
            return delegate.size();
          }
        };
      }
      return es;
    }

    @Override public boolean add(E o) {
      throw new UnsupportedOperationException();
    }

    @Override public boolean addAll(Collection<? extends E> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int setCount(E element, int count) {
      checkNonnegative(count, "count");

      if (count == count(element)) {
        return count;
      } else if (count == 0) {
        remove(element);
        return 1;
      } else {
        throw new UnsupportedOperationException();
      }
    }

    @Override
    public boolean setCount(E element, int oldCount, int newCount) {
      return setCountImpl(this, element, oldCount, newCount);
    }

    @Override public boolean equals(@Nullable Object object) {
      if (object instanceof Multiset) {
        Multiset<?> that = (Multiset<?>) object;
        return this.size() == that.size() && delegate.equals(that.elementSet());
      }
      return false;
    }

    @Override public int hashCode() {
      int sum = 0;
      for (E e : this) {
        sum += ((e == null) ? 0 : e.hashCode()) ^ 1;
      }
      return sum;
    }

    /** @see SetMultiset#elementSet */
    class ElementSet extends ForwardingSet<E> {
      @Override protected Set<E> delegate() {
        return delegate;
      }

      @Override public boolean add(E o) {
        throw new UnsupportedOperationException();
      }

      @Override public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
      }
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns the expected number of distinct elements given the specified
   * elements. The number of distinct elements is only computed if {@code
   * elements} is an instance of {@code Multiset}; otherwise the default value
   * of 11 is returned.
   */
  static int inferDistinctElements(Iterable<?> elements) {
    if (elements instanceof Multiset) {
      return ((Multiset<?>) elements).elementSet().size();
    }
    return 11; // initial capacity will be rounded up to 16
  }

  /**
   * Returns an unmodifiable <b>view</b> of the intersection of two multisets.
   * An element's count in the multiset is the smaller of its counts in the two
   * backing multisets. The iteration order of the returned multiset matches the
   * element set of {@code multiset1}, with repeated occurrences of the same
   * element appearing consecutively.
   *
   * <p>Results are undefined if {@code multiset1} and {@code multiset2} are
   * based on different equivalence relations (as {@code HashMultiset} and
   * {@code TreeMultiset} are).
   *
   * @since 2.0
   */
  public static <E> Multiset<E> intersection(
      final Multiset<E> multiset1, final Multiset<?> multiset2) {
    checkNotNull(multiset1);
    checkNotNull(multiset2);

    return new AbstractMultiset<E>() {
      @Override
      public int count(Object element) {
        int count1 = multiset1.count(element);
        return (count1 == 0) ? 0 : Math.min(count1, multiset2.count(element));
      }

      @Override
      Set<E> createElementSet() {
        return Sets.intersection(
            multiset1.elementSet(), multiset2.elementSet());
      }

      @Override
      Iterator<Entry<E>> entryIterator() {
        final Iterator<Entry<E>> iterator1 = multiset1.entrySet().iterator();
        return new AbstractIterator<Entry<E>>() {
          @Override
          protected Entry<E> computeNext() {
            while (iterator1.hasNext()) {
              Entry<E> entry1 = iterator1.next();
              E element = entry1.getElement();
              int count = Math.min(entry1.getCount(), multiset2.count(element));
              if (count > 0) {
                return Multisets.immutableEntry(element, count);
              }
            }
            return endOfData();
          }
        };
      }

      @Override
      int distinctElements() {
        return elementSet().size();
      }
    };
  }

  /**
   * Returns {@code true} if {@code subMultiset.count(o) <=
   * superMultiset.count(o)} for all {@code o}.
   *
   * @since 10.0
   */
  @Beta
  public static boolean containsOccurrences(
      Multiset<?> superMultiset, Multiset<?> subMultiset) {
    checkNotNull(superMultiset);
    checkNotNull(subMultiset);
    for (Entry<?> entry : subMultiset.entrySet()) {
      int superCount = superMultiset.count(entry.getElement());
      if (superCount < entry.getCount()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Modifies {@code multisetToModify} so that its count for an element
   * {@code e} is at most {@code multisetToRetain.count(e)}.
   *
   * <p>To be precise, {@code multisetToModify.count(e)} is set to
   * {@code Math.min(multisetToModify.count(e),
   * multisetToRetain.count(e))}. This is similar to
   * {@link #intersection(Multiset, Multiset) intersection}
   * {@code (multisetToModify, multisetToRetain)}, but mutates
   * {@code multisetToModify} instead of returning a view.
   *
   * <p>In contrast, {@code multisetToModify.retainAll(multisetToRetain)} keeps
   * all occurrences of elements that appear at all in {@code
   * multisetToRetain}, and deletes all occurrences of all other elements.
   *
   * @return {@code true} if {@code multisetToModify} was changed as a result
   *         of this operation
   * @since 10.0
   */
  @Beta public static boolean retainOccurrences(Multiset<?> multisetToModify,
      Multiset<?> multisetToRetain) {
    return retainOccurrencesImpl(multisetToModify, multisetToRetain);
  }

  /**
   * Delegate implementation which cares about the element type.
   */
  private static <E> boolean retainOccurrencesImpl(
      Multiset<E> multisetToModify, Multiset<?> occurrencesToRetain) {
    checkNotNull(multisetToModify);
    checkNotNull(occurrencesToRetain);
    // Avoiding ConcurrentModificationExceptions is tricky.
    Iterator<Entry<E>> entryIterator = multisetToModify.entrySet().iterator();
    boolean changed = false;
    while (entryIterator.hasNext()) {
      Entry<E> entry = entryIterator.next();
      int retainCount = occurrencesToRetain.count(entry.getElement());
      if (retainCount == 0) {
        entryIterator.remove();
        changed = true;
      } else if (retainCount < entry.getCount()) {
        multisetToModify.setCount(entry.getElement(), retainCount);
        changed = true;
      }
    }
    return changed;
  }

  /**
   * For each occurrence of an element {@code e} in {@code occurrencesToRemove},
   * removes one occurrence of {@code e} in {@code multisetToModify}.
   *
   * <p>Equivalently, this method modifies {@code multisetToModify} so that
   * {@code multisetToModify.count(e)} is set to
   * {@code Math.max(0, multisetToModify.count(e) -
   * occurrencesToRemove.count(e))}.
   *
   * <p>This is <i>not</i> the same as {@code multisetToModify.}
   * {@link Multiset#removeAll removeAll}{@code (occurrencesToRemove)}, which
   * removes all occurrences of elements that appear in
   * {@code occurrencesToRemove}. However, this operation <i>is</i> equivalent
   * to, albeit more efficient than, the following: <pre>   {@code
   *
   *   for (E e : occurrencesToRemove) {
   *     multisetToModify.remove(e);
   *   }}</pre>
   *
   * @return {@code true} if {@code multisetToModify} was changed as a result of
   *         this operation
   * @since 10.0
   */
  @Beta public static boolean removeOccurrences(
      Multiset<?> multisetToModify, Multiset<?> occurrencesToRemove) {
    return removeOccurrencesImpl(multisetToModify, occurrencesToRemove);
  }

  /**
   * Delegate that cares about the element types in occurrencesToRemove.
   */
  private static <E> boolean removeOccurrencesImpl(
      Multiset<E> multisetToModify, Multiset<?> occurrencesToRemove) {
    // TODO(user): generalize to removing an Iterable, perhaps
    checkNotNull(multisetToModify);
    checkNotNull(occurrencesToRemove);

    boolean changed = false;
    Iterator<Entry<E>> entryIterator = multisetToModify.entrySet().iterator();
    while (entryIterator.hasNext()) {
      Entry<E> entry = entryIterator.next();
      int removeCount = occurrencesToRemove.count(entry.getElement());
      if (removeCount >= entry.getCount()) {
        entryIterator.remove();
        changed = true;
      } else if (removeCount > 0) {
        multisetToModify.remove(entry.getElement(), removeCount);
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Implementation of the {@code equals}, {@code hashCode}, and
   * {@code toString} methods of {@link Multiset.Entry}.
   */
  abstract static class AbstractEntry<E> implements Multiset.Entry<E> {
    /**
     * Indicates whether an object equals this entry, following the behavior
     * specified in {@link Multiset.Entry#equals}.
     */
    @Override public boolean equals(@Nullable Object object) {
      if (object instanceof Multiset.Entry) {
        Multiset.Entry<?> that = (Multiset.Entry<?>) object;
        return this.getCount() == that.getCount()
            && Objects.equal(this.getElement(), that.getElement());
      }
      return false;
    }

    /**
     * Return this entry's hash code, following the behavior specified in
     * {@link Multiset.Entry#hashCode}.
     */
    @Override public int hashCode() {
      E e = getElement();
      return ((e == null) ? 0 : e.hashCode()) ^ getCount();
    }

    /**
     * Returns a string representation of this multiset entry. The string
     * representation consists of the associated element if the associated count
     * is one, and otherwise the associated element followed by the characters
     * " x " (space, x and space) followed by the count. Elements and counts are
     * converted to strings as by {@code String.valueOf}.
     */
    @Override public String toString() {
      String text = String.valueOf(getElement());
      int n = getCount();
      return (n == 1) ? text : (text + " x " + n);
    }
  }

  /**
   * An implementation of {@link Multiset#equals}.
   */
  static boolean equalsImpl(Multiset<?> multiset, @Nullable Object object) {
    if (object == multiset) {
      return true;
    }
    if (object instanceof Multiset) {
      Multiset<?> that = (Multiset<?>) object;
      /*
       * We can't simply check whether the entry sets are equal, since that
       * approach fails when a TreeMultiset has a comparator that returns 0
       * when passed unequal elements.
       */

      if (multiset.size() != that.size()
          || multiset.entrySet().size() != that.entrySet().size()) {
        return false;
      }
      for (Entry<?> entry : that.entrySet()) {
        if (multiset.count(entry.getElement()) != entry.getCount()) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * An implementation of {@link Multiset#addAll}.
   */
  static <E> boolean addAllImpl(
      Multiset<E> self, Collection<? extends E> elements) {
    if (elements.isEmpty()) {
      return false;
    }
    if (elements instanceof Multiset) {
      Multiset<? extends E> that = cast(elements);
      for (Entry<? extends E> entry : that.entrySet()) {
        self.add(entry.getElement(), entry.getCount());
      }
    } else {
      Iterators.addAll(self, elements.iterator());
    }
    return true;
  }

  /**
   * An implementation of {@link Multiset#removeAll}.
   */
  static boolean removeAllImpl(
      Multiset<?> self, Collection<?> elementsToRemove) {
    Collection<?> collection = (elementsToRemove instanceof Multiset)
        ? ((Multiset<?>) elementsToRemove).elementSet() : elementsToRemove;

    return self.elementSet().removeAll(collection);
  }

  /**
   * An implementation of {@link Multiset#retainAll}.
   */
  static boolean retainAllImpl(
      Multiset<?> self, Collection<?> elementsToRetain) {
    Collection<?> collection = (elementsToRetain instanceof Multiset)
        ? ((Multiset<?>) elementsToRetain).elementSet() : elementsToRetain;

    return self.elementSet().retainAll(collection);
  }

  /**
   * An implementation of {@link Multiset#setCount(Object, int)}.
   */
  static <E> int setCountImpl(Multiset<E> self, E element, int count) {
    checkNonnegative(count, "count");

    int oldCount = self.count(element);

    int delta = count - oldCount;
    if (delta > 0) {
      self.add(element, delta);
    } else if (delta < 0) {
      self.remove(element, -delta);
    }

    return oldCount;
  }

  /**
   * An implementation of {@link Multiset#setCount(Object, int, int)}.
   */
  static <E> boolean setCountImpl(
      Multiset<E> self, E element, int oldCount, int newCount) {
    checkNonnegative(oldCount, "oldCount");
    checkNonnegative(newCount, "newCount");

    if (self.count(element) == oldCount) {
      self.setCount(element, newCount);
      return true;
    } else {
      return false;
    }
  }

  static abstract class ElementSet<E> extends AbstractSet<E> {
    abstract Multiset<E> multiset();

    @Override public void clear() {
      multiset().clear();
    }

    @Override public boolean contains(Object o) {
      return multiset().contains(o);
    }

    @Override public boolean containsAll(Collection<?> c) {
      return multiset().containsAll(c);
    }

    @Override public boolean isEmpty() {
      return multiset().isEmpty();
    }

    @Override public Iterator<E> iterator() {
      return Iterators.transform(multiset().entrySet().iterator(),
          new Function<Entry<E>, E>() {
            @Override public E apply(Entry<E> entry) {
              return entry.getElement();
            }
          });
    }

    @Override
    public boolean remove(Object o) {
      int count = multiset().count(o);
      if (count > 0) {
        multiset().remove(o, count);
        return true;
      }
      return false;
    }

    @Override public int size() {
      return multiset().entrySet().size();
    }
  }

  static abstract class EntrySet<E> extends AbstractSet<Entry<E>>{
    abstract Multiset<E> multiset();

    @Override public boolean contains(@Nullable Object o) {
      if (o instanceof Entry) {
        @SuppressWarnings("cast")
        Entry<?> entry = (Entry<?>) o;
        if (entry.getCount() <= 0) {
          return false;
        }
        int count = multiset().count(entry.getElement());
        return count == entry.getCount();

      }
      return false;
    }

    @SuppressWarnings("cast")
    @Override public boolean remove(Object o) {
      return contains(o)
          && multiset().elementSet().remove(((Entry<?>) o).getElement());
    }

    @Override public void clear() {
      multiset().clear();
    }
  }

  /**
   * An implementation of {@link Multiset#iterator}.
   */
  static <E> Iterator<E> iteratorImpl(Multiset<E> multiset) {
    return new MultisetIteratorImpl<E>(
        multiset, multiset.entrySet().iterator());
  }

  static final class MultisetIteratorImpl<E> implements Iterator<E> {
    private final Multiset<E> multiset;
    private final Iterator<Entry<E>> entryIterator;
    private Entry<E> currentEntry;
    /** Count of subsequent elements equal to current element */
    private int laterCount;
    /** Count of all elements equal to current element */
    private int totalCount;
    private boolean canRemove;

    MultisetIteratorImpl(
        Multiset<E> multiset, Iterator<Entry<E>> entryIterator) {
      this.multiset = multiset;
      this.entryIterator = entryIterator;
    }

    @Override
    public boolean hasNext() {
      return laterCount > 0 || entryIterator.hasNext();
    }

    @Override
    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      if (laterCount == 0) {
        currentEntry = entryIterator.next();
        totalCount = laterCount = currentEntry.getCount();
      }
      laterCount--;
      canRemove = true;
      return currentEntry.getElement();
    }

    @Override
    public void remove() {
      checkState(
          canRemove, "no calls to next() since the last call to remove()");
      if (totalCount == 1) {
        entryIterator.remove();
      } else {
        multiset.remove(currentEntry.getElement());
      }
      totalCount--;
      canRemove = false;
    }
  }

  /**
   * An implementation of {@link Multiset#size}.
   */
  static int sizeImpl(Multiset<?> multiset) {
    long size = 0;
    for (Entry<?> entry : multiset.entrySet()) {
      size += entry.getCount();
    }
    return Ints.saturatedCast(size);
  }

  static void checkNonnegative(int count, String name) {
    checkArgument(count >= 0, "%s cannot be negative: %s", name, count);
  }

  /**
   * Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557
   */
  static <T> Multiset<T> cast(Iterable<T> iterable) {
    return (Multiset<T>) iterable;
  }

  private static final Ordering<Entry<?>> DECREASING_COUNT_ORDERING = new Ordering<Entry<?>>() {
    @Override
    public int compare(Entry<?> entry1, Entry<?> entry2) {
      return Ints.compare(entry2.getCount(), entry1.getCount());
    }
  };

  /**
   * Returns a copy of {@code multiset} as an {@link ImmutableMultiset} whose iteration order is
   * highest count first, with ties broken by the iteration order of the original multiset.
   *
   * @since 11.0
   */
  @Beta
  public static <E> ImmutableMultiset<E> copyHighestCountFirst(Multiset<E> multiset) {
    List<Entry<E>> sortedEntries =
        Multisets.DECREASING_COUNT_ORDERING.sortedCopy(multiset.entrySet());
    return ImmutableMultiset.copyFromEntries(sortedEntries);
  }
}
