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

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.testing.features.FeatureEnumTest.assertGoodFeatureEnum;
import static com.google.common.collect.testing.features.FeatureUtil.addImpliedFeatures;
import static com.google.common.collect.testing.features.FeatureUtil.buildDeclaredTesterRequirements;
import static com.google.common.collect.testing.features.FeatureUtil.buildTesterRequirements;
import static com.google.common.collect.testing.features.FeatureUtil.getTesterAnnotations;
import static com.google.common.collect.testing.features.FeatureUtil.impliedFeatures;
import static com.google.common.collect.testing.features.FeatureUtilTest.ExampleFeature.BAR;
import static com.google.common.collect.testing.features.FeatureUtilTest.ExampleFeature.FOO;
import static com.google.common.collect.testing.features.FeatureUtilTest.ExampleFeature.IMPLIES_BAR;
import static com.google.common.collect.testing.features.FeatureUtilTest.ExampleFeature.IMPLIES_FOO;
import static com.google.common.collect.testing.features.FeatureUtilTest.ExampleFeature.IMPLIES_IMPLIES_FOO;
import static com.google.common.collect.testing.features.FeatureUtilTest.ExampleFeature.IMPLIES_IMPLIES_FOO_AND_IMPLIES_BAR;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.testing.features.FeatureUtilTest.ExampleFeature.NotTesterAnnotation;
import com.google.common.collect.testing.features.FeatureUtilTest.ExampleFeature.Require;
import com.google.errorprone.annotations.Keep;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.reflect.Method;
import java.util.Set;
import junit.framework.TestCase;

/**
 * @author George van den Driessche
 */
public class FeatureUtilTest extends TestCase {
  enum ExampleFeature implements Feature<Object> {
    FOO,
    IMPLIES_FOO,
    IMPLIES_IMPLIES_FOO,
    BAR,
    IMPLIES_BAR,
    IMPLIES_IMPLIES_FOO_AND_IMPLIES_BAR;

    @Override
    public ImmutableSet<Feature<? super Object>> getImpliedFeatures() {
      switch (this) {
        case IMPLIES_FOO:
          return ImmutableSet.of(FOO);
        case IMPLIES_IMPLIES_FOO:
          return ImmutableSet.of(IMPLIES_FOO);
        case IMPLIES_BAR:
          return ImmutableSet.of(BAR);
        case IMPLIES_IMPLIES_FOO_AND_IMPLIES_BAR:
          return ImmutableSet.of(IMPLIES_FOO, IMPLIES_BAR);
        default:
          return ImmutableSet.of();
      }
    }

    @Retention(RUNTIME)
    @Inherited
    @TesterAnnotation
    @interface Require {
      ExampleFeature[] value() default {};

      ExampleFeature[] absent() default {};
    }

    @Retention(RUNTIME)
    @Inherited
    @interface NotTesterAnnotation {
      ExampleFeature[] value() default {};

      ExampleFeature[] absent() default {};
    }
  }

  public void testTestFeatureEnums() {
    // Haha! Let's test our own test rig!
    assertGoodFeatureEnum(ExampleFeature.class);
  }

  public void testAddImpliedFeatures_returnsSameSetInstance() {
    Set<Feature<?>> features = newHashSet(FOO);
    assertThat(addImpliedFeatures(features)).isSameInstanceAs(features);
  }

  public void testAddImpliedFeatures_addsImpliedFeatures() {
    assertThat(addImpliedFeatures(newHashSet(FOO))).containsExactly(FOO);

    assertThat(addImpliedFeatures(newHashSet(IMPLIES_IMPLIES_FOO)))
        .containsExactly(IMPLIES_IMPLIES_FOO, IMPLIES_FOO, FOO);

    assertThat(addImpliedFeatures(newHashSet(IMPLIES_IMPLIES_FOO_AND_IMPLIES_BAR)))
        .containsExactly(IMPLIES_IMPLIES_FOO_AND_IMPLIES_BAR, IMPLIES_FOO, FOO, IMPLIES_BAR, BAR);
  }

  public void testImpliedFeatures_returnsNewSetInstance() {
    Set<Feature<?>> features = newHashSet(IMPLIES_FOO);
    assertThat(impliedFeatures(features)).isNotSameInstanceAs(features);
  }

