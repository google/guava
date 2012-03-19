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

package com.google.common.collect;

import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionFeature.NON_STANDARD_TOSTRING;
import static com.google.common.collect.testing.features.CollectionFeature.RESTRICTS_ELEMENTS;
import static com.google.common.collect.testing.testers.NavigableSetNavigationTester.getHoleMethods;

import com.google.common.collect.testing.NavigableSetTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.SetGenerators.ContiguousSetDescendingGenerator;
import com.google.common.collect.testing.google.SetGenerators.ContiguousSetGenerator;
import com.google.common.collect.testing.google.SetGenerators.ContiguousSetHeadsetGenerator;
import com.google.common.collect.testing.google.SetGenerators.ContiguousSetSubsetGenerator;
import com.google.common.collect.testing.google.SetGenerators.ContiguousSetTailsetGenerator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Gregory Kick
 */
public class ContiguousSetNonGwtTest extends TestCase {
  public static class BuiltTests extends TestCase {
    public static Test suite() {
      TestSuite suite = new TestSuite();

      suite.addTest(NavigableSetTestSuiteBuilder.using(
          new ContiguousSetGenerator())
          .named("Range.asSet")
          .withFeatures(CollectionSize.ANY, KNOWN_ORDER, ALLOWS_NULL_QUERIES,
              NON_STANDARD_TOSTRING, RESTRICTS_ELEMENTS)
          .suppressing(getHoleMethods())
          .createTestSuite());

      suite.addTest(NavigableSetTestSuiteBuilder.using(
          new ContiguousSetHeadsetGenerator())
          .named("Range.asSet, headset")
          .withFeatures(CollectionSize.ANY, KNOWN_ORDER, ALLOWS_NULL_QUERIES,
              NON_STANDARD_TOSTRING, RESTRICTS_ELEMENTS)
          .suppressing(getHoleMethods())
          .createTestSuite());

      suite.addTest(NavigableSetTestSuiteBuilder.using(
          new ContiguousSetTailsetGenerator())
          .named("Range.asSet, tailset")
          .withFeatures(CollectionSize.ANY, KNOWN_ORDER, ALLOWS_NULL_QUERIES,
              NON_STANDARD_TOSTRING, RESTRICTS_ELEMENTS)
          .suppressing(getHoleMethods())
          .createTestSuite());

      suite.addTest(NavigableSetTestSuiteBuilder.using(
          new ContiguousSetSubsetGenerator())
          .named("Range.asSet, subset")
          .withFeatures(CollectionSize.ANY, KNOWN_ORDER, ALLOWS_NULL_QUERIES,
              NON_STANDARD_TOSTRING, RESTRICTS_ELEMENTS)
          .suppressing(getHoleMethods())
          .createTestSuite());

      suite.addTest(NavigableSetTestSuiteBuilder.using(
          new ContiguousSetDescendingGenerator())
          .named("Range.asSet.descendingSet")
          .withFeatures(CollectionSize.ANY, KNOWN_ORDER, ALLOWS_NULL_QUERIES,
              NON_STANDARD_TOSTRING, RESTRICTS_ELEMENTS)
          .suppressing(getHoleMethods())
          .createTestSuite());

      return suite;
    }
  }

  public void testNothing() {
    /*
     * It's a warning if a TestCase subclass contains no tests, so we add one.
     * Alternatively, we could stop extending TestCase, but I worry that someone
     * will add a test in the future and not realize that it's being ignored.
     */
  }
}
