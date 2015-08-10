/*
 * Copyright (C) 2006 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;

/**
 * Hidden superclass of {@link Futures} that provides us a place to declare special GWT versions of
 * the {@link Futures#catching(ListenableFuture, Class, com.google.common.base.Function)
 * Futures.catching} family of methods. Those versions have slightly different signatures.
 */
@GwtCompatible(emulated = true)
abstract class GwtFuturesCatchingSpecialization {
  /*
   * This server copy of the class is empty. The corresponding GWT copy contains alternative
   * versions of catching() and catchingAsync() with slightly different signatures from the ones
   * found in Futures.java.
   */
}
