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

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * A class that can supply objects of a single type; a pre-Java-8 version of {@link
 * java.util.function.Supplier java.util.function.Supplier}. Semantically, this could be a factory,
 * generator, builder, closure, or something else entirely. No guarantees are implied by this
 * interface.
 *
 * <p>The {@link Suppliers} class provides common suppliers and related utilities.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/FunctionalExplained">the use of functional types</a>.
 *
 * <h3>For Java 8+ users</h3>
 *
 * <p>This interface is now a legacy type. Use {@code java.util.function.Supplier} (or the
 * appropriate primitive specialization such as {@code IntSupplier}) instead whenever possible.
 * Otherwise, at least reduce <i>explicit</i> dependencies on this type by using lambda expressions
 * or method references instead of classes, leaving your code easier to migrate in the future.
 *
 * <p>To use an existing supplier instance (say, named {@code supplier}) in a context where the
 * <i>other type</i> of supplier is expected, use the method reference {@code supplier::get}. A
 * future version of {@code com.google.common.base.Supplier} will be made to <i>extend</i> {@code
 * java.util.function.Supplier}, making conversion code necessary only in one direction. At that
 * time, this interface will be officially discouraged.
 *
 * @author Harry Heymann
 * @since 2.0
 */
@GwtCompatible
public interface Supplier<T> {
  /**
   * Retrieves an instance of the appropriate type. The returned object may or may not be a new
   * instance, depending on the implementation.
   *
   * @return an instance of the appropriate type
   */
  @CanIgnoreReturnValue
  T get();
}
