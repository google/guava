/*
 * Copyright (C) 2020 The Guava Authors
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

package com.google.common.base;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * A class that uses a couple Java 8 features but doesn't really do anything. This lets us attempt
 * to load it and log a warning if that fails, giving users advance notice of our dropping Java 8
 * support.
 */
/*
 * This class should be annotated @GwtCompatible. But if we annotate it @GwtCompatible, then we need
 * to build GwtCompatible.java (-source 7 -target 7 in the Android flavor) before we build
 * Java8Usage.java (-source 8 target 8, which we already need to build before the rest of
 * common.base). We could configure Maven to do that, but it's easier to just skip the annotation.
 */
final class Java8Usage {
  @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
  @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
  private @interface SomeTypeAnnotation {}

  @CanIgnoreReturnValue
  static @SomeTypeAnnotation String performCheck() {
    Runnable r = () -> {};
    r.run();
    return "";
  }

  private Java8Usage() {}
}
