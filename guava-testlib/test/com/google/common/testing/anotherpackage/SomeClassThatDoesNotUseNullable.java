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

package com.google.common.testing.anotherpackage;

/** Does not check null, but should not matter since it's in a different package. */
@SuppressWarnings("unused") // For use by NullPointerTester
public class SomeClassThatDoesNotUseNullable {

  void packagePrivateButDoesNotCheckNull(String s) {}

  protected void protectedButDoesNotCheckNull(String s) {}

  public void publicButDoesNotCheckNull(String s) {}

  public static void staticButDoesNotCheckNull(String s) {}
}
