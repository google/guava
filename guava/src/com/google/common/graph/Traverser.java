/*
 * Copyright (C) 2017 The Guava Authors
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

package com.google.common.graph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An object that can traverse the nodes that are reachable from a specified (set of) start node(s)
 * using a specified {@link SuccessorsFunction}.
 *
 * <p>There are two entry points for creating a {@code Traverser}: {@link
 * #forTree(SuccessorsFunction)} and {@link #forGraph(SuccessorsFunction)}. You should choose one
 * based on your answers to the following questions:
 *
 * <ol>
 *   <li>Is there only one path to any node that's reachable from any start node? (If so, the graph
 *       to be traversed is a tree or forest even if it is a subgraph of a graph which is neither.)
 *   <li>Are the node objects' implementations of {@code equals()}/{@code hashCode()} <a
 *       href="https://github.com/google/guava/wiki/GraphsExplained#non-recursiveness">recursive</a>?
 * </ol>
 *
 * <p>If your answers are:
 *
 * <ul>
 *   <li>(1) "no" and (2) "no", use {@link #forGraph(SuccessorsFunction)}.
 *   <li>(1) "yes" and (2) "yes", use {@link #forTree(SuccessorsFunction)}.
 *   <li>(1) "yes" and (2) "no", you can use either, but {@code forTree()} will be more efficient.
 *   <li>(1) "no" and (2) "yes", <b><i>neither will work</i></b>, but if you transform your node
 *       objects into a non-recursive form, you can use {@code forGraph()}.
 * </ul>
 *
 * @author Jens Nyman
 * @param <N> Node parameter type
 * @since 23.1
 */
@Beta
public abstract class Traverser<N> {

  /**
   * Creates a new traverser for the given general {@code graph}.
   *
   * <p>Traversers created using this method are guaranteed to visit each node reachable from the
   * start node(s) at most once.
   *
   * <p>If you know that no node in {@code graph} is reachable by more than one path from the start
   * node(s), consider using {@link #forTree(SuccessorsFunction)} instead.
   *
   * <p><b>Performance notes</b>
   *
   * <ul>
   *   <li>Traversals require <i>O(n)</i> time (where <i>n</i> is the number of nodes reachable from
   *       the start node), assuming that the node objects have <i>O(1)</i> {@code equals()} and
   *       {@code hashCode()} implementations. (See the <a
   *       href="https://github.com/google/guava/wiki/GraphsExplained#elements-must-be-useable-as-map-keys">
   *       notes on element objects</a> for more information.)
   *   <li>While traversing, the traverser will use <i>O(n)</i> space (where <i>n</i> is the number
   *       of nodes that have thus far been visited), plus <i>O(H)</i> space (where <i>H</i> is the
   *       number of nodes that have been seen but not yet visited, that is, the "horizon").
   * </ul>
   *
   * @param graph {@link SuccessorsFunction} representing a general graph that may have cycles.
   */
  public static <N> Traverser<N> forGraph(SuccessorsFunction<N> graph) {
    checkNotNull(graph);
    return new GraphTraverser<>(graph);
  }