  public void testImpliedFeatures_returnsImpliedFeatures() {
    assertThat(impliedFeatures(newHashSet(FOO))).isEmpty();

    assertThat(impliedFeatures(newHashSet(IMPLIES_IMPLIES_FOO))).containsExactly(IMPLIES_FOO, FOO);

    assertThat(impliedFeatures(newHashSet(IMPLIES_IMPLIES_FOO_AND_IMPLIES_BAR)))
        .containsExactly(IMPLIES_FOO, FOO, IMPLIES_BAR, BAR);
  }

  public void testBuildTesterRequirements_class_notAnnotated() throws Exception {
    class Tester {}

    TesterRequirements requirements = buildTesterRequirements(Tester.class);

    assertThat(requirements.getPresentFeatures()).isEmpty();
    assertThat(requirements.getAbsentFeatures()).isEmpty();
  }

  public void testBuildTesterRequirements_class_empty() throws Exception {
    @Require
    class Tester {}

    TesterRequirements requirements = buildTesterRequirements(Tester.class);

    assertThat(requirements.getPresentFeatures()).isEmpty();
    assertThat(requirements.getAbsentFeatures()).isEmpty();
  }

  public void testBuildTesterRequirements_class_present() throws Exception {
    @Require({IMPLIES_IMPLIES_FOO, IMPLIES_BAR})
    class Tester {}

    TesterRequirements requirements = buildTesterRequirements(Tester.class);

    assertThat(requirements.getPresentFeatures())
        .containsExactly(IMPLIES_IMPLIES_FOO, IMPLIES_FOO, FOO, IMPLIES_BAR, BAR);
    assertThat(requirements.getAbsentFeatures()).isEmpty();
  }

  public void testBuildTesterRequirements_class_absent() throws Exception {
    @Require(absent = {IMPLIES_IMPLIES_FOO, IMPLIES_BAR})
    class Tester {}

    TesterRequirements requirements = buildTesterRequirements(Tester.class);

    assertThat(requirements.getPresentFeatures()).isEmpty();
    assertThat(requirements.getAbsentFeatures()).containsExactly(IMPLIES_IMPLIES_FOO, IMPLIES_BAR);
  }

  public void testBuildTesterRequirements_class_present_and_absent() throws Exception {
    @Require(value = IMPLIES_FOO, absent = IMPLIES_IMPLIES_FOO)
    class Tester {}

    TesterRequirements requirements = buildTesterRequirements(Tester.class);

    assertThat(requirements.getPresentFeatures()).containsExactly(IMPLIES_FOO, FOO);
    assertThat(requirements.getAbsentFeatures()).containsExactly(IMPLIES_IMPLIES_FOO);
  }

  public void testBuildTesterRequirements_class_present_method_present() throws Exception {
    @Require(IMPLIES_BAR)
    class Tester {
      @Keep
      @Require(IMPLIES_IMPLIES_FOO)
      public void test() {}
    }

    TesterRequirements requirements = buildTesterRequirements(Tester.class.getMethod("test"));

    assertThat(requirements.getPresentFeatures())
        .containsExactly(IMPLIES_IMPLIES_FOO, IMPLIES_FOO, FOO, IMPLIES_BAR, BAR);
    assertThat(requirements.getAbsentFeatures()).isEmpty();
  }

  public void testBuildTesterRequirements_class_absent_method_absent() throws Exception {
    @Require(absent = IMPLIES_BAR)
    class Tester {
      @Keep
      @Require(absent = IMPLIES_IMPLIES_FOO)
      public void test() {}
    }

    TesterRequirements requirements = buildTesterRequirements(Tester.class.getMethod("test"));

    assertThat(requirements.getPresentFeatures()).isEmpty();
    assertThat(requirements.getAbsentFeatures()).containsExactly(IMPLIES_IMPLIES_FOO, IMPLIES_BAR);
  }

