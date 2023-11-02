/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.io;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;

/**
 * Modes for opening a file for writing. The default when mode when none is specified is to truncate
 * the file before writing.
 *
 * @author Colin Decker
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public enum FileWriteMode {
  /** Specifies that writes to the opened file should append to the end of the file. */
  APPEND
}
