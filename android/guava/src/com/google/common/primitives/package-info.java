/*
 * Copyright (C) 2010 The Guava Authors
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
 * Static utilities for the eight primitive types and {@code void}, and value types for treating
 * them as unsigned or storing them in immutable arrays.
 *
 * <p>This package is a part of the open-source <a href="https://github.com/google/guava">Guava</a>
 * library.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/PrimitivesExplained">primitive utilities</a>.
 *
 * <h2>Contents</h2>
 *
 * <h3>Value types</h3>
 *
 * <ul>
 *   <li>{@link ImmutableDoubleArray}
 *   <li>{@link ImmutableIntArray}
 *   <li>{@link ImmutableLongrray}
 *   <li>{@link UnsignedInteger}
 *   <li>{@link UnsignedLong}
 * </ul>
 *
 * <h3>Per-type static utilities</h3>
 *
 * <ul>
 *   <li>{@link Booleans}
 *   <li>{@link Bytes}
 *       <ul>
 *         <li>{@link SignedBytes}
 *         <li>{@link UnsignedBytes}
 *       </ul>
 *   <li>{@link Chars}
 *   <li>{@link Doubles}
 *   <li>{@link Floats}
 *   <li>{@link Ints}
 *       <ul>
 *         <li>{@link UnsignedInts}
 *       </ul>
 *   <li>{@link Longs}
 *       <ul>
 *         <li>{@link UnsignedLongs}
 *       </ul>
 *   <li>{@link Shorts}
 * </ul>
 *
 * <h3>General static utilities</h3>
 *
 * <ul>
 *   <li>{@link Primitives}
 * </ul>
 */
@ParametersAreNonnullByDefault
@CheckReturnValue
package com.google.common.primitives;

import com.google.errorprone.annotations.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
