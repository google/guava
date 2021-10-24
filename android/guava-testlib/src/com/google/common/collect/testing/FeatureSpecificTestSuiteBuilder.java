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

import static java.util.Collections.disjoint;
import static java.util.logging.Level.FINER;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.ConflictingRequirementsException;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.FeatureUtil;
import com.google.common.collect.testing.features.TesterRequirements;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests the object generated
 * by a G, selecting appropriate tests by matching them against specified features.
 *
 * @param <B> The concrete type of this builder (the 'self-type'). All the Builder methods of this
 *     class (such as {@link #named}) return this type, so that Builder methods of more derived
 *     classes can be chained onto them without casting.
 * @param <G> The type of the generator to be passed to testers in the generated test suite. An
 *     instance of G should somehow provide an instance of the class under test, plus any other
 *     information required to parameterize the test.
 * @author George van den Driessche
 */
@GwtIncompatible
public abstract class FeatureSpecificTestSuiteBuilder<
    B extends FeatureSpecificTestSuiteBuilder<B, G>, G> {
  @SuppressWarnings("unchecked")
  protected B self() {
    return (B) this;
  }

  // Test Data

  private G subjectGenerator;
  // Gets run before every test.
  private Runnable setUp;
  // Gets run at the conclusion of every test.
  private Runnable tearDown;

  protected B usingGenerator(G subjectGenerator) {
    this.subjectGenerator = subjectGenerator;
    return self();
  }

  public G getSubjectGenerator() {
    return subjectGenerator;
  }

  public B withSetUp(Runnable setUp) {
    this.setUp = setUp;
    return self();
  }

  public Runnable getSetUp() {
    return setUp;
  }

  public B withTearDown(Runnable tearDown) {
    this.tearDown = tearDown;
    return self();
  }

  public Runnable getTearDown() {
    return tearDown;
  }

  // Features

  private final Set<Feature<?>> features = new LinkedHashSet<>();

  /**
   * Configures this builder to produce tests appropriate for the given features. This method may be
   * called more than once to add features in multiple groups.
   */
  public B withFeatures(Feature<?>... features) {
    return withFeatures(Arrays.asList(features));
  }

  public B withFeatures(Iterable<? extends Feature<?>> features) {
    for (Feature<?> feature : features) {
      this.features.add(feature);
    }
    return self();
  }

  public Set<Feature<?>> getFeatures() {
    return Collections.unmodifiableSet(features);
  }

  // Name

  private String name;

  /** Configures this builder produce a TestSuite with the given name. */
  public B named(String name) {
    if (name.contains("(")) {
      throw new IllegalArgumentException(
          "Eclipse hides all characters after "
              + "'('; please use '[]' or other characters instead of parentheses");
    }
    this.name = name;
    return self();
  }

  public String getName() {
    return name;
  }

  // Test suppression

  private final Set<Method> suppressedTests = new HashSet<>();

  /**
   * Prevents the given methods from being run as part of the test suite.
   *
   * <p><em>Note:</em> in principle this should never need to be used, but it might be useful if the
   * semantics of an implementation disagree in unforeseen ways with the semantics expected by a
   * test, or to keep dependent builds clean in spite of an erroneous test.
   */
  public B suppressing(Method... methods) {
    return suppressing(Arrays.asList(methods));
  }

  public B suppressing(Collection<Method> methods) {
    suppressedTests.addAll(methods);
    return self();
  }

  public Set<Method> getSuppressedTests() {
    return suppressedTests;
  }

  private static final Logger logger =
      Logger.getLogger(FeatureSpecificTestSuiteBuilder.class.getName());

  /** Creates a runnable JUnit test suite based on the criteria already given. */
  /*
   * Class parameters must be raw. This annotation should go on testerClass in
   * the for loop, but the 1.5 javac crashes on annotations in for loops:
   * <http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6294589>
   */
  @SuppressWarnings("unchecked")
  public TestSuite createTestSuite() {
    checkCanCreate();

    logger.fine(" Testing: " + name);
    logger.fine("Features: " + formatFeatureSet(features));

    FeatureUtil.addImpliedFeatures(features);

    logger.fine("Expanded: " + formatFeatureSet(features));

    // Class parameters must be raw.
    List<Class<? extends AbstractTester>> testers = getTesters();

    TestSuite suite = new TestSuite(name);
    for (Class<? extends AbstractTester> testerClass : testers) {
      TestSuite testerSuite =
          makeSuiteForTesterClass((Class<? extends AbstractTester<?>>) testerClass);
      if (testerSuite.countTestCases() > 0) {
        suite.addTest(testerSuite);
      }
    }
    return suite;
  }

  /** Throw {@link IllegalStateException} if {@link #createTestSuite()} can't be called yet. */
  protected void checkCanCreate() {
    if (subjectGenerator == null) {
      throw new IllegalStateException("Call using() before createTestSuite().");
    }
    if (name == null) {
      throw new IllegalStateException("Call named() before createTestSuite().");
    }
    if (features == null) {
      throw new IllegalStateException("Call withFeatures() before createTestSuite().");
    }
  }

  // Class parameters must be raw.
  protected abstract List<Class<? extends AbstractTester>> getTesters();

  private boolean matches(Test test) {
    Method method;
    try {
      method = extractMethod(test);
    } catch (IllegalArgumentException e) {
      logger.finer(Platform.format("%s: including by default: %s", test, e.getMessage()));
      return true;
    }
    if (suppressedTests.contains(method)) {
      logger.finer(Platform.format("%s: excluding because it was explicitly suppressed.", test));
      return false;
    }
    TesterRequirements requirements;
    try {
      requirements = FeatureUtil.getTesterRequirements(method);
    } catch (ConflictingRequirementsException e) {
      throw new RuntimeException(e);
    }
    if (!features.containsAll(requirements.getPresentFeatures())) {
      if (logger.isLoggable(FINER)) {
        Set<Feature<?>> missingFeatures = Helpers.copyToSet(requirements.getPresentFeatures());
        missingFeatures.removeAll(features);
        logger.finer(
            Platform.format(
                "%s: skipping because these features are absent: %s", method, missingFeatures));
      }
      return false;
    }
    if (intersect(features, requirements.getAbsentFeatures())) {
      if (logger.isLoggable(FINER)) {
        Set<Feature<?>> unwantedFeatures = Helpers.copyToSet(requirements.getAbsentFeatures());
        unwantedFeatures.retainAll(features);
        logger.finer(
            Platform.format(
                "%s: skipping because these features are present: %s", method, unwantedFeatures));
      }
      return false;
    }
    return true;
  }

  private static boolean intersect(Set<?> a, Set<?> b) {
    return !disjoint(a, b);
  }

  private static Method extractMethod(Test test) {
    if (test instanceof AbstractTester) {
      AbstractTester<?> tester = (AbstractTester<?>) test;
      return Helpers.getMethod(tester.getClass(), tester.getTestMethodName());
    } else if (test instanceof TestCase) {
      TestCase testCase = (TestCase) test;
      return Helpers.getMethod(testCase.getClass(), testCase.getName());
    } else {
      throw new IllegalArgumentException("unable to extract method from test: not a TestCase.");
    }
  }

  protected TestSuite makeSuiteForTesterClass(Class<? extends AbstractTester<?>> testerClass) {
    TestSuite candidateTests = new TestSuite(testerClass);
    TestSuite suite = filterSuite(candidateTests);

    Enumeration<?> allTests = suite.tests();
    while (allTests.hasMoreElements()) {
      Object test = allTests.nextElement();
      if (test instanceof AbstractTester) {
        @SuppressWarnings("unchecked")
        AbstractTester<? super G> tester = (AbstractTester<? super G>) test;
        tester.init(subjectGenerator, name, setUp, tearDown);
      }
    }

    return suite;
  }

  private TestSuite filterSuite(TestSuite suite) {
    TestSuite filtered = new TestSuite(suite.getName());
    Enumeration<?> tests = suite.tests();
    while (tests.hasMoreElements()) {
      Test test = (Test) tests.nextElement();
      if (matches(test)) {
        filtered.addTest(test);
      }
    }
    return filtered;
  }

  protected static String formatFeatureSet(Set<? extends Feature<?>> features) {
    List<String> temp = new ArrayList<>();
    for (Feature<?> feature : features) {
      Object featureAsObject = feature; // to work around bogus JDK warning
      if (featureAsObject instanceof Enum) {
        Enum<?> f = (Enum<?>) featureAsObject;
        temp.add(f.getDeclaringClass().getSimpleName() + "." + feature);
      } else {
        temp.add(feature.toString());
      }
    }
    return temp.toString();
  }
}
