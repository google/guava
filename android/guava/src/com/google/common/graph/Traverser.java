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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

/**
 * Provides methods for traversing a graph.
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
   * <p>If {@code graph} is known to be tree-shaped, consider using {@link
   * #forTree(SuccessorsFunction)} instead.
   *
   * <p><b>Performance notes</b>
   *
   * <ul>
   *   <li>Traversals require <i>O(n)</i> time (where <i>n</i> is the number of nodes reachable from
   *       the start node), assuming that the node objects have <i>O(1)</i> {@code equals()} and
   *       {@code hashCode()} implementations.
   *   <li>While traversing, the traverser will use <i>O(n)</i> space (where <i>n</i> is the number
   *       of nodes that have thus far been visited), plus <i>O(H)</i> space (where <i>H</i> is the
   *       number of nodes that have been seen but not yet visited, that is, the "horizon").
   * </ul>
   *
   * @param graph {@link SuccessorsFunction} representing a general graph that may have cycles.
   */
  public static <N> Traverser<N> forGraph(SuccessorsFunction<N> graph) {
    return new GraphTraverser<>(graph);
  }

  /**
   * Creates a new traverser for a directed acyclic graph that has at most one path from the start
   * node to any node reachable from the start node, such as a tree.
   *
   * <p>Providing graphs that don't conform to the above description may lead to:
   *
   * <ul>
   *   <li>Traversal not terminating (if the graph has cycles)
   *   <li>Nodes being visited multiple times (if multiple paths exist from the start node to any
   *       node reachable from it)
   * </ul>
   *
   * In these cases, use {@link #forGraph(SuccessorsFunction)} instead.
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
   * <p><b>Examples</b>
   *
   * <p>This is a valid input graph (all edges are directed facing downwards):
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
   * <p>This is <b>not</b> a valid input graph (all edges are directed facing downwards):
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
   * <p>because there are two paths from {@code b} to {@code f} ({@code b->d->f} and {@code
   * b->e->f}).
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
    // TODO(b/27898002): Implement
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Returns an unmodifiable iterable over the nodes in the graph, using breadth-first traversal.
   * That is, all the nodes of depth 0 are returned, then depth 1, then 2, and so on.
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
   * <p>The returned iterable can be iterated over multiple times. Every iterator will compute its
   * next element on the fly. It is thus possible to limit the traversal to a certain number of
   * nodes as follows:
   *
   * <pre>{@code
   * Iterables.limit(Traverser.forGraph(graph).breadthFirst(node), maxNumberOfNodes);
   * }</pre>
   *
   * <p>See <a href="https://en.wikipedia.org/wiki/Breadth-first_search">Wikipedia</a> for more
   * info.
   */
  public abstract Iterable<N> breadthFirst(N startNode);

  /**
   * Returns an unmodifiable iterable over the nodes in the graph, using depth-first pre-order
   * traversal. That is, the nodes are returned in the order they are visited for the first time.
   *
   * <p><b>Example:</b> The following graph with {@code startNode} {@code a} would return nodes in
   * the order {@code abcdef} (assuming successors are returned in alphabetical order).
   *
   * <pre>{@code
   * b ---- a ---- f
   * |      |
   * |      |
   * c ---- d ---- e
   * }</pre>
   *
   * <p>The behavior of this method is undefined if the nodes, or the topology of the graph, change
   * while iteration is in progress.
   *
   * <p>The returned iterable can be iterated over multiple times. Every iterator will compute its
   * next element on the fly. It is thus possible to limit the traversal to a certain number of
   * nodes as follows:
   *
   * <pre>{@code
   * Iterables.limit(
   *     Traverser.forGraph(graph).depthFirstPreOrder(node), maxNumberOfNodes);
   * }</pre>
   *
   * <p>See <a href="https://en.wikipedia.org/wiki/Depth-first_search">Wikipedia</a> for more info.
   */
  public abstract Iterable<N> depthFirstPreOrder(N startNode);

  /**
   * Returns an unmodifiable iterable over the nodes in the graph, using depth-first post-order
   * traversal. That is, the nodes are returned in the order they are visited for the last time.
   *
   * <p><b>Example:</b> The following graph with {@code startNode} {@code a} would return nodes in
   * the order {@code edcbfa} (assuming successors are returned in alphabetical order).
   *
   * <pre>{@code
   * b ---- a ---- f
   * |      |
   * |      |
   * c ---- d ---- e
   * }</pre>
   *
   * <p>The behavior of this method is undefined if the nodes, or the topology of the graph, change
   * while iteration is in progress.
   *
   * <p>The returned iterable can be iterated over multiple times. Every iterator will compute its
   * next element on the fly. It is thus possible to limit the traversal to a certain number of
   * nodes as follows:
   *
   * <pre>{@code
   * Iterables.limit(
   *     Traverser.forGraph(graph).depthFirstPostOrder(node), maxNumberOfNodes);
   * }</pre>
   *
   * <p>See <a href="https://en.wikipedia.org/wiki/Depth-first_search">Wikipedia</a> for more info.
   */
  public abstract Iterable<N> depthFirstPostOrder(N startNode);

  private static final class GraphTraverser<N> extends Traverser<N> {
    private final SuccessorsFunction<N> graph;

    GraphTraverser(SuccessorsFunction<N> graph) {
      this.graph = checkNotNull(graph);
    }

    @Override
    public Iterable<N> breadthFirst(final N startNode) {
      return new Iterable<N>() {
        @Override
        public Iterator<N> iterator() {
          return new BreadthFirstIterator(startNode);
        }
      };
    }

    @Override
    public Iterable<N> depthFirstPreOrder(final N startNode) {
      return new Iterable<N>() {
        @Override
        public Iterator<N> iterator() {
          return new DepthFirstIterator(startNode, Order.PREORDER);
        }
      };
    }

    @Override
    public Iterable<N> depthFirstPostOrder(final N startNode) {
      return new Iterable<N>() {
        @Override
        public Iterator<N> iterator() {
          return new DepthFirstIterator(startNode, Order.POSTORDER);
        }
      };
    }

    private final class BreadthFirstIterator extends UnmodifiableIterator<N> {
      private final Queue<N> queue = new ArrayDeque<>();
      private final Set<N> visited = new HashSet<>();

      BreadthFirstIterator(N root) {
        queue.add(root);
        visited.add(root);
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

      DepthFirstIterator(N root, Order order) {
        // our invariant is that in computeNext we call next on the iterator at the top first, so we
        // need to start with one additional item on that iterator
        stack.push(withSuccessors(root));
        this.order = order;
      }

      @Override
      protected N computeNext() {
        while (true) {
          if (stack.isEmpty()) {
            return endOfData();
          }
          NodeAndSuccessors node = stack.getFirst();
          boolean firstVisit = visited.add(node.node);
          boolean lastVisit = !node.successors.hasNext();
          boolean produceNode =
              (firstVisit && order == Order.PREORDER) || (lastVisit && order == Order.POSTORDER);
          if (lastVisit) {
            stack.pop();
          } else {
            // we need to push a neighbor, but only if we haven't already seen it
            N child = node.successors.next();
            if (!visited.contains(child)) {
              stack.push(withSuccessors(child));
            }
          }
          if (produceNode) {
            return node.node;
          }
        }
      }

      NodeAndSuccessors withSuccessors(N node) {
        return new NodeAndSuccessors(node, graph.successors(node));
      }

      /** A simple tuple of a node and a partially iterated {@link Iterator} of its successors. */
      private final class NodeAndSuccessors {
        final N node;
        final Iterator<? extends N> successors;

        NodeAndSuccessors(N node, Iterable<? extends N> successors) {
          this.node = node;
          this.successors = successors.iterator();
        }
      }
    }
  }

  private enum Order {
    PREORDER,
    POSTORDER
  }
}
