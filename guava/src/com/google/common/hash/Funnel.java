/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.hash;

import com.google.common.annotations.Beta;

/**
 * An object which can send data from an object of type {@code T} into a {@code Sink}.
 * 
 * @author Dimitris Andreou
 * @since 11.0
 */
@Beta
public interface Funnel<T> {
  /**
   * Sends a stream of data from the {@code from} object into the sink {@code into}. There
   * is no requirement that this data be complete enough to fully reconstitute the object
   * later.
   */
  void funnel(T from, Sink into);
}
