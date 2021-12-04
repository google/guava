/*
 * Copyright (C) 2020 The Guava Authors
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

package com.google.common.primitives;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;

/** Web specializations for {@link Doubles} methods. */
abstract class DoublesMethodsForWeb {

  @JsMethod(name = "Math.min", namespace = JsPackage.GLOBAL)
  public static native double min(double... array);

  @JsMethod(name = "Math.max", namespace = JsPackage.GLOBAL)
  public static native double max(double... array);
}
