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

package com.google.common.collect.testing;

import com.google.common.collect.testing.features.CollectionFeature;
import java.util.Collections;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import org.junit.Ignore;

/** @author Max Ross */
public class FeatureSpecificTestSuiteBuilderTest extends TestCase {

  static boolean testWasRun;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    testWasRun = false;
  }

  @Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
  public static final class MyAbstractTester extends AbstractTester<Void> {
    public void testNothing() {
      testWasRun = true;
    }
  }

  private static final class MyTestSuiteBuilder
      extends FeatureSpecificTestSuiteBuilder<MyTestSuiteBuilder, String> {

    @Override
    protected List<Class<? extends AbstractTester>> getTesters() {
      return Collections.<Class<? extends AbstractTester>>singletonList(MyAbstractTester.class);
    }
  }

  public void testLifecycle() {
    final boolean setUp[] = {false};
    Runnable setUpRunnable =
        new Runnable() {
          @Override
          public void run() {
            setUp[0] = true;
          }
        };

    final boolean tearDown[] = {false};
    Runnable tearDownRunnable =
        new Runnable() {
          @Override
          public void run() {
            tearDown[0] = true;
          }
        };

    MyTestSuiteBuilder builder = new MyTestSuiteBuilder();
    Test test =
        builder
            .usingGenerator("yam")
            .named("yam")
            .withFeatures(CollectionFeature.NONE)
            .withSetUp(setUpRunnable)
            .withTearDown(tearDownRunnable)
            .createTestSuite();
    TestResult result = new TestResult();
    test.run(result);
    assertTrue(testWasRun);
    assertTrue(setUp[0]);
    assertTrue(tearDown[0]);
  }
}
