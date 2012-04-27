// Copyright 2011 Google. All Rights Reserved.

package com.google.common.collect.testing;

import com.google.common.collect.testing.features.CollectionFeature;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;

import java.util.Collections;
import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class FeatureSpecificTestSuiteBuilderTest extends TestCase {

  static boolean testWasRun;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    testWasRun = false;
  }

  public static final class MyAbstractTester extends AbstractTester {
    public void testNothing() {
      testWasRun = true;
    }
  }

  private static final class MyTestSuiteBuilder extends
      FeatureSpecificTestSuiteBuilder<MyTestSuiteBuilder, String> {

    @Override
    protected List<Class<? extends AbstractTester>> getTesters() {
      return Collections.<Class<? extends AbstractTester>>singletonList(MyAbstractTester.class);
    }
  }

  public void testLifecycle() {
    final boolean setUp[] = {false};
    Runnable setUpRunnable = new Runnable() {
      @Override
      public void run() {
        setUp[0] = true;
      }
    };

    final boolean tearDown[] = {false};
    Runnable tearDownRunnable = new Runnable() {
      @Override
      public void run() {
        tearDown[0] = true;
      }
    };

    MyTestSuiteBuilder builder = new MyTestSuiteBuilder();
    Test test = builder.usingGenerator("yam").named("yam")
        .withFeatures(CollectionFeature.NONE).withSetUp(setUpRunnable)
        .withTearDown(tearDownRunnable).createTestSuite();
    TestResult result = new TestResult();
    test.run(result);
    assertTrue(testWasRun);
    assertTrue(setUp[0]);
    assertTrue(tearDown[0]);
  }
}
