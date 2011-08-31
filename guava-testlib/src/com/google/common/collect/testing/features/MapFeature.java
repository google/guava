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

import com.google.common.collect.testing.Helpers;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.Set;

/**
 * Optional features of classes derived from {@code Map}.
 *
 * <p>This class is GWT compatible.
 *
 * @author George van den Driessche
 */
// Enum values use constructors with generic varargs.
@SuppressWarnings("unchecked")
public enum MapFeature implements Feature<Map> {
  /**
   * The map does not throw {@code NullPointerException} on calls such as
   * {@code containsKey(null)}, {@code get(null)}, or {@code remove(null)}.
   */
  ALLOWS_NULL_QUERIES,
  ALLOWS_NULL_KEYS (ALLOWS_NULL_QUERIES),
  ALLOWS_NULL_VALUES,
  RESTRICTS_KEYS,
  RESTRICTS_VALUES,
  SUPPORTS_PUT,
  SUPPORTS_PUT_ALL,
  SUPPORTS_REMOVE,
  SUPPORTS_CLEAR,
  /**
   * Indicates that the constructor or factory method of a map, usually an
   * immutable map, throws an {@link IllegalArgumentException} when presented
   * with duplicate keys instead of discarding all but one.
   */
  REJECTS_DUPLICATES_AT_CREATION,

  GENERAL_PURPOSE(
      SUPPORTS_PUT,
      SUPPORTS_PUT_ALL,
      SUPPORTS_REMOVE,
      SUPPORTS_CLEAR
  ),

  /** Features supported by maps where only removal is allowed. */
  REMOVE_OPERATIONS(
      SUPPORTS_REMOVE,
      SUPPORTS_CLEAR
    );

  private final Set<Feature<? super Map>> implied;

  MapFeature(Feature<? super Map> ... implied) {
    this.implied = Helpers.copyToSet(implied);
  }

  @Override
  public Set<Feature<? super Map>> getImpliedFeatures() {
    return implied;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @TesterAnnotation
  public @interface Require {
    public abstract MapFeature[] value() default {};
    public abstract MapFeature[] absent() default {};
  }
}
