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
 * @see Graph
 * @since 20.0
 */
@Beta
public final class Graphs {

  public static final GraphConfig MULTIGRAPH = config().multigraph();

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
    if (!graph.config().isMultigraph()) {
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
  public static <N, E> DirectedGraph<N, E> copyOf(DirectedGraph<N, E> graph) {
    checkNotNull(graph, "graph");
    DirectedGraph<N, E> copy = createDirected(graph.config()
            .expectedNodeCount(graph.nodes().size())
            .expectedEdgeCount(graph.edges().size()));
    mergeNodesFrom(graph, copy);
    mergeEdgesFrom(graph, copy);
    return copy;
  }

  /**
   * Creates a mutable copy of {@code graph}, using all of its elements that satisfy
   * {@code nodePredicate} and {@code edgePredicate}.
   */
  public static <N, E> DirectedGraph<N, E> copyOf(
      DirectedGraph<N, E> graph,
      Predicate<? super N> nodePredicate,
      Predicate<? super E> edgePredicate) {
    checkNotNull(graph, "graph");
    checkNotNull(nodePredicate, "nodePredicate");
    checkNotNull(edgePredicate, "edgePredicate");
    DirectedGraph<N, E> copy = createDirected(graph.config()
            .expectedNodeCount(graph.nodes().size())
            .expectedEdgeCount(graph.edges().size()));
    mergeNodesFrom(graph, copy, nodePredicate);
    // We can't just call mergeEdgesFrom(graph, copy, edgePredicate) because addEdge() can add
    // the edge's incident nodes if they are not present; we need to run them past nodePredicate.
    if (edgePredicate.equals(Predicates.<E>alwaysFalse())) {
      return copy; // no edges to add
    }

    for (E edge : graph.edges()) {
      if (edgePredicate.apply(edge)) {
        N source = graph.source(edge);
        N target = graph.target(edge);
        if (nodePredicate.apply(source) && nodePredicate.apply(target)) {
          copy.addEdge(edge, source, target);
        }
      }
    }

    return copy;
  }

  /**
   * Copies all nodes from {@code original} into {@code copy}.
   */
  public static <N, E> void mergeNodesFrom(Graph<N, E> original, Graph<N, E> copy) {
    checkNotNull(original, "original");
    checkNotNull(copy, "copy");
    for (N node : original.nodes()) {
      copy.addNode(node);
    }
  }

  /**
   * Copies all nodes from {@code original} into {@code copy} that satisfy {@code nodePredicate}.
   */
  public static <N, E> void mergeNodesFrom(
      Graph<N, E> original, Graph<N, E> copy, Predicate<? super N> nodePredicate) {
    checkNotNull(original, "original");
    checkNotNull(copy, "copy");
    checkNotNull(nodePredicate, "nodePredicate");
    if (nodePredicate.equals(Predicates.<N>alwaysFalse())) {
      return; // nothing to do
    }

    if (nodePredicate.equals(Predicates.<N>alwaysTrue())) {
      mergeNodesFrom(original, copy); // optimization
    } else {
      for (N node : original.nodes()) {
        if (nodePredicate.apply(node)) {
          copy.addNode(node);
        }
      }
    }
  }

  /**
   * Copies all edges from {@code original} into {@code copy}.  Also copies all nodes incident
   * to these edges.
   */
  public static <N, E> void mergeEdgesFrom(DirectedGraph<N, E> original, DirectedGraph<N, E> copy) {
    checkNotNull(original, "original");
    checkNotNull(copy, "copy");
    for (E edge : original.edges()) {
      copy.addEdge(edge, original.source(edge), original.target(edge));
    }
  }

  /**
   * Copies all edges from {@code original} into {@code copy} that satisfy {@code edgePredicate}.
   * Also copies all nodes incident to these edges.
   */
  public static <N, E> void mergeEdgesFrom(
      DirectedGraph<N, E> original, DirectedGraph<N, E> copy, Predicate<? super E> edgePredicate) {
    checkNotNull(original, "original");
    checkNotNull(copy, "copy");
    checkNotNull(edgePredicate, "edgePredicate");
    if (edgePredicate.equals(Predicates.<E>alwaysFalse())) {
      return; // nothing to do
    }

    if (edgePredicate.equals(Predicates.<E>alwaysTrue())) {
      mergeEdgesFrom(original, copy); // optimization
    } else {
      for (E edge : original.edges()) {
        if (edgePredicate.apply(edge)) {
          copy.addEdge(edge, original.source(edge), original.target(edge));
        }
      }
    }
  }

  /**
   * Creates a mutable copy of {@code graph}, using the same node and edge elements.
   */
  public static <N, E> UndirectedGraph<N, E> copyOf(UndirectedGraph<N, E> graph) {
    checkNotNull(graph, "graph");
    UndirectedGraph<N, E> copy = createUndirected(graph.config()
            .expectedNodeCount(graph.nodes().size())
            .expectedEdgeCount(graph.edges().size()));
    mergeNodesFrom(graph, copy);
    mergeEdgesFrom(graph, copy);
    return copy;
  }

  /**
   * Creates a mutable copy of {@code graph}, using all of its elements that satisfy
   * {@code nodePredicate} and {@code edgePredicate}.
   */
  public static <N, E> UndirectedGraph<N, E> copyOf(
      UndirectedGraph<N, E> graph,
      Predicate<? super N> nodePredicate,
      Predicate<? super E> edgePredicate) {
    checkNotNull(graph, "graph");
    checkNotNull(nodePredicate, "nodePredicate");
    checkNotNull(edgePredicate, "edgePredicate");
    UndirectedGraph<N, E> copy = createUndirected(graph.config()
            .expectedNodeCount(graph.nodes().size())
            .expectedEdgeCount(graph.edges().size()));
    mergeNodesFrom(graph, copy, nodePredicate);

    // We can't just call mergeEdgesFrom(graph, copy, edgePredicate) because addEdge() can add
    // the edge's incident nodes if they are not present; we need to run them past nodePredicate.
    for (E edge : graph.edges()) {
      if (edgePredicate.apply(edge)) {
        boolean nodesOk = true;
        Set<N> incidentNodes = graph.incidentNodes(edge);
        for (N node : incidentNodes) {
          nodesOk &= nodePredicate.apply(node);
        }
        if (nodesOk) {
          addEdge(copy, edge, incidentNodes);
        }
      }
    }

    return copy;
  }

  /**
   * Copies all edges from {@code original} into {@code copy}.  Also copies all nodes incident
   * to these edges.
   */
  public static <N, E> void mergeEdgesFrom(Graph<N, E> original, Graph<N, E> copy) {
    checkNotNull(original, "original");
    checkNotNull(copy, "copy");
    for (E edge : original.edges()) {
      addEdge(copy, edge, original.incidentNodes(edge));
    }
  }

  /**
   * Copies all edges from {@code original} into {@code copy} that satisfy {@code edgePredicate}.
   * Also copies all nodes incident to these edges.
   */
  // NOTE: this is identical to mergeEdgesFrom(DirectedGraph) except for the call to addEdge
  public static <N, E> void mergeEdgesFrom(Graph<N, E> original, Graph<N, E> copy,
      Predicate<? super E> edgePredicate) {
    checkNotNull(original, "original");
    checkNotNull(copy, "copy");
    checkNotNull(edgePredicate, "edgePredicate");
    if (edgePredicate.equals(Predicates.<E>alwaysFalse())) {
      return; // nothing to do
    }

    if (edgePredicate.equals(Predicates.<E>alwaysTrue())) {
      mergeEdgesFrom(original, copy); // optimization
    } else {
      for (E edge : original.edges()) {
        if (edgePredicate.apply(edge)) {
          addEdge(copy, edge, original.incidentNodes(edge));
        }
      }
    }
  }

  /**
   * Copies all nodes and edges from {@code original} to {@code copy} that satisfy
   * {@code nodePredicate} and {@code edgePredicate}.
   */
  public static <N, E> void copyFrom(
      Graph<N, E> original,
      Graph<N, E> copy,
      Predicate<? super N> nodePredicate,
      Predicate<? super E> edgePredicate) {
    checkNotNull(original, "original");
    checkNotNull(copy, "copy");
    checkNotNull(nodePredicate, "nodePredicate");
    checkNotNull(edgePredicate, "edgePredicate");
    mergeNodesFrom(original, copy, nodePredicate);
    mergeEdgesFrom(original, copy, edgePredicate);
  }

  /**
   * Returns a new default instance of {@code GraphConfig}.
   *
   * @see GraphConfig
   */
  public static GraphConfig config() {
    return new GraphConfig();
  }

  /**
   * Returns a new instance of {@link DirectedGraph} with the default
   * graph configuration.
   *
   * @see GraphConfig
   */
  public static <N, E> DirectedGraph<N, E> createDirected() {
    return new IncidenceSetDirectedGraph<N, E>(config());
  }

  /**
   * Returns a new instance of {@link DirectedGraph} with the graph
   * configuration specified by {@code config}.
   */
  public static <N, E> DirectedGraph<N, E> createDirected(GraphConfig config) {
    return new IncidenceSetDirectedGraph<N, E>(config);
  }

  /**
   * Returns a new instance of {@link UndirectedGraph} with the default
   * graph configuration.
   *
   * @see GraphConfig
   */
  public static <N, E> UndirectedGraph<N, E> createUndirected() {
    return new IncidenceSetUndirectedGraph<N, E>(config());
  }

  /**
   * Returns a new instance of {@link UndirectedGraph} with the graph
   * configuration specified by {@code config}.
   */
  public static <N, E> UndirectedGraph<N, E> createUndirected(GraphConfig config) {
    return new IncidenceSetUndirectedGraph<N, E>(config);
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
      // TODO(b/27195992): Consider an optimization for the case where both graphs are undirected.
      if (!graph1.outEdges(node).equals(graph2.outEdges(node))) {
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
   * is a {@link DirectedGraph}.
   */
  public static String toString(Graph<?, ?> graph) {
    return String.format("config: %s, nodes: %s, edges: %s",
        graph.config(),
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
    if (graph instanceof DirectedGraph) {
      @SuppressWarnings("unchecked")
      final DirectedGraph<?, ?> directedGraph = (DirectedGraph<?, ?>) graph;
      return new Function<Object, String>() {
        @Override
        public String apply(Object edge) {
          return String.format("<%s -> %s>",
              directedGraph.source(edge), directedGraph.target(edge));
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
}
