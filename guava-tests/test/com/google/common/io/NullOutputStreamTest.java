/*
 * Copyright (C) 2002 The Guava Authors
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

package com.google.common.io;

import junit.framework.TestCase;

/**
 * Unit tests for {@link NullOutputStream}.
 *
 * @author Spencer Kimball
 */
public class NullOutputStreamTest extends TestCase {

  public void testBasicOperation() throws Exception {
    // create a null output stream
    NullOutputStream nos = new NullOutputStream();
    assertNotNull(nos);
    // write to the output stream
    nos.write('n');
    String test = "Test string for NullOutputStream";
    nos.write(test.getBytes());
    nos.write(test.getBytes(), 2, 10);
  }
}
