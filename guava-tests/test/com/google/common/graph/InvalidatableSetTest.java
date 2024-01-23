package com.google.common.graph;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
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
  @SuppressWarnings("TruthSelfEquals")
  public void testEquals() {
    // sanity check on construction of copyOfWrappedSet
    assertThat(wrappedSet).isEqualTo(copyOfWrappedSet);

    // test that setToTest is still valid
    assertThat(setToTest).isEqualTo(wrappedSet);
    assertThat(setToTest).isEqualTo(copyOfWrappedSet);

    // invalidate setToTest
    wrappedSet.remove(1);
    // sanity check on update of wrappedSet
    assertThat(wrappedSet).isNotEqualTo(copyOfWrappedSet);

    ImmutableSet<Integer> copyOfModifiedSet = ImmutableSet.copyOf(wrappedSet); // {2,3}
    // sanity check on construction of copyOfModifiedSet
    assertThat(wrappedSet).isEqualTo(copyOfModifiedSet);

    // setToTest should throw when it calls equals(), or equals is called on it, except for itself
    assertThat(setToTest).isEqualTo(setToTest);
    assertThrows(IllegalStateException.class, () -> setToTest.equals(wrappedSet));
    assertThrows(IllegalStateException.class, () -> setToTest.equals(copyOfWrappedSet));
    assertThrows(IllegalStateException.class, () -> setToTest.equals(copyOfModifiedSet));
    assertThrows(IllegalStateException.class, () -> wrappedSet.equals(setToTest));
    assertThrows(IllegalStateException.class, () -> copyOfWrappedSet.equals(setToTest));
    assertThrows(IllegalStateException.class, () -> copyOfModifiedSet.equals(setToTest));
  }
}
