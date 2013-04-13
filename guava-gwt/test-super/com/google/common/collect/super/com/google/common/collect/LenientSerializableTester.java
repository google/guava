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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

/**
 * Variant of {@link SerializableTester} that does not require the reserialized object's class to be
 * identical to the original.
 *
 * @author Chris Povirk
 */
/*
 * The whole thing is really @GwtIncompatible, but GwtJUnitConvertedTestModule doesn't have a
 * parameter for non-GWT, non-test files, and it didn't seem worth adding one for this unusual case.
 */
@GwtCompatible(emulated = true)
final class LenientSerializableTester {
  /*
   * TODO(cpovirk): move this to c.g.c.testing if we allow for c.g.c.annotations dependencies so
   * that it can be GWTified?
   */

  private LenientSerializableTester() {}
}

