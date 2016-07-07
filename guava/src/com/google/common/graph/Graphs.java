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
import static com.google.common.graph.GraphConstants.ENDPOINTS_GRAPH_DIRECTEDNESS;
import static com.google.common.graph.GraphConstants.NETWORK_WITH_PARALLEL_EDGE;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Static utility methods for {@link Graph} instances.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
public final class Graphs {

  private static final String GRAPH_FORMAT = "%s, nodes: %s, edges: %s";
  private static final String DIRECTED_FORMAT = "<%s -> %s>";
  private static final String UNDIRECTED_FORMAT = "[%s, %s]";

  private Graphs() {}

  // Graph query methods

  /**
   * Returns the subset of nodes in {@code graph} that have no predecessors.
   *
   * <p>Note that in an undirected graph, this is equivalent to all isolated nodes.
   */
  public static <N> Set<N> roots(final Graph<N> graph) {
    return Sets.filter(graph.nodes(), new Predicate<N>() {
      @Override
      public boolean apply(N node) {
        return graph.predecessors(node).isEmpty();
      }
    });
  }

  /**
   * Returns an unmodifiable view of edges that are parallel to {@code edge}, i.e. the set of edges
   * that connect the same nodes in the same direction (if any). An edge is not parallel to itself.
   *
   * @throws IllegalArgumentException if {@code edge} is not present in {@code graph}
   */
  public static <N, E> Set<E> parallelEdges(Network<N, E> graph, Object edge) {
    Endpoints<N> endpoints = graph.incidentNodes(edge); // Verifies that edge is in graph
    if (!graph.allowsParallelEdges()) {
      return ImmutableSet.of();
    }
    return Sets.difference(graph.edgesConnecting(endpoints.nodeA(), endpoints.nodeB()),
        ImmutableSet.of(edge)); // An edge is not parallel to itself.
  }

  // Graph mutation methods

  /**
   * Adds {@code edge} to {@code graph} with the specified {@code endpoints}.
   */
  @CanIgnoreReturnValue
  public static <N, E> boolean addEdge(MutableNetwork<N, E> graph, E edge, Endpoints<N> endpoints) {
    checkNotNull(graph, "graph");
    checkNotNull(edge, "edge");
    checkNotNull(endpoints, "endpoints");
    checkArgument(endpoints.isDirected() == graph.isDirected(),
        ENDPOINTS_GRAPH_DIRECTEDNESS, endpoints.isDirected(), graph.isDirected());
    return graph.addEdge(edge, endpoints.nodeA(), endpoints.nodeB());
  }

  // Graph transformation methods

  /**
   * Returns an induced subgraph of {@code graph}. This subgraph is a new graph that contains
   * all of the nodes in {@code nodes}, and all of the edges from {@code graph} for which the
   * edge's incident nodes are both contained by {@code nodes}.
   *
   * @throws IllegalArgumentException if any element in {@code nodes} is not a node in the graph
   */
  public static <N, E> MutableNetwork<N, E> inducedSubgraph(Network<N, E> graph,
      Iterable<? extends N> nodes) {
    NetworkBuilder<N, E> builder = NetworkBuilder.from(graph);
    if (nodes instanceof Collection) {
      builder = builder.expectedNodeCount(((Collection<?>) nodes).size());
    }
    MutableNetwork<N, E> subgraph = builder.build();
    for (N node : nodes) {
      subgraph.addNode(node);
    }
    for (N node : subgraph.nodes()) {
      for (E edge : graph.outEdges(node)) {
        N otherNode = graph.incidentNodes(edge).otherNode(node);
        if (subgraph.nodes().contains(otherNode)) {
          subgraph.addEdge(edge, node, otherNode);
        }
      }
    }
    return subgraph;
  }

  /**
   * Creates a mutable copy of {@code graph}, using the same nodes and edges.
   */
  @SuppressWarnings("unchecked")
  public static <N> MutableGraph<N> copyOf(Graph<N> graph) {
    checkNotNull(graph, "graph");
    // TODO(b/28087289): we can remove this restriction when Graph supports parallel edges
    checkArgument(!((graph instanceof Network) && ((Network<N, ?>) graph).allowsParallelEdges()),
        NETWORK_WITH_PARALLEL_EDGE);
    MutableGraph<N> copy = GraphBuilder.from(graph)
        .expectedNodeCount(graph.nodes().size())
        .build();

    for (N node : graph.nodes()) {
      copy.addNode(node);
      for (N successor : graph.successors(node)) {
        // TODO(b/28087289): Ensure that multiplicity is preserved if parallel edges are supported.
        copy.addEdge(node, successor);
      }
    }

    return copy;
  }

  /**
   * Creates a mutable copy of {@code graph}, using the same node and edge elements.
   */
  public static <N, E> MutableNetwork<N, E> copyOf(Network<N, E> graph) {
    checkNotNull(graph, "graph");
    MutableNetwork<N, E> copy = NetworkBuilder.from(graph)
        .expectedNodeCount(graph.nodes().size())
        .expectedEdgeCount(graph.edges().size())
        .build();

    for (N node : graph.nodes()) {
      copy.addNode(node);
    }
    for (E edge : graph.edges()) {
      addEdge(copy, edge, graph.incidentNodes(edge));
    }

    return copy;
  }

