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

package com.google.common.collect.testing.features;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Minimal stubs to keep the collection test suite builder compilable in GWT.
 *
 * @author Hayward Chan
 */
public class FeatureUtil {

  public static Set<Feature<?>> addImpliedFeatures(Set<Feature<?>> features) {
    throw new UnsupportedOperationException("Should not be called in GWT.");
  }

  public static Set<Feature<?>> impliedFeatures(Set<Feature<?>> features) {
    throw new UnsupportedOperationException("Should not be called in GWT.");
  }

  public static TesterRequirements getTesterRequirements(Class<?> testerClass) {
    throw new UnsupportedOperationException("Should not be called in GWT.");
  }

  public static TesterRequirements getTesterRequirements(Method testerMethod)
      throws ConflictingRequirementsException {
    throw new UnsupportedOperationException("Should not be called in GWT.");
  }
}
