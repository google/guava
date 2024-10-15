package com.google.common.graph;

import com.google.common.annotations.Beta;
import java.util.Set;

/**
 * Supertype for {@link Graphs}, containing the old signatures of methods whose signatures we've
 * changed. This provides binary compatibility for users who compiled against the old signatures.
 */
@Beta
@ElementTypesAreNonnullByDefault
abstract class GraphsBridgeMethods {

  @SuppressWarnings("PreferredInterfaceType")
  public static <N> Graph<N> transitiveClosure(Graph<N> graph) {
    return Graphs.transitiveClosure(graph);
  }

  @SuppressWarnings("PreferredInterfaceType")
  public static <N> Set<N> reachableNodes(Graph<N> graph, N node) {
    return Graphs.reachableNodes(graph, node);
  }
}
