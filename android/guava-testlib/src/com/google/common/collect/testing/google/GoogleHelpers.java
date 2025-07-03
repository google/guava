/*
 * Copyright (C) 2015 The Guava Authors
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

package com.google.common.collect.testing.google;

import static junit.framework.Assert.fail;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;

/**
 * Helper methods/assertions for use with {@code com.google.common.collect} types.
 *
 * @author Colin Decker
 */
@GwtCompatible
final class GoogleHelpers {

  private GoogleHelpers() {}

  static void assertEmpty(Multimap<?, ?> multimap) {
    if (!multimap.isEmpty()) {
      fail("Not true that " + multimap + " is empty");
    }
  }
}
