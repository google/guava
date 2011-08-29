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

package com.google.common.util.concurrent;

import junit.framework.TestCase;

import java.util.concurrent.Callable;

/**
 * Unit tests for {@link Callables}.
 *
 * @author Isaac Shum
 */
public class CallablesTest extends TestCase {

  public void testReturning() throws Exception {
    assertNull(Callables.returning(null).call());

    Object value = new Object();
    Callable<Object> callable = Callables.returning(value);
    assertSame(value, callable.call());
    // Expect the same value on subsequent calls
    assertSame(value, callable.call());
  }
}
