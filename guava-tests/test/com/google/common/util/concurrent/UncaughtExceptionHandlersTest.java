/*
 * Copyright (C) 2010 The Guava Authors
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.util.concurrent.UncaughtExceptionHandlers.Exiter;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * @author Gregory Kick
 */
@NullUnmarked
@GwtIncompatible
@J2ktIncompatible
public class UncaughtExceptionHandlersTest extends TestCase {
  public void testExiter() {
    int[] exitCode = {-1};
    new Exiter(code -> exitCode[0] = code).uncaughtException(new Thread(), new Exception());
    assertThat(exitCode[0]).isEqualTo(1);
  }
}
