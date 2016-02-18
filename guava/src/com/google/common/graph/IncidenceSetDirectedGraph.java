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

/**
 * Configurable implementation of a directed graph consisting of nodes of type N
 * and edges of type E.
 *
 * <p>{@link Graphs#createDirected()} should be used to get an instance of this class.
 *
 * <p>Some invariants/assumptions are maintained in this implementation:
 * <ul>
 * <li>An edge has exactly two end-points (source node and target node), which
 *     may or may not be distinct.
 * <li>By default, this is not a multigraph, that is, parallel edges (multiple
 *     edges directed from n1 to n2) are not allowed.  If you want a multigraph,
 *     create the graph with the 'multigraph' option:
 *     <pre>Graphs.createDirected(Graphs.config().multigraph());</pre>
 * <li>Anti-parallel edges (same incident nodes but in opposite direction,
 *     e.g. (n1, n2) and (n2, n1)) are always allowed.
 * <li>By default, self-loop edges are allowed. If you want to disallow them,
 *     create the graph without the option of self-loops:
 *     <pre>Graphs.createDirected(Graphs.config().noSelfLoops());</pre>
 * <li>Edges are not adjacent to themselves by definition. In the case of a
 *     self-loop, a node can be adjacent to itself, but an edge will never be.
 * </ul>
 *
 * <p>Time complexities for mutation methods:
 * <ul>
 * <li>{@code addNode}: O(1).
 * <li>{@code addEdge(E edge, N node1, N node2)}: O(1).
 * <li>{@code removeNode(node)}: O(d_node).
 * <li>{@code removeEdge}: O(1), unless this graph is a multigraph (supports parallel edges);
 *     in that case this method is O(min(outD_edgeSource, inD_edgeTarget)).
 * </ul>
 * where d_node is the degree of node, inD_node is the in-degree of node, and outD_node is the
 * out-degree of node.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @see AbstractConfigurableGraph
 * @see Graphs
 */
final class IncidenceSetDirectedGraph<N, E> extends AbstractConfigurableGraph<N, E>
    implements DirectedGraph<N, E> {

  IncidenceSetDirectedGraph(GraphConfig config) {
    super(config);
  }

  @Override
  NodeConnections<N, E> newNodeConnections() {
    return DirectedNodeConnections.of();
  }

  @Override
  public N source(Object edge) {
    return checkedIncidentNodes(edge).node1();
  }

  @Override
  public N target(Object edge) {
    return checkedIncidentNodes(edge).node2();
  }
}