  /**
   * Creates a new traverser for a directed acyclic graph that has at most one path from the start
   * node(s) to any node reachable from the start node(s), and has no paths from any start node to
   * any other start node, such as a tree or forest.
   *
   * <p>{@code forTree()} is especially useful (versus {@code forGraph()}) in cases where the data
   * structure being traversed is, in addition to being a tree/forest, also defined <a
   * href="https://github.com/google/guava/wiki/GraphsExplained#non-recursiveness">recursively</a>.
   * This is because the {@code forTree()}-based implementations don't keep track of visited nodes,
   * and therefore don't need to call `equals()` or `hashCode()` on the node objects; this saves
   * both time and space versus traversing the same graph using {@code forGraph()}.
   *
   * <p>Providing a graph to be traversed for which there is more than one path from the start
   * node(s) to any node may lead to:
   *
   * <ul>
   *   <li>Traversal not terminating (if the graph has cycles)
   *   <li>Nodes being visited multiple times (if multiple paths exist from any start node to any
   *       node reachable from any start node)
   * </ul>
   *
   * <p><b>Performance notes</b>
   *
   * <ul>
   *   <li>Traversals require <i>O(n)</i> time (where <i>n</i> is the number of nodes reachable from
   *       the start node).
   *   <li>While traversing, the traverser will use <i>O(H)</i> space (where <i>H</i> is the number
   *       of nodes that have been seen but not yet visited, that is, the "horizon").
   * </ul>
   *
   * <p><b>Examples</b> (all edges are directed facing downwards)
   *
   * <p>The graph below would be valid input with start nodes of {@code a, f, c}. However, if {@code
   * b} were <i>also</i> a start node, then there would be multiple paths to reach {@code e} and
   * {@code h}.
   *
   * <pre>{@code
   *    a     b      c
   *   / \   / \     |
   *  /   \ /   \    |
   * d     e     f   g
   *       |
   *       |
   *       h
   * }</pre>
   *
   * <p>.
   *
   * <p>The graph below would be a valid input with start nodes of {@code a, f}. However, if {@code
   * b} were a start node, there would be multiple paths to {@code f}.
   *
   * <pre>{@code
   *    a     b
   *   / \   / \
   *  /   \ /   \
   * c     d     e
   *        \   /
   *         \ /
   *          f
   * }</pre>
   *
   * <p><b>Note on binary trees</b>
   *
   * <p>This method can be used to traverse over a binary tree. Given methods {@code
   * leftChild(node)} and {@code rightChild(node)}, this method can be called as
   *
   * <pre>{@code
   * Traverser.forTree(node -> ImmutableList.of(leftChild(node), rightChild(node)));
   * }</pre>
   *
   * @param tree {@link SuccessorsFunction} representing a directed acyclic graph that has at most
   *     one path between any two nodes
   */
  public static <N> Traverser<N> forTree(SuccessorsFunction<N> tree) {
    checkNotNull(tree);
    if (tree instanceof BaseGraph) {
      checkArgument(((BaseGraph<?>) tree).isDirected(), "Undirected graphs can never be trees.");
    }
    if (tree instanceof Network) {
      checkArgument(((Network<?, ?>) tree).isDirected(), "Undirected networks can never be trees.");
    }
    return new TreeTraverser<>(tree);
  }

  /**
   * Returns an unmodifiable {@code Iterable} over the nodes reachable from {@code startNode}, in
   * the order of a breadth-first traversal. That is, all the nodes of depth 0 are returned, then
   * depth 1, then 2, and so on.
   *
   * <p><b>Example:</b> The following graph with {@code startNode} {@code a} would return nodes in
   * the order {@code abcdef} (assuming successors are returned in alphabetical order).
   *
   * <pre>{@code
   * b ---- a ---- d
   * |      |
   * |      |
   * e ---- c ---- f
   * }</pre>
   *
   * <p>The behavior of this method is undefined if the nodes, or the topology of the graph, change
   * while iteration is in progress.
   *
   * <p>The returned {@code Iterable} can be iterated over multiple times. Every iterator will
   * compute its next element on the fly. It is thus possible to limit the traversal to a certain
   * number of nodes as follows:
   *
   * <pre>{@code
   * Iterables.limit(Traverser.forGraph(graph).breadthFirst(node), maxNumberOfNodes);
   * }</pre>
   *
   * <p>See <a href="https://en.wikipedia.org/wiki/Breadth-first_search">Wikipedia</a> for more
   * info.
   *
   * @throws IllegalArgumentException if {@code startNode} is not an element of the graph
   */
  public abstract Iterable<N> breadthFirst(N startNode);

  /**
   * Returns an unmodifiable {@code Iterable} over the nodes reachable from any of the {@code
   * startNodes}, in the order of a breadth-first traversal. This is equivalent to a breadth-first
   * traversal of a graph with an additional root node whose successors are the listed {@code
   * startNodes}.
   *
   * @throws IllegalArgumentException if any of {@code startNodes} is not an element of the graph
   * @see #breadthFirst(Object)
   * @since 24.1
   */
  public abstract Iterable<N> breadthFirst(Iterable<? extends N> startNodes);

