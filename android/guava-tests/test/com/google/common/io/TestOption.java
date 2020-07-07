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

package com.google.common.io;

/**
 * Options controlling the behavior of sources/sinks/streams for testing.
 *
 * @author Colin Decker
 */
public enum TestOption {
  OPEN_THROWS,
  SKIP_THROWS,
  READ_THROWS,
  WRITE_THROWS,
  CLOSE_THROWS,
  AVAILABLE_ALWAYS_ZERO
}
