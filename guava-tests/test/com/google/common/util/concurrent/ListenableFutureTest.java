/*
 * Copyright (C) 2023 The Guava Authors
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

import static com.google.common.truth.Truth.assertWithMessage;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import junit.framework.TestCase;

/** Test for {@link ListenableFuture}. */
public class ListenableFutureTest extends TestCase {
  public void testNoNewApis() throws Exception {
    assertWithMessage(
            "Do not add new methods to ListenableFuture. Its API needs to continue to match the"
                + " version we released in a separate artifact com.google.guava:listenablefuture.")
        .that(ListenableFuture.class.getDeclaredMethods())
        .asList()
        .containsExactly(
            ListenableFuture.class.getMethod("addListener", Runnable.class, Executor.class));
    assertWithMessage(
            "Do not add new supertypes to ListenableFuture. Its API needs to continue to match the"
                + " version we released in a separate artifact com.google.guava:listenablefuture.")
        .that(ListenableFuture.class.getInterfaces())
        .asList()
        .containsExactly(Future.class);
  }
}