  /**
   * Returns an unmodifiable {@code Iterable} over the nodes reachable from {@code startNode}, in
   * the order of a depth-first pre-order traversal. "Pre-order" implies that nodes appear in the
   * {@code Iterable} in the order in which they are first visited.
   *
   * <p><b>Example:</b> The following graph with {@code startNode} {@code a} would return nodes in
   * the order {@code abecfd} (assuming successors are returned in alphabetical order).
   *
   * <pre>{@code
   * b ---- a ---- d
   * |      |
   * |      |
   * e ---- c ---- f
   * }</pre>
   *
   * <p>The behavior of this method is undefined if the nodes, or the topology of the graph, change
   * while iteration is in progress.
   *
   * <p>The returned {@code Iterable} can be iterated over multiple times. Every iterator will
   * compute its next element on the fly. It is thus possible to limit the traversal to a certain
   * number of nodes as follows:
   *
   * <pre>{@code
   * Iterables.limit(
   *     Traverser.forGraph(graph).depthFirstPreOrder(node), maxNumberOfNodes);
   * }</pre>
   *
   * <p>See <a href="https://en.wikipedia.org/wiki/Depth-first_search">Wikipedia</a> for more info.
   *
   * @throws IllegalArgumentException if {@code startNode} is not an element of the graph
   */
  public abstract Iterable<N> depthFirstPreOrder(N startNode);

  /**
   * Returns an unmodifiable {@code Iterable} over the nodes reachable from any of the {@code
   * startNodes}, in the order of a depth-first pre-order traversal. This is equivalent to a
   * depth-first pre-order traversal of a graph with an additional root node whose successors are
   * the listed {@code startNodes}.
   *
   * @throws IllegalArgumentException if any of {@code startNodes} is not an element of the graph
   * @see #depthFirstPreOrder(Object)
   * @since 24.1
   */
  public abstract Iterable<N> depthFirstPreOrder(Iterable<? extends N> startNodes);

  /**
   * Returns an unmodifiable {@code Iterable} over the nodes reachable from {@code startNode}, in
   * the order of a depth-first post-order traversal. "Post-order" implies that nodes appear in the
   * {@code Iterable} in the order in which they are visited for the last time.
   *
   * <p><b>Example:</b> The following graph with {@code startNode} {@code a} would return nodes in
   * the order {@code fcebda} (assuming successors are returned in alphabetical order).
   *
   * <pre>{@code
   * b ---- a ---- d
   * |      |
   * |      |
   * e ---- c ---- f
   * }</pre>
   *
   * <p>The behavior of this method is undefined if the nodes, or the topology of the graph, change
   * while iteration is in progress.
   *
   * <p>The returned {@code Iterable} can be iterated over multiple times. Every iterator will
   * compute its next element on the fly. It is thus possible to limit the traversal to a certain
   * number of nodes as follows:
   *
   * <pre>{@code
   * Iterables.limit(
   *     Traverser.forGraph(graph).depthFirstPostOrder(node), maxNumberOfNodes);
   * }</pre>
   *
   * <p>See <a href="https://en.wikipedia.org/wiki/Depth-first_search">Wikipedia</a> for more info.
   *
   * @throws IllegalArgumentException if {@code startNode} is not an element of the graph
   */
  public abstract Iterable<N> depthFirstPostOrder(N startNode);

  /**
   * Returns an unmodifiable {@code Iterable} over the nodes reachable from any of the {@code
   * startNodes}, in the order of a depth-first post-order traversal. This is equivalent to a
   * depth-first post-order traversal of a graph with an additional root node whose successors are
   * the listed {@code startNodes}.
   *
   * @throws IllegalArgumentException if any of {@code startNodes} is not an element of the graph
   * @see #depthFirstPostOrder(Object)
   * @since 24.1
   */
  public abstract Iterable<N> depthFirstPostOrder(Iterable<? extends N> startNodes);

  // Avoid subclasses outside of this class
  private Traverser() {}

  private static final class GraphTraverser<N> extends Traverser<N> {
    private final SuccessorsFunction<N> graph;

    GraphTraverser(SuccessorsFunction<N> graph) {
      this.graph = checkNotNull(graph);
    }

    @Override
    public Iterable<N> breadthFirst(final N startNode) {
      checkNotNull(startNode);
      return breadthFirst(ImmutableSet.of(startNode));
    }

    @Override
    public Iterable<N> breadthFirst(final Iterable<? extends N> startNodes) {
      checkNotNull(startNodes);
      if (Iterables.isEmpty(startNodes)) {
        return ImmutableSet.of();
      }
      for (N startNode : startNodes) {
        checkThatNodeIsInGraph(startNode);
      }
      return new Iterable<N>() {
        @Override
        public Iterator<N> iterator() {
          return new BreadthFirstIterator(startNodes);
        }
      };
    }

    @Override
    public Iterable<N> depthFirstPreOrder(final N startNode) {
      checkNotNull(startNode);
      return depthFirstPreOrder(ImmutableSet.of(startNode));
    }

