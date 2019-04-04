/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.FeatureUtil;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import junit.framework.TestSuite;

/**
 * This builder creates a composite test suite, containing a separate test suite for each {@link
 * CollectionSize} present in the features specified by {@link #withFeatures(Feature...)}.
 *
 * @param <B> The concrete type of this builder (the 'self-type'). All the Builder methods of this
 *     class (such as {@link #named(String)}) return this type, so that Builder methods of more
 *     derived classes can be chained onto them without casting.
 * @param <G> The type of the generator to be passed to testers in the generated test suite. An
 *     instance of G should somehow provide an instance of the class under test, plus any other
 *     information required to parameterize the test.
 * @see FeatureSpecificTestSuiteBuilder
 * @author George van den Driessche
 */
@GwtIncompatible
public abstract class PerCollectionSizeTestSuiteBuilder<
        B extends PerCollectionSizeTestSuiteBuilder<B, G, T, E>,
        G extends TestContainerGenerator<T, E>,
        T,
        E>
    extends FeatureSpecificTestSuiteBuilder<B, G> {
  private static final Logger logger =
      Logger.getLogger(PerCollectionSizeTestSuiteBuilder.class.getName());

  /** Creates a runnable JUnit test suite based on the criteria already given. */
  @Override
  public TestSuite createTestSuite() {
    checkCanCreate();

    String name = getName();
    // Copy this set, so we can modify it.
    Set<Feature<?>> features = Helpers.copyToSet(getFeatures());
    List<Class<? extends AbstractTester>> testers = getTesters();

    logger.finest(" Testing: " + name);

    // Split out all the specified sizes.
    Set<Feature<?>> sizesToTest = Helpers.<Feature<?>>copyToSet(CollectionSize.values());
    sizesToTest.retainAll(features);
    features.removeAll(sizesToTest);

    FeatureUtil.addImpliedFeatures(sizesToTest);
    sizesToTest.retainAll(
        Arrays.asList(CollectionSize.ZERO, CollectionSize.ONE, CollectionSize.SEVERAL));

    logger.finest("   Sizes: " + formatFeatureSet(sizesToTest));

    if (sizesToTest.isEmpty()) {
      throw new IllegalStateException(
          name
              + ": no CollectionSizes specified (check the argument to "
              + "FeatureSpecificTestSuiteBuilder.withFeatures().)");
    }

    TestSuite suite = new TestSuite(name);
    for (Feature<?> collectionSize : sizesToTest) {
      String oneSizeName =
          Platform.format(
              "%s [collection size: %s]", name, collectionSize.toString().toLowerCase());
      OneSizeGenerator<T, E> oneSizeGenerator =
          new OneSizeGenerator<>(getSubjectGenerator(), (CollectionSize) collectionSize);
      Set<Feature<?>> oneSizeFeatures = Helpers.copyToSet(features);
      oneSizeFeatures.add(collectionSize);
      Set<Method> oneSizeSuppressedTests = getSuppressedTests();

      OneSizeTestSuiteBuilder<T, E> oneSizeBuilder =
          new OneSizeTestSuiteBuilder<T, E>(testers)
              .named(oneSizeName)
              .usingGenerator(oneSizeGenerator)
              .withFeatures(oneSizeFeatures)
              .withSetUp(getSetUp())
              .withTearDown(getTearDown())
              .suppressing(oneSizeSuppressedTests);
      TestSuite oneSizeSuite = oneSizeBuilder.createTestSuite();
      suite.addTest(oneSizeSuite);

      for (TestSuite derivedSuite : createDerivedSuites(oneSizeBuilder)) {
        oneSizeSuite.addTest(derivedSuite);
      }
    }
    return suite;
  }

  protected List<TestSuite> createDerivedSuites(
      FeatureSpecificTestSuiteBuilder<?, ? extends OneSizeTestContainerGenerator<T, E>>
          parentBuilder) {
    return new ArrayList<>();
  }

  /** Builds a test suite for one particular {@link CollectionSize}. */
  private static final class OneSizeTestSuiteBuilder<T, E>
      extends FeatureSpecificTestSuiteBuilder<
          OneSizeTestSuiteBuilder<T, E>, OneSizeGenerator<T, E>> {
    private final List<Class<? extends AbstractTester>> testers;

    public OneSizeTestSuiteBuilder(List<Class<? extends AbstractTester>> testers) {
      this.testers = testers;
    }

    @Override
    protected List<Class<? extends AbstractTester>> getTesters() {
      return testers;
    }
  }
}
