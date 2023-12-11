/*
 * Copyright (C) 2010 The Guava Authors
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/** @author Luiz-Otavio "Z" Zorzella */
@GwtCompatible
public class TearDownStackTest extends TestCase {

  private TearDownStack tearDownStack = new TearDownStack();

  public void testSingleTearDown() throws Exception {
    final TearDownStack stack = buildTearDownStack();

    final SimpleTearDown tearDown = new SimpleTearDown();
    stack.addTearDown(tearDown);

    assertEquals(false, tearDown.ran);

    stack.runTearDown();

    assertEquals("tearDown should have run", true, tearDown.ran);
  }

  public void testMultipleTearDownsHappenInOrder() throws Exception {
    final TearDownStack stack = buildTearDownStack();

    final SimpleTearDown tearDownOne = new SimpleTearDown();
    stack.addTearDown(tearDownOne);

    final Callback callback =
        new Callback() {
          @Override
          public void run() {
            assertEquals(
                "tearDownTwo should have been run before tearDownOne", false, tearDownOne.ran);
          }
        };

    final SimpleTearDown tearDownTwo = new SimpleTearDown(callback);
    stack.addTearDown(tearDownTwo);

    assertEquals(false, tearDownOne.ran);
    assertEquals(false, tearDownTwo.ran);

    stack.runTearDown();

    assertEquals("tearDownOne should have run", true, tearDownOne.ran);
    assertEquals("tearDownTwo should have run", true, tearDownTwo.ran);
  }

  public void testThrowingTearDown() throws Exception {
    final TearDownStack stack = buildTearDownStack();

    final ThrowingTearDown tearDownOne = new ThrowingTearDown("one");
    stack.addTearDown(tearDownOne);

    final ThrowingTearDown tearDownTwo = new ThrowingTearDown("two");
    stack.addTearDown(tearDownTwo);

    assertEquals(false, tearDownOne.ran);
    assertEquals(false, tearDownTwo.ran);

    try {
      stack.runTearDown();
      fail("runTearDown should have thrown an exception");
    } catch (ClusterException expected) {
      assertThat(expected).hasCauseThat().hasMessageThat().isEqualTo("two");
    } catch (RuntimeException e) {
      throw new RuntimeException(
          "A ClusterException should have been thrown, rather than a " + e.getClass().getName(), e);
    }

    assertEquals(true, tearDownOne.ran);
    assertEquals(true, tearDownTwo.ran);
  }

  @Override
  public final void runBare() throws Throwable {
    try {
      setUp();
      runTest();
    } finally {
      tearDown();
    }
  }

  @Override
  protected void tearDown() {
    tearDownStack.runTearDown();
  }

  /** Builds a {@link TearDownStack} that makes sure it's clear by the end of this test. */
  private TearDownStack buildTearDownStack() {
    final TearDownStack result = new TearDownStack();
    tearDownStack.addTearDown(
        new TearDown() {

          @Override
          public void tearDown() throws Exception {
            synchronized (result.stack) {
              assertEquals(
                  "The test should have cleared the stack (say, by virtue of running runTearDown)",
                  0,
                  result.stack.size());
            }
          }
        });
    return result;
  }

  private static final class ThrowingTearDown implements TearDown {

    private final String id;
    boolean ran = false;

    ThrowingTearDown(String id) {
      this.id = id;
    }

    @Override
    public void tearDown() throws Exception {
      ran = true;
      throw new RuntimeException(id);
    }
  }

  private static final class SimpleTearDown implements TearDown {

    boolean ran = false;
    @Nullable Callback callback = null;

    public SimpleTearDown() {}

    public SimpleTearDown(Callback callback) {
      this.callback = callback;
    }

    @Override
    public void tearDown() throws Exception {
      if (callback != null) {
        callback.run();
      }
      ran = true;
    }
  }

  private interface Callback {
    void run();
  }
}