    @Override
    public Iterable<N> depthFirstPreOrder(final Iterable<? extends N> startNodes) {
      checkNotNull(startNodes);
      if (Iterables.isEmpty(startNodes)) {
        return ImmutableSet.of();
      }
      for (N startNode : startNodes) {
        checkThatNodeIsInGraph(startNode);
      }
      return new Iterable<N>() {
        @Override
        public Iterator<N> iterator() {
          return new DepthFirstIterator(startNodes, Order.PREORDER);
        }
      };
    }

    @Override
    public Iterable<N> depthFirstPostOrder(final N startNode) {
      checkNotNull(startNode);
      return depthFirstPostOrder(ImmutableSet.of(startNode));
    }

    @Override
    public Iterable<N> depthFirstPostOrder(final Iterable<? extends N> startNodes) {
      checkNotNull(startNodes);
      if (Iterables.isEmpty(startNodes)) {
        return ImmutableSet.of();
      }
      for (N startNode : startNodes) {
        checkThatNodeIsInGraph(startNode);
      }
      return new Iterable<N>() {
        @Override
        public Iterator<N> iterator() {
          return new DepthFirstIterator(startNodes, Order.POSTORDER);
        }
      };
    }

    @SuppressWarnings("CheckReturnValue")
    private void checkThatNodeIsInGraph(N startNode) {
      // successors() throws an IllegalArgumentException for nodes that are not an element of the
      // graph.
      graph.successors(startNode);
    }

    private final class BreadthFirstIterator extends UnmodifiableIterator<N> {
      private final Queue<N> queue = new ArrayDeque<>();
      private final Set<N> visited = new HashSet<>();

      BreadthFirstIterator(Iterable<? extends N> roots) {
        for (N root : roots) {
          // add all roots to the queue, skipping duplicates
          if (visited.add(root)) {
            queue.add(root);
          }
        }
      }

      @Override
      public boolean hasNext() {
        return !queue.isEmpty();
      }

      @Override
      public N next() {
        N current = queue.remove();
        for (N neighbor : graph.successors(current)) {
          if (visited.add(neighbor)) {
            queue.add(neighbor);
          }
        }
        return current;
      }
    }

    private final class DepthFirstIterator extends AbstractIterator<N> {
      private final Deque<NodeAndSuccessors> stack = new ArrayDeque<>();
      private final Set<N> visited = new HashSet<>();
      private final Order order;

      DepthFirstIterator(Iterable<? extends N> roots, Order order) {
        stack.push(new NodeAndSuccessors(null, roots));
        this.order = order;
      }

      @Override
      protected N computeNext() {
        while (true) {
          if (stack.isEmpty()) {
            return endOfData();
          }
          NodeAndSuccessors nodeAndSuccessors = stack.getFirst();
          boolean firstVisit = visited.add(nodeAndSuccessors.node);
          boolean lastVisit = !nodeAndSuccessors.successorIterator.hasNext();
          boolean produceNode =
              (firstVisit && order == Order.PREORDER) || (lastVisit && order == Order.POSTORDER);
          if (lastVisit) {
            stack.pop();
          } else {
            // we need to push a neighbor, but only if we haven't already seen it
            N successor = nodeAndSuccessors.successorIterator.next();
            if (!visited.contains(successor)) {
              stack.push(withSuccessors(successor));
            }
          }
          if (produceNode && nodeAndSuccessors.node != null) {
            return nodeAndSuccessors.node;
          }
        }
      }

      NodeAndSuccessors withSuccessors(N node) {
        return new NodeAndSuccessors(node, graph.successors(node));
      }

      /** A simple tuple of a node and a partially iterated {@link Iterator} of its successors. */
      private final class NodeAndSuccessors {
        final @Nullable N node;
        final Iterator<? extends N> successorIterator;

        NodeAndSuccessors(@Nullable N node, Iterable<? extends N> successors) {
          this.node = node;
          this.successorIterator = successors.iterator();
        }
      }
    }
  }

  private static final class TreeTraverser<N> extends Traverser<N> {
    private final SuccessorsFunction<N> tree;

    TreeTraverser(SuccessorsFunction<N> tree) {
      this.tree = checkNotNull(tree);
    }

    @Override
    public Iterable<N> breadthFirst(final N startNode) {
      checkNotNull(startNode);
      return breadthFirst(ImmutableSet.of(startNode));
    }

