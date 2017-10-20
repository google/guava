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
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Chars;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraverserTest {

  /**
   * The undirected graph in the {@link Traverser#breadthFirst(Object)} javadoc:
   *
   * <pre>{@code
   * b ---- a ---- d
   * |      |
   * |      |
   * e ---- c ---- f
   * }</pre>
   */
  private static final SuccessorsFunction<Character> JAVADOC_GRAPH =
      createUndirectedGraph("ba", "ad", "be", "ac", "ec", "cf");

  /**
   * A diamond shaped directed graph (arrows going down):
   *
   * <pre>{@code
   *   a
   *  / \
   * b   c
   *  \ /
   *   d
   * }</pre>
   */
  private static final SuccessorsFunction<Character> DIAMOND_GRAPH =
      createDirectedGraph("ab", "ac", "bd", "cd");

  /**
   * Same as {@link #DIAMOND_GRAPH}, but with an extra c->a edge and some self edges:
   *
   * <pre>{@code
   *   a<>
   *  / \\
   * b   c
   *  \ /
   *   d<>
   * }</pre>
   *
   * {@code <>} indicates a self-loop
   */
  private static final SuccessorsFunction<Character> MULTI_GRAPH =
      createDirectedGraph("aa", "dd", "ab", "ac", "ca", "cd", "bd");

  /** A directed graph with a single cycle: a -> b -> c -> d -> a. */
  private static final SuccessorsFunction<Character> CYCLE_GRAPH =
      createDirectedGraph("ab", "bc", "cd", "da");

  /**
   * Same as {@link #CYCLE_GRAPH}, but with an extra a->c edge.
   *
   * <pre>{@code
   * |--------------|
   * v              |
   * a -> b -> c -> d
   * |         ^
   * |---------|
   * }</pre>
   */
  private static final SuccessorsFunction<Character> TWO_CYCLES_GRAPH =
      createDirectedGraph("ab", "ac", "bc", "cd", "da");

  /**
   * A tree-shaped graph that looks as follows (all edges are directed facing downwards):
   *
   * <pre>{@code
   *        h
   *       /|\
   *      / | \
   *     /  |  \
   *    d   e   g
   *   /|\      |
   *  / | \     |
   * a  b  c    f
   * }</pre>
   */
  private static final SuccessorsFunction<Character> TREE =
      createDirectedGraph("hd", "he", "hg", "da", "db", "dc", "gf");

  /**
   * Two disjoint tree-shaped graphs that look as follows (all edges are directed facing downwards):
   *
   * <pre>{@code
   * a   c
   * |   |
   * |   |
   * b   d
   * }</pre>
   */
  private static final SuccessorsFunction<Character> TWO_TREES = createDirectedGraph("ab", "cd");

  /**
   * A graph consisting of a single root {@code a}:
   *
   * <pre>{@code
   * a
   * }</pre>
   */
  private static final SuccessorsFunction<Character> SINGLE_ROOT = createSingleRootGraph();

  /**
   * A graph that is not a tree (for example, it has two antiparallel edge between {@code e} and
   * {@code f} and thus has a cycle) but is a valid input to {@link Traverser#forTree} when starting
   * e.g. at node {@code a} (all edges without an arrow are directed facing downwards):
   *
   * <pre>{@code
   *     a
   *    /
   *   b   e <----> f
   *  / \ /
   * c   d
   * }</pre>
   */
  private static final SuccessorsFunction<Character> CYCLIC_GRAPH_CONTAINING_TREE =
      createDirectedGraph("ab", "bc", "bd", "ed", "ef", "fe");

  /**
   * A graph that is not a tree (for example, {@code h} is reachable from {@code f} via both {@code
   * e} and {@code g}) but is a valid input to {@link Traverser#forTree} when starting e.g. at node
   * {@code a} (all edges are directed facing downwards):
   *
   * <pre>{@code
   *     a   f
   *    /   / \
   *   b   e   g
   *  / \ / \ /
   * c   d   h
   * }</pre>
   */
  private static final SuccessorsFunction<Character> GRAPH_CONTAINING_TREE_AND_DIAMOND =
      createDirectedGraph("ab", "fe", "fg", "bc", "bd", "ed", "eh", "gh");

  @Test
  public void forGraph_breadthFirst_javadocExample_canBeIteratedMultipleTimes() {
    Iterable<Character> result = Traverser.forGraph(JAVADOC_GRAPH).breadthFirst('a');

    assertEqualCharNodes(result, "abcdef");
    assertEqualCharNodes(result, "abcdef");
  }

  @Test
  public void forGraph_breadthFirst_diamond() {
    Traverser<Character> traverser = Traverser.forGraph(DIAMOND_GRAPH);
    assertEqualCharNodes(traverser.breadthFirst('a'), "abcd");
    assertEqualCharNodes(traverser.breadthFirst('b'), "bd");
    assertEqualCharNodes(traverser.breadthFirst('c'), "cd");
    assertEqualCharNodes(traverser.breadthFirst('d'), "d");
  }

  @Test
  public void forGraph_breadthFirst_multiGraph() {
    Traverser<Character> traverser = Traverser.forGraph(MULTI_GRAPH);
    assertEqualCharNodes(traverser.breadthFirst('a'), "abcd");
    assertEqualCharNodes(traverser.breadthFirst('b'), "bd");
    assertEqualCharNodes(traverser.breadthFirst('c'), "cadb");
    assertEqualCharNodes(traverser.breadthFirst('d'), "d");
  }

  @Test
  public void forGraph_breadthFirst_cycle() {
    Traverser<Character> traverser = Traverser.forGraph(CYCLE_GRAPH);
    assertEqualCharNodes(traverser.breadthFirst('a'), "abcd");
    assertEqualCharNodes(traverser.breadthFirst('b'), "bcda");
    assertEqualCharNodes(traverser.breadthFirst('c'), "cdab");
    assertEqualCharNodes(traverser.breadthFirst('d'), "dabc");
  }

  @Test
  public void forGraph_breadthFirst_twoCycles() {
    Traverser<Character> traverser = Traverser.forGraph(TWO_CYCLES_GRAPH);
    assertEqualCharNodes(traverser.breadthFirst('a'), "abcd");
    assertEqualCharNodes(traverser.breadthFirst('b'), "bcda");
    assertEqualCharNodes(traverser.breadthFirst('c'), "cdab");
    assertEqualCharNodes(traverser.breadthFirst('d'), "dabc");
  }

  @Test
  public void forGraph_breadthFirst_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forGraph(TREE);

    assertEqualCharNodes(traverser.breadthFirst('h'), "hdegabcf");
    assertEqualCharNodes(traverser.breadthFirst('d'), "dabc");
    assertEqualCharNodes(traverser.breadthFirst('a'), "a");
  }

  @Test
  public void forGraph_breadthFirst_twoTrees() {
    Iterable<Character> result = Traverser.forGraph(TWO_TREES).breadthFirst('a');

    assertEqualCharNodes(result, "ab");
  }

  @Test
  public void forGraph_breadthFirst_singleRoot() {
    Iterable<Character> result = Traverser.forGraph(SINGLE_ROOT).breadthFirst('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forGraph_breadthFirst_emptyGraph() {
    try {
      Traverser.forGraph(createDirectedGraph()).breadthFirst('a');
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  /**
   * Checks that the elements of the iterable are calculated on the fly. Concretely, that means that
   * {@link SuccessorsFunction#successors(Object)} can only be called for a subset of all nodes.
   */
  @Test
  public void forGraph_breadthFirst_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(DIAMOND_GRAPH);
    Iterable<Character> result = Traverser.forGraph(graph).breadthFirst('a');

    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'b');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'a', 'b', 'b');
  }

  @Test
  public void forGraph_depthFirstPreOrder_javadocExample_canBeIteratedMultipleTimes() {
    Iterable<Character> result = Traverser.forGraph(JAVADOC_GRAPH).depthFirstPreOrder('a');

    assertEqualCharNodes(result, "abecfd");
    assertEqualCharNodes(result, "abecfd");
  }

  @Test
  public void forGraph_depthFirstPreOrder_diamond() {
    Traverser<Character> traverser = Traverser.forGraph(DIAMOND_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPreOrder('a'), "abdc");
    assertEqualCharNodes(traverser.depthFirstPreOrder('b'), "bd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('c'), "cd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('d'), "d");
  }

  @Test
  public void forGraph_depthFirstPreOrder_multigraph() {
    Traverser<Character> traverser = Traverser.forGraph(MULTI_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPreOrder('a'), "abdc");
    assertEqualCharNodes(traverser.depthFirstPreOrder('b'), "bd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('c'), "cabd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('d'), "d");
  }

  @Test
  public void forGraph_depthFirstPreOrder_cycle() {
    Traverser<Character> traverser = Traverser.forGraph(CYCLE_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPreOrder('a'), "abcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('b'), "bcda");
    assertEqualCharNodes(traverser.depthFirstPreOrder('c'), "cdab");
    assertEqualCharNodes(traverser.depthFirstPreOrder('d'), "dabc");
  }

  @Test
  public void forGraph_depthFirstPreOrder_twoCycles() {
    Traverser<Character> traverser = Traverser.forGraph(TWO_CYCLES_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPreOrder('a'), "abcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('b'), "bcda");
    assertEqualCharNodes(traverser.depthFirstPreOrder('c'), "cdab");
    assertEqualCharNodes(traverser.depthFirstPreOrder('d'), "dabc");
  }

  @Test
  public void forGraph_depthFirstPreOrder_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forGraph(TREE);

    assertEqualCharNodes(traverser.depthFirstPreOrder('h'), "hdabcegf");
    assertEqualCharNodes(traverser.depthFirstPreOrder('d'), "dabc");
    assertEqualCharNodes(traverser.depthFirstPreOrder('a'), "a");
  }

  @Test
  public void forGraph_depthFirstPreOrder_twoTrees() {
    Iterable<Character> result = Traverser.forGraph(TWO_TREES).depthFirstPreOrder('a');

    assertEqualCharNodes(result, "ab");
  }

  @Test
  public void forGraph_depthFirstPreOrder_singleRoot() {
    Iterable<Character> result = Traverser.forGraph(SINGLE_ROOT).depthFirstPreOrder('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forGraph_depthFirstPreOrder_emptyGraph() {
    try {
      Traverser.forGraph(createDirectedGraph()).depthFirstPreOrder('a');
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void forGraph_depthFirstPreOrder_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(DIAMOND_GRAPH);
    Iterable<Character> result = Traverser.forGraph(graph).depthFirstPreOrder('a');

    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'b', 'd');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'a', 'b', 'b', 'd', 'd');
  }

  @Test
  public void forGraph_depthFirstPostOrder_javadocExample_canBeIteratedMultipleTimes() {
    Iterable<Character> result = Traverser.forGraph(JAVADOC_GRAPH).depthFirstPostOrder('a');
    assertEqualCharNodes(result, "fcebda");
    assertEqualCharNodes(result, "fcebda");
  }

  @Test
  public void forGraph_depthFirstPostOrder_diamond() {
    Traverser<Character> traverser = Traverser.forGraph(DIAMOND_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "dbca");
    assertEqualCharNodes(traverser.depthFirstPostOrder('b'), "db");
    assertEqualCharNodes(traverser.depthFirstPostOrder('c'), "dc");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "d");
  }

  @Test
  public void forGraph_depthFirstPostOrder_multigraph() {
    Traverser<Character> traverser = Traverser.forGraph(MULTI_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "dbca");
    assertEqualCharNodes(traverser.depthFirstPostOrder('b'), "db");
    assertEqualCharNodes(traverser.depthFirstPostOrder('c'), "dbac");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "d");
  }

  @Test
  public void forGraph_depthFirstPostOrder_cycle() {
    Traverser<Character> traverser = Traverser.forGraph(CYCLE_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "dcba");
    assertEqualCharNodes(traverser.depthFirstPostOrder('b'), "adcb");
    assertEqualCharNodes(traverser.depthFirstPostOrder('c'), "badc");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "cbad");
  }

  @Test
  public void forGraph_depthFirstPostOrder_twoCycles() {
    Traverser<Character> traverser = Traverser.forGraph(TWO_CYCLES_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "dcba");
    assertEqualCharNodes(traverser.depthFirstPostOrder('b'), "adcb");
    assertEqualCharNodes(traverser.depthFirstPostOrder('c'), "badc");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "cbad");
  }

  @Test
  public void forGraph_depthFirstPostOrder_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forGraph(TREE);

    assertEqualCharNodes(traverser.depthFirstPostOrder('h'), "abcdefgh");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "abcd");
    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "a");
  }

  @Test
  public void forGraph_depthFirstPostOrder_twoTrees() {
    Iterable<Character> result = Traverser.forGraph(TWO_TREES).depthFirstPostOrder('a');

    assertEqualCharNodes(result, "ba");
  }

  @Test
  public void forGraph_depthFirstPostOrder_singleRoot() {
    Iterable<Character> result = Traverser.forGraph(SINGLE_ROOT).depthFirstPostOrder('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forGraph_depthFirstPostOrder_emptyGraph() {
    try {
      Traverser.forGraph(createDirectedGraph()).depthFirstPostOrder('a');
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void forGraph_depthFirstPostOrder_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(DIAMOND_GRAPH);
    Iterable<Character> result = Traverser.forGraph(graph).depthFirstPostOrder('a');

    assertEqualCharNodes(Iterables.limit(result, 2), "db");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'b', 'd');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "db");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'a', 'b', 'b', 'd', 'd');
  }

  @Test
  @SuppressWarnings("CheckReturnValue")
  public void forTree_acceptsDirectedGraph() throws Exception {
    MutableGraph<String> graph = GraphBuilder.directed().build();
    graph.putEdge("a", "b");

    Traverser.forTree(graph); // Does not throw
  }

  @Test
  public void forTree_withUndirectedGraph_throws() throws Exception {
    MutableGraph<String> graph = GraphBuilder.undirected().build();
    graph.putEdge("a", "b");

    try {
      Traverser.forTree(graph);
      fail("Expected exception");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  @SuppressWarnings("CheckReturnValue")
  public void forTree_acceptsDirectedValueGraph() throws Exception {
    MutableValueGraph<String, Integer> valueGraph = ValueGraphBuilder.directed().build();
    valueGraph.putEdgeValue("a", "b", 11);

    Traverser.forTree(valueGraph); // Does not throw
  }

  @Test
  public void forTree_withUndirectedValueGraph_throws() throws Exception {
    MutableValueGraph<String, Integer> valueGraph = ValueGraphBuilder.undirected().build();
    valueGraph.putEdgeValue("a", "b", 11);

    try {
      Traverser.forTree(valueGraph);
      fail("Expected exception");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  @SuppressWarnings("CheckReturnValue")
  public void forTree_acceptsDirectedNetwork() throws Exception {
    MutableNetwork<String, Integer> network = NetworkBuilder.directed().build();
    network.addEdge("a", "b", 11);

    Traverser.forTree(network); // Does not throw
  }

  @Test
  public void forTree_withUndirectedNetwork_throws() throws Exception {
    MutableNetwork<String, Integer> network = NetworkBuilder.undirected().build();
    network.addEdge("a", "b", 11);

    try {
      Traverser.forTree(network);
      fail("Expected exception");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void forTree_breadthFirst_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(TREE);

    assertEqualCharNodes(traverser.breadthFirst('h'), "hdegabcf");
    assertEqualCharNodes(traverser.breadthFirst('d'), "dabc");
    assertEqualCharNodes(traverser.breadthFirst('a'), "a");
  }

  @Test
  public void forTree_breadthFirst_cyclicGraphContainingTree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(CYCLIC_GRAPH_CONTAINING_TREE);

    assertEqualCharNodes(traverser.breadthFirst('a'), "abcd");
    assertEqualCharNodes(traverser.breadthFirst('b'), "bcd");
    assertEqualCharNodes(traverser.breadthFirst('d'), "d");
  }

  @Test
  public void forTree_breadthFirst_graphContainingTreeAndDiamond() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(GRAPH_CONTAINING_TREE_AND_DIAMOND);

    assertEqualCharNodes(traverser.breadthFirst('a'), "abcd");
    assertEqualCharNodes(traverser.breadthFirst('b'), "bcd");
    assertEqualCharNodes(traverser.breadthFirst('d'), "d");
  }

  @Test
  public void forTree_breadthFirst_twoTrees() {
    Iterable<Character> result = Traverser.forTree(TWO_TREES).breadthFirst('a');

    assertEqualCharNodes(result, "ab");
  }

  @Test
  public void forTree_breadthFirst_singleRoot() {
    Iterable<Character> result = Traverser.forTree(SINGLE_ROOT).breadthFirst('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forTree_breadthFirst_emptyGraph() {
    try {
      Traverser.forTree(createDirectedGraph()).breadthFirst('a');
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void forTree_breadthFirst_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(TREE);
    Iterable<Character> result = Traverser.forGraph(graph).breadthFirst('h');

    assertEqualCharNodes(Iterables.limit(result, 2), "hd");
    assertThat(graph.requestedNodes).containsExactly('h', 'h', 'd');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "hd");
    assertThat(graph.requestedNodes).containsExactly('h', 'h', 'h', 'd', 'd');
  }

  @Test
  public void forTree_depthFirstPreOrder_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(TREE);

    assertEqualCharNodes(traverser.depthFirstPreOrder('h'), "hdabcegf");
    assertEqualCharNodes(traverser.depthFirstPreOrder('d'), "dabc");
    assertEqualCharNodes(traverser.depthFirstPreOrder('a'), "a");
  }

  @Test
  public void forTree_depthFirstPreOrder_cyclicGraphContainingTree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(CYCLIC_GRAPH_CONTAINING_TREE);

    assertEqualCharNodes(traverser.depthFirstPreOrder('a'), "abcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('b'), "bcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('d'), "d");
  }

  @Test
  public void forTree_depthFirstPreOrder_graphContainingTreeAndDiamond() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(GRAPH_CONTAINING_TREE_AND_DIAMOND);

    assertEqualCharNodes(traverser.depthFirstPreOrder('a'), "abcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('b'), "bcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('d'), "d");
  }

  @Test
  public void forTree_depthFirstPreOrder_twoTrees() {
    Iterable<Character> result = Traverser.forTree(TWO_TREES).depthFirstPreOrder('a');

    assertEqualCharNodes(result, "ab");
  }

  @Test
  public void forTree_depthFirstPreOrder_singleRoot() {
    Iterable<Character> result = Traverser.forTree(SINGLE_ROOT).depthFirstPreOrder('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forTree_depthFirstPreOrder_emptyGraph() {
    try {
      Traverser.forTree(createDirectedGraph()).depthFirstPreOrder('a');
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void forTree_depthFirstPreOrder_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(TREE);
    Iterable<Character> result = Traverser.forGraph(graph).depthFirstPreOrder('h');

    assertEqualCharNodes(Iterables.limit(result, 2), "hd");
    assertThat(graph.requestedNodes).containsExactly('h', 'h', 'd', 'a');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "hd");
    assertThat(graph.requestedNodes).containsExactly('h', 'h', 'h', 'd', 'd', 'a', 'a');
  }

  @Test
  public void forTree_depthFirstPostOrder_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(TREE);

    assertEqualCharNodes(traverser.depthFirstPostOrder('h'), "abcdefgh");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "abcd");
    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "a");
  }

  @Test
  public void forTree_depthFirstPostOrder_cyclicGraphContainingTree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(CYCLIC_GRAPH_CONTAINING_TREE);

    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "cdba");
    assertEqualCharNodes(traverser.depthFirstPostOrder('b'), "cdb");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "d");
  }

  @Test
  public void forTree_depthFirstPostOrder_graphContainingTreeAndDiamond() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(GRAPH_CONTAINING_TREE_AND_DIAMOND);

    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "cdba");
    assertEqualCharNodes(traverser.depthFirstPostOrder('b'), "cdb");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "d");
  }

  @Test
  public void forTree_depthFirstPostOrder_twoTrees() {
    Iterable<Character> result = Traverser.forTree(TWO_TREES).depthFirstPostOrder('a');

    assertEqualCharNodes(result, "ba");
  }

  @Test
  public void forTree_depthFirstPostOrder_singleRoot() {
    Iterable<Character> result = Traverser.forTree(SINGLE_ROOT).depthFirstPostOrder('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forTree_depthFirstPostOrder_emptyGraph() {
    try {
      Traverser.forTree(createDirectedGraph()).depthFirstPostOrder('a');
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void forTree_depthFirstPostOrder_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(TREE);
    Iterable<Character> result = Traverser.forGraph(graph).depthFirstPostOrder('h');

    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('h', 'h', 'd', 'a', 'b');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('h', 'h', 'h', 'd', 'd', 'a', 'a', 'b', 'b');
  }

  private static SuccessorsFunction<Character> createDirectedGraph(String... edges) {
    return createGraph(/* directed = */ true, edges);
  }

  private static SuccessorsFunction<Character> createUndirectedGraph(String... edges) {
    return createGraph(/* directed = */ false, edges);
  }

  /**
   * Creates a graph from a list of node pairs (encoded as strings, e.g. "ab" means that this graph
   * has an edge between 'a' and 'b').
   *
   * <p>The {@code successors} are always returned in alphabetical order.
   */
  private static SuccessorsFunction<Character> createGraph(boolean directed, String... edges) {
    ImmutableMultimap.Builder<Character, Character> graphMapBuilder = ImmutableMultimap.builder();
    for (String edge : edges) {
      checkArgument(
          edge.length() == 2, "Expecting each edge to consist of 2 characters but got %s", edge);
      char node1 = edge.charAt(0);
      char node2 = edge.charAt(1);
      graphMapBuilder.put(node1, node2);
      if (!directed) {
        graphMapBuilder.put(node2, node1);
      }
    }
    final ImmutableMultimap<Character, Character> graphMap = graphMapBuilder.build();

    return new SuccessorsFunction<Character>() {
      @Override
      public Iterable<? extends Character> successors(Character node) {
        checkArgument(
            graphMap.containsKey(node) || graphMap.containsValue(node),
            "Node %s is not an element of this graph",
            node);
        return Ordering.natural().immutableSortedCopy(graphMap.get(node));
      }
    };
  }

  private static ImmutableGraph<Character> createSingleRootGraph() {
    MutableGraph<Character> graph = GraphBuilder.directed().build();
    graph.addNode('a');
    return ImmutableGraph.copyOf(graph);
  }

  private static void assertEqualCharNodes(Iterable<Character> result, String expectedCharacters) {
    assertThat(ImmutableList.copyOf(result))
        .containsExactlyElementsIn(Chars.asList(expectedCharacters.toCharArray()))
        .inOrder();
  }

  private static class RequestSavingGraph implements SuccessorsFunction<Character> {
    private final SuccessorsFunction<Character> delegate;
    final Multiset<Character> requestedNodes = HashMultiset.create();

    RequestSavingGraph(SuccessorsFunction<Character> delegate) {
      this.delegate = checkNotNull(delegate);
    }

    @Override
    public Iterable<? extends Character> successors(Character node) {
      requestedNodes.add(node);
      return delegate.successors(node);
    }
  }
}
