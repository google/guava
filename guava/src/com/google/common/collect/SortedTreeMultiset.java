/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;

import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

/**
 * An implementation of a sorted multiset based on size balanced binary trees, as described in
 * Stephen Adams, "Efficient sets: a balancing act", Journal of Functional Programming
 * 3(4):553-562, October 1993, <a href="http://www.swiss.ai.mit.edu/~adams/BB/">
 * http://www.swiss.ai.mit.edu/~adams/BB/</a>. It does not support null elements.
 *
 * <p>This implementation provides guaranteed log(n) time cost for the {@code contains}, {@code
 * count}, {@code add}, {@code remove}, {@code setCount} {@code firstEntry}, {@code lastEntry},
 * {@code pollFirstEntry}, and {@code pollLastEntry} operations, where n is the number of distinct
 * elements.
 *
 * <p>The iterators returned by {@code iterator()} and the {@code iterator()} methods of the
 * various collection views will, on a best-effort basis, throw a {@code
 * ConcurrentModificationException} in the event that the multiset is modified in any way while
 * iteration is in progress, except through the iterator's own {@code remove()} method.
 *
 * <p>The entries returned by {@code firstEntry}, {@code lastEntry}, {@code pollFirstEntry}, {@code
 * pollLastEntry}, and {@code entrySet().iterator()} are all snapshots of entries at the time they
 * were produced.
 *
 * @author Louis Wasserman
 */
final class SortedTreeMultiset<E> extends AbstractSortedMultiset<E> {
  /**
   * Returns an empty {@code SortedTreeMultiset} ordered by the natural ordering of its elements.
   */
  public static <E extends Comparable> SortedTreeMultiset<E> create() {
    return new SortedTreeMultiset<E>(Ordering.natural());
  }

  /**
   * Returns an empty {@code SortedTreeMultiset} ordered by the specified comparator.
   */
  public static <E> SortedTreeMultiset<E> create(Comparator<? super E> comparator) {
    return new SortedTreeMultiset<E>(comparator);
  }

  /**
   * Returns a {@code SortedTreeMultiset} ordered by the natural ordering of its elements,
   * initialized to contain the specified elements.
   */
  public static <E extends Comparable> SortedTreeMultiset<E> create(
      Iterable<? extends E> elements) {
    SortedTreeMultiset<E> multiset = create();
    Iterables.addAll(multiset, elements);
    return multiset;
  }

  private SortedTreeMultiset(Comparator<? super E> comparator) {
    super(comparator);
    this.range = GeneralRange.all(comparator);
    this.rootReference = new AtomicReference<Node>();
  }

  private SortedTreeMultiset(GeneralRange<E> range, AtomicReference<Node> root) {
    super(range.comparator());
    this.range = range;
    this.rootReference = root;
  }

  @SuppressWarnings("unchecked")
  E checkElement(Object o) {
    checkNotNull(o);
    return (E) o;
  }

  private final GeneralRange<E> range;

  private final AtomicReference<Node> rootReference;

  @Override
  int distinctElements() {
    Node root = rootReference.get();
    return BstRangeOps.totalInRange(distinctAggregate, range, root);
  }

  @Override
  public int size() {
    Node root = rootReference.get();
    return BstRangeOps.totalInRange(sizeAggregate, range, root);
  }

  @Override
  public int count(@Nullable Object element) {
    if (element == null) {
      return 0;
    }
    try {
      E e = checkElement(element);
      if (range.contains(e)) {
        Node node = BstOperations.seek(comparator(), rootReference.get(), e);
        return (node == null) ? 0 : node.elemOccurrences;
      }
      return 0;
    } catch (ClassCastException e) {
      return 0;
    }
  }

  private int mutate(E e, MultisetModifier modifier) {
    BstMutationRule<E, Node> mutationRule = BstMutationRule.createRule(modifier,
        BstCountBasedBalancePolicies.<E, Node>singleRebalancePolicy(distinctAggregate),
        nodeFactory);
    BstMutationResult<E, Node> mutationResult =
        BstOperations.mutate(comparator(), mutationRule, rootReference.get(), e);
    if (!rootReference.compareAndSet(
        mutationResult.getOriginalRoot(), mutationResult.getChangedRoot())) {
      throw new ConcurrentModificationException();
    }
    Node original = mutationResult.getOriginalTarget();
    return (original == null) ? 0 : original.elemOccurrences;
  }

  @Override
  public int add(E element, int occurrences) {
    checkNotNull(element);
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
    checkNotNull(element);
    checkArgument(range.contains(element));
    return mutate(element, new ConditionalSetCountModifier(oldCount, newCount)) == oldCount;
  }

  @Override
  public int setCount(E element, int count) {
    checkNotNull(element);
    checkArgument(range.contains(element));
    return mutate(element, new SetCountModifier(count));
  }

