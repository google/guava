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

package com.google.common.util.concurrent;

/** Methods factored out so that they can be emulated differently in GWT. */
final class Platform {
  static boolean isInstanceOfThrowableClass(Throwable t, Class<? extends Throwable> expectedClass) {
    /*
     * This method is used only by CatchingFuture, and CatchingFuture accepts only Throwable.class
     * under GWT.
     */
    return true;
  }

  private Platform() {}
}
