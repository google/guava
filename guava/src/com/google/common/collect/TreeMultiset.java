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
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.MoreObjects;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.ObjIntConsumer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A multiset which maintains the ordering of its elements, according to either their natural order
 * or an explicit {@link Comparator}. In all cases, this implementation uses {@link
 * Comparable#compareTo} or {@link Comparator#compare} instead of {@link Object#equals} to determine
 * equivalence of instances.
 *
 * <p><b>Warning:</b> The comparison must be <i>consistent with equals</i> as explained by the
 * {@link Comparable} class specification. Otherwise, the resulting multiset will violate the {@link
 * java.util.Collection} contract, which is specified in terms of {@link Object#equals}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#multiset"> {@code
 * Multiset}</a>.
 *
 * @author Louis Wasserman
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class TreeMultiset<E> extends AbstractSortedMultiset<E> implements Serializable {

  /**
   * Creates a new, empty multiset, sorted according to the elements' natural order. All elements
   * inserted into the multiset must implement the {@code Comparable} interface. Furthermore, all
   * such elements must be <i>mutually comparable</i>: {@code e1.compareTo(e2)} must not throw a
   * {@code ClassCastException} for any elements {@code e1} and {@code e2} in the multiset. If the
   * user attempts to add an element to the multiset that violates this constraint (for example, the
   * user attempts to add a string element to a set whose elements are integers), the {@code
   * add(Object)} call will throw a {@code ClassCastException}.
   *
   * <p>The type specification is {@code <E extends Comparable>}, instead of the more specific
   * {@code <E extends Comparable<? super E>>}, to support classes defined without generics.
   */
  public static <E extends Comparable> TreeMultiset<E> create() {
    return new TreeMultiset<E>(Ordering.natural());
  }

  /**
   * Creates a new, empty multiset, sorted according to the specified comparator. All elements
   * inserted into the multiset must be <i>mutually comparable</i> by the specified comparator:
   * {@code comparator.compare(e1, e2)} must not throw a {@code ClassCastException} for any elements
   * {@code e1} and {@code e2} in the multiset. If the user attempts to add an element to the
   * multiset that violates this constraint, the {@code add(Object)} call will throw a {@code
   * ClassCastException}.
   *
   * @param comparator the comparator that will be used to sort this multiset. A null value
   *     indicates that the elements' <i>natural ordering</i> should be used.
   */
  @SuppressWarnings("unchecked")
  public static <E> TreeMultiset<E> create(@Nullable Comparator<? super E> comparator) {
    return (comparator == null)
        ? new TreeMultiset<E>((Comparator) Ordering.natural())
        : new TreeMultiset<E>(comparator);
  }

  /**
   * Creates an empty multiset containing the given initial elements, sorted according to the
   * elements' natural order.
   *
   * <p>This implementation is highly efficient when {@code elements} is itself a {@link Multiset}.
   *
   * <p>The type specification is {@code <E extends Comparable>}, instead of the more specific
   * {@code <E extends Comparable<? super E>>}, to support classes defined without generics.
   */
  public static <E extends Comparable> TreeMultiset<E> create(Iterable<? extends E> elements) {
    TreeMultiset<E> multiset = create();
    Iterables.addAll(multiset, elements);
    return multiset;
  }

  private final transient Reference<AvlNode<E>> rootReference;
  private final transient GeneralRange<E> range;
  private final transient AvlNode<E> header;

  TreeMultiset(Reference<AvlNode<E>> rootReference, GeneralRange<E> range, AvlNode<E> endLink) {
    super(range.comparator());
    this.rootReference = rootReference;
    this.range = range;
    this.header = endLink;
  }

  TreeMultiset(Comparator<? super E> comparator) {
    super(comparator);
    this.range = GeneralRange.all(comparator);
    this.header = new AvlNode<E>(null, 1);
    successor(header, header);
    this.rootReference = new Reference<>();
  }

  /** A function which can be summed across a subtree. */
  private enum Aggregate {
    SIZE {
      @Override
      int nodeAggregate(AvlNode<?> node) {
        return node.elemCount;
      }

      @Override
      long treeAggregate(@Nullable AvlNode<?> root) {
        return (root == null) ? 0 : root.totalCount;
      }
    },
    DISTINCT {
      @Override
      int nodeAggregate(AvlNode<?> node) {
        return 1;
      }

      @Override
      long treeAggregate(@Nullable AvlNode<?> root) {
        return (root == null) ? 0 : root.distinctElements;
      }
    };

    abstract int nodeAggregate(AvlNode<?> node);

    abstract long treeAggregate(@Nullable AvlNode<?> root);
  }

  private long aggregateForEntries(Aggregate aggr) {
    AvlNode<E> root = rootReference.get();
    long total = aggr.treeAggregate(root);
    if (range.hasLowerBound()) {
      total -= aggregateBelowRange(aggr, root);
    }
    if (range.hasUpperBound()) {
      total -= aggregateAboveRange(aggr, root);
    }
    return total;
  }

  private long aggregateBelowRange(Aggregate aggr, @Nullable AvlNode<E> node) {
    if (node == null) {
      return 0;
    }
    int cmp = comparator().compare(range.getLowerEndpoint(), node.elem);
    if (cmp < 0) {
      return aggregateBelowRange(aggr, node.left);
    } else if (cmp == 0) {
      switch (range.getLowerBoundType()) {
        case OPEN:
          return aggr.nodeAggregate(node) + aggr.treeAggregate(node.left);
        case CLOSED:
          return aggr.treeAggregate(node.left);
        default:
          throw new AssertionError();
      }
    } else {
      return aggr.treeAggregate(node.left)
          + aggr.nodeAggregate(node)
          + aggregateBelowRange(aggr, node.right);
    }
  }

  private long aggregateAboveRange(Aggregate aggr, @Nullable AvlNode<E> node) {
    if (node == null) {
      return 0;
    }
    int cmp = comparator().compare(range.getUpperEndpoint(), node.elem);
    if (cmp > 0) {
      return aggregateAboveRange(aggr, node.right);
    } else if (cmp == 0) {
      switch (range.getUpperBoundType()) {
        case OPEN:
          return aggr.nodeAggregate(node) + aggr.treeAggregate(node.right);
        case CLOSED:
          return aggr.treeAggregate(node.right);
        default:
          throw new AssertionError();
      }
    } else {
      return aggr.treeAggregate(node.right)
          + aggr.nodeAggregate(node)
          + aggregateAboveRange(aggr, node.left);
    }
  }

  @Override
  public int size() {
    return Ints.saturatedCast(aggregateForEntries(Aggregate.SIZE));
  }

  @Override
  int distinctElements() {
    return Ints.saturatedCast(aggregateForEntries(Aggregate.DISTINCT));
  }

  static int distinctElements(@Nullable AvlNode<?> node) {
    return (node == null) ? 0 : node.distinctElements;
  }

  @Override
  public int count(@Nullable Object element) {
    try {
      @SuppressWarnings("unchecked")
      E e = (E) element;
      AvlNode<E> root = rootReference.get();
      if (!range.contains(e) || root == null) {
        return 0;
      }
      return root.count(comparator(), e);
    } catch (ClassCastException | NullPointerException e) {
      return 0;
    }
  }

  @CanIgnoreReturnValue
  @Override
  public int add(@Nullable E element, int occurrences) {
    checkNonnegative(occurrences, "occurrences");
    if (occurrences == 0) {
      return count(element);
    }
    checkArgument(range.contains(element));
    AvlNode<E> root = rootReference.get();
    if (root == null) {
      comparator().compare(element, element);
      AvlNode<E> newRoot = new AvlNode<E>(element, occurrences);
      successor(header, newRoot, header);
      rootReference.checkAndSet(root, newRoot);
      return 0;
    }
    int[] result = new int[1]; // used as a mutable int reference to hold result
    AvlNode<E> newRoot = root.add(comparator(), element, occurrences, result);
    rootReference.checkAndSet(root, newRoot);
    return result[0];
  }

  @CanIgnoreReturnValue
  @Override
  public int remove(@Nullable Object element, int occurrences) {
    checkNonnegative(occurrences, "occurrences");
    if (occurrences == 0) {
      return count(element);
    }
    AvlNode<E> root = rootReference.get();
    int[] result = new int[1]; // used as a mutable int reference to hold result
    AvlNode<E> newRoot;
    try {
      @SuppressWarnings("unchecked")
      E e = (E) element;
      if (!range.contains(e) || root == null) {
        return 0;
      }
      newRoot = root.remove(comparator(), e, occurrences, result);
    } catch (ClassCastException | NullPointerException e) {
      return 0;
    }
    rootReference.checkAndSet(root, newRoot);
    return result[0];
  }

  @CanIgnoreReturnValue
  @Override
  public int setCount(@Nullable E element, int count) {
    checkNonnegative(count, "count");
    if (!range.contains(element)) {
      checkArgument(count == 0);
      return 0;
    }

    AvlNode<E> root = rootReference.get();
    if (root == null) {
      if (count > 0) {
        add(element, count);
      }
      return 0;
    }
    int[] result = new int[1]; // used as a mutable int reference to hold result
    AvlNode<E> newRoot = root.setCount(comparator(), element, count, result);
    rootReference.checkAndSet(root, newRoot);
    return result[0];
  }

  @CanIgnoreReturnValue
  @Override
  public boolean setCount(@Nullable E element, int oldCount, int newCount) {
    checkNonnegative(newCount, "newCount");
    checkNonnegative(oldCount, "oldCount");
    checkArgument(range.contains(element));

    AvlNode<E> root = rootReference.get();
    if (root == null) {
      if (oldCount == 0) {
        if (newCount > 0) {
          add(element, newCount);
        }
        return true;
      } else {
        return false;
      }
    }
    int[] result = new int[1]; // used as a mutable int reference to hold result
    AvlNode<E> newRoot = root.setCount(comparator(), element, oldCount, newCount, result);
    rootReference.checkAndSet(root, newRoot);
    return result[0] == oldCount;
  }

  @Override
  public void clear() {
    if (!range.hasLowerBound() && !range.hasUpperBound()) {
      // We can do this in O(n) rather than removing one by one, which could force rebalancing.
      for (AvlNode<E> current = header.succ; current != header; ) {
        AvlNode<E> next = current.succ;

        current.elemCount = 0;
        // Also clear these fields so that one deleted Entry doesn't retain all elements.
        current.left = null;
        current.right = null;
        current.pred = null;
        current.succ = null;

        current = next;
      }
      successor(header, header);
      rootReference.clear();
    } else {
      // TODO(cpovirk): Perhaps we can optimize in this case, too?
      Iterators.clear(entryIterator());
    }
  }

  private Entry<E> wrapEntry(final AvlNode<E> baseEntry) {
    return new Multisets.AbstractEntry<E>() {
      @Override
      public E getElement() {
        return baseEntry.getElement();
      }

      @Override
      public int getCount() {
        int result = baseEntry.getCount();
        if (result == 0) {
          return count(getElement());
        } else {
          return result;
        }
      }
    };
  }

  /** Returns the first node in the tree that is in range. */
  private @Nullable AvlNode<E> firstNode() {
    AvlNode<E> root = rootReference.get();
    if (root == null) {
      return null;
    }
    AvlNode<E> node;
    if (range.hasLowerBound()) {
      E endpoint = range.getLowerEndpoint();
      node = rootReference.get().ceiling(comparator(), endpoint);
      if (node == null) {
        return null;
      }
      if (range.getLowerBoundType() == BoundType.OPEN
          && comparator().compare(endpoint, node.getElement()) == 0) {
        node = node.succ;
      }
    } else {
      node = header.succ;
    }
    return (node == header || !range.contains(node.getElement())) ? null : node;
  }

  private @Nullable AvlNode<E> lastNode() {
    AvlNode<E> root = rootReference.get();
    if (root == null) {
      return null;
    }
    AvlNode<E> node;
    if (range.hasUpperBound()) {
      E endpoint = range.getUpperEndpoint();
      node = rootReference.get().floor(comparator(), endpoint);
      if (node == null) {
        return null;
      }
      if (range.getUpperBoundType() == BoundType.OPEN
          && comparator().compare(endpoint, node.getElement()) == 0) {
        node = node.pred;
      }
    } else {
      node = header.pred;
    }
    return (node == header || !range.contains(node.getElement())) ? null : node;
  }

  @Override
  Iterator<E> elementIterator() {
    return Multisets.elementIterator(entryIterator());
  }

  @Override
  Iterator<Entry<E>> entryIterator() {
    return new Iterator<Entry<E>>() {
      AvlNode<E> current = firstNode();
      @Nullable Entry<E> prevEntry;

      @Override
      public boolean hasNext() {
        if (current == null) {
          return false;
        } else if (range.tooHigh(current.getElement())) {
          current = null;
          return false;
        } else {
          return true;
        }
      }

      @Override
      public Entry<E> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        Entry<E> result = wrapEntry(current);
        prevEntry = result;
        if (current.succ == header) {
          current = null;
        } else {
          current = current.succ;
        }
        return result;
      }

      @Override
      public void remove() {
        checkRemove(prevEntry != null);
        setCount(prevEntry.getElement(), 0);
        prevEntry = null;
      }
    };
  }

  @Override
  Iterator<Entry<E>> descendingEntryIterator() {
    return new Iterator<Entry<E>>() {
      AvlNode<E> current = lastNode();
      Entry<E> prevEntry = null;

      @Override
      public boolean hasNext() {
        if (current == null) {
          return false;
        } else if (range.tooLow(current.getElement())) {
          current = null;
          return false;
        } else {
          return true;
        }
      }

      @Override
      public Entry<E> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        Entry<E> result = wrapEntry(current);
        prevEntry = result;
        if (current.pred == header) {
          current = null;
        } else {
          current = current.pred;
        }
        return result;
      }

      @Override
      public void remove() {
        checkRemove(prevEntry != null);
        setCount(prevEntry.getElement(), 0);
        prevEntry = null;
      }
    };
  }

  @Override
  public void forEachEntry(ObjIntConsumer<? super E> action) {
    checkNotNull(action);
    for (AvlNode<E> node = firstNode();
        node != header && node != null && !range.tooHigh(node.getElement());
        node = node.succ) {
      action.accept(node.getElement(), node.getCount());
    }
  }

  @Override
  public Iterator<E> iterator() {
    return Multisets.iteratorImpl(this);
  }

  @Override
  public SortedMultiset<E> headMultiset(@Nullable E upperBound, BoundType boundType) {
    return new TreeMultiset<E>(
        rootReference,
        range.intersect(GeneralRange.upTo(comparator(), upperBound, boundType)),
        header);
  }

  @Override
  public SortedMultiset<E> tailMultiset(@Nullable E lowerBound, BoundType boundType) {
    return new TreeMultiset<E>(
        rootReference,
        range.intersect(GeneralRange.downTo(comparator(), lowerBound, boundType)),
        header);
  }

  private static final class Reference<T> {
    private @Nullable T value;

    public @Nullable T get() {
      return value;
    }

    public void checkAndSet(@Nullable T expected, T newValue) {
      if (value != expected) {
        throw new ConcurrentModificationException();
      }
      value = newValue;
    }

    void clear() {
      value = null;
    }
  }

  private static final class AvlNode<E> {
    private final @Nullable E elem;

    // elemCount is 0 iff this node has been deleted.
    private int elemCount;

    private int distinctElements;
    private long totalCount;
    private int height;
    private @Nullable AvlNode<E> left;
    private @Nullable AvlNode<E> right;
    private @Nullable AvlNode<E> pred;
    private @Nullable AvlNode<E> succ;

    AvlNode(@Nullable E elem, int elemCount) {
      checkArgument(elemCount > 0);
      this.elem = elem;
      this.elemCount = elemCount;
      this.totalCount = elemCount;
      this.distinctElements = 1;
      this.height = 1;
      this.left = null;
      this.right = null;
    }

    public int count(Comparator<? super E> comparator, E e) {
      int cmp = comparator.compare(e, elem);
      if (cmp < 0) {
        return (left == null) ? 0 : left.count(comparator, e);
      } else if (cmp > 0) {
        return (right == null) ? 0 : right.count(comparator, e);
      } else {
        return elemCount;
      }
    }

    private AvlNode<E> addRightChild(E e, int count) {
      right = new AvlNode<E>(e, count);
      successor(this, right, succ);
      height = Math.max(2, height);
      distinctElements++;
      totalCount += count;
      return this;
    }

    private AvlNode<E> addLeftChild(E e, int count) {
      left = new AvlNode<E>(e, count);
      successor(pred, left, this);
      height = Math.max(2, height);
      distinctElements++;
      totalCount += count;
      return this;
    }

    AvlNode<E> add(Comparator<? super E> comparator, @Nullable E e, int count, int[] result) {
      /*
       * It speeds things up considerably to unconditionally add count to totalCount here,
       * but that destroys failure atomicity in the case of count overflow. =(
       */
      int cmp = comparator.compare(e, elem);
      if (cmp < 0) {
        AvlNode<E> initLeft = left;
        if (initLeft == null) {
          result[0] = 0;
          return addLeftChild(e, count);
        }
        int initHeight = initLeft.height;

        left = initLeft.add(comparator, e, count, result);
        if (result[0] == 0) {
          distinctElements++;
        }
        this.totalCount += count;
        return (left.height == initHeight) ? this : rebalance();
      } else if (cmp > 0) {
        AvlNode<E> initRight = right;
        if (initRight == null) {
          result[0] = 0;
          return addRightChild(e, count);
        }
        int initHeight = initRight.height;

        right = initRight.add(comparator, e, count, result);
        if (result[0] == 0) {
          distinctElements++;
        }
        this.totalCount += count;
        return (right.height == initHeight) ? this : rebalance();
      }

      // adding count to me!  No rebalance possible.
      result[0] = elemCount;
      long resultCount = (long) elemCount + count;
      checkArgument(resultCount <= Integer.MAX_VALUE);
      this.elemCount += count;
      this.totalCount += count;
      return this;
    }

    AvlNode<E> remove(Comparator<? super E> comparator, @Nullable E e, int count, int[] result) {
      int cmp = comparator.compare(e, elem);
      if (cmp < 0) {
        AvlNode<E> initLeft = left;
        if (initLeft == null) {
          result[0] = 0;
          return this;
        }

        left = initLeft.remove(comparator, e, count, result);

        if (result[0] > 0) {
          if (count >= result[0]) {
            this.distinctElements--;
            this.totalCount -= result[0];
          } else {
            this.totalCount -= count;
          }
        }
        return (result[0] == 0) ? this : rebalance();
      } else if (cmp > 0) {
        AvlNode<E> initRight = right;
        if (initRight == null) {
          result[0] = 0;
          return this;
        }

        right = initRight.remove(comparator, e, count, result);

        if (result[0] > 0) {
          if (count >= result[0]) {
            this.distinctElements--;
            this.totalCount -= result[0];
          } else {
            this.totalCount -= count;
          }
        }
        return rebalance();
      }

      // removing count from me!
      result[0] = elemCount;
      if (count >= elemCount) {
        return deleteMe();
      } else {
        this.elemCount -= count;
        this.totalCount -= count;
        return this;
      }
    }

    AvlNode<E> setCount(Comparator<? super E> comparator, @Nullable E e, int count, int[] result) {
      int cmp = comparator.compare(e, elem);
      if (cmp < 0) {
        AvlNode<E> initLeft = left;
        if (initLeft == null) {
          result[0] = 0;
          return (count > 0) ? addLeftChild(e, count) : this;
        }

        left = initLeft.setCount(comparator, e, count, result);

        if (count == 0 && result[0] != 0) {
          this.distinctElements--;
        } else if (count > 0 && result[0] == 0) {
          this.distinctElements++;
        }

        this.totalCount += count - result[0];
        return rebalance();
      } else if (cmp > 0) {
        AvlNode<E> initRight = right;
        if (initRight == null) {
          result[0] = 0;
          return (count > 0) ? addRightChild(e, count) : this;
        }

        right = initRight.setCount(comparator, e, count, result);

        if (count == 0 && result[0] != 0) {
          this.distinctElements--;
        } else if (count > 0 && result[0] == 0) {
          this.distinctElements++;
        }

        this.totalCount += count - result[0];
        return rebalance();
      }

      // setting my count
      result[0] = elemCount;
      if (count == 0) {
        return deleteMe();
      }
      this.totalCount += count - elemCount;
      this.elemCount = count;
      return this;
    }

    AvlNode<E> setCount(
        Comparator<? super E> comparator,
        @Nullable E e,
        int expectedCount,
        int newCount,
        int[] result) {
      int cmp = comparator.compare(e, elem);
      if (cmp < 0) {
        AvlNode<E> initLeft = left;
        if (initLeft == null) {
          result[0] = 0;
          if (expectedCount == 0 && newCount > 0) {
            return addLeftChild(e, newCount);
          }
          return this;
        }

        left = initLeft.setCount(comparator, e, expectedCount, newCount, result);

        if (result[0] == expectedCount) {
          if (newCount == 0 && result[0] != 0) {
            this.distinctElements--;
          } else if (newCount > 0 && result[0] == 0) {
            this.distinctElements++;
          }
          this.totalCount += newCount - result[0];
        }
        return rebalance();
      } else if (cmp > 0) {
        AvlNode<E> initRight = right;
        if (initRight == null) {
          result[0] = 0;
          if (expectedCount == 0 && newCount > 0) {
            return addRightChild(e, newCount);
          }
          return this;
        }

        right = initRight.setCount(comparator, e, expectedCount, newCount, result);

        if (result[0] == expectedCount) {
          if (newCount == 0 && result[0] != 0) {
            this.distinctElements--;
          } else if (newCount > 0 && result[0] == 0) {
            this.distinctElements++;
          }
          this.totalCount += newCount - result[0];
        }
        return rebalance();
      }

      // setting my count
      result[0] = elemCount;
      if (expectedCount == elemCount) {
        if (newCount == 0) {
          return deleteMe();
        }
        this.totalCount += newCount - elemCount;
        this.elemCount = newCount;
      }
      return this;
    }

    private AvlNode<E> deleteMe() {
      int oldElemCount = this.elemCount;
      this.elemCount = 0;
      successor(pred, succ);
      if (left == null) {
        return right;
      } else if (right == null) {
        return left;
      } else if (left.height >= right.height) {
        AvlNode<E> newTop = pred;
        // newTop is the maximum node in my left subtree
        newTop.left = left.removeMax(newTop);
        newTop.right = right;
        newTop.distinctElements = distinctElements - 1;
        newTop.totalCount = totalCount - oldElemCount;
        return newTop.rebalance();
      } else {
        AvlNode<E> newTop = succ;
        newTop.right = right.removeMin(newTop);
        newTop.left = left;
        newTop.distinctElements = distinctElements - 1;
        newTop.totalCount = totalCount - oldElemCount;
        return newTop.rebalance();
      }
    }

    // Removes the minimum node from this subtree to be reused elsewhere
    private AvlNode<E> removeMin(AvlNode<E> node) {
      if (left == null) {
        return right;
      } else {
        left = left.removeMin(node);
        distinctElements--;
        totalCount -= node.elemCount;
        return rebalance();
      }
    }

    // Removes the maximum node from this subtree to be reused elsewhere
    private AvlNode<E> removeMax(AvlNode<E> node) {
      if (right == null) {
        return left;
      } else {
        right = right.removeMax(node);
        distinctElements--;
        totalCount -= node.elemCount;
        return rebalance();
      }
    }

    private void recomputeMultiset() {
      this.distinctElements =
          1 + TreeMultiset.distinctElements(left) + TreeMultiset.distinctElements(right);
      this.totalCount = elemCount + totalCount(left) + totalCount(right);
    }

    private void recomputeHeight() {
      this.height = 1 + Math.max(height(left), height(right));
    }

    private void recompute() {
      recomputeMultiset();
      recomputeHeight();
    }

    private AvlNode<E> rebalance() {
      switch (balanceFactor()) {
        case -2:
          if (right.balanceFactor() > 0) {
            right = right.rotateRight();
          }
          return rotateLeft();
        case 2:
          if (left.balanceFactor() < 0) {
            left = left.rotateLeft();
          }
          return rotateRight();
        default:
          recomputeHeight();
          return this;
      }
    }

    private int balanceFactor() {
      return height(left) - height(right);
    }

    private AvlNode<E> rotateLeft() {
      checkState(right != null);
      AvlNode<E> newTop = right;
      this.right = newTop.left;
      newTop.left = this;
      newTop.totalCount = this.totalCount;
      newTop.distinctElements = this.distinctElements;
      this.recompute();
      newTop.recomputeHeight();
      return newTop;
    }

    private AvlNode<E> rotateRight() {
      checkState(left != null);
      AvlNode<E> newTop = left;
      this.left = newTop.right;
      newTop.right = this;
      newTop.totalCount = this.totalCount;
      newTop.distinctElements = this.distinctElements;
      this.recompute();
      newTop.recomputeHeight();
      return newTop;
    }

    private static long totalCount(@Nullable AvlNode<?> node) {
      return (node == null) ? 0 : node.totalCount;
    }

    private static int height(@Nullable AvlNode<?> node) {
      return (node == null) ? 0 : node.height;
    }

    private @Nullable AvlNode<E> ceiling(Comparator<? super E> comparator, E e) {
      int cmp = comparator.compare(e, elem);
      if (cmp < 0) {
        return (left == null) ? this : MoreObjects.firstNonNull(left.ceiling(comparator, e), this);
      } else if (cmp == 0) {
        return this;
      } else {
        return (right == null) ? null : right.ceiling(comparator, e);
      }
    }

    private @Nullable AvlNode<E> floor(Comparator<? super E> comparator, E e) {
      int cmp = comparator.compare(e, elem);
      if (cmp > 0) {
        return (right == null) ? this : MoreObjects.firstNonNull(right.floor(comparator, e), this);
      } else if (cmp == 0) {
        return this;
      } else {
        return (left == null) ? null : left.floor(comparator, e);
      }
    }

    E getElement() {
      return elem;
    }

    int getCount() {
      return elemCount;
    }

    @Override
    public String toString() {
      return Multisets.immutableEntry(getElement(), getCount()).toString();
    }
  }

  private static <T> void successor(AvlNode<T> a, AvlNode<T> b) {
    a.succ = b;
    b.pred = a;
  }

  private static <T> void successor(AvlNode<T> a, AvlNode<T> b, AvlNode<T> c) {
    successor(a, b);
    successor(b, c);
  }

  /*
   * TODO(jlevy): Decide whether entrySet() should return entries with an equals() method that
   * calls the comparator to compare the two keys. If that change is made,
   * AbstractMultiset.equals() can simply check whether two multisets have equal entry sets.
   */

  /**
   * @serialData the comparator, the number of distinct elements, the first element, its count, the
   *     second element, its count, and so on
   */
  @GwtIncompatible // java.io.ObjectOutputStream
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(elementSet().comparator());
    Serialization.writeMultiset(this, stream);
  }

  @GwtIncompatible // java.io.ObjectInputStream
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    @SuppressWarnings("unchecked")
    // reading data stored by writeObject
    Comparator<? super E> comparator = (Comparator<? super E>) stream.readObject();
    Serialization.getFieldSetter(AbstractSortedMultiset.class, "comparator").set(this, comparator);
    Serialization.getFieldSetter(TreeMultiset.class, "range")
        .set(this, GeneralRange.all(comparator));
    Serialization.getFieldSetter(TreeMultiset.class, "rootReference")
        .set(this, new Reference<AvlNode<E>>());
    AvlNode<E> header = new AvlNode<E>(null, 1);
    Serialization.getFieldSetter(TreeMultiset.class, "header").set(this, header);
    successor(header, header);
    Serialization.populateMultiset(this, stream);
  }

  @GwtIncompatible // not needed in emulated source
  private static final long serialVersionUID = 1;
}
