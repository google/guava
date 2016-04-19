/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.graph;

/**
 * A utility class to hold various constants used by the Guava Graph library.
 */
// TODO(user): Decide what else to put here (error message strings, node/edge map sizes, etc.)
final class GraphConstants {

  private GraphConstants() {}

  // TODO(user): Enable users to specify the expected (in/out?) degree of nodes.
  static final int EXPECTED_DEGREE = 11;
}
