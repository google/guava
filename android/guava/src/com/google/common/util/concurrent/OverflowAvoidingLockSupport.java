/*
 * Copyright (C) 2020 The Guava Authors
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

import static java.lang.Math.min;

import com.google.common.annotations.J2ktIncompatible;
import java.util.concurrent.locks.LockSupport;
import javax.annotation.CheckForNull;

/**
 * Works around an android bug, where parking for more than INT_MAX seconds can produce an abort
 * signal on 32 bit devices running Android Q.
 */
@J2ktIncompatible
@ElementTypesAreNonnullByDefault
final class OverflowAvoidingLockSupport {
  // Represents the max nanoseconds representable on a linux timespec with a 32 bit tv_sec
  static final long MAX_NANOSECONDS_THRESHOLD = (1L + Integer.MAX_VALUE) * 1_000_000_000L - 1L;

  private OverflowAvoidingLockSupport() {}

  static void parkNanos(@CheckForNull Object blocker, long nanos) {
    // Even in the extremely unlikely event that a thread unblocks itself early after only 68 years,
    // this is indistinguishable from a spurious wakeup, which LockSupport allows.
    LockSupport.parkNanos(blocker, min(nanos, MAX_NANOSECONDS_THRESHOLD));
  }
}
