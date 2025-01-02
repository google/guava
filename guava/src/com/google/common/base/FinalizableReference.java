/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.base;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.DoNotMock;

/**
 * Implemented by references that have code to run after garbage collection of their referents.
 *
 * @see FinalizableReferenceQueue
 * @author Bob Lee
 * @since 2.0
 */
@DoNotMock("Use an instance of one of the Finalizable*Reference classes")
@J2ktIncompatible
@GwtIncompatible
public interface FinalizableReference {
  /**
   * Invoked on a background thread after the referent has been garbage collected unless security
   * restrictions prevented starting a background thread, in which case this method is invoked when
   * new references are created.
   */
  void finalizeReferent();
}
