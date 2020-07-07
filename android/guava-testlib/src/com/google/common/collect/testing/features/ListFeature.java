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
import java.util.List;
import java.util.Set;

/**
 * Optional features of classes derived from {@code List}.
 *
 * @author George van den Driessche
 */
// Enum values use constructors with generic varargs.
@SuppressWarnings("unchecked")
@GwtCompatible
public enum ListFeature implements Feature<List> {
  SUPPORTS_SET,
  SUPPORTS_ADD_WITH_INDEX(CollectionFeature.SUPPORTS_ADD),
  SUPPORTS_REMOVE_WITH_INDEX(CollectionFeature.SUPPORTS_REMOVE),

  GENERAL_PURPOSE(
      CollectionFeature.GENERAL_PURPOSE,
      SUPPORTS_SET,
      SUPPORTS_ADD_WITH_INDEX,
      SUPPORTS_REMOVE_WITH_INDEX),

  /** Features supported by lists where only removal is allowed. */
  REMOVE_OPERATIONS(CollectionFeature.REMOVE_OPERATIONS, SUPPORTS_REMOVE_WITH_INDEX);

  private final Set<Feature<? super List>> implied;

  ListFeature(Feature<? super List>... implied) {
    this.implied = Helpers.copyToSet(implied);
  }

  @Override
  public Set<Feature<? super List>> getImpliedFeatures() {
    return implied;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @TesterAnnotation
  public @interface Require {
    ListFeature[] value() default {};

    ListFeature[] absent() default {};
  }
}
