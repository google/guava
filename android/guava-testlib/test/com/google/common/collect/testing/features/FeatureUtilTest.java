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

package com.google.common.collect.testing.features;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import junit.framework.TestCase;

/** @author George van den Driessche */
// Enum values use constructors with generic varargs.
@SuppressWarnings("unchecked")
public class FeatureUtilTest extends TestCase {
  interface ExampleBaseInterface {
    void behave();
  }

  interface ExampleDerivedInterface extends ExampleBaseInterface {
    void misbehave();
  }

  enum ExampleBaseFeature implements Feature<ExampleBaseInterface> {
    BASE_FEATURE_1,
    BASE_FEATURE_2;

    @Override
    public Set<Feature<? super ExampleBaseInterface>> getImpliedFeatures() {
      return Collections.emptySet();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @TesterAnnotation
    @interface Require {
      ExampleBaseFeature[] value() default {};

      ExampleBaseFeature[] absent() default {};
    }
  }

  enum ExampleDerivedFeature implements Feature<ExampleDerivedInterface> {
    DERIVED_FEATURE_1,
    DERIVED_FEATURE_2(ExampleBaseFeature.BASE_FEATURE_1),
    DERIVED_FEATURE_3,

    COMPOUND_DERIVED_FEATURE(
        DERIVED_FEATURE_1, DERIVED_FEATURE_2, ExampleBaseFeature.BASE_FEATURE_2);

    private Set<Feature<? super ExampleDerivedInterface>> implied;

    ExampleDerivedFeature(Feature<? super ExampleDerivedInterface>... implied) {
      this.implied = ImmutableSet.copyOf(implied);
    }

