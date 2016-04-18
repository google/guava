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

package com.google.common.io;

import com.google.common.annotations.GwtIncompatible;

/**
 * Package-protected abstract class that implements the line reading algorithm used by
 * {@link LineReader}. Line separators are per {@link java.io.BufferedReader}: line feed, carriage
 * return, or carriage return followed immediately by a linefeed.
 *
 * <p>Subclasses must implement {@link #handleLine}, call {@link #add} to pass character data, and
 * call {@link #finish} at the end of stream.
 *
 * @author Chris Nokleberg
 * @since 1.0
 */
@GwtIncompatible
abstract class LineBuffer {
}
