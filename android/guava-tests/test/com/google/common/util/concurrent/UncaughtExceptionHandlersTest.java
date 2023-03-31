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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.UncaughtExceptionHandlers.Exiter;
import junit.framework.TestCase;

/** @author Gregory Kick */
public class UncaughtExceptionHandlersTest extends TestCase {

  private Runtime runtimeMock;

  @Override
  protected void setUp() {
    runtimeMock = mock(Runtime.class);
  }

  public void testExiter() {
    new Exiter(runtimeMock).uncaughtException(new Thread(), new Exception());
    verify(runtimeMock).exit(1);
  }
}
