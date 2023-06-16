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
import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import junit.framework.AssertionFailedError;

/**
 * Implementation helper for {@link EqualsTester} and {@link EquivalenceTester} that tests for
 * equivalence classes.
 *
 * @author Gregory Kick
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
final class RelationshipTester<T> {

  static class ItemReporter {
    String reportItem(Item<?> item) {
      return item.toString();
    }
  }

  /**
   * A word about using {@link Equivalence}, which automatically checks for {@code null} and
   * identical inputs: This sounds like it ought to be a problem here, since the goals of this class
   * include testing that {@code equals()} is reflexive and is tolerant of {@code null}. However,
   * there's no problem. The reason: {@link EqualsTester} tests {@code null} and identical inputs
   * directly against {@code equals()} rather than through the {@code Equivalence}.
   */
  private final Equivalence<? super T> equivalence;

  private final String relationshipName;
  private final String hashName;
  private final ItemReporter itemReporter;
  private final List<ImmutableList<T>> groups = Lists.newArrayList();

  RelationshipTester(
      Equivalence<? super T> equivalence,
      String relationshipName,
      String hashName,
      ItemReporter itemReporter) {
    this.equivalence = checkNotNull(equivalence);
    this.relationshipName = checkNotNull(relationshipName);
    this.hashName = checkNotNull(hashName);
    this.itemReporter = checkNotNull(itemReporter);
  }

  // TODO(cpovirk): should we reject null items, since the tests already check null automatically?
  @CanIgnoreReturnValue
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
        for (int unrelatedGroupNumber = 0;
            unrelatedGroupNumber < groups.size();
            unrelatedGroupNumber++) {
          if (groupNumber != unrelatedGroupNumber) {
            ImmutableList<T> unrelatedGroup = groups.get(unrelatedGroupNumber);
            for (int unrelatedItemNumber = 0;
                unrelatedItemNumber < unrelatedGroup.size();
                unrelatedItemNumber++) {
              assertUnrelated(groupNumber, itemNumber, unrelatedGroupNumber, unrelatedItemNumber);
            }
          }
        }
      }
    }
  }

  private void assertRelated(int groupNumber, int itemNumber, int relatedItemNumber) {
    Item<T> itemInfo = getItem(groupNumber, itemNumber);
    Item<T> relatedInfo = getItem(groupNumber, relatedItemNumber);

    T item = itemInfo.value;
    T related = relatedInfo.value;
    assertWithTemplate(
        "$ITEM must be $RELATIONSHIP to $OTHER",
        itemInfo,
        relatedInfo,
        equivalence.equivalent(item, related));

    int itemHash = equivalence.hash(item);
    int relatedHash = equivalence.hash(related);
    assertWithTemplate(
        "the $HASH ("
            + itemHash
            + ") of $ITEM must be equal to the $HASH ("
            + relatedHash
            + ") of $OTHER",
        itemInfo,
        relatedInfo,
        itemHash == relatedHash);
  }

  private void assertUnrelated(
      int groupNumber, int itemNumber, int unrelatedGroupNumber, int unrelatedItemNumber) {
    Item<T> itemInfo = getItem(groupNumber, itemNumber);
    Item<T> unrelatedInfo = getItem(unrelatedGroupNumber, unrelatedItemNumber);

    assertWithTemplate(
        "$ITEM must not be $RELATIONSHIP to $OTHER",
        itemInfo,
        unrelatedInfo,
        !equivalence.equivalent(itemInfo.value, unrelatedInfo.value));
  }

  private void assertWithTemplate(String template, Item<T> item, Item<T> other, boolean condition) {
    if (!condition) {
      throw new AssertionFailedError(
          template
              .replace("$RELATIONSHIP", relationshipName)
              .replace("$HASH", hashName)
              .replace("$ITEM", itemReporter.reportItem(item))
              .replace("$OTHER", itemReporter.reportItem(other)));
    }
  }

  private Item<T> getItem(int groupNumber, int itemNumber) {
    return new Item<>(groups.get(groupNumber).get(itemNumber), groupNumber, itemNumber);
  }

  static final class Item<T> {
    final T value;
    final int groupNumber;
    final int itemNumber;

    Item(T value, int groupNumber, int itemNumber) {
      this.value = value;
      this.groupNumber = groupNumber;
      this.itemNumber = itemNumber;
    }

    @Override
    public String toString() {
      return value + " [group " + (groupNumber + 1) + ", item " + (itemNumber + 1) + ']';
    }
  }
}