    @Override
    public Iterable<N> breadthFirst(final Iterable<? extends N> startNodes) {
      checkNotNull(startNodes);
      if (Iterables.isEmpty(startNodes)) {
        return ImmutableSet.of();
      }
      for (N startNode : startNodes) {
        checkThatNodeIsInTree(startNode);
      }
      return new Iterable<N>() {
        @Override
        public Iterator<N> iterator() {
          return new BreadthFirstIterator(startNodes);
        }
      };
    }

    @Override
    public Iterable<N> depthFirstPreOrder(final N startNode) {
      checkNotNull(startNode);
      return depthFirstPreOrder(ImmutableSet.of(startNode));
    }

    @Override
    public Iterable<N> depthFirstPreOrder(final Iterable<? extends N> startNodes) {
      checkNotNull(startNodes);
      if (Iterables.isEmpty(startNodes)) {
        return ImmutableSet.of();
      }
      for (N node : startNodes) {
        checkThatNodeIsInTree(node);
      }
      return new Iterable<N>() {
        @Override
        public Iterator<N> iterator() {
          return new DepthFirstPreOrderIterator(startNodes);
        }
      };
    }

    @Override
    public Iterable<N> depthFirstPostOrder(final N startNode) {
      checkNotNull(startNode);
      return depthFirstPostOrder(ImmutableSet.of(startNode));
    }

    @Override
    public Iterable<N> depthFirstPostOrder(final Iterable<? extends N> startNodes) {
      checkNotNull(startNodes);
      if (Iterables.isEmpty(startNodes)) {
        return ImmutableSet.of();
      }
      for (N startNode : startNodes) {
        checkThatNodeIsInTree(startNode);
      }
      return new Iterable<N>() {
        @Override
        public Iterator<N> iterator() {
          return new DepthFirstPostOrderIterator(startNodes);
        }
      };
    }

    @SuppressWarnings("CheckReturnValue")
    private void checkThatNodeIsInTree(N startNode) {
      // successors() throws an IllegalArgumentException for nodes that are not an element of the
      // graph.
      tree.successors(startNode);
    }

    private final class BreadthFirstIterator extends UnmodifiableIterator<N> {
      private final Queue<N> queue = new ArrayDeque<>();

      BreadthFirstIterator(Iterable<? extends N> roots) {
        for (N root : roots) {
          queue.add(root);
        }
      }

      @Override
      public boolean hasNext() {
        return !queue.isEmpty();
      }

      @Override
      public N next() {
        N current = queue.remove();
        Iterables.addAll(queue, tree.successors(current));
        return current;
      }
    }

    private final class DepthFirstPreOrderIterator extends UnmodifiableIterator<N> {
      private final Deque<Iterator<? extends N>> stack = new ArrayDeque<>();

      DepthFirstPreOrderIterator(Iterable<? extends N> roots) {
        stack.addLast(roots.iterator());
      }

      @Override
      public boolean hasNext() {
        return !stack.isEmpty();
      }

      @Override
      public N next() {
        Iterator<? extends N> iterator = stack.getLast(); // throws NoSuchElementException if empty
        N result = checkNotNull(iterator.next());
        if (!iterator.hasNext()) {
          stack.removeLast();
        }
        Iterator<? extends N> childIterator = tree.successors(result).iterator();
        if (childIterator.hasNext()) {
          stack.addLast(childIterator);
        }
        return result;
      }
    }

    private final class DepthFirstPostOrderIterator extends AbstractIterator<N> {
      private final ArrayDeque<NodeAndChildren> stack = new ArrayDeque<>();

      DepthFirstPostOrderIterator(Iterable<? extends N> roots) {
        stack.addLast(new NodeAndChildren(null, roots));
      }

      @Override
      protected N computeNext() {
        while (!stack.isEmpty()) {
          NodeAndChildren top = stack.getLast();
          if (top.childIterator.hasNext()) {
            N child = top.childIterator.next();
            stack.addLast(withChildren(child));
          } else {
            stack.removeLast();
            if (top.node != null) {
              return top.node;
            }
          }
        }
        return endOfData();
      }

      NodeAndChildren withChildren(N node) {
        return new NodeAndChildren(node, tree.successors(node));
      }

      /** A simple tuple of a node and a partially iterated {@link Iterator} of its children. */
      private final class NodeAndChildren {
        final @Nullable N node;
        final Iterator<? extends N> childIterator;

        NodeAndChildren(@Nullable N node, Iterable<? extends N> children) {
          this.node = node;
          this.childIterator = children.iterator();
        }
      }
    }
  }

  private enum Order {
    PREORDER,
    POSTORDER
  }
}
