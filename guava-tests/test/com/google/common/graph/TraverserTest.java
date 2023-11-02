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
import static com.google.common.collect.Lists.charactersOf;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

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
  public void forGraph_breadthFirstIterable_javadocExample_canBeIteratedMultipleTimes() {
    Iterable<Character> result = Traverser.forGraph(JAVADOC_GRAPH).breadthFirst(charactersOf("bf"));

    assertEqualCharNodes(result, "bfaecd");
    assertEqualCharNodes(result, "bfaecd");
  }

  @Test
  public void forGraph_breadthFirst_infinite() {
    Iterable<Integer> result =
        Traverser.forGraph(fixedSuccessors(Iterables.cycle(1, 2, 3))).breadthFirst(0);
    assertThat(Iterables.limit(result, 4)).containsExactly(0, 1, 2, 3).inOrder();
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
  public void forGraph_breadthFirstIterable_diamond() {
    Traverser<Character> traverser = Traverser.forGraph(DIAMOND_GRAPH);
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("")), "");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("bc")), "bcd");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("a")), "abcd");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("acdb")), "acdb");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("db")), "db");
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
  public void forGraph_breadthFirstIterable_multiGraph() {
    Traverser<Character> traverser = Traverser.forGraph(MULTI_GRAPH);
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("ac")), "acbd");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("cb")), "cbad");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("db")), "db");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("d")), "d");
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
  public void forGraph_breadthFirstIterable_cycle() {
    Traverser<Character> traverser = Traverser.forGraph(CYCLE_GRAPH);
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("a")), "abcd");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("bd")), "bdca");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("dc")), "dcab");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("bc")), "bcda");
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
  public void forGraph_breadthFirstIterable_twoCycles() {
    Traverser<Character> traverser = Traverser.forGraph(TWO_CYCLES_GRAPH);
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("a")), "abcd");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("bd")), "bdca");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("dc")), "dcab");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("bc")), "bcda");
  }

  @Test
  public void forGraph_breadthFirst_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forGraph(TREE);

    assertEqualCharNodes(traverser.breadthFirst('h'), "hdegabcf");
    assertEqualCharNodes(traverser.breadthFirst('d'), "dabc");
    assertEqualCharNodes(traverser.breadthFirst('a'), "a");
  }

  @Test
  public void forGraph_breadthFirstIterable_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forGraph(TREE);

    assertEqualCharNodes(traverser.breadthFirst(charactersOf("hg")), "hgdefabc");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("gd")), "gdfabc");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("bdgh")), "bdghacfe");
  }

  @Test
  public void forGraph_breadthFirst_twoTrees() {
    Iterable<Character> result = Traverser.forGraph(TWO_TREES).breadthFirst('a');

    assertEqualCharNodes(result, "ab");
  }

  @Test
  public void forGraph_breadthFirstIterable_twoTrees() {
    assertEqualCharNodes(Traverser.forGraph(TWO_TREES).breadthFirst(charactersOf("a")), "ab");
    assertEqualCharNodes(Traverser.forGraph(TWO_TREES).breadthFirst(charactersOf("ac")), "acbd");
  }

  @Test
  public void forGraph_breadthFirst_singleRoot() {
    Iterable<Character> result = Traverser.forGraph(SINGLE_ROOT).breadthFirst('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forGraph_breadthFirstIterable_singleRoot() {
    Iterable<Character> result = Traverser.forGraph(SINGLE_ROOT).breadthFirst(charactersOf("a"));

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forGraph_breadthFirst_emptyGraph() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forGraph(createDirectedGraph()).breadthFirst('a'));
  }

  /**
   * Checks that the elements of the iterable are calculated on the fly. Concretely, that means that
   * {@link SuccessorsFunction#successors(Object)} can only be called for a subset of all nodes.
   */
  @Test
  public void forGraph_breadthFirstIterable_emptyGraph() {
    assertEqualCharNodes(
        Traverser.forGraph(createDirectedGraph()).breadthFirst(charactersOf("")), "");
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forGraph(createDirectedGraph()).breadthFirst(charactersOf("a")));
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
  public void forGraph_breadthFirstIterable_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(DIAMOND_GRAPH);
    Iterable<Character> result = Traverser.forGraph(graph).breadthFirst(charactersOf("ab"));

    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'b', 'b');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'a', 'b', 'b', 'b');
  }

  @Test
  public void forGraph_depthFirstPreOrder_javadocExample_canBeIteratedMultipleTimes() {
    Iterable<Character> result = Traverser.forGraph(JAVADOC_GRAPH).depthFirstPreOrder('a');

    assertEqualCharNodes(result, "abecfd");
    assertEqualCharNodes(result, "abecfd");
  }

  @Test
  public void forGraph_depthFirstPreOrderIterable_javadocExample_canBeIteratedMultipleTimes() {
    Iterable<Character> result =
        Traverser.forGraph(JAVADOC_GRAPH).depthFirstPreOrder(charactersOf("bc"));

    assertEqualCharNodes(result, "bacefd");
    assertEqualCharNodes(result, "bacefd");
  }

  @Test
  public void forGraph_depthFirstPreOrder_infinite() {
    Iterable<Integer> result =
        Traverser.forGraph(fixedSuccessors(Iterables.cycle(1, 2, 3))).depthFirstPreOrder(0);
    assertThat(Iterables.limit(result, 3)).containsExactly(0, 1, 2).inOrder();
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
  public void forGraph_depthFirstPreOrderIterable_diamond() {
    Traverser<Character> traverser = Traverser.forGraph(DIAMOND_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("")), "");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("bc")), "bdc");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("a")), "abdc");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("acdb")), "abdc");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("db")), "db");
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
  public void forGraph_depthFirstPreOrderIterable_multigraph() {
    Traverser<Character> traverser = Traverser.forGraph(MULTI_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("ac")), "abdc");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("cb")), "cabd");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("db")), "db");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("d")), "d");
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
  public void forGraph_depthFirstPreOrderIterable_cycle() {
    Traverser<Character> traverser = Traverser.forGraph(CYCLE_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("a")), "abcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("bd")), "bcda");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("dc")), "dabc");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("bc")), "bcda");
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
  public void forGraph_depthFirstPreOrderIterable_twoCycles() {
    Traverser<Character> traverser = Traverser.forGraph(TWO_CYCLES_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("a")), "abcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("bd")), "bcda");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("dc")), "dabc");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("bc")), "bcda");
  }

  @Test
  public void forGraph_depthFirstPreOrder_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forGraph(TREE);

    assertEqualCharNodes(traverser.depthFirstPreOrder('h'), "hdabcegf");
    assertEqualCharNodes(traverser.depthFirstPreOrder('d'), "dabc");
    assertEqualCharNodes(traverser.depthFirstPreOrder('a'), "a");
  }

  @Test
  public void forGraph_depthFirstPreOrderIterable_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forGraph(TREE);

    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("hg")), "hdabcegf");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("gd")), "gfdabc");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("bdgh")), "bdacgfhe");
  }

  @Test
  public void forGraph_depthFirstPreOrder_twoTrees() {
    Iterable<Character> result = Traverser.forGraph(TWO_TREES).depthFirstPreOrder('a');

    assertEqualCharNodes(result, "ab");
  }

  @Test
  public void forGraph_depthFirstPreOrderIterable_twoTrees() {
    assertEqualCharNodes(Traverser.forGraph(TWO_TREES).depthFirstPreOrder(charactersOf("a")), "ab");
    assertEqualCharNodes(
        Traverser.forGraph(TWO_TREES).depthFirstPreOrder(charactersOf("ac")), "abcd");
  }

  @Test
  public void forGraph_depthFirstPreOrder_singleRoot() {
    Iterable<Character> result = Traverser.forGraph(SINGLE_ROOT).depthFirstPreOrder('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forGraph_depthFirstPreOrderIterable_singleRoot() {
    Iterable<Character> result =
        Traverser.forGraph(SINGLE_ROOT).depthFirstPreOrder(charactersOf("a"));

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forGraph_depthFirstPreOrder_emptyGraph() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forGraph(createDirectedGraph()).depthFirstPreOrder('a'));
  }

  @Test
  public void forGraph_depthFirstPreOrderIterable_emptyGraph() {
    assertEqualCharNodes(
        Traverser.forGraph(createDirectedGraph()).depthFirstPreOrder(charactersOf("")), "");
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forGraph(createDirectedGraph()).depthFirstPreOrder(charactersOf("a")));
  }

  @Test
  public void forGraph_depthFirstPreOrder_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(DIAMOND_GRAPH);
    Iterable<Character> result = Traverser.forGraph(graph).depthFirstPreOrder('a');

    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'b');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'a', 'b', 'b');
  }

  @Test
  public void forGraph_depthFirstPreOrderIterable_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(DIAMOND_GRAPH);
    Iterable<Character> result = Traverser.forGraph(graph).depthFirstPreOrder(charactersOf("ac"));

    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'b', 'c');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'a', 'b', 'b', 'c');
  }

  @Test
  public void forGraph_depthFirstPostOrder_javadocExample_canBeIteratedMultipleTimes() {
    Iterable<Character> result = Traverser.forGraph(JAVADOC_GRAPH).depthFirstPostOrder('a');
    assertEqualCharNodes(result, "fcebda");
    assertEqualCharNodes(result, "fcebda");
  }

  @Test
  public void forGraph_depthFirstPostOrderIterable_javadocExample_canBeIteratedMultipleTimes() {
    Iterable<Character> result =
        Traverser.forGraph(JAVADOC_GRAPH).depthFirstPostOrder(charactersOf("bf"));
    assertEqualCharNodes(result, "efcdab");
    assertEqualCharNodes(result, "efcdab");
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
  public void forGraph_depthFirstPostOrderIterable_diamond() {
    Traverser<Character> traverser = Traverser.forGraph(DIAMOND_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("")), "");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("bc")), "dbc");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("a")), "dbca");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("acdb")), "dbca");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("db")), "db");
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
  public void forGraph_depthFirstPostOrderIterable_multigraph() {
    Traverser<Character> traverser = Traverser.forGraph(MULTI_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("ac")), "dbca");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("cb")), "dbac");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("db")), "db");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("d")), "d");
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
  public void forGraph_depthFirstPostOrderIterable_cycle() {
    Traverser<Character> traverser = Traverser.forGraph(CYCLE_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("a")), "dcba");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("bd")), "adcb");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("dc")), "cbad");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("bc")), "adcb");
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
  public void forGraph_depthFirstPostOrderIterable_twoCycles() {
    Traverser<Character> traverser = Traverser.forGraph(TWO_CYCLES_GRAPH);
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("a")), "dcba");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("bd")), "adcb");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("dc")), "cbad");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("bc")), "adcb");
  }

  @Test
  public void forGraph_depthFirstPostOrder_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forGraph(TREE);

    assertEqualCharNodes(traverser.depthFirstPostOrder('h'), "abcdefgh");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "abcd");
    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "a");
  }

  @Test
  public void forGraph_depthFirstPostOrderIterable_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forGraph(TREE);

    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("hg")), "abcdefgh");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("gd")), "fgabcd");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("bdgh")), "bacdfgeh");
  }

  @Test
  public void forGraph_depthFirstPostOrder_twoTrees() {
    Iterable<Character> result = Traverser.forGraph(TWO_TREES).depthFirstPostOrder('a');

    assertEqualCharNodes(result, "ba");
  }

  @Test
  public void forGraph_depthFirstPostOrderIterable_twoTrees() {
    assertEqualCharNodes(
        Traverser.forGraph(TWO_TREES).depthFirstPostOrder(charactersOf("a")), "ba");
    assertEqualCharNodes(
        Traverser.forGraph(TWO_TREES).depthFirstPostOrder(charactersOf("ac")), "badc");
  }

  @Test
  public void forGraph_depthFirstPostOrder_singleRoot() {
    Iterable<Character> result = Traverser.forGraph(SINGLE_ROOT).depthFirstPostOrder('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forGraph_depthFirstPostOrderIterable_singleRoot() {
    Iterable<Character> result =
        Traverser.forGraph(SINGLE_ROOT).depthFirstPostOrder(charactersOf("a"));

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forGraph_depthFirstPostOrder_emptyGraph() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forGraph(createDirectedGraph()).depthFirstPostOrder('a'));
  }

  @Test
  public void forGraph_depthFirstPostOrderIterable_emptyGraph() {
    assertEqualCharNodes(
        Traverser.forGraph(createDirectedGraph()).depthFirstPostOrder(charactersOf("")), "");
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forGraph(createDirectedGraph()).depthFirstPostOrder(charactersOf("a")));
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
  public void forGraph_depthFirstPostOrderIterable_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(DIAMOND_GRAPH);
    Iterable<Character> result = Traverser.forGraph(graph).depthFirstPostOrder(charactersOf("ac"));

    assertEqualCharNodes(Iterables.limit(result, 2), "db");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'b', 'c', 'd');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "db");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'a', 'b', 'b', 'c', 'd', 'd');
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

    assertThrows(IllegalArgumentException.class, () -> Traverser.forTree(graph));
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

    assertThrows(IllegalArgumentException.class, () -> Traverser.forTree(valueGraph));
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

    assertThrows(IllegalArgumentException.class, () -> Traverser.forTree(network));
  }

  @Test
  public void forTree_breadthFirst_infinite() {
    Iterable<Integer> result =
        Traverser.forTree(fixedSuccessors(Iterables.cycle(1, 2, 3))).breadthFirst(0);
    assertThat(Iterables.limit(result, 8)).containsExactly(0, 1, 2, 3, 1, 2, 3, 1).inOrder();
  }

  @Test
  public void forTree_breadthFirst_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(TREE);

    assertEqualCharNodes(traverser.breadthFirst('h'), "hdegabcf");
    assertEqualCharNodes(traverser.breadthFirst('d'), "dabc");
    assertEqualCharNodes(traverser.breadthFirst('a'), "a");
  }

  @Test
  public void forTree_breadthFirstIterable_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(TREE);

    assertEqualCharNodes(traverser.breadthFirst(charactersOf("")), "");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("h")), "hdegabcf");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("gd")), "gdfabc");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("age")), "agef");
  }

  @Test
  public void forTree_breadthFirst_cyclicGraphContainingTree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(CYCLIC_GRAPH_CONTAINING_TREE);

    assertEqualCharNodes(traverser.breadthFirst('a'), "abcd");
    assertEqualCharNodes(traverser.breadthFirst('b'), "bcd");
    assertEqualCharNodes(traverser.breadthFirst('d'), "d");
  }

  @Test
  public void forTree_breadthFirstIterable_cyclicGraphContainingTree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(CYCLIC_GRAPH_CONTAINING_TREE);

    assertEqualCharNodes(traverser.breadthFirst(charactersOf("a")), "abcd");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("b")), "bcd");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("cd")), "cd");
  }

  @Test
  public void forTree_breadthFirst_graphContainingTreeAndDiamond() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(GRAPH_CONTAINING_TREE_AND_DIAMOND);

    assertEqualCharNodes(traverser.breadthFirst('a'), "abcd");
    assertEqualCharNodes(traverser.breadthFirst('b'), "bcd");
    assertEqualCharNodes(traverser.breadthFirst('d'), "d");
  }

  @Test
  public void forTree_breadthFirstIterable_graphContainingTreeAndDiamond() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(GRAPH_CONTAINING_TREE_AND_DIAMOND);

    assertEqualCharNodes(traverser.breadthFirst(charactersOf("a")), "abcd");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("bg")), "bgcdh");
    assertEqualCharNodes(traverser.breadthFirst(charactersOf("ga")), "gahbcd");
  }

  @Test
  public void forTree_breadthFirst_twoTrees() {
    Iterable<Character> result = Traverser.forTree(TWO_TREES).breadthFirst('a');

    assertEqualCharNodes(result, "ab");
  }

  @Test
  public void forTree_breadthFirstIterable_twoTrees() {
    assertEqualCharNodes(Traverser.forTree(TWO_TREES).breadthFirst(charactersOf("a")), "ab");
    assertEqualCharNodes(Traverser.forTree(TWO_TREES).breadthFirst(charactersOf("ca")), "cadb");
  }

  @Test
  public void forTree_breadthFirst_singleRoot() {
    Iterable<Character> result = Traverser.forTree(SINGLE_ROOT).breadthFirst('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forTree_breadthFirstIterable_singleRoot() {
    Iterable<Character> result = Traverser.forTree(SINGLE_ROOT).breadthFirst(charactersOf("a"));

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forTree_breadthFirst_emptyGraph() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forTree(createDirectedGraph()).breadthFirst('a'));
  }

  @Test
  public void forTree_breadthFirstIterable_emptyGraph() {
    assertEqualCharNodes(
        Traverser.forTree(createDirectedGraph()).breadthFirst(charactersOf("")), "");
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forTree(createDirectedGraph()).breadthFirst(charactersOf("a")));
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
  public void forTree_breadthFirstIterable_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(TREE);
    Iterable<Character> result = Traverser.forGraph(graph).breadthFirst(charactersOf("dg"));

    assertEqualCharNodes(Iterables.limit(result, 3), "dga");
    assertThat(graph.requestedNodes).containsExactly('a', 'd', 'd', 'g', 'g');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 3), "dga");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'd', 'd', 'd', 'g', 'g', 'g');
  }

  @Test
  public void forTree_depthFirstPreOrder_infinite() {
    Iterable<Integer> result =
        Traverser.forTree(fixedSuccessors(Iterables.cycle(1, 2, 3))).depthFirstPreOrder(0);
    assertThat(Iterables.limit(result, 3)).containsExactly(0, 1, 1).inOrder();
  }

  @Test
  public void forTree_depthFirstPreOrderIterable_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(TREE);

    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("h")), "hdabcegf");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("d")), "dabc");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("a")), "a");
  }

  @Test
  public void forTree_depthFirstPreOrderIterableIterable_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(TREE);

    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("")), "");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("h")), "hdabcegf");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("gd")), "gfdabc");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("age")), "agfe");
  }

  @Test
  public void forTree_depthFirstPreOrder_cyclicGraphContainingTree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(CYCLIC_GRAPH_CONTAINING_TREE);

    assertEqualCharNodes(traverser.depthFirstPreOrder('a'), "abcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('b'), "bcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('d'), "d");
  }

  @Test
  public void forTree_depthFirstPreOrderIterable_cyclicGraphContainingTree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(CYCLIC_GRAPH_CONTAINING_TREE);

    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("a")), "abcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("b")), "bcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("cd")), "cd");
  }

  @Test
  public void forTree_depthFirstPreOrder_graphContainingTreeAndDiamond() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(GRAPH_CONTAINING_TREE_AND_DIAMOND);

    assertEqualCharNodes(traverser.depthFirstPreOrder('a'), "abcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('b'), "bcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder('d'), "d");
  }

  @Test
  public void forTree_depthFirstPreOrderIterable_graphContainingTreeAndDiamond() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(GRAPH_CONTAINING_TREE_AND_DIAMOND);

    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("a")), "abcd");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("bg")), "bcdgh");
    assertEqualCharNodes(traverser.depthFirstPreOrder(charactersOf("ga")), "ghabcd");
  }

  @Test
  public void forTree_depthFirstPreOrder_twoTrees() {
    Iterable<Character> result = Traverser.forTree(TWO_TREES).depthFirstPreOrder('a');

    assertEqualCharNodes(result, "ab");
  }

  @Test
  public void forTree_depthFirstPreOrderIterable_twoTrees() {
    assertEqualCharNodes(Traverser.forTree(TWO_TREES).depthFirstPreOrder(charactersOf("a")), "ab");
    assertEqualCharNodes(
        Traverser.forTree(TWO_TREES).depthFirstPreOrder(charactersOf("ca")), "cdab");
  }

  @Test
  public void forTree_depthFirstPreOrder_singleRoot() {
    Iterable<Character> result = Traverser.forTree(SINGLE_ROOT).depthFirstPreOrder('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forTree_depthFirstPreOrderIterable_singleRoot() {
    Iterable<Character> result =
        Traverser.forTree(SINGLE_ROOT).depthFirstPreOrder(charactersOf("a"));

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forTree_depthFirstPreOrder_emptyGraph() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forTree(createDirectedGraph()).depthFirstPreOrder('a'));
  }

  @Test
  public void forTree_depthFirstPreOrderIterable_emptyGraph() {
    assertEqualCharNodes(
        Traverser.forTree(createDirectedGraph()).depthFirstPreOrder(charactersOf("")), "");
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forTree(createDirectedGraph()).depthFirstPreOrder(charactersOf("a")));
  }

  @Test
  public void forTree_depthFirstPreOrder_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(TREE);
    Iterable<Character> result = Traverser.forGraph(graph).depthFirstPreOrder('h');

    assertEqualCharNodes(Iterables.limit(result, 2), "hd");
    assertThat(graph.requestedNodes).containsExactly('h', 'h', 'd');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "hd");
    assertThat(graph.requestedNodes).containsExactly('h', 'h', 'h', 'd', 'd');
  }

  @Test
  public void forTree_depthFirstPreOrderIterable_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(TREE);
    Iterable<Character> result = Traverser.forGraph(graph).depthFirstPreOrder(charactersOf("dg"));

    assertEqualCharNodes(Iterables.limit(result, 2), "da");
    assertThat(graph.requestedNodes).containsExactly('a', 'd', 'd', 'g');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "da");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'd', 'd', 'd', 'g');
  }

  @Test
  public void forTree_depthFirstPostOrder_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(TREE);

    assertEqualCharNodes(traverser.depthFirstPostOrder('h'), "abcdefgh");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "abcd");
    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "a");
  }

  @Test
  public void forTree_depthFirstPostOrderIterable_tree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(TREE);

    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("")), "");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("h")), "abcdefgh");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("gd")), "fgabcd");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("age")), "afge");
  }

  @Test
  public void forTree_depthFirstPostOrder_cyclicGraphContainingTree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(CYCLIC_GRAPH_CONTAINING_TREE);

    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "cdba");
    assertEqualCharNodes(traverser.depthFirstPostOrder('b'), "cdb");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "d");
  }

  @Test
  public void forTree_depthFirstPostOrderIterable_cyclicGraphContainingTree() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(CYCLIC_GRAPH_CONTAINING_TREE);

    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("a")), "cdba");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("b")), "cdb");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("cd")), "cd");
  }

  @Test
  public void forTree_depthFirstPostOrder_graphContainingTreeAndDiamond() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(GRAPH_CONTAINING_TREE_AND_DIAMOND);

    assertEqualCharNodes(traverser.depthFirstPostOrder('a'), "cdba");
    assertEqualCharNodes(traverser.depthFirstPostOrder('b'), "cdb");
    assertEqualCharNodes(traverser.depthFirstPostOrder('d'), "d");
  }

  @Test
  public void forTree_depthFirstPostOrderIterable_graphContainingTreeAndDiamond() throws Exception {
    Traverser<Character> traverser = Traverser.forTree(GRAPH_CONTAINING_TREE_AND_DIAMOND);

    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("a")), "cdba");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("bg")), "cdbhg");
    assertEqualCharNodes(traverser.depthFirstPostOrder(charactersOf("ga")), "hgcdba");
  }

  @Test
  public void forTree_depthFirstPostOrder_twoTrees() {
    Iterable<Character> result = Traverser.forTree(TWO_TREES).depthFirstPostOrder('a');

    assertEqualCharNodes(result, "ba");
  }

  @Test
  public void forTree_depthFirstPostOrderIterable_twoTrees() {
    assertEqualCharNodes(Traverser.forTree(TWO_TREES).depthFirstPostOrder(charactersOf("a")), "ba");
    assertEqualCharNodes(
        Traverser.forTree(TWO_TREES).depthFirstPostOrder(charactersOf("ca")), "dcba");
  }

  @Test
  public void forTree_depthFirstPostOrder_singleRoot() {
    Iterable<Character> result = Traverser.forTree(SINGLE_ROOT).depthFirstPostOrder('a');

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forTree_depthFirstPostOrderIterable_singleRoot() {
    Iterable<Character> result =
        Traverser.forTree(SINGLE_ROOT).depthFirstPostOrder(charactersOf("a"));

    assertEqualCharNodes(result, "a");
  }

  @Test
  public void forTree_depthFirstPostOrder_emptyGraph() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forTree(createDirectedGraph()).depthFirstPostOrder('a'));
  }

  @Test
  public void forTree_depthFirstPostOrderIterable_emptyGraph() {
    assertEqualCharNodes(
        Traverser.forTree(createDirectedGraph()).depthFirstPostOrder(charactersOf("")), "");
    assertThrows(
        IllegalArgumentException.class,
        () -> Traverser.forTree(createDirectedGraph()).depthFirstPostOrder(charactersOf("a")));
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

  @Test
  public void forTree_depthFirstPostOrderIterable_iterableIsLazy() {
    RequestSavingGraph graph = new RequestSavingGraph(TREE);
    Iterable<Character> result = Traverser.forGraph(graph).depthFirstPostOrder(charactersOf("dg"));

    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'b', 'd', 'd', 'g');

    // Iterate again to see if calculation is done again
    assertEqualCharNodes(Iterables.limit(result, 2), "ab");
    assertThat(graph.requestedNodes).containsExactly('a', 'a', 'b', 'b', 'd', 'd', 'd', 'g');
  }

  private static SuccessorsFunction<Character> createDirectedGraph(String... edges) {
    return createGraph(/* directed= */ true, edges);
  }

  private static SuccessorsFunction<Character> createUndirectedGraph(String... edges) {
    return createGraph(/* directed= */ false, edges);
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

  private static <N> SuccessorsFunction<N> fixedSuccessors(final Iterable<N> successors) {
    return new SuccessorsFunction<N>() {
      @Override
      public Iterable<N> successors(N n) {
        return successors;
      }
    };
  }
}
