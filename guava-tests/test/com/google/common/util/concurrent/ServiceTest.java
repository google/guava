/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.util.concurrent;

import static com.google.common.util.concurrent.Service.State.FAILED;
import static com.google.common.util.concurrent.Service.State.NEW;
import static com.google.common.util.concurrent.Service.State.RUNNING;
import static com.google.common.util.concurrent.Service.State.STARTING;
import static com.google.common.util.concurrent.Service.State.STOPPING;
import static com.google.common.util.concurrent.Service.State.TERMINATED;

import java.util.Locale;
import junit.framework.TestCase;

/** Unit tests for {@link Service} */
public class ServiceTest extends TestCase {

  /** Assert on the comparison ordering of the State enum since we guarantee it. */
  public void testStateOrdering() {
    // List every valid (direct) state transition.
    assertLessThan(NEW, STARTING);
    assertLessThan(NEW, TERMINATED);

    assertLessThan(STARTING, RUNNING);
    assertLessThan(STARTING, STOPPING);
    assertLessThan(STARTING, FAILED);

    assertLessThan(RUNNING, STOPPING);
    assertLessThan(RUNNING, FAILED);

    assertLessThan(STOPPING, FAILED);
    assertLessThan(STOPPING, TERMINATED);
  }

  private static <T extends Comparable<? super T>> void assertLessThan(T a, T b) {
    if (a.compareTo(b) >= 0) {
      fail(String.format(Locale.ROOT, "Expected %s to be less than %s", a, b));
    }
  }
}
