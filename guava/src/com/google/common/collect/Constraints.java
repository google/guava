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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;

/**
 * Factories and utilities pertaining to the {@link Constraint} interface.
 *
 * @author Mike Bostock
 * @author Jared Levy
 */
@GwtCompatible
final class Constraints {
  private Constraints() {}

  /**
   * Returns a constrained view of the specified collection, using the specified
   * constraint. Any operations that add new elements to the collection will
   * call the provided constraint. However, this method does not verify that
   * existing elements satisfy the constraint.
   *
   * <p>The returned collection is not serializable.
   *
   * @param collection the collection to constrain
   * @param constraint the constraint that validates added elements
   * @return a constrained view of the collection
   */
  public static <E> Collection<E> constrainedCollection(
      Collection<E> collection, Constraint<? super E> constraint) {
    return new ConstrainedCollection<E>(collection, constraint);
  }

  /** @see Constraints#constrainedCollection */
  static class ConstrainedCollection<E> extends ForwardingCollection<E> {
    private final Collection<E> delegate;
    private final Constraint<? super E> constraint;

    public ConstrainedCollection(Collection<E> delegate, Constraint<? super E> constraint) {
      this.delegate = checkNotNull(delegate);
      this.constraint = checkNotNull(constraint);
    }

    @Override
    protected Collection<E> delegate() {
      return delegate;
    }

    @Override
    public boolean add(E element) {
      constraint.checkElement(element);
      return delegate.add(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> elements) {
      return delegate.addAll(checkElements(elements, constraint));
    }
  }

  /**
   * Returns a constrained view of the specified set, using the specified
   * constraint. Any operations that add new elements to the set will call the
   * provided constraint. However, this method does not verify that existing
   * elements satisfy the constraint.
   *
   * <p>The returned set is not serializable.
   *
   * @param set the set to constrain
   * @param constraint the constraint that validates added elements
   * @return a constrained view of the set
   */
  public static <E> Set<E> constrainedSet(Set<E> set, Constraint<? super E> constraint) {
    return new ConstrainedSet<E>(set, constraint);
  }

  /** @see Constraints#constrainedSet */
  static class ConstrainedSet<E> extends ForwardingSet<E> {
    private final Set<E> delegate;
    private final Constraint<? super E> constraint;

    public ConstrainedSet(Set<E> delegate, Constraint<? super E> constraint) {
      this.delegate = checkNotNull(delegate);
      this.constraint = checkNotNull(constraint);
    }

    @Override
    protected Set<E> delegate() {
      return delegate;
    }

    @Override
    public boolean add(E element) {
      constraint.checkElement(element);
      return delegate.add(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> elements) {
      return delegate.addAll(checkElements(elements, constraint));
    }
  }

  /**
   * Returns a constrained view of the specified sorted set, using the specified
   * constraint. Any operations that add new elements to the sorted set will
   * call the provided constraint. However, this method does not verify that
   * existing elements satisfy the constraint.
   *
   * <p>The returned set is not serializable.
   *
   * @param sortedSet the sorted set to constrain
   * @param constraint the constraint that validates added elements
   * @return a constrained view of the sorted set
   */
  public static <E> SortedSet<E> constrainedSortedSet(
      SortedSet<E> sortedSet, Constraint<? super E> constraint) {
    return new ConstrainedSortedSet<E>(sortedSet, constraint);
  }

  /** @see Constraints#constrainedSortedSet */
  private static class ConstrainedSortedSet<E> extends ForwardingSortedSet<E> {
    final SortedSet<E> delegate;
    final Constraint<? super E> constraint;

    ConstrainedSortedSet(SortedSet<E> delegate, Constraint<? super E> constraint) {
      this.delegate = checkNotNull(delegate);
      this.constraint = checkNotNull(constraint);
    }

    @Override
    protected SortedSet<E> delegate() {
      return delegate;
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
      return constrainedSortedSet(delegate.headSet(toElement), constraint);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
      return constrainedSortedSet(delegate.subSet(fromElement, toElement), constraint);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
      return constrainedSortedSet(delegate.tailSet(fromElement), constraint);
    }

    @Override
    public boolean add(E element) {
      constraint.checkElement(element);
      return delegate.add(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> elements) {
      return delegate.addAll(checkElements(elements, constraint));
    }
  }

