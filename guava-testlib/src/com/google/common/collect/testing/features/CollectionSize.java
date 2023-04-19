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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.Helpers;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * When describing the features of the collection produced by a given generator (i.e. in a call to
 * {@link
 * com.google.common.collect.testing.FeatureSpecificTestSuiteBuilder#withFeatures(Feature...)}),
 * this annotation specifies each of the different sizes for which a test suite should be built. (In
 * a typical case, the features should include {@link CollectionSize#ANY}.) These semantics are thus
 * a little different from those of other Collection-related features such as {@link
 * CollectionFeature} or {@link SetFeature}.
 *
 * <p>However, when {@link CollectionSize.Require} is used to annotate a test it behaves normally
 * (i.e. it requires the collection instance under test to be a certain size for the test to run).
 * Note that this means a test should not require more than one CollectionSize, since a particular
 * collection instance can only be one size at once.
 *
 * @author George van den Driessche
 */
// Enum values use constructors with generic varargs.
@SuppressWarnings("unchecked")
@GwtCompatible
public enum CollectionSize implements Feature<Collection>, Comparable<CollectionSize> {
  /** Test an empty collection. */
  ZERO(0),
  /** Test a one-element collection. */
  ONE(1),
  /** Test a three-element collection. */
  SEVERAL(3),
  /*
   * TODO: add VERY_LARGE, noting that we currently assume that the fourth
   * sample element is not in any collection
   */

  ANY(ZERO, ONE, SEVERAL);

  private final Set<Feature<? super Collection>> implied;
  private final @Nullable Integer numElements;

  CollectionSize(int numElements) {
    this.implied = Collections.emptySet();
    this.numElements = numElements;
  }

  CollectionSize(Feature<? super Collection>... implied) {
    // Keep the order here, so that PerCollectionSizeTestSuiteBuilder
    // gives a predictable order of test suites.
    this.implied = Helpers.copyToSet(implied);
    this.numElements = null;
  }

  @Override
  public Set<Feature<? super Collection>> getImpliedFeatures() {
    return implied;
  }

  public int getNumElements() {
    if (numElements == null) {
      throw new IllegalStateException(
          "A compound CollectionSize doesn't specify a number of elements.");
    }
    return numElements;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @TesterAnnotation
  public @interface Require {
    CollectionSize[] value() default {};

    CollectionSize[] absent() default {};
  }
}
