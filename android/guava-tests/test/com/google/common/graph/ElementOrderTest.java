/*
 * Copyright (C) 2016 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.graph.ElementOrder.insertion;
import static com.google.common.graph.ElementOrder.unordered;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Ordering;
import java.util.Comparator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for ordering the elements of graphs. */
@RunWith(JUnit4.class)
public final class ElementOrderTest {
  // Node order tests

  @Test
  public void nodeOrder_none() {
    MutableGraph<Integer> graph = GraphBuilder.directed().nodeOrder(unordered()).build();

    assertThat(graph.nodeOrder()).isEqualTo(unordered());
  }

  @Test
  public void nodeOrder_insertion() {
    MutableGraph<Integer> graph = GraphBuilder.directed().nodeOrder(insertion()).build();

    addNodes(graph);

    assertThat(graph.nodeOrder()).isEqualTo(insertion());
    assertThat(graph.nodes()).containsExactly(3, 1, 4).inOrder();
  }

  // The default ordering is INSERTION unless otherwise specified.
  @Test
  public void nodeOrder_default() {
    MutableGraph<Integer> graph = GraphBuilder.directed().build();

    addNodes(graph);

    assertThat(graph.nodeOrder()).isEqualTo(insertion());
    assertThat(graph.nodes()).containsExactly(3, 1, 4).inOrder();
  }

  @Test
  public void nodeOrder_natural() {
    MutableGraph<Integer> graph =
        GraphBuilder.directed().nodeOrder(ElementOrder.<Integer>natural()).build();

    addNodes(graph);

    assertThat(graph.nodeOrder()).isEqualTo(ElementOrder.sorted(Ordering.<Integer>natural()));
    assertThat(graph.nodes()).containsExactly(1, 3, 4).inOrder();
  }

  @Test
  public void nodeOrder_sorted() {
    MutableGraph<Integer> graph =
        GraphBuilder.directed()
            .nodeOrder(ElementOrder.sorted(Ordering.<Integer>natural().reverse()))
            .build();

    addNodes(graph);

    assertThat(graph.nodeOrder())
        .isEqualTo(ElementOrder.sorted(Ordering.<Integer>natural().reverse()));
    assertThat(graph.nodes()).containsExactly(4, 3, 1).inOrder();
  }

  // Edge order tests

  @Test
  public void edgeOrder_none() {
    MutableNetwork<Integer, String> network =
        NetworkBuilder.directed().edgeOrder(unordered()).build();

    assertThat(network.edgeOrder()).isEqualTo(unordered());
    assertThat(network.nodeOrder()).isEqualTo(insertion()); // default
  }

  @Test
  public void edgeOrder_insertion() {
    MutableNetwork<Integer, String> network =
        NetworkBuilder.directed().edgeOrder(insertion()).build();

    addEdges(network);

    assertThat(network.edgeOrder()).isEqualTo(ElementOrder.insertion());
    assertThat(network.edges()).containsExactly("i", "e", "p").inOrder();
    assertThat(network.nodeOrder()).isEqualTo(ElementOrder.insertion()); // default
  }

  // The default ordering is INSERTION unless otherwise specified.
  @Test
  public void edgeOrder_default() {
    MutableNetwork<Integer, String> network = NetworkBuilder.directed().build();

    addEdges(network);

    assertThat(network.edgeOrder()).isEqualTo(ElementOrder.insertion());
    assertThat(network.edges()).containsExactly("i", "e", "p").inOrder();
    assertThat(network.nodeOrder()).isEqualTo(ElementOrder.insertion()); // default
  }

  @Test
  public void edgeOrder_natural() {
    MutableNetwork<Integer, String> network =
        NetworkBuilder.directed().edgeOrder(ElementOrder.<String>natural()).build();

    addEdges(network);

    assertThat(network.edgeOrder()).isEqualTo(ElementOrder.sorted(Ordering.<String>natural()));
    assertThat(network.edges()).containsExactly("e", "i", "p").inOrder();
    assertThat(network.nodeOrder()).isEqualTo(insertion()); // default
  }

