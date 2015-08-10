/*
 * Copyright (C) 2010 The Guava Authors
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

/**
 * Static utilities for working with the eight primitive types and {@code void},
 * and value types for treating them as unsigned.
 *
 * <p>This package is a part of the open-source
 * <a href="http://guava-libraries.googlecode.com">Guava libraries</a>.
 * 
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/PrimitivesExplained">
 * primitive utilities</a>.
 *
 * <h2>Contents</h2>
 *
 * <h3>General static utilities</h3>
 *
 * <ul>
 * <li>{@link com.google.common.primitives.Primitives}
 * </ul>
 *
 * <h3>Per-type static utilities</h3>
 *
 * <ul>
 * <li>{@link com.google.common.primitives.Booleans}
 * <li>{@link com.google.common.primitives.Bytes}
 *   <ul>
 *     <li>{@link com.google.common.primitives.SignedBytes}
 *     <li>{@link com.google.common.primitives.UnsignedBytes}
 *   </ul>
 * <li>{@link com.google.common.primitives.Chars}
 * <li>{@link com.google.common.primitives.Doubles}
 * <li>{@link com.google.common.primitives.Floats}
 * <li>{@link com.google.common.primitives.Ints}
 *   <ul>
 *     <li>{@link com.google.common.primitives.UnsignedInts}
 *   </ul>
 * <li>{@link com.google.common.primitives.Longs}
 *   <ul>
 *     <li>{@link com.google.common.primitives.UnsignedLongs}
 *   </ul>
 * <li>{@link com.google.common.primitives.Shorts}
 * </ul>
 *
 * <h3>Value types</h3>
 * <ul>
 *   <li>{@link com.google.common.primitives.UnsignedInteger}
 *   <li>{@link com.google.common.primitives.UnsignedLong}
 * </ul>
 */
@ParametersAreNonnullByDefault
package com.google.common.primitives;

import javax.annotation.ParametersAreNonnullByDefault;

