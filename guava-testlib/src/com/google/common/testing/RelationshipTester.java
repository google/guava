/*
 * Copyright (C) 2011 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import junit.framework.AssertionFailedError;

import java.util.List;

/**
 * Tests a collection of objects according to the rules specified in a
 * {@link RelationshipAssertion}.
 *
 * @author Gregory Kick
 */
@GwtCompatible
final class RelationshipTester<T> {
  private final List<ImmutableList<T>> groups = Lists.newArrayList();
  private final RelationshipAssertion<T> assertion;

  RelationshipTester(RelationshipAssertion<T> assertion) {
    this.assertion = checkNotNull(assertion);
  }

  public RelationshipTester<T> addRelatedGroup(Iterable<? extends T> group) {
    groups.add(ImmutableList.copyOf(group));
    return this;
  }

  public void test() {
    for (int groupNumber = 0; groupNumber < groups.size(); groupNumber++) {
      ImmutableList<T> group = groups.get(groupNumber);
      for (int itemNumber = 0; itemNumber < group.size(); itemNumber++) {
        // check related items in same group
        for (int relatedItemNumber = 0; relatedItemNumber < group.size(); relatedItemNumber++) {
          if (itemNumber != relatedItemNumber) {
            assertRelated(groupNumber, itemNumber, relatedItemNumber);
          }
        }
        // check unrelated items in all other groups
        for (int unrelatedGroupNumber = 0; unrelatedGroupNumber < groups.size();
            unrelatedGroupNumber++) {
          if (groupNumber != unrelatedGroupNumber) {
            ImmutableList<T> unrelatedGroup = groups.get(unrelatedGroupNumber);
            for (int unrelatedItemNumber = 0; unrelatedItemNumber < unrelatedGroup.size();
                unrelatedItemNumber++) {
              assertUnrelated(groupNumber, itemNumber, unrelatedGroupNumber, unrelatedItemNumber);
            }
          }
        }
      }
    }
  }

  private void assertRelated(int groupNumber, int itemNumber, int relatedItemNumber) {
    ImmutableList<T> group = groups.get(groupNumber);
    T item = group.get(itemNumber);
    T related = group.get(relatedItemNumber);
    try {
      assertion.assertRelated(item, related);
    } catch (AssertionFailedError e) {
      // TODO(gak): special handling for ComparisonFailure?
      throw new AssertionFailedError(e.getMessage()
          .replace("$ITEM", itemString(item, groupNumber, itemNumber))
          .replace("$RELATED", itemString(related, groupNumber, relatedItemNumber)));
    }
  }

  private void assertUnrelated(int groupNumber, int itemNumber, int unrelatedGroupNumber,
      int unrelatedItemNumber) {
    T item = groups.get(groupNumber).get(itemNumber);
    T unrelated = groups.get(unrelatedGroupNumber).get(unrelatedItemNumber);
    try {
      assertion.assertUnrelated(item, unrelated);
    } catch (AssertionFailedError e) {
      // TODO(gak): special handling for ComparisonFailure?
      throw new AssertionFailedError(e.getMessage()
          .replace("$ITEM", itemString(item, groupNumber, itemNumber))
          .replace("$UNRELATED", itemString(unrelated, unrelatedGroupNumber, unrelatedItemNumber)));
    }
  }

  private static String itemString(Object item, int groupNumber, int itemNumber) {
    return new StringBuilder()
        .append(item)
        .append(" [group ")
        .append(groupNumber + 1)
        .append(", item ")
        .append(itemNumber + 1)
        .append(']')
        .toString();
  }

  /**
   * A strategy for testing the relationship between objects.  Methods are expected to throw
   * {@link AssertionFailedError} whenever the relationship is violated.
   *
   * <p>As a convenience, any occurrence of {@code $ITEM}, {@code $RELATED} or {@code $UNRELATED} in
   * the error message will be replaced with a string that combines the {@link Object#toString()},
   * item number and group number of the respective item.
   *
   */
  interface RelationshipAssertion<T> {
    void assertRelated(T item, T related);

    void assertUnrelated(T item, T unrelated);
  }
}
