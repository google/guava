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
import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;

import java.io.Serializable;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import javax.annotation.Nullable;

import com.google.common.annotations.GwtCompatible;
import com.google.common.primitives.Ints;

/**
 * A multiset which maintains the ordering of its elements, according to either
 * their natural order or an explicit {@link Comparator}. In all cases, this
 * implementation uses {@link Comparable#compareTo} or {@link
 * Comparator#compare} instead of {@link Object#equals} to determine
 * equivalence of instances.
 *
 * <p><b>Warning:</b> The comparison must be <i>consistent with equals</i> as
 * explained by the {@link Comparable} class specification. Otherwise, the
 * resulting multiset will violate the {@link java.util.Collection} contract,
 * which is specified in terms of {@link Object#equals}.
 *
 * @author Louis Wasserman
 * @author Jared Levy
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(emulated = true)
public final class TreeMultiset<E> extends AbstractSortedMultiset<E>
    implements Serializable {

  /**
   * Creates a new, empty multiset, sorted according to the elements' natural
   * order. All elements inserted into the multiset must implement the
   * {@code Comparable} interface. Furthermore, all such elements must be
   * <i>mutually comparable</i>: {@code e1.compareTo(e2)} must not throw a
   * {@code ClassCastException} for any elements {@code e1} and {@code e2} in
   * the multiset. If the user attempts to add an element to the multiset that
   * violates this constraint (for example, the user attempts to add a string
   * element to a set whose elements are integers), the {@code add(Object)}
   * call will throw a {@code ClassCastException}.
   *
   * <p>The type specification is {@code <E extends Comparable>}, instead of the
   * more specific {@code <E extends Comparable<? super E>>}, to support
   * classes defined without generics.
   */
  public static <E extends Comparable> TreeMultiset<E> create() {
    return new TreeMultiset<E>(Ordering.natural());
  }

  /**
   * Creates a new, empty multiset, sorted according to the specified
   * comparator. All elements inserted into the multiset must be <i>mutually
   * comparable</i> by the specified comparator: {@code comparator.compare(e1,
   * e2)} must not throw a {@code ClassCastException} for any elements {@code
   * e1} and {@code e2} in the multiset. If the user attempts to add an element
   * to the multiset that violates this constraint, the {@code add(Object)} call
   * will throw a {@code ClassCastException}.
   *
   * @param comparator the comparator that will be used to sort this multiset. A
   *     null value indicates that the elements' <i>natural ordering</i> should
   *     be used.
   */
  @SuppressWarnings("unchecked")
  public static <E> TreeMultiset<E> create(
      @Nullable Comparator<? super E> comparator) {
    return (comparator == null)
           ? new TreeMultiset<E>((Comparator) Ordering.natural())
           : new TreeMultiset<E>(comparator);
  }

  /**
   * Creates an empty multiset containing the given initial elements, sorted
   * according to the elements' natural order.
   *
   * <p>This implementation is highly efficient when {@code elements} is itself
   * a {@link Multiset}.
   *
   * <p>The type specification is {@code <E extends Comparable>}, instead of the
   * more specific {@code <E extends Comparable<? super E>>}, to support
   * classes defined without generics.
   */
  public static <E extends Comparable> TreeMultiset<E> create(
      Iterable<? extends E> elements) {
    TreeMultiset<E> multiset = create();
    Iterables.addAll(multiset, elements);
    return multiset;
  }

  /**
   * Returns an iterator over the elements contained in this collection.
   */
  @Override
  public Iterator<E> iterator() {
    // Needed to avoid Javadoc bug.
    return super.iterator();
  }

  private TreeMultiset(Comparator<? super E> comparator) {
    super(comparator);
    this.range = GeneralRange.all(comparator);
    this.rootReference = new Reference<Node<E>>();
  }

  private TreeMultiset(GeneralRange<E> range, Reference<Node<E>> root) {
    super(range.comparator());
    this.range = range;
    this.rootReference = root;
  }

  @SuppressWarnings("unchecked")
  E checkElement(Object o) {
    return (E) o;
  }

  private transient final GeneralRange<E> range;

  private transient final Reference<Node<E>> rootReference;

  static final class Reference<T> {
    T value;

    public Reference() {}

    public T get() {
      return value;
    }

    public boolean compareAndSet(T expected, T newValue) {
      if (value == expected) {
        value = newValue;
        return true;
      }
      return false;
    }
  }

  @Override
  int distinctElements() {
    Node<E> root = rootReference.get();
    return Ints.checkedCast(BstRangeOps.totalInRange(distinctAggregate(), range, root));
  }

  @Override
  public int size() {
    Node<E> root = rootReference.get();
    return Ints.saturatedCast(BstRangeOps.totalInRange(sizeAggregate(), range, root));
  }

  @Override
  public int count(@Nullable Object element) {
    try {
      E e = checkElement(element);
      if (range.contains(e)) {
        Node<E> node = BstOperations.seek(comparator(), rootReference.get(), e);
        return countOrZero(node);
      }
      return 0;
    } catch (ClassCastException e) {
      return 0;
    } catch (NullPointerException e) {
      return 0;
    }
  }

  private int mutate(@Nullable E e, MultisetModifier modifier) {
    BstMutationRule<E, Node<E>> mutationRule = BstMutationRule.createRule(
        modifier,
        BstCountBasedBalancePolicies.
          <E, Node<E>>singleRebalancePolicy(distinctAggregate()),
        nodeFactory());
    BstMutationResult<E, Node<E>> mutationResult =
        BstOperations.mutate(comparator(), mutationRule, rootReference.get(), e);
    if (!rootReference.compareAndSet(
        mutationResult.getOriginalRoot(), mutationResult.getChangedRoot())) {
      throw new ConcurrentModificationException();
    }
    Node<E> original = mutationResult.getOriginalTarget();
    return countOrZero(original);
  }

  @Override
  public int add(E element, int occurrences) {
    checkElement(element);
    if (occurrences == 0) {
      return count(element);
    }
    checkArgument(range.contains(element));
    return mutate(element, new AddModifier(occurrences));
  }

  @Override
  public int remove(@Nullable Object element, int occurrences) {
    if (element == null) {
      return 0;
    } else if (occurrences == 0) {
      return count(element);
    }
    try {
      E e = checkElement(element);
      return range.contains(e) ? mutate(e, new RemoveModifier(occurrences)) : 0;
    } catch (ClassCastException e) {
      return 0;
    }
  }

  @Override
  public boolean setCount(E element, int oldCount, int newCount) {
    checkElement(element);
    checkArgument(range.contains(element));
    return mutate(element, new ConditionalSetCountModifier(oldCount, newCount))
        == oldCount;
  }

  @Override
  public int setCount(E element, int count) {
    checkElement(element);
    checkArgument(range.contains(element));
    return mutate(element, new SetCountModifier(count));
  }

  private BstPathFactory<Node<E>, BstInOrderPath<Node<E>>> pathFactory() {
    return BstInOrderPath.inOrderFactory();
  }

  @Override
  Iterator<Entry<E>> entryIterator() {
    Node<E> root = rootReference.get();
    final BstInOrderPath<Node<E>> startingPath =
        BstRangeOps.furthestPath(range, LEFT, pathFactory(), root);
    return iteratorInDirection(startingPath, RIGHT);
  }

  @Override
  Iterator<Entry<E>> descendingEntryIterator() {
    Node<E> root = rootReference.get();
    final BstInOrderPath<Node<E>> startingPath =
        BstRangeOps.furthestPath(range, RIGHT, pathFactory(), root);
    return iteratorInDirection(startingPath, LEFT);
  }

  private Iterator<Entry<E>> iteratorInDirection(
      @Nullable BstInOrderPath<Node<E>> start, final BstSide direction) {
    final Iterator<BstInOrderPath<Node<E>>> pathIterator =
        new AbstractLinkedIterator<BstInOrderPath<Node<E>>>(start) {
          @Override
          protected BstInOrderPath<Node<E>> computeNext(BstInOrderPath<Node<E>> previous) {
            if (!previous.hasNext(direction)) {
              return null;
            }
            BstInOrderPath<Node<E>> next = previous.next(direction);
            // TODO(user): only check against one side
            return range.contains(next.getTip().getKey()) ? next : null;
          }
        };
    return new Iterator<Entry<E>>() {
      E toRemove = null;

      @Override
      public boolean hasNext() {
        return pathIterator.hasNext();
      }

      @Override
      public Entry<E> next() {
        BstInOrderPath<Node<E>> path = pathIterator.next();
        return new LiveEntry(
            toRemove = path.getTip().getKey(), path.getTip().elemCount());
      }

      @Override
      public void remove() {
        checkState(toRemove != null);
        setCount(toRemove, 0);
        toRemove = null;
      }
    };
  }

  class LiveEntry extends Multisets.AbstractEntry<E> {
    private Node<E> expectedRoot;
    private final E element;
    private int count;

    private LiveEntry(E element, int count) {
      this.expectedRoot = rootReference.get();
      this.element = element;
      this.count = count;
    }

    @Override
    public E getElement() {
      return element;
    }

    @Override
    public int getCount() {
      if (rootReference.get() == expectedRoot) {
        return count;
      } else {
        // check for updates
        expectedRoot = rootReference.get();
        return count = TreeMultiset.this.count(element);
      }
    }
  }

  @Override
  public void clear() {
    Node<E> root = rootReference.get();
    Node<E> cleared = BstRangeOps.minusRange(range,
        BstCountBasedBalancePolicies.<E, Node<E>>fullRebalancePolicy(distinctAggregate()),
        nodeFactory(), root);
    if (!rootReference.compareAndSet(root, cleared)) {
      throw new ConcurrentModificationException();
    }
  }

  @Override
  public SortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
    checkNotNull(upperBound);
    return new TreeMultiset<E>(
        range.intersect(GeneralRange.upTo(comparator, upperBound, boundType)), rootReference);
  }

  @Override
  public SortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
    checkNotNull(lowerBound);
    return new TreeMultiset<E>(
        range.intersect(GeneralRange.downTo(comparator, lowerBound, boundType)), rootReference);
  }

  /**
   * {@inheritDoc}
   *
   * @since 11.0
   */
  @Override
  public Comparator<? super E> comparator() {
    return super.comparator();
  }

  private static final class Node<E> extends BstNode<E, Node<E>> implements Serializable {
    private final long size;
    private final int distinct;

    private Node(E key, int elemCount, @Nullable Node<E> left,
        @Nullable Node<E> right) {
      super(key, left, right);
      checkArgument(elemCount > 0);
      this.size = (long) elemCount + sizeOrZero(left) + sizeOrZero(right);
      this.distinct = 1 + distinctOrZero(left) + distinctOrZero(right);
    }

    int elemCount() {
      long result = size - sizeOrZero(childOrNull(LEFT))
          - sizeOrZero(childOrNull(RIGHT));
      return Ints.checkedCast(result);
    }

    private Node(E key, int elemCount) {
      this(key, elemCount, null, null);
    }

    private static final long serialVersionUID = 0;
  }

  private static long sizeOrZero(@Nullable Node<?> node) {
    return (node == null) ? 0 : node.size;
  }

  private static int distinctOrZero(@Nullable Node<?> node) {
    return (node == null) ? 0 : node.distinct;
  }

  private static int countOrZero(@Nullable Node<?> entry) {
    return (entry == null) ? 0 : entry.elemCount();
  }

  @SuppressWarnings("unchecked")
  private BstAggregate<Node<E>> distinctAggregate() {
    return (BstAggregate) DISTINCT_AGGREGATE;
  }

  private static final BstAggregate<Node<Object>> DISTINCT_AGGREGATE =
      new BstAggregate<Node<Object>>() {
    @Override
    public int entryValue(Node<Object> entry) {
      return 1;
    }

    @Override
    public long treeValue(@Nullable Node<Object> tree) {
      return distinctOrZero(tree);
    }
  };

  @SuppressWarnings("unchecked")
  private BstAggregate<Node<E>> sizeAggregate() {
    return (BstAggregate) SIZE_AGGREGATE;
  }

  private static final BstAggregate<Node<Object>> SIZE_AGGREGATE =
      new BstAggregate<Node<Object>>() {
        @Override
        public int entryValue(Node<Object> entry) {
          return entry.elemCount();
        }

        @Override
        public long treeValue(@Nullable Node<Object> tree) {
          return sizeOrZero(tree);
        }
      };

  @SuppressWarnings("unchecked")
  private BstNodeFactory<Node<E>> nodeFactory() {
    return (BstNodeFactory) NODE_FACTORY;
  }

  private static final BstNodeFactory<Node<Object>> NODE_FACTORY =
      new BstNodeFactory<Node<Object>>() {
        @Override
        public Node<Object> createNode(Node<Object> source, @Nullable Node<Object> left,
            @Nullable Node<Object> right) {
          return new Node<Object>(source.getKey(), source.elemCount(), left, right);
        }
      };

  private abstract class MultisetModifier implements BstModifier<E, Node<E>> {
    abstract int newCount(int oldCount);

    @Nullable
    @Override
    public BstModificationResult<Node<E>> modify(E key, @Nullable Node<E> originalEntry) {
      int oldCount = countOrZero(originalEntry);
      int newCount = newCount(oldCount);
      if (oldCount == newCount) {
        return BstModificationResult.identity(originalEntry);
      } else if (newCount == 0) {
        return BstModificationResult.rebalancingChange(originalEntry, null);
      } else if (oldCount == 0) {
        return BstModificationResult.rebalancingChange(null, new Node<E>(key, newCount));
      } else {
        return BstModificationResult.rebuildingChange(originalEntry,
            new Node<E>(originalEntry.getKey(), newCount));
      }
    }
  }

  private final class AddModifier extends MultisetModifier {
    private final int countToAdd;

    private AddModifier(int countToAdd) {
      checkArgument(countToAdd > 0);
      this.countToAdd = countToAdd;
    }

    @Override
    int newCount(int oldCount) {
      checkArgument(countToAdd <= Integer.MAX_VALUE - oldCount, "Cannot add this many elements");
      return oldCount + countToAdd;
    }
  }

  private final class RemoveModifier extends MultisetModifier {
    private final int countToRemove;

    private RemoveModifier(int countToRemove) {
      checkArgument(countToRemove > 0);
      this.countToRemove = countToRemove;
    }

    @Override
    int newCount(int oldCount) {
      return Math.max(0, oldCount - countToRemove);
    }
  }

  private final class SetCountModifier extends MultisetModifier {
    private final int countToSet;

    private SetCountModifier(int countToSet) {
      checkArgument(countToSet >= 0);
      this.countToSet = countToSet;
    }

    @Override
    int newCount(int oldCount) {
      return countToSet;
    }
  }

  private final class ConditionalSetCountModifier extends MultisetModifier {
    private final int expectedCount;
    private final int setCount;

    private ConditionalSetCountModifier(int expectedCount, int setCount) {
      checkArgument(setCount >= 0 & expectedCount >= 0);
      this.expectedCount = expectedCount;
      this.setCount = setCount;
    }

    @Override
    int newCount(int oldCount) {
      return (oldCount == expectedCount) ? setCount : oldCount;
    }
  }

  /*
   * TODO(jlevy): Decide whether entrySet() should return entries with an
   * equals() method that calls the comparator to compare the two keys. If that
   * change is made, AbstractMultiset.equals() can simply check whether two
   * multisets have equal entry sets.
   */
}
