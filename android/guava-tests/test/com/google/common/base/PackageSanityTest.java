/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.base;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.AbstractPackageSanityTests;
import org.jspecify.annotations.NullUnmarked;

/** Basic sanity tests for classes in {@code common.base}. */

@J2ktIncompatible
@GwtIncompatible
@NullUnmarked
public class PackageSanityTest extends AbstractPackageSanityTests {
  private static final ImmutableSet<Class<?>> IGNORED_CLASSES =
      ImmutableSet.of(
          Preconditions.class,
          Verify.class);

  public PackageSanityTest() {
    // package private classes like FunctionalEquivalence are tested through the public API.
    publicApiOnly();
    ignoreClasses(IGNORED_CLASSES::contains);
  }
}