  /**
   * Returns true iff {@code graph1} and {@code graph2} have the same node connections.
   *
   * <p>Note: {@link Network} instances can only be equal to other {@link Network} instances.
   * In particular, {@link Graph}s that are not also {@link Network}s cannot be equal
   * to {@link Network}s.
   *
   * @see Graph#equals(Object)
   */
  public static boolean equal(@Nullable Graph<?> graph1, @Nullable Graph<?> graph2) {
    // If both graphs are Network instances, use equal(Network, Network) instead
    if (graph1 instanceof Network && graph2 instanceof Network) {
      return equal((Network<?, ?>) graph1, (Network<?, ?>) graph2);
    }

    // Otherwise, if either graph is a Network (but not both), they can't be equal.
    if (graph1 instanceof Network || graph2 instanceof Network) {
      return false;
    }

    if (graph1 == graph2) {
      return true;
    }

    if (graph1 == null || graph2 == null) {
      return false;
    }

    if (!graph1.nodes().equals(graph2.nodes())) {
      return false;
    }

    for (Object node : graph1.nodes()) {
      if (!graph1.successors(node).equals(graph2.successors(node))) {
        return false;
      }
      boolean bothUndirected = !graph1.isDirected() && !graph2.isDirected();
      if (!bothUndirected && !graph1.predecessors(node).equals(graph2.predecessors(node))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns true iff {@code graph1} and {@code graph2} have the same node/edge relationships.
   *
   * @see Network#equals(Object)
   */
  public static boolean equal(@Nullable Network<?, ?> graph1, @Nullable Network<?, ?> graph2) {
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
  public static int hashCode(Graph<?> graph) {
    if (graph instanceof Network) {
      return hashCode((Network<?, ?>) graph);
    }
    return nodeToAdjacentNodes(graph).hashCode();
  }

  /**
   * Returns the hash code of {@code graph}.
   *
   * @see Network#hashCode()
   */
  public static int hashCode(Network<?, ?> graph) {
    return nodeToIncidentEdges(graph).hashCode();
  }

  /**
   * Returns a string representation of {@code graph}. Encodes edge direction if {@code graph}
   * is directed.
   */
  public static String toString(Graph<?> graph) {
    if (graph instanceof Network) {
      return toString((Network<?, ?>) graph);
    }
    return String.format(GRAPH_FORMAT,
        getPropertiesString(graph),
        graph.nodes(),
        adjacentNodesString(graph));
  }

  /**
   * Returns a string representation of {@code graph}. Encodes edge direction if {@code graph}
   * is directed.
   */
  public static String toString(Network<?, ?> graph) {
    return String.format(GRAPH_FORMAT,
        getPropertiesString(graph),
        graph.nodes(),
        Maps.asMap(graph.edges(), edgeToIncidentNodesString(graph)));
  }

  /**
   * Returns a String of the adjacent node relationships for {@code graph}.
   */
  private static <N> String adjacentNodesString(final Graph<N> graph) {
    checkNotNull(graph, "graph");
    List<String> adjacencies = new ArrayList<String>();
    // This will list each undirected edge twice (once as [n1, n2] and once as [n2, n1]); this is OK
    for (N node : graph.nodes()) {
      for (N successor : graph.successors(node)) {
        adjacencies.add(
            String.format(
                graph.isDirected() ? DIRECTED_FORMAT : UNDIRECTED_FORMAT,
                node, successor));
      }
    }

    return String.format("{%s}", Joiner.on(", ").join(adjacencies));
  }

  /**
   * Returns a map that is a live view of {@code graph}, with nodes as keys
   * and the set of incident edges as values.
   */
  private static <N, E> Map<N, Set<E>> nodeToIncidentEdges(final Network<N, E> graph) {
    checkNotNull(graph, "graph");
    return Maps.asMap(graph.nodes(), new Function<N, Set<E>>() {
      @Override
      public Set<E> apply(N node) {
        return graph.incidentEdges(node);
      }
    });
  }

  /**
   * Returns a map that is a live view of {@code graph}, with nodes as keys
   * and the set of adjacent nodes as values.
   */
  private static <N> Map<N, Set<N>> nodeToAdjacentNodes(final Graph<N> graph) {
    checkNotNull(graph, "graph");
    return Maps.asMap(graph.nodes(), new Function<N, Set<N>>() {
      @Override
      public Set<N> apply(N node) {
        return graph.adjacentNodes(node);
      }
    });
  }

  /**
   * Returns a function that transforms an edge into a string representation of its incident nodes
   * in {@code graph}. The function's {@code apply} method will throw an
   * {@link IllegalArgumentException} if {@code graph} does not contain {@code edge}.
   */
  private static Function<Object, String> edgeToIncidentNodesString(final Network<?, ?> graph) {
    checkNotNull(graph, "graph");
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
  // TODO(b/28087289): add allowsParallelEdges() once that's supported
  private static String getPropertiesString(Graph<?> graph) {
    if (graph instanceof Network) {
      return getPropertiesString((Network<?, ?>) graph);
    }
    return String.format("isDirected: %s, allowsSelfLoops: %s",
        graph.isDirected(), graph.allowsSelfLoops());
  }

  /**
   * Returns a string representation of the properties of {@code graph}.
   */
  private static String getPropertiesString(Network<?, ?> graph) {
    return String.format("isDirected: %s, allowsParallelEdges: %s, allowsSelfLoops: %s",
        graph.isDirected(), graph.allowsParallelEdges(), graph.allowsSelfLoops());
  }
}
