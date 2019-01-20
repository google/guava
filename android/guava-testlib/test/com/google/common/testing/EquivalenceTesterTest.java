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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Equivalence;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Tests for {@link EquivalenceTester}.
 *
 * @author Gregory Kick
 */
@GwtCompatible
public class EquivalenceTesterTest extends TestCase {
  private EquivalenceTester<Object> tester;
  private MockEquivalence equivalenceMock;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.equivalenceMock = new MockEquivalence();
    this.tester = EquivalenceTester.of(equivalenceMock);
  }

  /** Test null reference yields error */
  public void testOf_NullPointerException() {
    try {
      EquivalenceTester.of(null);
      fail("Should fail on null reference");
    } catch (NullPointerException expected) {
    }
  }

  public void testTest_NoData() {
    tester.test();
  }

  public void testTest() {
    Object group1Item1 = new TestObject(1, 1);
    Object group1Item2 = new TestObject(1, 2);
    Object group2Item1 = new TestObject(2, 1);
    Object group2Item2 = new TestObject(2, 2);

    equivalenceMock.expectEquivalent(group1Item1, group1Item2);
    equivalenceMock.expectDistinct(group1Item1, group2Item1);
    equivalenceMock.expectDistinct(group1Item1, group2Item2);
    equivalenceMock.expectEquivalent(group1Item2, group1Item1);
    equivalenceMock.expectDistinct(group1Item2, group2Item1);
    equivalenceMock.expectDistinct(group1Item2, group2Item2);
    equivalenceMock.expectDistinct(group2Item1, group1Item1);
    equivalenceMock.expectDistinct(group2Item1, group1Item2);
    equivalenceMock.expectEquivalent(group2Item1, group2Item2);
    equivalenceMock.expectDistinct(group2Item2, group1Item1);
    equivalenceMock.expectDistinct(group2Item2, group1Item2);
    equivalenceMock.expectEquivalent(group2Item2, group2Item1);

    equivalenceMock.expectHash(group1Item1, 1);
    equivalenceMock.expectHash(group1Item2, 1);
    equivalenceMock.expectHash(group2Item1, 2);
    equivalenceMock.expectHash(group2Item2, 2);

    equivalenceMock.replay();

    tester
        .addEquivalenceGroup(group1Item1, group1Item2)
        .addEquivalenceGroup(group2Item1, group2Item2)
        .test();
  }

  public void testTest_symmetric() {
    Object group1Item1 = new TestObject(1, 1);
    Object group1Item2 = new TestObject(1, 2);

    equivalenceMock.expectEquivalent(group1Item1, group1Item2);
    equivalenceMock.expectDistinct(group1Item2, group1Item1);

    equivalenceMock.expectHash(group1Item1, 1);
    equivalenceMock.expectHash(group1Item2, 1);

    equivalenceMock.replay();

    try {
      tester.addEquivalenceGroup(group1Item1, group1Item2).test();
    } catch (AssertionFailedError expected) {
      assertThat(expected.getMessage())
          .contains(
              "TestObject{group=1, item=2} [group 1, item 2] must be equivalent to "
                  + "TestObject{group=1, item=1} [group 1, item 1]");
      return;
    }
    fail();
  }

  public void testTest_trasitive() {
    Object group1Item1 = new TestObject(1, 1);
    Object group1Item2 = new TestObject(1, 2);
    Object group1Item3 = new TestObject(1, 3);

    equivalenceMock.expectEquivalent(group1Item1, group1Item2);
    equivalenceMock.expectEquivalent(group1Item1, group1Item3);
    equivalenceMock.expectEquivalent(group1Item2, group1Item1);
    equivalenceMock.expectDistinct(group1Item2, group1Item3);
    equivalenceMock.expectEquivalent(group1Item3, group1Item1);
    equivalenceMock.expectEquivalent(group1Item3, group1Item2);

    equivalenceMock.expectHash(group1Item1, 1);
    equivalenceMock.expectHash(group1Item2, 1);
    equivalenceMock.expectHash(group1Item3, 1);

    equivalenceMock.replay();

    try {
      tester.addEquivalenceGroup(group1Item1, group1Item2, group1Item3).test();
    } catch (AssertionFailedError expected) {
      assertThat(expected.getMessage())
          .contains(
              "TestObject{group=1, item=2} [group 1, item 2] must be equivalent to "
                  + "TestObject{group=1, item=3} [group 1, item 3]");
      return;
    }
    fail();
  }

  public void testTest_inequivalence() {
    Object group1Item1 = new TestObject(1, 1);
    Object group2Item1 = new TestObject(2, 1);

    equivalenceMock.expectEquivalent(group1Item1, group2Item1);
    equivalenceMock.expectDistinct(group2Item1, group1Item1);

    equivalenceMock.expectHash(group1Item1, 1);
    equivalenceMock.expectHash(group2Item1, 2);

    equivalenceMock.replay();

    try {
      tester.addEquivalenceGroup(group1Item1).addEquivalenceGroup(group2Item1).test();
    } catch (AssertionFailedError expected) {
      assertThat(expected.getMessage())
          .contains(
              "TestObject{group=1, item=1} [group 1, item 1] must not be equivalent to "
                  + "TestObject{group=2, item=1} [group 2, item 1]");
      return;
    }
    fail();
  }

  public void testTest_hash() {
    Object group1Item1 = new TestObject(1, 1);
    Object group1Item2 = new TestObject(1, 2);

    equivalenceMock.expectEquivalent(group1Item1, group1Item2);
    equivalenceMock.expectEquivalent(group1Item2, group1Item1);

    equivalenceMock.expectHash(group1Item1, 1);
    equivalenceMock.expectHash(group1Item2, 2);

    equivalenceMock.replay();

    try {
      tester.addEquivalenceGroup(group1Item1, group1Item2).test();
    } catch (AssertionFailedError expected) {
      String expectedMessage =
          "the hash (1) of TestObject{group=1, item=1} [group 1, item 1] must be "
              + "equal to the hash (2) of TestObject{group=1, item=2} [group 1, item 2]";
      if (!expected.getMessage().contains(expectedMessage)) {
        fail("<" + expected.getMessage() + "> expected to contain <" + expectedMessage + ">");
      }
      return;
    }
    fail();
  }

  /** An object with a friendly {@link #toString()}. */
  private static final class TestObject {
    final int group;
    final int item;

    TestObject(int group, int item) {
      this.group = group;
      this.item = item;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper("TestObject")
          .add("group", group)
          .add("item", item)
          .toString();
    }
  }

  private static final class MockEquivalence extends Equivalence<Object> {
    final ImmutableTable.Builder<Object, Object, Boolean> equivalentExpectationsBuilder =
        ImmutableTable.builder();
    final ImmutableMap.Builder<Object, Integer> hashExpectationsBuilder = ImmutableMap.builder();
    ImmutableTable<Object, Object, Boolean> equivalentExpectations;
    ImmutableMap<Object, Integer> hashExpectations;

    void expectEquivalent(Object a, Object b) {
      checkRecording();
      equivalentExpectationsBuilder.put(a, b, true);
    }

    void expectDistinct(Object a, Object b) {
      checkRecording();
      equivalentExpectationsBuilder.put(a, b, false);
    }

    void expectHash(Object object, int hash) {
      checkRecording();
      hashExpectationsBuilder.put(object, hash);
    }

    void replay() {
      checkRecording();
      equivalentExpectations = equivalentExpectationsBuilder.build();
      hashExpectations = hashExpectationsBuilder.build();
    }

    @Override
    protected boolean doEquivalent(Object a, Object b) {
      return equivalentExpectations.get(a, b);
    }

    @Override
    protected int doHash(Object object) {
      return hashExpectations.get(object);
    }

    void checkRecording() {
      checkState(equivalentExpectations == null && hashExpectations == null);
    }
  }
}
