/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

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
