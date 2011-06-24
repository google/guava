/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.testing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.testing.GuavaAsserts.assertEquals;
import static com.google.common.testing.GuavaAsserts.assertTrue;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

/**
 * Tester for equals() and hashCode() methods of a class.
 *
 * <p>To use, create a new EqualsTester and add equality groups where each group
 * contains objects that are supposed to be equal to each other, and objects of
 * different groups are expected to be unequal. For example:
 * <pre>
 * new EqualsTester()
 *     .addEqualityGroup("hello", "h" + "ello")
 *     .addEqualityGroup("world", "wor" + "ld")
 *     .addEqualityGroup(2, 1 + 1)
 *     .testEquals();
 * </pre>
 * This tests:
 * <ul>
 * <li>comparing each object against itself returns true
 * <li>comparing each object against null returns false
 * <li>comparing each object an instance of an incompatible class returns false
 * <li>comparing each pair of objects within the same equality group returns
 *     true
 * <li>comparing each pair of objects from different equality groups returns
 *     false
 * <li>the hash code of any two equal objects are equal
 * </ul>
 * For backward compatibility, the following usage pattern is also supported:
 * <ol>
 * <li>Create a reference instance of the class under test and use to create a
 * new EqualsTester.
 *
 * <li>Create one or more new instances of the class that should be equal to the
 * reference instance and pass to addEqualObject(). Multiple instances can be
 * used to test subclasses.
 *
 * <li>Create one or more new instances that should not be equal to the
 * reference instance and pass to addNotEqualObject. For complete testing,
 * you should add one instance that varies in each aspect checked by equals().
 *
 * <li>Invoke {@link #testEquals} on the EqualsTester.
 * </ol>
 *
 * @author Jim McMaster
 * @author Jige Yu
 * @since Guava release 10
 */
@Beta
@GwtCompatible
public final class EqualsTester {
  private final List<Object> defaultEqualObjects = Lists.newArrayList();
  private final List<Object> defaultNotEqualObjects = Lists.newArrayList();
  private final List<List<Object>> equalityGroups = Lists.newArrayList();

  /**
   * Constructs an empty EqualsTester instance
   */
  public EqualsTester() {
    equalityGroups.add(defaultEqualObjects);
  }

  /**
   * Constructs a new EqualsTester for a given reference object
   *
   * @param reference reference object for comparison
   */
  public EqualsTester(Object reference) {
    this();
    checkNotNull(reference, "Reference object cannot be null");
    defaultEqualObjects.add(reference);
  }

  /**
   * Adds {@code equalityGroup} with objects that are supposed to be equal to
   * each other and not equal to any other equality groups added to this tester.
   */
  public EqualsTester addEqualityGroup(Object... equalityGroup) {
    checkNotNull(equalityGroup);
    equalityGroups.add(ImmutableList.copyOf(equalityGroup));
    return this;
  }

  /**
   * Add one or more objects that should be equal to the reference object
   */
  public EqualsTester addEqualObject(Object... equalObjects) {
    checkNotNull(equalObjects);
    Collections.addAll(defaultEqualObjects, equalObjects);
    return this;
  }

  /**
   * Add one or more objects that should not be equal to the reference object.
   */
  public EqualsTester addNotEqualObject(Object... notEqualObjects) {
    checkNotNull(notEqualObjects);
    Collections.addAll(defaultNotEqualObjects, notEqualObjects);
    return this;
  }

  /**
   * Run tests on equals method, throwing a failure on an invalid test
   */
  public EqualsTester testEquals() {
    assertEquality();
    assertInequality();
    return this;
  }

  private void assertEquality() {
    // Objects in defaultNotEqualObjects don't have to be equal to each other
    // for backward compatibility
    for (Iterable<Object> group : equalityGroups) {
      for (Object reference : group) {
        assertTrue(reference != null);
        assertTrue(reference + " is expected to be equal to itself",
            reference.equals(reference));
        assertTrue(!reference.equals(NotAnInstance.SINGLETON));
        for (Object right : group) {
          if (reference != right) {
            assertEquals(reference + " is expected to be equal to " + right,
                reference, right);
            assertEquals(
                reference + " hash code is expected to be equal to "
                + right + " hash code",
                reference.hashCode(), right.hashCode());
          }
        }
      }
    }
  }

  private void assertInequality() {
    // defaultNotEqualObjects should participate in inequality test with other
    // equality groups.
    Iterable<List<Object>> inequalityGroups = Iterables.concat(
        equalityGroups, Collections.singletonList(defaultNotEqualObjects));
    for (Iterable<Object> group : inequalityGroups) {
      for (Iterable<Object> anotherGroup : inequalityGroups) {
        // compare every two equality groups
        if (group == anotherGroup) {
          // same group, ignore
          continue;
        }
        for (Object left : group) {
          for (Object right : anotherGroup) {
            // No two objects from different equality group can be equal
            assertTrue("Should not be equal: <" + left +"> and <" + right + ">",
                !left.equals(right));
          }
        }
      }
    }
  }

  /**
   * Class used to test whether equals() correctly handles an instance
   * of an incompatible class.  Since it is a private inner class, the
   * invoker can never pass in an instance to the tester
   */
  private static final class NotAnInstance {

    static final NotAnInstance SINGLETON = new NotAnInstance();

    @Override public String toString() {
      return "equal_to_nothing";
    }
  }
}
