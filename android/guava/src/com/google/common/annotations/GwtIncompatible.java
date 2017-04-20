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

package com.google.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The presence of this annotation on an API indicates that the method may <em>not</em> be used with
 * the <a href="http://www.gwtproject.org/">Google Web Toolkit</a> (GWT).
 *
 * <p>This annotation behaves identically to <a href=
 * "http://www.gwtproject.org/javadoc/latest/com/google/gwt/core/shared/GwtIncompatible.html">the
 * {@code @GwtIncompatible} annotation in GWT itself</a>.
 *
 * @author Charles Fry
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@Documented
@GwtCompatible
public @interface GwtIncompatible {
  /**
   * Describes why the annotated element is incompatible with GWT. Since this is generally due to a
   * dependence on a type/method which GWT doesn't support, it is sufficient to simply reference the
   * unsupported type/method. E.g. "Class.isInstance".
   *
   * <p>As of Guava 20.0, this value is optional. We encourage authors who wish to describe why an
   * API is {@code @GwtIncompatible} to instead leave an implementation comment.
   */
  String value() default "";
}
