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

/**
 * This package contains utility methods and classes for working with Java I/O; for example input
 * streams, output streams, readers, writers, and files.
 *
 * <p>At the core of this package are the Source/Sink types: {@link com.google.common.io.ByteSource
 * ByteSource}, {@link com.google.common.io.CharSource CharSource}, {@link
 * com.google.common.io.ByteSink ByteSink} and {@link com.google.common.io.CharSink CharSink}. They
 * are factories for I/O streams that provide many convenience methods that handle both opening and
 * closing streams for you.
 *
 * <p>This package is a part of the open-source <a href="http://github.com/google/guava">Guava</a>
 * library. For more information on Sources and Sinks as well as other features of this package, see
 * <a href="https://github.com/google/guava/wiki/IOExplained">I/O Explained</a> on the Guava wiki.
 *
 * @author Chris Nokleberg
 */
@CheckReturnValue
@ParametersAreNonnullByDefault
package com.google.common.io;

import com.google.errorprone.annotations.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
