/*
 * Copyright (C) 2014 The Guava Authors
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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Static utility methods for {@link Graph} instances.
 *
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
public final class Graphs {

  private Graphs() {}

  /**
   * Returns the node at the other end of {@code edge} from {@code node}.
   *
   * @throws UnsupportedOperationException if {@code graph} is a {@link Hypergraph}
   * @throws IllegalArgumentException if {@code edge} is not incident to {@code node}
   */
  public static <N> N oppositeNode(Graph<N, ?> graph, Object edge, Object node) {
    if (graph instanceof Hypergraph) {
      throw new UnsupportedOperationException();
    }

    checkNotNull(node, "node");
    Iterator<N> incidentNodesIterator = graph.incidentNodes(edge).iterator();
    N node1 = incidentNodesIterator.next();
    N node2 = incidentNodesIterator.hasNext() ? incidentNodesIterator.next() : node1;
    if (node.equals(node1)) {
      return node2;
    } else {
      checkArgument(node.equals(node2), "Edge %s is not incident to node %s", edge, node);
      return node1;
    }
  }

  /**
   * Returns an unmodifiable view of edges that are parallel to {@code edge}, i.e. the set of edges
   * that connect the same nodes in the same direction (if any). An edge is not parallel to itself.
   *
   * @throws UnsupportedOperationException if {@code graph} is a {@link Hypergraph}
   * @throws IllegalArgumentException if {@code edge} is not present in {@code graph}
   */
  public static <N, E> Set<E> parallelEdges(Graph<N, E> graph, Object edge) {
    if (graph instanceof Hypergraph) {
      throw new UnsupportedOperationException();
    }

    Set<N> incidentNodes = graph.incidentNodes(edge); // Verifies that edge is in graph
    if (!graph.allowsParallelEdges()) {
      return ImmutableSet.of();
    }
    Iterator<N> incidentNodesIterator = incidentNodes.iterator();
    N node1 = incidentNodesIterator.next();
    N node2 = incidentNodesIterator.hasNext() ? incidentNodesIterator.next() : node1;
    return Sets.difference(graph.edgesConnecting(node1, node2), ImmutableSet.of(edge));
  }

  /**
   * Adds {@code edge} to {@code graph} with the specified incident {@code nodes}, in the order
   * returned by {@code nodes}' iterator.
   */
  @CanIgnoreReturnValue
  public static <N, E> boolean addEdge(Graph<N, E> graph, E edge, Iterable<N> nodes) {
    checkNotNull(graph, "graph");
    checkNotNull(edge, "edge");
    checkNotNull(nodes, "nodes");
    if (graph instanceof Hypergraph) {
      return ((Hypergraph<N, E>) graph).addEdge(edge, nodes);
    }

    Iterator<N> nodesIterator = nodes.iterator();
    checkArgument(nodesIterator.hasNext(),
        "'graph' is not a Hypergraph, and 'nodes' has < 1 elements: %s", nodes);
    N node1 = nodesIterator.next();
    N node2 = nodesIterator.hasNext() ? nodesIterator.next() : node1;
    checkArgument(!nodesIterator.hasNext(),
        "'graph' is not a Hypergraph, and 'nodes' has > 2 elements: %s", nodes);
    return graph.addEdge(edge, node1, node2);
  }

  /**
   * Creates a mutable copy of {@code graph}, using the same node and edge elements.
   */
  public static <N, E> Graph<N, E> copyOf(Graph<N, E> graph) {
    return copyOf(graph, Predicates.alwaysTrue(), Predicates.alwaysTrue());
  }

  /**
   * Creates a mutable copy of {@code graph}, using all of its elements that satisfy
   * {@code nodePredicate} and {@code edgePredicate}.
   */
  public static <N, E> Graph<N, E> copyOf(
      Graph<N, E> graph,
      Predicate<? super N> nodePredicate,
      Predicate<? super E> edgePredicate) {
    checkNotNull(graph, "graph");
    checkNotNull(nodePredicate, "nodePredicate");
    checkNotNull(edgePredicate, "edgePredicate");
    Graph<N, E> copy = GraphBuilder.from(graph)
        .expectedNodeCount(graph.nodes().size()).expectedEdgeCount(graph.edges().size()).build();
    mergeNodesFrom(graph, copy, nodePredicate);

    // We can't just call mergeEdgesFrom(graph, copy, edgePredicate) because addEdge() can add
    // the edge's incident nodes if they are not present.
    for (E edge : graph.edges()) {
      if (edgePredicate.apply(edge)) {
        Set<N> incidentNodes = graph.incidentNodes(edge);
        if (copy.nodes().containsAll(incidentNodes)) {
          addEdge(copy, edge, incidentNodes);
        }
      }
    }

    return copy;
  }

  /**
   * Copies all nodes from {@code original} into {@code copy}.
   */
  public static <N, E> void mergeNodesFrom(Graph<N, E> original, Graph<N, E> copy) {
    mergeNodesFrom(original, copy, Predicates.alwaysTrue());
  }

  /**
   * Copies all nodes from {@code original} into {@code copy} that satisfy {@code nodePredicate}.
   */
  public static <N, E> void mergeNodesFrom(
      Graph<N, E> original, Graph<N, E> copy, Predicate<? super N> nodePredicate) {
    checkNotNull(original, "original");
    checkNotNull(copy, "copy");
    checkNotNull(nodePredicate, "nodePredicate");
    for (N node : Sets.filter(original.nodes(), nodePredicate)) {
      copy.addNode(node);
    }
  }

  /**
   * Copies all edges from {@code original} into {@code copy}. Also copies all nodes incident
   * to these edges.
   */
  public static <N, E> void mergeEdgesFrom(Graph<N, E> original, Graph<N, E> copy) {
    mergeEdgesFrom(original, copy, Predicates.alwaysTrue());
  }

  /**
   * Copies all edges from {@code original} into {@code copy} that satisfy {@code edgePredicate}.
   * Also copies all nodes incident to these edges.
   */
  public static <N, E> void mergeEdgesFrom(
      Graph<N, E> original, Graph<N, E> copy, Predicate<? super E> edgePredicate) {
    checkNotNull(original, "original");
    checkNotNull(copy, "copy");
    checkNotNull(edgePredicate, "edgePredicate");
    for (E edge : Sets.filter(original.edges(), edgePredicate)) {
      addEdge(copy, edge, original.incidentNodes(edge));
    }
  }

  /**
   * Returns true iff {@code graph1} and {@code graph2} have the same node/edge relationships.
   *
   * @see Graph#equals(Object)
   */
  public static boolean equal(@Nullable Graph<?, ?> graph1, @Nullable Graph<?, ?> graph2) {
    if (graph1 == graph2) {
      return true;
    }

    if (graph1 == null || graph2 == null) {
      return false;
    }

    if (graph1.edges().size() != graph2.edges().size()) {
      return false;
    }

    if (!graph1.nodes().equals(graph2.nodes())) {
      return false;
    }

    for (Object node : graph1.nodes()) {
      if (!graph1.inEdges(node).equals(graph2.inEdges(node))) {
        return false;
      }
      boolean bothUndirected = !graph1.isDirected() && !graph2.isDirected();
      if (!bothUndirected && !graph1.outEdges(node).equals(graph2.outEdges(node))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns the hash code of {@code graph}.
   *
   * @see Graph#hashCode()
   */
  public static int hashCode(Graph<?, ?> graph) {
    return nodeToIncidentEdges(graph).hashCode();
  }

  /**
   * Returns a string representation of {@code graph}. Encodes edge direction if {@code graph}
   * is directed.
   */
  public static String toString(Graph<?, ?> graph) {
    return String.format("%s, nodes: %s, edges: %s",
        getPropertiesString(graph),
        graph.nodes(),
        Maps.asMap(graph.edges(), edgeToIncidentNodesString(graph)));
  }

  /**
   * Returns a {@link Predicate} that returns {@code true} if the input edge is a self-loop in
   * {@code graph}. A self-loop is defined as an edge whose set of incident nodes has exactly one
   * element. The predicate's {@code apply} method will throw an {@link IllegalArgumentException} if
   * {@code graph} does not contain {@code edge}.
   */
  public static <E> Predicate<E> selfLoopPredicate(final Graph<?, E> graph) {
    checkNotNull(graph, "graph");
    return new Predicate<E>() {
      @Override
      public boolean apply(E edge) {
        return (graph.incidentNodes(edge).size() == 1);
      }
    };
  }

  /**
   * Returns a map that is a live view of {@code graph}, with nodes as keys
   * and the set of incident edges as values.
   */
  private static <N, E> Map<N, Set<E>> nodeToIncidentEdges(final Graph<N, E> graph) {
    checkNotNull(graph, "graph");
    return Maps.asMap(graph.nodes(), new Function<N, Set<E>>() {
      @Override
      public Set<E> apply(N node) {
        return graph.incidentEdges(node);
      }
    });
  }

  /**
   * Returns a function that transforms an edge into a string representation of its incident nodes
   * in {@code graph}. The function's {@code apply} method will throw an
   * {@link IllegalArgumentException} if {@code graph} does not contain {@code edge}.
   */
  private static Function<Object, String> edgeToIncidentNodesString(final Graph<?, ?> graph) {
    if (graph.isDirected()) {
      return new Function<Object, String>() {
        @Override
        public String apply(Object edge) {
          return String.format("<%s -> %s>",
              graph.source(edge), graph.target(edge));
          }
        };
    }
    return new Function<Object, String>() {
      @Override
      public String apply(Object edge) {
        return graph.incidentNodes(edge).toString();
      }
    };
  }

  /**
   * Returns a string representation of the properties of {@code graph}.
   */
  @VisibleForTesting
  static String getPropertiesString(Graph<?, ?> graph) {
    return String.format("isDirected: %s , allowsParallelEdges: %s, allowsSelfLoops: %s",
        graph.isDirected(), graph.allowsParallelEdges(), graph.allowsSelfLoops());
  }
}
