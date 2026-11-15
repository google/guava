package com.google.common.graph;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jspecify.annotations.NullUnmarked;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@NullUnmarked
public final class InvalidatableSetTest {
  Set<Integer> wrappedSet;
  Set<Integer> copyOfWrappedSet;
  InvalidatableSet<Integer> setToTest;

  @Before
  public void createSets() {
    wrappedSet = new HashSet<>();
    wrappedSet.add(1);
    wrappedSet.add(2);
    wrappedSet.add(3);

    copyOfWrappedSet = ImmutableSet.copyOf(wrappedSet);
    setToTest =
        InvalidatableSet.of(wrappedSet, () -> wrappedSet.contains(1), () -> 1 + "is not present");
  }

  @Test
  public void testHashCode_invalidatedNetworkAdjacentEdges() {
    MutableNetwork<Integer, String> network = NetworkBuilder.directed().build();
    network.addEdge(1, 2, "a");
    network.addEdge(2, 3, "b");
    Set<String> view = network.adjacentEdges("a");
    assertThat(view.hashCode()).isEqualTo(ImmutableSet.of("b").hashCode());

    network.removeNode(1);

    assertThrows(IllegalStateException.class, view::size);
    int unused = view.hashCode(); // No particular hash value is promised for an invalidated view.
  }

  @Test
  public void testHashCode_invalidatedGraphAsNetworkInEdges() {
    MutableGraph<Integer> graph = GraphBuilder.directed().build();
    graph.putEdge(1, 2);
    Set<EndpointPair<Integer>> view = graph.asNetwork().inEdges(2);
    assertThat(view.hashCode()).isEqualTo(ImmutableSet.of(EndpointPair.ordered(1, 2)).hashCode());

    graph.removeNode(2);

    assertThrows(IllegalStateException.class, view::size);
    int unused = view.hashCode();
  }

  @Test
  public void testHashCode_invalidatedValueGraphAsGraphIncidentEdges() {
    MutableValueGraph<Integer, String> graph = ValueGraphBuilder.directed().build();
    graph.putEdgeValue(1, 2, "a");
    Set<EndpointPair<Integer>> view = graph.asGraph().incidentEdges(1);
    assertThat(view.hashCode()).isEqualTo(ImmutableSet.of(EndpointPair.ordered(1, 2)).hashCode());

    graph.removeNode(1);

    assertThrows(IllegalStateException.class, view::size);
    int unused = view.hashCode();
  }

  @Test
  public void testHashCode_validViewReflectsChanges() {
    assertThat(setToTest.hashCode()).isEqualTo(copyOfWrappedSet.hashCode());
    wrappedSet.remove(2);
    wrappedSet.add(4);
    assertThat(setToTest).containsExactly(1, 3, 4);
    assertThat(setToTest.hashCode()).isEqualTo(ImmutableSet.of(1, 3, 4).hashCode());
  }

  @Test
  public void testHashCode_invalidatedSimpleView() {
    wrappedSet.remove(1);
    assertThrows(IllegalStateException.class, setToTest::size);
    int unused = setToTest.hashCode();
  }

  @Test
  public void testHashCode_validDelegateExceptionPropagates() {
    IllegalArgumentException expected = new IllegalArgumentException("element hashCode failed");
    Object element =
        new Object() {
          @Override
          public int hashCode() {
            throw expected;
          }
        };
    Set<Object> view =
        InvalidatableSet.of(Collections.singleton(element), () -> true, () -> "invalid");
    assertThat(assertThrows(IllegalArgumentException.class, view::hashCode))
        .isSameInstanceAs(expected);
  }

  @Test
  @SuppressWarnings("SelfEquals") // test of our equals() implementation
  public void testEquals() {
    // sanity check on construction of copyOfWrappedSet
    assertThat(wrappedSet).isEqualTo(copyOfWrappedSet);

    // test that setToTest is still valid
    new EqualsTester().addEqualityGroup(setToTest, wrappedSet, copyOfWrappedSet).testEquals();

    // invalidate setToTest
    wrappedSet.remove(1);
    // sanity check on update of wrappedSet
    assertThat(wrappedSet).isNotEqualTo(copyOfWrappedSet);

    ImmutableSet<Integer> copyOfModifiedSet = ImmutableSet.copyOf(wrappedSet); // {2,3}
    // sanity check on construction of copyOfModifiedSet
    assertThat(wrappedSet).isEqualTo(copyOfModifiedSet);

    // setToTest should throw when it calls equals(), or equals is called on it, except for itself
    assertThat(setToTest.equals(setToTest)).isTrue();
    assertThrows(IllegalStateException.class, () -> setToTest.equals(wrappedSet));
    assertThrows(IllegalStateException.class, () -> setToTest.equals(copyOfWrappedSet));
    assertThrows(IllegalStateException.class, () -> setToTest.equals(copyOfModifiedSet));
    assertThrows(IllegalStateException.class, () -> wrappedSet.equals(setToTest));
    assertThrows(IllegalStateException.class, () -> copyOfWrappedSet.equals(setToTest));
    assertThrows(IllegalStateException.class, () -> copyOfModifiedSet.equals(setToTest));
  }
}
