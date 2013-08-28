/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.collect.testing;

/**
 * Minimal GWT emulation of {@code com.google.common.collect.testing.Platform}.
 *
 * <p><strong>This .java file should never be consumed by javac.</strong>
 *
 * @author Hayward Chan
 */
final class Platform {

  static boolean checkIsInstance(Class<?> clazz, Object obj) {
    /*
     * In GWT, we can't tell whether obj is an instance of clazz because GWT
     * doesn't support reflections.  For testing purposes, we give up this
     * particular assertion (so that we can keep the rest).
     */
    return true;
  }

  // Class.cast is not supported in GWT.
  static void checkCast(Class<?> clazz, Object obj) {
  }

  static <T> T[] clone(T[] array) {
    return GwtPlatform.clone(array);
  }

  // TODO: Consolidate different copies in one single place.
  static String format(String template, Object... args) {
    // start substituting the arguments into the '%s' placeholders
    StringBuilder builder = new StringBuilder(
        template.length() + 16 * args.length);
    int templateStart = 0;
    int i = 0;
    while (i < args.length) {
      int placeholderStart = template.indexOf("%s", templateStart);
      if (placeholderStart == -1) {
        break;
      }
      builder.append(template.substring(templateStart, placeholderStart));
      builder.append(args[i++]);
      templateStart = placeholderStart + 2;
    }
    builder.append(template.substring(templateStart));

    // if we run out of placeholders, append the extra args in square braces
    if (i < args.length) {
      builder.append(" [");
      builder.append(args[i++]);
      while (i < args.length) {
        builder.append(", ");
        builder.append(args[i++]);
      }
      builder.append("]");
    }

    return builder.toString();
  }
  
  static String classGetSimpleName(Class<?> clazz) {
    throw new UnsupportedOperationException("Shouldn't be called in GWT.");
  }

  private Platform() {}
}
