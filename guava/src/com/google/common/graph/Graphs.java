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
import static com.google.common.graph.GraphConstants.NODE_NOT_IN_GRAPH;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;

/**
 * Static utility methods for {@link Graph}, {@link ValueGraph}, and {@link Network} instances.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
@ElementTypesAreNonnullByDefault
public final class Graphs {

  private Graphs() {}

  // Graph query methods

  /**
   * Returns true if {@code graph} has at least one cycle. A cycle is defined as a non-empty subset
   * of edges in a graph arranged to form a path (a sequence of adjacent outgoing edges) starting
   * and ending with the same node.
   *
   * <p>This method will detect any non-empty cycle, including self-loops (a cycle of length 1).
   */
  public static <N> boolean hasCycle(Graph<N> graph) {
    int numEdges = graph.edges().size();
    if (numEdges == 0) {
      return false; // An edge-free graph is acyclic by definition.
    }
    if (!graph.isDirected() && numEdges >= graph.nodes().size()) {
      return true; // Optimization for the undirected case: at least one cycle must exist.
    }

    Map<Object, NodeVisitState> visitedNodes =
        Maps.newHashMapWithExpectedSize(graph.nodes().size());
    for (N node : graph.nodes()) {
      if (subgraphHasCycle(graph, visitedNodes, node, null)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if {@code network} has at least one cycle. A cycle is defined as a non-empty
   * subset of edges in a graph arranged to form a path (a sequence of adjacent outgoing edges)
   * starting and ending with the same node.
   *
   * <p>This method will detect any non-empty cycle, including self-loops (a cycle of length 1).
   */
  public static boolean hasCycle(Network<?, ?> network) {
    // In a directed graph, parallel edges cannot introduce a cycle in an acyclic graph.
    // However, in an undirected graph, any parallel edge induces a cycle in the graph.
    if (!network.isDirected()
        && network.allowsParallelEdges()
        && network.edges().size() > network.asGraph().edges().size()) {
      return true;
    }
    return hasCycle(network.asGraph());
  }

  /**
   * Performs a traversal of the nodes reachable from {@code node}. If we ever reach a node we've
   * already visited (following only outgoing edges and without reusing edges), we know there's a
   * cycle in the graph.
   */
  private static <N> boolean subgraphHasCycle(
      Graph<N> graph,
      Map<Object, NodeVisitState> visitedNodes,
      N node,
      @CheckForNull N previousNode) {
    NodeVisitState state = visitedNodes.get(node);
    if (state == NodeVisitState.COMPLETE) {
      return false;
    }
    if (state == NodeVisitState.PENDING) {
      return true;
    }

    visitedNodes.put(node, NodeVisitState.PENDING);
    for (N nextNode : graph.successors(node)) {
      if (canTraverseWithoutReusingEdge(graph, nextNode, previousNode)
          && subgraphHasCycle(graph, visitedNodes, nextNode, node)) {
        return true;
      }
    }
    visitedNodes.put(node, NodeVisitState.COMPLETE);
    return false;
  }

  /**
   * Determines whether an edge has already been used during traversal. In the directed case a cycle
   * is always detected before reusing an edge, so no special logic is required. In the undirected
   * case, we must take care not to "backtrack" over an edge (i.e. going from A to B and then going
   * from B to A).
   */
  private static boolean canTraverseWithoutReusingEdge(
      Graph<?> graph, Object nextNode, @CheckForNull Object previousNode) {
    if (graph.isDirected() || !Objects.equal(previousNode, nextNode)) {
      return true;
    }
    // This falls into the undirected A->B->A case. The Graph interface does not support parallel
    // edges, so this traversal would require reusing the undirected AB edge.
    return false;
  }

  /**
   * Returns the transitive closure of {@code graph}. The transitive closure of a graph is another
   * graph with an edge connecting node A to node B if node B is {@link #reachableNodes(Graph,
   * Object) reachable} from node A.
   *
   * <p>This is a "snapshot" based on the current topology of {@code graph}, rather than a live view
   * of the transitive closure of {@code graph}. In other words, the returned {@link Graph} will not
   * be updated after modifications to {@code graph}.
   */
  // TODO(b/31438252): Consider potential optimizations for this algorithm.
  public static <N> Graph<N> transitiveClosure(Graph<N> graph) {
    MutableGraph<N> transitiveClosure = GraphBuilder.from(graph).allowsSelfLoops(true).build();
    // Every node is, at a minimum, reachable from itself. Since the resulting transitive closure
    // will have no isolated nodes, we can skip adding nodes explicitly and let putEdge() do it.

    if (graph.isDirected()) {
      // Note: works for both directed and undirected graphs, but we only use in the directed case.
      for (N node : graph.nodes()) {
        for (N reachableNode : reachableNodes(graph, node)) {
          transitiveClosure.putEdge(node, reachableNode);
        }
      }
    } else {
      // An optimization for the undirected case: for every node B reachable from node A,
      // node A and node B have the same reachability set.
      Set<N> visitedNodes = new HashSet<N>();
      for (N node : graph.nodes()) {
        if (!visitedNodes.contains(node)) {
          Set<N> reachableNodes = reachableNodes(graph, node);
          visitedNodes.addAll(reachableNodes);
          int pairwiseMatch = 1; // start at 1 to include self-loops
          for (N nodeU : reachableNodes) {
            for (N nodeV : Iterables.limit(reachableNodes, pairwiseMatch++)) {
              transitiveClosure.putEdge(nodeU, nodeV);
            }
          }
        }
      }
    }

    return transitiveClosure;
  }

  /**
   * Returns the set of nodes that are reachable from {@code node}. Node B is defined as reachable
   * from node A if there exists a path (a sequence of adjacent outgoing edges) starting at node A
   * and ending at node B. Note that a node is always reachable from itself via a zero-length path.
   *
   * <p>This is a "snapshot" based on the current topology of {@code graph}, rather than a live view
   * of the set of nodes reachable from {@code node}. In other words, the returned {@link Set} will
   * not be updated after modifications to {@code graph}.
   *
   * @throws IllegalArgumentException if {@code node} is not present in {@code graph}
   */
  public static <N> Set<N> reachableNodes(Graph<N> graph, N node) {
    checkArgument(graph.nodes().contains(node), NODE_NOT_IN_GRAPH, node);
    return ImmutableSet.copyOf(Traverser.forGraph(graph).breadthFirst(node));
  }

  // Graph mutation methods

  // Graph view methods

  /**
   * Returns a view of {@code graph} with the direction (if any) of every edge reversed. All other
   * properties remain intact, and further updates to {@code graph} will be reflected in the view.
   */
  public static <N> Graph<N> transpose(Graph<N> graph) {
    if (!graph.isDirected()) {
      return graph; // the transpose of an undirected graph is an identical graph
    }

    if (graph instanceof TransposedGraph) {
      return ((TransposedGraph<N>) graph).graph;
    }

    return new TransposedGraph<N>(graph);
  }

  /**
   * Returns a view of {@code graph} with the direction (if any) of every edge reversed. All other
   * properties remain intact, and further updates to {@code graph} will be reflected in the view.
   */
  public static <N, V> ValueGraph<N, V> transpose(ValueGraph<N, V> graph) {
    if (!graph.isDirected()) {
      return graph; // the transpose of an undirected graph is an identical graph
    }

    if (graph instanceof TransposedValueGraph) {
      return ((TransposedValueGraph<N, V>) graph).graph;
    }

    return new TransposedValueGraph<>(graph);
  }

  /**
   * Returns a view of {@code network} with the direction (if any) of every edge reversed. All other
   * properties remain intact, and further updates to {@code network} will be reflected in the view.
   */
  public static <N, E> Network<N, E> transpose(Network<N, E> network) {
    if (!network.isDirected()) {
      return network; // the transpose of an undirected network is an identical network
    }

    if (network instanceof TransposedNetwork) {
      return ((TransposedNetwork<N, E>) network).network;
    }

    return new TransposedNetwork<>(network);
  }

  static <N> EndpointPair<N> transpose(EndpointPair<N> endpoints) {
    if (endpoints.isOrdered()) {
      return EndpointPair.ordered(endpoints.target(), endpoints.source());
    }
    return endpoints;
  }

  // NOTE: this should work as long as the delegate graph's implementation of edges() (like that of
  // AbstractGraph) derives its behavior from calling successors().
  private static class TransposedGraph<N> extends ForwardingGraph<N> {
    private final Graph<N> graph;

    TransposedGraph(Graph<N> graph) {
      this.graph = graph;
    }

    @Override
    Graph<N> delegate() {
      return graph;
    }

    @Override
    public Set<N> predecessors(N node) {
      return delegate().successors(node); // transpose
    }

    @Override
    public Set<N> successors(N node) {
      return delegate().predecessors(node); // transpose
    }

    @Override
    public Set<EndpointPair<N>> incidentEdges(N node) {
      return new IncidentEdgeSet<N>(this, node) {
        @Override
        public Iterator<EndpointPair<N>> iterator() {
          return Iterators.transform(
              delegate().incidentEdges(node).iterator(),
              edge -> EndpointPair.of(delegate(), edge.nodeV(), edge.nodeU()));
        }
      };
    }

    @Override
    public int inDegree(N node) {
      return delegate().outDegree(node); // transpose
    }

    @Override
    public int outDegree(N node) {
      return delegate().inDegree(node); // transpose
    }

    @Override
    public boolean hasEdgeConnecting(N nodeU, N nodeV) {
      return delegate().hasEdgeConnecting(nodeV, nodeU); // transpose
    }

    @Override
    public boolean hasEdgeConnecting(EndpointPair<N> endpoints) {
      return delegate().hasEdgeConnecting(transpose(endpoints));
    }
  }

  // NOTE: this should work as long as the delegate graph's implementation of edges() (like that of
  // AbstractValueGraph) derives its behavior from calling successors().
  private static class TransposedValueGraph<N, V> extends ForwardingValueGraph<N, V> {
    private final ValueGraph<N, V> graph;

    TransposedValueGraph(ValueGraph<N, V> graph) {
      this.graph = graph;
    }

    @Override
    ValueGraph<N, V> delegate() {
      return graph;
    }

    @Override
    public Set<N> predecessors(N node) {
      return delegate().successors(node); // transpose
    }

    @Override
    public Set<N> successors(N node) {
      return delegate().predecessors(node); // transpose
    }

    @Override
    public int inDegree(N node) {
      return delegate().outDegree(node); // transpose
    }

    @Override
    public int outDegree(N node) {
      return delegate().inDegree(node); // transpose
    }

    @Override
    public boolean hasEdgeConnecting(N nodeU, N nodeV) {
      return delegate().hasEdgeConnecting(nodeV, nodeU); // transpose
    }

    @Override
    public boolean hasEdgeConnecting(EndpointPair<N> endpoints) {
      return delegate().hasEdgeConnecting(transpose(endpoints));
    }

    @Override
    public Optional<V> edgeValue(N nodeU, N nodeV) {
      return delegate().edgeValue(nodeV, nodeU); // transpose
    }

    @Override
    public Optional<V> edgeValue(EndpointPair<N> endpoints) {
      return delegate().edgeValue(transpose(endpoints));
    }

    @Override
    @CheckForNull
    public V edgeValueOrDefault(N nodeU, N nodeV, @CheckForNull V defaultValue) {
      return delegate().edgeValueOrDefault(nodeV, nodeU, defaultValue); // transpose
    }

    @Override
    @CheckForNull
    public V edgeValueOrDefault(EndpointPair<N> endpoints, @CheckForNull V defaultValue) {
      return delegate().edgeValueOrDefault(transpose(endpoints), defaultValue);
    }
  }

  private static class TransposedNetwork<N, E> extends ForwardingNetwork<N, E> {
    private final Network<N, E> network;

    TransposedNetwork(Network<N, E> network) {
      this.network = network;
    }

    @Override
    Network<N, E> delegate() {
      return network;
    }

    @Override
    public Set<N> predecessors(N node) {
      return delegate().successors(node); // transpose
    }

    @Override
    public Set<N> successors(N node) {
      return delegate().predecessors(node); // transpose
    }

    @Override
    public int inDegree(N node) {
      return delegate().outDegree(node); // transpose
    }

    @Override
    public int outDegree(N node) {
      return delegate().inDegree(node); // transpose
    }

    @Override
    public Set<E> inEdges(N node) {
      return delegate().outEdges(node); // transpose
    }

    @Override
    public Set<E> outEdges(N node) {
      return delegate().inEdges(node); // transpose
    }

    @Override
    public EndpointPair<N> incidentNodes(E edge) {
      EndpointPair<N> endpointPair = delegate().incidentNodes(edge);
      return EndpointPair.of(network, endpointPair.nodeV(), endpointPair.nodeU()); // transpose
    }

    @Override
    public Set<E> edgesConnecting(N nodeU, N nodeV) {
      return delegate().edgesConnecting(nodeV, nodeU); // transpose
    }

    @Override
    public Set<E> edgesConnecting(EndpointPair<N> endpoints) {
      return delegate().edgesConnecting(transpose(endpoints));
    }

    @Override
    public Optional<E> edgeConnecting(N nodeU, N nodeV) {
      return delegate().edgeConnecting(nodeV, nodeU); // transpose
    }

    @Override
    public Optional<E> edgeConnecting(EndpointPair<N> endpoints) {
      return delegate().edgeConnecting(transpose(endpoints));
    }

    @Override
    @CheckForNull
    public E edgeConnectingOrNull(N nodeU, N nodeV) {
      return delegate().edgeConnectingOrNull(nodeV, nodeU); // transpose
    }

    @Override
    @CheckForNull
    public E edgeConnectingOrNull(EndpointPair<N> endpoints) {
      return delegate().edgeConnectingOrNull(transpose(endpoints));
    }

    @Override
    public boolean hasEdgeConnecting(N nodeU, N nodeV) {
      return delegate().hasEdgeConnecting(nodeV, nodeU); // transpose
    }

    @Override
    public boolean hasEdgeConnecting(EndpointPair<N> endpoints) {
      return delegate().hasEdgeConnecting(transpose(endpoints));
    }
  }

  // Graph copy methods

  /**
   * Returns the subgraph of {@code graph} induced by {@code nodes}. This subgraph is a new graph
   * that contains all of the nodes in {@code nodes}, and all of the {@link Graph#edges() edges}
   * from {@code graph} for which both nodes are contained by {@code nodes}.
   *
   * @throws IllegalArgumentException if any element in {@code nodes} is not a node in the graph
   */
  public static <N> MutableGraph<N> inducedSubgraph(Graph<N> graph, Iterable<? extends N> nodes) {
    MutableGraph<N> subgraph =
        (nodes instanceof Collection)
            ? GraphBuilder.from(graph).expectedNodeCount(((Collection) nodes).size()).build()
            : GraphBuilder.from(graph).build();
    for (N node : nodes) {
      subgraph.addNode(node);
    }
    for (N node : subgraph.nodes()) {
      for (N successorNode : graph.successors(node)) {
        if (subgraph.nodes().contains(successorNode)) {
          subgraph.putEdge(node, successorNode);
        }
      }
    }
    return subgraph;
  }

  /**
   * Returns the subgraph of {@code graph} induced by {@code nodes}. This subgraph is a new graph
   * that contains all of the nodes in {@code nodes}, and all of the {@link Graph#edges() edges}
   * (and associated edge values) from {@code graph} for which both nodes are contained by {@code
   * nodes}.
   *
   * @throws IllegalArgumentException if any element in {@code nodes} is not a node in the graph
   */
  public static <N, V> MutableValueGraph<N, V> inducedSubgraph(
      ValueGraph<N, V> graph, Iterable<? extends N> nodes) {
    MutableValueGraph<N, V> subgraph =
        (nodes instanceof Collection)
            ? ValueGraphBuilder.from(graph).expectedNodeCount(((Collection) nodes).size()).build()
            : ValueGraphBuilder.from(graph).build();
    for (N node : nodes) {
      subgraph.addNode(node);
    }
    for (N node : subgraph.nodes()) {
      for (N successorNode : graph.successors(node)) {
        if (subgraph.nodes().contains(successorNode)) {
          // requireNonNull is safe because the endpoint pair comes from the graph.
          subgraph.putEdgeValue(
              node,
              successorNode,
              requireNonNull(graph.edgeValueOrDefault(node, successorNode, null)));
        }
      }
    }
    return subgraph;
  }

  /**
   * Returns the subgraph of {@code network} induced by {@code nodes}. This subgraph is a new graph
   * that contains all of the nodes in {@code nodes}, and all of the {@link Network#edges() edges}
   * from {@code network} for which the {@link Network#incidentNodes(Object) incident nodes} are
   * both contained by {@code nodes}.
   *
   * @throws IllegalArgumentException if any element in {@code nodes} is not a node in the graph
   */
  public static <N, E> MutableNetwork<N, E> inducedSubgraph(
      Network<N, E> network, Iterable<? extends N> nodes) {
    MutableNetwork<N, E> subgraph =
        (nodes instanceof Collection)
            ? NetworkBuilder.from(network).expectedNodeCount(((Collection) nodes).size()).build()
            : NetworkBuilder.from(network).build();
    for (N node : nodes) {
      subgraph.addNode(node);
    }
    for (N node : subgraph.nodes()) {
      for (E edge : network.outEdges(node)) {
        N successorNode = network.incidentNodes(edge).adjacentNode(node);
        if (subgraph.nodes().contains(successorNode)) {
          subgraph.addEdge(node, successorNode, edge);
        }
      }
    }
    return subgraph;
  }

  /** Creates a mutable copy of {@code graph} with the same nodes and edges. */
  public static <N> MutableGraph<N> copyOf(Graph<N> graph) {
    MutableGraph<N> copy = GraphBuilder.from(graph).expectedNodeCount(graph.nodes().size()).build();
    for (N node : graph.nodes()) {
      copy.addNode(node);
    }
    for (EndpointPair<N> edge : graph.edges()) {
      copy.putEdge(edge.nodeU(), edge.nodeV());
    }
    return copy;
  }

  /** Creates a mutable copy of {@code graph} with the same nodes, edges, and edge values. */
  public static <N, V> MutableValueGraph<N, V> copyOf(ValueGraph<N, V> graph) {
    MutableValueGraph<N, V> copy =
        ValueGraphBuilder.from(graph).expectedNodeCount(graph.nodes().size()).build();
    for (N node : graph.nodes()) {
      copy.addNode(node);
    }
    for (EndpointPair<N> edge : graph.edges()) {
      // requireNonNull is safe because the endpoint pair comes from the graph.
      copy.putEdgeValue(
          edge.nodeU(),
          edge.nodeV(),
          requireNonNull(graph.edgeValueOrDefault(edge.nodeU(), edge.nodeV(), null)));
    }
    return copy;
  }

  /** Creates a mutable copy of {@code network} with the same nodes and edges. */
  public static <N, E> MutableNetwork<N, E> copyOf(Network<N, E> network) {
    MutableNetwork<N, E> copy =
        NetworkBuilder.from(network)
            .expectedNodeCount(network.nodes().size())
            .expectedEdgeCount(network.edges().size())
            .build();
    for (N node : network.nodes()) {
      copy.addNode(node);
    }
    for (E edge : network.edges()) {
      EndpointPair<N> endpointPair = network.incidentNodes(edge);
      copy.addEdge(endpointPair.nodeU(), endpointPair.nodeV(), edge);
    }
    return copy;
  }

  @CanIgnoreReturnValue
  static int checkNonNegative(int value) {
    checkArgument(value >= 0, "Not true that %s is non-negative.", value);
    return value;
  }

  @CanIgnoreReturnValue
  static long checkNonNegative(long value) {
    checkArgument(value >= 0, "Not true that %s is non-negative.", value);
    return value;
  }

  @CanIgnoreReturnValue
  static int checkPositive(int value) {
    checkArgument(value > 0, "Not true that %s is positive.", value);
    return value;
  }

  @CanIgnoreReturnValue
  static long checkPositive(long value) {
    checkArgument(value > 0, "Not true that %s is positive.", value);
    return value;
  }

  /**
   * An enum representing the state of a node during DFS. {@code PENDING} means that the node is on
   * the stack of the DFS, while {@code COMPLETE} means that the node and all its successors have
   * been already explored. Any node that has not been explored will not have a state at all.
   */
  private enum NodeVisitState {
    PENDING,
    COMPLETE
  }
}
