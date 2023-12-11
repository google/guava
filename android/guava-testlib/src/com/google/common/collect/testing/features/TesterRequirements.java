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
import java.util.Collections;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Encapsulates the constraints that a class under test must satisfy in order for a tester method to
 * be run against that class.
 *
 * @author George van den Driessche
 */
@GwtCompatible
public final class TesterRequirements {
  private final Set<Feature<?>> presentFeatures;
  private final Set<Feature<?>> absentFeatures;

  public TesterRequirements(Set<Feature<?>> presentFeatures, Set<Feature<?>> absentFeatures) {
    this.presentFeatures = Helpers.copyToSet(presentFeatures);
    this.absentFeatures = Helpers.copyToSet(absentFeatures);
  }

  public TesterRequirements(TesterRequirements tr) {
    this(tr.getPresentFeatures(), tr.getAbsentFeatures());
  }

  public TesterRequirements() {
    this(Collections.<Feature<?>>emptySet(), Collections.<Feature<?>>emptySet());
  }

  public final Set<Feature<?>> getPresentFeatures() {
    return presentFeatures;
  }

  public final Set<Feature<?>> getAbsentFeatures() {
    return absentFeatures;
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof TesterRequirements) {
      TesterRequirements that = (TesterRequirements) object;
      return this.presentFeatures.equals(that.presentFeatures)
          && this.absentFeatures.equals(that.absentFeatures);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return presentFeatures.hashCode() * 31 + absentFeatures.hashCode();
  }

  @Override
  public String toString() {
    return "{TesterRequirements: present=" + presentFeatures + ", absent=" + absentFeatures + "}";
  }

  private static final long serialVersionUID = 0;
}