  @Test
  public void edgeOrder_sorted() {
    MutableNetwork<Integer, String> network =
        NetworkBuilder.directed()
            .edgeOrder(ElementOrder.sorted(Ordering.<String>natural().reverse()))
            .build();

    addEdges(network);

    assertThat(network.edgeOrder())
        .isEqualTo(ElementOrder.sorted(Ordering.<String>natural().reverse()));
    assertThat(network.edges()).containsExactly("p", "i", "e").inOrder();
    assertThat(network.nodeOrder()).isEqualTo(ElementOrder.insertion()); // default
  }

  // Combined node and edge order tests

  @Test
  public void nodeOrderUnorderedAndEdgesSorted() {
    MutableNetwork<Integer, String> network =
        NetworkBuilder.directed()
            .nodeOrder(unordered())
            .edgeOrder(ElementOrder.sorted(Ordering.<String>natural().reverse()))
            .build();

    addEdges(network);

    assertThat(network.edgeOrder())
        .isEqualTo(ElementOrder.sorted(Ordering.<String>natural().reverse()));
    assertThat(network.edges()).containsExactly("p", "i", "e").inOrder();
    assertThat(network.nodeOrder()).isEqualTo(unordered());
    assertThat(network.nodes()).containsExactly(4, 1, 3);
  }

  // Sorting of user-defined classes

  @Test
  public void customComparator() {
    Comparator<NonComparableSuperClass> comparator =
        new Comparator<NonComparableSuperClass>() {
          @Override
          public int compare(NonComparableSuperClass left, NonComparableSuperClass right) {
            return left.value.compareTo(right.value);
          }
        };

    MutableGraph<NonComparableSuperClass> graph =
        GraphBuilder.undirected().nodeOrder(ElementOrder.sorted(comparator)).build();

    NonComparableSuperClass node1 = new NonComparableSuperClass(1);
    NonComparableSuperClass node3 = new NonComparableSuperClass(3);
    NonComparableSuperClass node5 = new NonComparableSuperClass(5);
    NonComparableSuperClass node7 = new NonComparableSuperClass(7);

    graph.addNode(node1);
    graph.addNode(node7);
    graph.addNode(node5);
    graph.addNode(node3);

    assertThat(graph.nodeOrder().comparator()).isEqualTo(comparator);
    assertThat(graph.nodes()).containsExactly(node1, node3, node5, node7).inOrder();
  }

  @Test
  public void customComparable() {
    MutableGraph<ComparableSubClass> graph =
        GraphBuilder.undirected().nodeOrder(ElementOrder.<ComparableSubClass>natural()).build();

    ComparableSubClass node2 = new ComparableSubClass(2);
    ComparableSubClass node4 = new ComparableSubClass(4);
    ComparableSubClass node6 = new ComparableSubClass(6);
    ComparableSubClass node8 = new ComparableSubClass(8);

    graph.addNode(node4);
    graph.addNode(node2);
    graph.addNode(node6);
    graph.addNode(node8);

    assertThat(graph.nodeOrder().comparator()).isEqualTo(Ordering.natural());
    assertThat(graph.nodes()).containsExactly(node2, node4, node6, node8).inOrder();
  }

  private static void addNodes(MutableGraph<Integer> graph) {
    graph.addNode(3);
    graph.addNode(1);
    graph.addNode(4);
  }

  private static void addEdges(MutableNetwork<Integer, String> network) {
    network.addEdge(3, 1, "i");
    network.addEdge(1, 4, "e");
    network.addEdge(4, 3, "p");
  }

  private static class NonComparableSuperClass {
    final Integer value;

    NonComparableSuperClass(Integer value) {
      this.value = checkNotNull(value);
    }

    @Override
    public String toString() {
      return "value=" + value;
    }
  }

  @SuppressWarnings("ComparableType")
  private static class ComparableSubClass extends NonComparableSuperClass
      implements Comparable<NonComparableSuperClass> {

    ComparableSubClass(Integer value) {
      super(value);
    }

    @Override
    public int compareTo(NonComparableSuperClass other) {
      return value.compareTo(other.value);
    }
  }
}