  private transient final BstPathFactory<Node, BstInOrderPath<Node>> pathFactory =
      BstInOrderPath.inOrderFactory();

  @Override
  Iterator<Entry<E>> entryIterator() {
    Node root = rootReference.get();
    final BstInOrderPath<Node> startingPath =
        BstRangeOps.furthestPath(range, LEFT, pathFactory, root);
    return iteratorInDirection(startingPath, RIGHT);
  }

  @Override
  Iterator<Entry<E>> descendingEntryIterator() {
    Node root = rootReference.get();
    final BstInOrderPath<Node> startingPath =
        BstRangeOps.furthestPath(range, RIGHT, pathFactory, root);
    return iteratorInDirection(startingPath, LEFT);
  }

  private Iterator<Entry<E>> iteratorInDirection(
      @Nullable BstInOrderPath<Node> start, final BstSide direction) {
    final Iterator<BstInOrderPath<Node>> pathIterator =
        new AbstractLinkedIterator<BstInOrderPath<Node>>(start) {
          @Override
          protected BstInOrderPath<Node> computeNext(BstInOrderPath<Node> previous) {
            if (!previous.hasNext(direction)) {
              return null;
            }
            BstInOrderPath<Node> next = previous.next(direction);
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
        BstInOrderPath<Node> path = pathIterator.next();
        return Multisets.immutableEntry(
            toRemove = path.getTip().getKey(), path.getTip().elemOccurrences);
      }

      @Override
      public void remove() {
        checkState(toRemove != null);
        setCount(toRemove, 0);
        toRemove = null;
      }
    };
  }

  @Override
  public void clear() {
    Node root = rootReference.get();
    Node cleared = BstRangeOps.minusRange(range,
        BstCountBasedBalancePolicies.<E, Node>fullRebalancePolicy(distinctAggregate),
        nodeFactory, root);
    if (!rootReference.compareAndSet(root, cleared)) {
      throw new ConcurrentModificationException();
    }
  }

  @Override
  public SortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
    checkNotNull(upperBound);
    return new SortedTreeMultiset<E>(
        range.intersect(GeneralRange.upTo(comparator, upperBound, boundType)), rootReference);
  }

  @Override
  public SortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
    checkNotNull(lowerBound);
    return new SortedTreeMultiset<E>(
        range.intersect(GeneralRange.downTo(comparator, lowerBound, boundType)), rootReference);
  }

  private final class Node extends BstNode<E, Node> {
    private final int elemOccurrences;
    private final int size;
    private final int distinct;

    private Node(E key, int elemCount, @Nullable Node left, @Nullable Node right) {
      super(checkElement(key), left, right);
      checkArgument(elemCount > 0);
      this.elemOccurrences = elemCount;
      this.size = elemCount + sizeOrZero(left) + sizeOrZero(right);
      this.distinct = 1 + distinctOrZero(left) + distinctOrZero(right);
    }

    private Node(E key, int elemCount) {
      this(key, elemCount, null, null);
    }
  }

  private int sizeOrZero(@Nullable Node node) {
    return (node == null) ? 0 : node.size;
  }

  private int distinctOrZero(@Nullable Node node) {
    return (node == null) ? 0 : node.distinct;
  }

  private transient final BstAggregate<Node> distinctAggregate = new BstAggregate<Node>() {
    @Override
    public int entryValue(Node entry) {
      return 1;
    }

    @Override
    public int treeValue(@Nullable Node tree) {
      return distinctOrZero(tree);
    }
  };

  private transient final BstAggregate<Node> sizeAggregate = new BstAggregate<Node>() {
    @Override
    public int entryValue(Node entry) {
      return entry.elemOccurrences;
    }

    @Override
    public int treeValue(@Nullable Node tree) {
      return sizeOrZero(tree);
    }
  };

  private transient final BstNodeFactory<Node> nodeFactory = new BstNodeFactory<Node>() {
    @Override
    public Node createNode(Node source, @Nullable Node left, @Nullable Node right) {
      return new Node(source.getKey(), source.elemOccurrences, left, right);
    }
  };

  private abstract class MultisetModifier implements BstModifier<E, Node> {
    abstract int newCount(int oldCount);

    @Nullable
    @Override
    public BstModificationResult<Node> modify(E key, @Nullable Node originalEntry) {
      int oldCount = (originalEntry == null) ? 0 : originalEntry.elemOccurrences;
      int newCount = newCount(oldCount);
      if (oldCount == newCount) {
        return BstModificationResult.identity(originalEntry);
      } else if (newCount == 0) {
        return BstModificationResult.rebalancingChange(originalEntry, null);
      } else if (oldCount == 0) {
        return BstModificationResult.rebalancingChange(null, new Node(key, newCount));
      } else {
        return BstModificationResult.rebuildingChange(originalEntry, new Node(key, newCount));
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
}
