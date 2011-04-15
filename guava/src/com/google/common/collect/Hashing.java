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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

/**
 * Static methods for implementing hash-based collections.
 *
 * @author Kevin Bourrillion
 * @author Jesse Wilson
 */
@GwtCompatible
final class Hashing {
  private Hashing() {}

  /*
   * This method was written by Doug Lea with assistance from members of JCP
   * JSR-166 Expert Group and released to the public domain, as explained at
   * http://creativecommons.org/licenses/publicdomain
   * 
   * As of 2010/06/11, this method is identical to the (package private) hash
   * method in OpenJDK 7's java.util.HashMap class.
   */
  static int smear(int hashCode) {
    hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
    return hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
  }
}
