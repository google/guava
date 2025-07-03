/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;
import java.util.concurrent.atomic.LongAdder;

/**
 * Source of {@link LongAddable} objects that deals with GWT and all that.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
final class LongAddables {
  public static LongAddable create() {
    return new JavaUtilConcurrentLongAdder();
  }

  private static final class JavaUtilConcurrentLongAdder extends LongAdder implements LongAddable {}
}
