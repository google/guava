/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.collect.testing.testers;

import com.google.common.annotations.GwtCompatible;

/**
 * The emulation source used in GWT.
 *
 * @author Hayward Chan
 */
@GwtCompatible(emulated = true)
final class Platform {

  // Use fewer steps in the ListIteratorTester in ListListIteratorTester because it's slow in prod
  // mode.
  static int listListIteratorTesterNumIterations() {
    // TODO(hhchan): It's 4 in java.  Figure out why even 3 is too slow in prod mode.
    return 2;
  }

  // Use fewer steps in the IteratorTester in CollectionIteratorTester because it's slow in prod
  // mode.
  static int collectionIteratorTesterNumIterations() {
    return 3;
  }

  // TODO: Consolidate different copies in one single place.
  static String format(String template, Object... args) {
    // start substituting the arguments into the '%s' placeholders
    StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
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

  private Platform() {}
}
