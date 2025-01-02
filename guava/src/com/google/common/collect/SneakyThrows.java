/*
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/** Static utility method for unchecked throwing of any {@link Throwable}. */
@GwtCompatible
final class SneakyThrows<T extends Throwable> {
  /**
   * Throws {@code t} as if it were an unchecked {@link Throwable}.
   *
   * <p>This method is useful primarily when we make a reflective call to a method with no {@code
   * throws} clause: Java forces us to handle an arbitrary {@link Throwable} from that method,
   * rather than just the {@link RuntimeException} or {@link Error} that should be possible. (And in
   * fact the static type of {@link Throwable} is occasionally justified even for a method with no
   * {@code throws} clause: Some such methods can in fact throw a checked exception (e.g., by
   * calling code written in Kotlin).) Typically, we want to let a {@link Throwable} from such a
   * method propagate untouched, just as we'd typically let it do for a non-reflective call.
   * However, we can't usually write {@code throw t;} when {@code t} has a static type of {@link
   * Throwable}. But we <i>can</i> write {@code sneakyThrow(t);}.
   *
   * <p>We sometimes also use {@code sneakyThrow} for testing how our code responds to
   * sneaky checked exception.
   *
   * @return never; this method declares a return type of {@link Error} only so that callers can
   *     write {@code throw sneakyThrow(t);} to convince the compiler that the statement will always
   *     throw.
   */
  @CanIgnoreReturnValue
  static Error sneakyThrow(Throwable t) {
    throw new SneakyThrows<Error>().throwIt(t);
  }

  @SuppressWarnings("unchecked") // not really safe, but that's the point
  private Error throwIt(Throwable t) throws T {
    throw (T) t;
  }

  private SneakyThrows() {}
}
