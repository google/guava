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
 * The presence of this annotation on a type indicates that the type may be used with <a
 * href="https://www.gwtproject.org/">GWT</a> or <a href="https://github.com/google/j2cl">J2CL</a>.
 *
 * <p>Note that a {@code GwtCompatible} type may have some {@link GwtIncompatible} methods.
 *
 * @author Charles Fry
 * @author Hayward Chan
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@GwtCompatible
public @interface GwtCompatible {

  /**
   * Obsolete; formerly used to indicate when a value was GWT serializable back before Guava dropped
   * support for GWT serialization.
   *
   * @see <a href=
   *     "https://www.gwtproject.org/doc/latest/DevGuideServerCommunication#DevGuideSerializableTypes">
   *     Documentation about GWT serialization</a>
   */
  boolean serializable() default false;

  /**
   * When {@code true}, the annotated type is emulated in GWT. The emulated source (also known as
   * super-source) is different from the implementation used by the JVM.
   *
   * @see <a href=
   *     "https://www.gwtproject.org/doc/latest/DevGuideOrganizingProjects.html#DevGuideModules">
   *     Documentation about GWT emulated source</a>
   */
  boolean emulated() default false;
}
