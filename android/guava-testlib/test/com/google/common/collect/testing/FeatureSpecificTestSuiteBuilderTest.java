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

/**
 * @author Max Ross
 */
public class FeatureSpecificTestSuiteBuilderTest extends TestCase {
  private static final class MyTestSuiteBuilder
      extends FeatureSpecificTestSuiteBuilder<MyTestSuiteBuilder, String> {
    @Override
    protected List<Class<? extends AbstractTester>> getTesters() {
      return Collections.<Class<? extends AbstractTester>>singletonList(MyTester.class);
    }
  }

  public void testLifecycle() {
    boolean[] setUp = {false};
    Runnable setUpRunnable =
        new Runnable() {
          @Override
          public void run() {
            setUp[0] = true;
          }
        };

    boolean[] tearDown = {false};
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
    int timesMyTesterWasRunBeforeSuite = MyTester.timesTestClassWasRun;
    test.run(result);
    assertEquals(timesMyTesterWasRunBeforeSuite + 1, MyTester.timesTestClassWasRun);
    assertTrue(setUp[0]);
    assertTrue(tearDown[0]);
  }
}
