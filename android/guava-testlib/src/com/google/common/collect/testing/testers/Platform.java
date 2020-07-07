/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect.testing.testers;

import com.google.common.annotations.GwtCompatible;
import java.util.Locale;

/**
 * This class is emulated in GWT.
 *
 * @author Hayward Chan
 */
@GwtCompatible
final class Platform {

  /** Format the template with args, only supports the placeholder {@code %s}. */
  static String format(String template, Object... args) {
    return String.format(Locale.ROOT, template, args);
  }

  /** See {@link ListListIteratorTester} */
  static int listListIteratorTesterNumIterations() {
    return 4;
  }

  /** See {@link CollectionIteratorTester} */
  static int collectionIteratorTesterNumIterations() {
    return 5;
  }

  private Platform() {}
}
