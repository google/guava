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
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.DoNotMock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.CheckForNull;

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
@DoNotMock(
    "Call forGraph or forTree, passing a lambda or a Graph with the desired edges (built with"
        + " GraphBuilder)")
@ElementTypesAreNonnullByDefault
public abstract class Traverser<N> {
  private final SuccessorsFunction<N> successorFunction;

  private Traverser(SuccessorsFunction<N> successorFunction) {
    this.successorFunction = checkNotNull(successorFunction);
  }

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
  public static <N> Traverser<N> forGraph(final SuccessorsFunction<N> graph) {
    return new Traverser<N>(graph) {
      @Override
      Traversal<N> newTraversal() {
        return Traversal.inGraph(graph);
      }
    };
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
  public static <N> Traverser<N> forTree(final SuccessorsFunction<N> tree) {
    if (tree instanceof BaseGraph) {
      checkArgument(((BaseGraph<?>) tree).isDirected(), "Undirected graphs can never be trees.");
    }
    if (tree instanceof Network) {
      checkArgument(((Network<?, ?>) tree).isDirected(), "Undirected networks can never be trees.");
    }
    return new Traverser<N>(tree) {
      @Override
      Traversal<N> newTraversal() {
        return Traversal.inTree(tree);
      }
    };
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
  public final Iterable<N> breadthFirst(N startNode) {
    return breadthFirst(ImmutableSet.of(startNode));
  }

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
  public final Iterable<N> breadthFirst(Iterable<? extends N> startNodes) {
    final ImmutableSet<N> validated = validate(startNodes);
    return new Iterable<N>() {
      @Override
      public Iterator<N> iterator() {
        return newTraversal().breadthFirst(validated.iterator());
      }
    };
  }

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
  public final Iterable<N> depthFirstPreOrder(N startNode) {
    return depthFirstPreOrder(ImmutableSet.of(startNode));
  }

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
  public final Iterable<N> depthFirstPreOrder(Iterable<? extends N> startNodes) {
    final ImmutableSet<N> validated = validate(startNodes);
    return new Iterable<N>() {
      @Override
      public Iterator<N> iterator() {
        return newTraversal().preOrder(validated.iterator());
      }
    };
  }

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
  public final Iterable<N> depthFirstPostOrder(N startNode) {
    return depthFirstPostOrder(ImmutableSet.of(startNode));
  }

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
  public final Iterable<N> depthFirstPostOrder(Iterable<? extends N> startNodes) {
    final ImmutableSet<N> validated = validate(startNodes);
    return new Iterable<N>() {
      @Override
      public Iterator<N> iterator() {
        return newTraversal().postOrder(validated.iterator());
      }
    };
  }

  abstract Traversal<N> newTraversal();

  @SuppressWarnings("CheckReturnValue")
  private ImmutableSet<N> validate(Iterable<? extends N> startNodes) {
    ImmutableSet<N> copy = ImmutableSet.copyOf(startNodes);
    for (N node : copy) {
      successorFunction.successors(node); // Will throw if node doesn't exist
    }
    return copy;
  }

  /**
   * Abstracts away the difference between traversing a graph vs. a tree. For a tree, we just take
   * the next element from the next non-empty iterator; for graph, we need to loop through the next
   * non-empty iterator to find first unvisited node.
   */
  private abstract static class Traversal<N> {
    final SuccessorsFunction<N> successorFunction;

    Traversal(SuccessorsFunction<N> successorFunction) {
      this.successorFunction = successorFunction;
    }

    static <N> Traversal<N> inGraph(SuccessorsFunction<N> graph) {
      final Set<N> visited = new HashSet<>();
      return new Traversal<N>(graph) {
        @Override
        @CheckForNull
        N visitNext(Deque<Iterator<? extends N>> horizon) {
          Iterator<? extends N> top = horizon.getFirst();
          while (top.hasNext()) {
            N element = top.next();
            // requireNonNull is safe because horizon contains only graph nodes.
            /*
             * TODO(cpovirk): Replace these two statements with one (`N element =
             * requireNonNull(top.next())`) once our checker supports it.
             *
             * (The problem is likely
             * https://github.com/jspecify/nullness-checker-for-checker-framework/blob/61aafa4ae52594830cfc2d61c8b113009dbdb045/src/main/java/com/google/jspecify/nullness/NullSpecAnnotatedTypeFactory.java#L896)
             */
            requireNonNull(element);
            if (visited.add(element)) {
              return element;
            }
          }
          horizon.removeFirst();
          return null;
        }
      };
    }

    static <N> Traversal<N> inTree(SuccessorsFunction<N> tree) {
      return new Traversal<N>(tree) {
        @CheckForNull
        @Override
        N visitNext(Deque<Iterator<? extends N>> horizon) {
          Iterator<? extends N> top = horizon.getFirst();
          if (top.hasNext()) {
            return checkNotNull(top.next());
          }
          horizon.removeFirst();
          return null;
        }
      };
    }

    final Iterator<N> breadthFirst(Iterator<? extends N> startNodes) {
      return topDown(startNodes, InsertionOrder.BACK);
    }

    final Iterator<N> preOrder(Iterator<? extends N> startNodes) {
      return topDown(startNodes, InsertionOrder.FRONT);
    }

    /**
     * In top-down traversal, an ancestor node is always traversed before any of its descendant
     * nodes. The traversal order among descendant nodes (particularly aunts and nieces) are
     * determined by the {@code InsertionOrder} parameter: nieces are placed at the FRONT before
     * aunts for pre-order; while in BFS they are placed at the BACK after aunts.
     */
    private Iterator<N> topDown(Iterator<? extends N> startNodes, final InsertionOrder order) {
      final Deque<Iterator<? extends N>> horizon = new ArrayDeque<>();
      horizon.add(startNodes);
      return new AbstractIterator<N>() {
        @Override
        @CheckForNull
        protected N computeNext() {
          do {
            N next = visitNext(horizon);
            if (next != null) {
              Iterator<? extends N> successors = successorFunction.successors(next).iterator();
              if (successors.hasNext()) {
                // BFS: horizon.addLast(successors)
                // Pre-order: horizon.addFirst(successors)
                order.insertInto(horizon, successors);
              }
              return next;
            }
          } while (!horizon.isEmpty());
          return endOfData();
        }
      };
    }

    final Iterator<N> postOrder(Iterator<? extends N> startNodes) {
      final Deque<N> ancestorStack = new ArrayDeque<>();
      final Deque<Iterator<? extends N>> horizon = new ArrayDeque<>();
      horizon.add(startNodes);
      return new AbstractIterator<N>() {
        @Override
        @CheckForNull
        protected N computeNext() {
          for (N next = visitNext(horizon); next != null; next = visitNext(horizon)) {
            Iterator<? extends N> successors = successorFunction.successors(next).iterator();
            if (!successors.hasNext()) {
              return next;
            }
            horizon.addFirst(successors);
            ancestorStack.push(next);
          }
          return ancestorStack.isEmpty() ? endOfData() : ancestorStack.pop();
        }
      };
    }

    /**
     * Visits the next node from the top iterator of {@code horizon} and returns the visited node.
     * Null is returned to indicate reaching the end of the top iterator.
     *
     * <p>For example, if horizon is {@code [[a, b], [c, d], [e]]}, {@code visitNext()} will return
     * {@code [a, b, null, c, d, null, e, null]} sequentially, encoding the topological structure.
     * (Note, however, that the callers of {@code visitNext()} often insert additional iterators
     * into {@code horizon} between calls to {@code visitNext()}. This causes them to receive
     * additional values interleaved with those shown above.)
     */
    @CheckForNull
    abstract N visitNext(Deque<Iterator<? extends N>> horizon);
  }

  /** Poor man's method reference for {@code Deque::addFirst} and {@code Deque::addLast}. */
  private enum InsertionOrder {
    FRONT {
      @Override
      <T> void insertInto(Deque<T> deque, T value) {
        deque.addFirst(value);
      }
    },
    BACK {
      @Override
      <T> void insertInto(Deque<T> deque, T value) {
        deque.addLast(value);
      }
    };

    abstract <T> void insertInto(Deque<T> deque, T value);
  }
}