  /**
   * Returns a constrained view of the specified list, using the specified
   * constraint. Any operations that add new elements to the list will call the
   * provided constraint. However, this method does not verify that existing
   * elements satisfy the constraint.
   *
   * <p>If {@code list} implements {@link RandomAccess}, so will the returned
   * list. The returned list is not serializable.
   *
   * @param list the list to constrain
   * @param constraint the constraint that validates added elements
   * @return a constrained view of the list
   */
  public static <E> List<E> constrainedList(List<E> list, Constraint<? super E> constraint) {
    return (list instanceof RandomAccess)
        ? new ConstrainedRandomAccessList<E>(list, constraint)
        : new ConstrainedList<E>(list, constraint);
  }

  /** @see Constraints#constrainedList */
  @GwtCompatible
  private static class ConstrainedList<E> extends ForwardingList<E> {
    final List<E> delegate;
    final Constraint<? super E> constraint;

    ConstrainedList(List<E> delegate, Constraint<? super E> constraint) {
      this.delegate = checkNotNull(delegate);
      this.constraint = checkNotNull(constraint);
    }

    @Override
    protected List<E> delegate() {
      return delegate;
    }

    @Override
    public boolean add(E element) {
      constraint.checkElement(element);
      return delegate.add(element);
    }

    @Override
    public void add(int index, E element) {
      constraint.checkElement(element);
      delegate.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends E> elements) {
      return delegate.addAll(checkElements(elements, constraint));
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> elements) {
      return delegate.addAll(index, checkElements(elements, constraint));
    }

    @Override
    public ListIterator<E> listIterator() {
      return constrainedListIterator(delegate.listIterator(), constraint);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
      return constrainedListIterator(delegate.listIterator(index), constraint);
    }

    @Override
    public E set(int index, E element) {
      constraint.checkElement(element);
      return delegate.set(index, element);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
      return constrainedList(delegate.subList(fromIndex, toIndex), constraint);
    }
  }

  /** @see Constraints#constrainedList */
  static class ConstrainedRandomAccessList<E> extends ConstrainedList<E> implements RandomAccess {
    ConstrainedRandomAccessList(List<E> delegate, Constraint<? super E> constraint) {
      super(delegate, constraint);
    }
  }

  /**
   * Returns a constrained view of the specified list iterator, using the
   * specified constraint. Any operations that would add new elements to the
   * underlying list will be verified by the constraint.
   *
   * @param listIterator the iterator for which to return a constrained view
   * @param constraint the constraint for elements in the list
   * @return a constrained view of the specified iterator
   */
  private static <E> ListIterator<E> constrainedListIterator(
      ListIterator<E> listIterator, Constraint<? super E> constraint) {
    return new ConstrainedListIterator<E>(listIterator, constraint);
  }

  /** @see Constraints#constrainedListIterator */
  static class ConstrainedListIterator<E> extends ForwardingListIterator<E> {
    private final ListIterator<E> delegate;
    private final Constraint<? super E> constraint;

    public ConstrainedListIterator(ListIterator<E> delegate, Constraint<? super E> constraint) {
      this.delegate = delegate;
      this.constraint = constraint;
    }

    @Override
    protected ListIterator<E> delegate() {
      return delegate;
    }

    @Override
    public void add(E element) {
      constraint.checkElement(element);
      delegate.add(element);
    }

    @Override
    public void set(E element) {
      constraint.checkElement(element);
      delegate.set(element);
    }
  }

  static <E> Collection<E> constrainedTypePreservingCollection(
      Collection<E> collection, Constraint<E> constraint) {
    if (collection instanceof SortedSet) {
      return constrainedSortedSet((SortedSet<E>) collection, constraint);
    } else if (collection instanceof Set) {
      return constrainedSet((Set<E>) collection, constraint);
    } else if (collection instanceof List) {
      return constrainedList((List<E>) collection, constraint);
    } else {
      return constrainedCollection(collection, constraint);
    }
  }

  /*
   * TODO(kevinb): For better performance, avoid making a copy of the elements
   * by having addAll() call add() repeatedly instead.
   */

  private static <E> Collection<E> checkElements(
      Collection<E> elements, Constraint<? super E> constraint) {
    Collection<E> copy = Lists.newArrayList(elements);
    for (E element : copy) {
      constraint.checkElement(element);
    }
    return copy;
  }
}