    @Override
    public Set<Feature<? super ExampleDerivedInterface>> getImpliedFeatures() {
      return implied;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @TesterAnnotation
    @interface Require {
      ExampleDerivedFeature[] value() default {};

      ExampleDerivedFeature[] absent() default {};
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface NonTesterAnnotation {}

  @ExampleBaseFeature.Require({ExampleBaseFeature.BASE_FEATURE_1})
  private abstract static class ExampleBaseInterfaceTester extends TestCase {
    protected final void doNotActuallyRunThis() {
      fail("Nobody's meant to actually run this!");
    }
  }

  @AndroidIncompatible // Android attempts to run directly
  @NonTesterAnnotation
  @ExampleDerivedFeature.Require({ExampleDerivedFeature.DERIVED_FEATURE_2})
  private static class ExampleDerivedInterfaceTester extends ExampleBaseInterfaceTester {
    // Exists to test that our framework doesn't run it:
    @SuppressWarnings("unused")
    @ExampleDerivedFeature.Require({
      ExampleDerivedFeature.DERIVED_FEATURE_1,
      ExampleDerivedFeature.DERIVED_FEATURE_2
    })
    public void testRequiringTwoExplicitDerivedFeatures() throws Exception {
      doNotActuallyRunThis();
    }

    // Exists to test that our framework doesn't run it:
    @SuppressWarnings("unused")
    @ExampleDerivedFeature.Require({
      ExampleDerivedFeature.DERIVED_FEATURE_1,
      ExampleDerivedFeature.DERIVED_FEATURE_3
    })
    public void testRequiringAllThreeDerivedFeatures() {
      doNotActuallyRunThis();
    }

    // Exists to test that our framework doesn't run it:
    @SuppressWarnings("unused")
    @ExampleBaseFeature.Require(absent = {ExampleBaseFeature.BASE_FEATURE_1})
    public void testRequiringConflictingFeatures() throws Exception {
      doNotActuallyRunThis();
    }
  }

  @ExampleDerivedFeature.Require(absent = {ExampleDerivedFeature.DERIVED_FEATURE_2})
  private static class ConflictingRequirementsExampleDerivedInterfaceTester
      extends ExampleBaseInterfaceTester {}

  public void testTestFeatureEnums() throws Exception {
    // Haha! Let's test our own test rig!
    FeatureEnumTest.assertGoodFeatureEnum(FeatureUtilTest.ExampleBaseFeature.class);
    FeatureEnumTest.assertGoodFeatureEnum(FeatureUtilTest.ExampleDerivedFeature.class);
  }

  public void testAddImpliedFeatures_returnsSameSetInstance() throws Exception {
    Set<Feature<?>> features = Sets.<Feature<?>>newHashSet(ExampleBaseFeature.BASE_FEATURE_1);
    assertSame(features, FeatureUtil.addImpliedFeatures(features));
  }

  public void testAddImpliedFeatures_addsImpliedFeatures() throws Exception {
    Set<Feature<?>> features;

    features = Sets.<Feature<?>>newHashSet(ExampleDerivedFeature.DERIVED_FEATURE_1);
    assertThat(FeatureUtil.addImpliedFeatures(features))
        .contains(ExampleDerivedFeature.DERIVED_FEATURE_1);

    features = Sets.<Feature<?>>newHashSet(ExampleDerivedFeature.DERIVED_FEATURE_2);
    assertThat(FeatureUtil.addImpliedFeatures(features))
        .containsExactly(
            ExampleDerivedFeature.DERIVED_FEATURE_2, ExampleBaseFeature.BASE_FEATURE_1);

    features = Sets.<Feature<?>>newHashSet(ExampleDerivedFeature.COMPOUND_DERIVED_FEATURE);
    assertThat(FeatureUtil.addImpliedFeatures(features))
        .containsExactly(
            ExampleDerivedFeature.COMPOUND_DERIVED_FEATURE,
            ExampleDerivedFeature.DERIVED_FEATURE_1,
            ExampleDerivedFeature.DERIVED_FEATURE_2,
            ExampleBaseFeature.BASE_FEATURE_1,
            ExampleBaseFeature.BASE_FEATURE_2);
  }

  public void testImpliedFeatures_returnsNewSetInstance() throws Exception {
    Set<Feature<?>> features = Sets.<Feature<?>>newHashSet(ExampleBaseFeature.BASE_FEATURE_1);
    assertNotSame(features, FeatureUtil.impliedFeatures(features));
  }

  public void testImpliedFeatures_returnsImpliedFeatures() throws Exception {
    Set<Feature<?>> features;

    features = Sets.<Feature<?>>newHashSet(ExampleDerivedFeature.DERIVED_FEATURE_1);
    assertTrue(FeatureUtil.impliedFeatures(features).isEmpty());

    features = Sets.<Feature<?>>newHashSet(ExampleDerivedFeature.DERIVED_FEATURE_2);
    assertThat(FeatureUtil.impliedFeatures(features)).contains(ExampleBaseFeature.BASE_FEATURE_1);

    features = Sets.<Feature<?>>newHashSet(ExampleDerivedFeature.COMPOUND_DERIVED_FEATURE);
    assertThat(FeatureUtil.impliedFeatures(features))
        .containsExactly(
            ExampleDerivedFeature.DERIVED_FEATURE_1,
            ExampleDerivedFeature.DERIVED_FEATURE_2,
            ExampleBaseFeature.BASE_FEATURE_1,
            ExampleBaseFeature.BASE_FEATURE_2);
  }

  @AndroidIncompatible // Android runs ExampleDerivedInterfaceTester directly if it exists
  public void testBuildTesterRequirements_class() throws Exception {
    assertEquals(
        FeatureUtil.buildTesterRequirements(ExampleBaseInterfaceTester.class),
        new TesterRequirements(
            Sets.<Feature<?>>newHashSet(ExampleBaseFeature.BASE_FEATURE_1),
            Collections.<Feature<?>>emptySet()));

    assertEquals(
        FeatureUtil.buildTesterRequirements(ExampleDerivedInterfaceTester.class),
        new TesterRequirements(
            Sets.<Feature<?>>newHashSet(
                ExampleBaseFeature.BASE_FEATURE_1, ExampleDerivedFeature.DERIVED_FEATURE_2),
            Collections.<Feature<?>>emptySet()));
  }

  @AndroidIncompatible // Android runs ExampleDerivedInterfaceTester directly if it exists
  public void testBuildTesterRequirements_method() throws Exception {
    assertEquals(
        FeatureUtil.buildTesterRequirements(
            ExampleDerivedInterfaceTester.class.getMethod(
                "testRequiringTwoExplicitDerivedFeatures")),
        new TesterRequirements(
            Sets.<Feature<?>>newHashSet(
                ExampleBaseFeature.BASE_FEATURE_1,
                ExampleDerivedFeature.DERIVED_FEATURE_1,
                ExampleDerivedFeature.DERIVED_FEATURE_2),
            Collections.<Feature<?>>emptySet()));
    assertEquals(
        FeatureUtil.buildTesterRequirements(
            ExampleDerivedInterfaceTester.class.getMethod("testRequiringAllThreeDerivedFeatures")),
        new TesterRequirements(
            Sets.<Feature<?>>newHashSet(
                ExampleBaseFeature.BASE_FEATURE_1,
                ExampleDerivedFeature.DERIVED_FEATURE_1,
                ExampleDerivedFeature.DERIVED_FEATURE_2,
                ExampleDerivedFeature.DERIVED_FEATURE_3),
            Collections.<Feature<?>>emptySet()));
  }

  @AndroidIncompatible // Android runs ExampleDerivedInterfaceTester directly if it exists
  public void testBuildTesterRequirements_classClassConflict() throws Exception {
    try {
      FeatureUtil.buildTesterRequirements(
          ConflictingRequirementsExampleDerivedInterfaceTester.class);
      fail("Expected ConflictingRequirementsException");
    } catch (ConflictingRequirementsException e) {
      assertThat(e.getConflicts()).contains(ExampleBaseFeature.BASE_FEATURE_1);
      assertEquals(ConflictingRequirementsExampleDerivedInterfaceTester.class, e.getSource());
    }
  }

  @AndroidIncompatible // Android runs ExampleDerivedInterfaceTester directly if it exists
  public void testBuildTesterRequirements_methodClassConflict() throws Exception {
    final Method method =
        ExampleDerivedInterfaceTester.class.getMethod("testRequiringConflictingFeatures");
    try {
      FeatureUtil.buildTesterRequirements(method);
      fail("Expected ConflictingRequirementsException");
    } catch (ConflictingRequirementsException e) {
      assertThat(e.getConflicts()).contains(ExampleBaseFeature.BASE_FEATURE_1);
      assertEquals(method, e.getSource());
    }
  }

  @AndroidIncompatible // Android runs ExampleDerivedInterfaceTester directly if it exists
  public void testBuildDeclaredTesterRequirements() throws Exception {
    assertEquals(
        FeatureUtil.buildDeclaredTesterRequirements(
            ExampleDerivedInterfaceTester.class.getMethod(
                "testRequiringTwoExplicitDerivedFeatures")),
        new TesterRequirements(
            FeatureUtil.addImpliedFeatures(
                Sets.<Feature<?>>newHashSet(
                    ExampleDerivedFeature.DERIVED_FEATURE_1,
                    ExampleDerivedFeature.DERIVED_FEATURE_2)),
            Collections.<Feature<?>>emptySet()));
  }
}
