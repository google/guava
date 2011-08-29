// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.collect;

import static com.google.common.collect.BstTesting.countAggregate;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.BstTesting.SimpleNode;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for the policies exported by {@link BstCountBasedBalancePolicies}
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class BstCountBasedBalancePoliciesTest extends TestCase {
  public static class NoRebalanceTest extends AbstractBstBalancePolicyTest {
    @Override
    protected BstBalancePolicy<SimpleNode> getBalancePolicy() {
      return BstCountBasedBalancePolicies.noRebalancePolicy(countAggregate);
    }
  }

  public static class SingleRebalanceTest extends AbstractBstBalancePolicyTest {
    @Override
    protected BstBalancePolicy<SimpleNode> getBalancePolicy() {
      return BstCountBasedBalancePolicies.<Character, SimpleNode>singleRebalancePolicy(
          countAggregate);
    }
  }

  public static class FullRebalanceTest extends AbstractBstBalancePolicyTest {
    @Override
    protected BstBalancePolicy<SimpleNode> getBalancePolicy() {
      return BstCountBasedBalancePolicies.<Character, SimpleNode>fullRebalancePolicy(
          countAggregate);
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(NoRebalanceTest.class);
    suite.addTestSuite(SingleRebalanceTest.class);
    suite.addTestSuite(FullRebalanceTest.class);
    return suite;
  }
}
