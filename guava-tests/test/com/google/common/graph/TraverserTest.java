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
import static com.google.common.truth.Truth.assertThat;

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

  @Test
  public void forGraph_breadthFirst_javadocExample_iterableCanBeIteratedMultipleTimes() {
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

  /**
   * Checks that the elements of the iterable are calculated on the fly. Concretely, that means that
   * {@link SuccessorsFunction#successors(Object)} can only be called for a subset of all nodes.
   */
  @Test
  public void forGraph_breadthFirst_iterableIsLazy() {
    IncrementingGraph graph = new IncrementingGraph();
    Iterable<Integer> result = Traverser.forGraph(graph).breadthFirst(1);

    assertThat(ImmutableList.copyOf(Iterables.limit(result, 2))).containsExactly(1, 2).inOrder();
    assertThat(graph.requestedNodes).containsExactly(1, 2);

    // Iterate again to see if calculation is done again
    assertThat(ImmutableList.copyOf(Iterables.limit(result, 2))).containsExactly(1, 2).inOrder();
    assertThat(graph.requestedNodes).containsExactly(1, 1, 2, 2);
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
        return Ordering.natural().immutableSortedCopy(graphMap.get(node));
      }
    };
  }

  private static void assertEqualCharNodes(Iterable<Character> result, String expectedCharacters) {
    assertThat(ImmutableList.copyOf(result))
        .containsExactlyElementsIn(Chars.asList(expectedCharacters.toCharArray()))
        .inOrder();
  }

  private static class IncrementingGraph implements SuccessorsFunction<Integer> {
    final Multiset<Integer> requestedNodes = HashMultiset.create();

    @Override
    public Iterable<? extends Integer> successors(Integer node) {
      requestedNodes.add(node);
      return ImmutableList.of(node + 1);
    }
  }
}