  public void testBuildTesterRequirements_class_present_method_absent() throws Exception {
    @Require(IMPLIES_IMPLIES_FOO)
    class Tester {
      @Keep
      @Require(absent = IMPLIES_IMPLIES_FOO_AND_IMPLIES_BAR)
      public void test() {}
    }

    TesterRequirements requirements = buildTesterRequirements(Tester.class.getMethod("test"));

    assertThat(requirements.getPresentFeatures())
        .containsExactly(IMPLIES_IMPLIES_FOO, IMPLIES_FOO, FOO);
    assertThat(requirements.getAbsentFeatures())
        .containsExactly(IMPLIES_IMPLIES_FOO_AND_IMPLIES_BAR);
  }

  public void testBuildTesterRequirements_class_absent_method_present() throws Exception {
    @Require(absent = IMPLIES_IMPLIES_FOO_AND_IMPLIES_BAR)
    class Tester {
      @Keep
      @Require(IMPLIES_IMPLIES_FOO)
      public void test() {}
    }

    TesterRequirements requirements = buildTesterRequirements(Tester.class.getMethod("test"));

    assertThat(requirements.getPresentFeatures())
        .containsExactly(IMPLIES_IMPLIES_FOO, IMPLIES_FOO, FOO);
    assertThat(requirements.getAbsentFeatures())
        .containsExactly(IMPLIES_IMPLIES_FOO_AND_IMPLIES_BAR);
  }

  public void testBuildTesterRequirements_classClassConflict() {
    @Require(value = FOO, absent = FOO)
    class Tester {}

    ConflictingRequirementsException e =
        assertThrows(
            ConflictingRequirementsException.class, () -> buildTesterRequirements(Tester.class));
    assertThat(e.getConflicts()).containsExactly(FOO);
    assertThat(e.getSource()).isEqualTo(Tester.class.getAnnotation(Require.class));
  }

  public void testBuildTesterRequirements_classClassConflict_inherited() {
    @Require(FOO)
    abstract class BaseTester {}
    @Require(absent = FOO)
    class Tester extends BaseTester {}

    ConflictingRequirementsException e =
        assertThrows(
            ConflictingRequirementsException.class, () -> buildTesterRequirements(Tester.class));
    assertThat(e.getConflicts()).containsExactly(FOO);
    assertThat(e.getSource()).isEqualTo(Tester.class);
  }

  public void testBuildTesterRequirements_classClassConflict_implied() {
    @Require(value = IMPLIES_FOO, absent = FOO)
    class Tester {}

    ConflictingRequirementsException e =
        assertThrows(
            ConflictingRequirementsException.class, () -> buildTesterRequirements(Tester.class));
    assertThat(e.getConflicts()).containsExactly(FOO);
    assertThat(e.getSource()).isEqualTo(Tester.class.getAnnotation(Require.class));
  }

  public void testBuildTesterRequirements_methodClassConflict() throws Exception {
    @Require(IMPLIES_FOO)
    class Tester {
      @Keep
      @Require(absent = FOO)
      public void test() {}
    }

    Method method = Tester.class.getMethod("test");
    ConflictingRequirementsException e =
        assertThrows(ConflictingRequirementsException.class, () -> buildTesterRequirements(method));
    assertThat(e.getConflicts()).containsExactly(FOO);
    assertThat(e.getSource()).isEqualTo(method);
  }

  public void testBuildDeclaredTesterRequirements() throws Exception {
    @Require(IMPLIES_FOO)
    abstract class BaseTester {}
    @Require(IMPLIES_BAR)
    class Tester extends BaseTester {}

    TesterRequirements requirements = buildDeclaredTesterRequirements(Tester.class);

    assertThat(requirements.getPresentFeatures()).containsExactly(IMPLIES_BAR, BAR);
    assertThat(requirements.getAbsentFeatures()).isEmpty();
  }

  public void testGetTesterAnnotations_class() {
    @Require
    @NotTesterAnnotation
    class Tester {}

    assertThat(getTesterAnnotations(Tester.class))
        .containsExactly(Tester.class.getAnnotation(Require.class));
  }

  public void testGetTesterAnnotations_method() throws Exception {
    class Tester {
      @Keep
      @Require
      @NotTesterAnnotation
      public void test() {}
    }
    Method method = Tester.class.getMethod("test");

    assertThat(getTesterAnnotations(method)).containsExactly(method.getAnnotation(Require.class));
  }
}
