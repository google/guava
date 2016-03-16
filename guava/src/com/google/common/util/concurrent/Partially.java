/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Outer class that exists solely to let us write {@code Partially.GwtIncompatible} instead of plain
 * {@code GwtIncompatible}. This is more accurate for {@link Futures#catching}, which is available
 * under GWT but with a slightly different signature.
 *
 * <p>We can't use {@code PartiallyGwtIncompatible} because then the GWT compiler wouldn't recognize
 * it as a {@code GwtIncompatible} annotation. And for {@code Futures.catching}, we need the GWT
 * compiler to autostrip the normal server method in order to expose the special, inherited GWT
 * version.
 */
@GwtCompatible
final class Partially {
  /**
   * The presence of this annotation on an API indicates that the method <i>may</i> be used with the
   * <a href="http://www.gwtproject.org/">Google Web Toolkit</a> (GWT) but that it has <i>some
   * restrictions</i>.
   */
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
  @Documented
  @interface GwtIncompatible {
    String value();
  }

  private Partially() {}
}
