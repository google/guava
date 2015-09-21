/*
 * Copyright (C) 2006 The Guava Authors
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

package com.google.common.base;

import com.google.common.annotations.*;
import junit.framework.*;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * Tests for {@link MoreObjects}.
 */
@GwtCompatible(emulated = true)
public class MoreObjectsTest extends TestCase {
  public void testFirstNonNull_withNonNull() throws Exception {
    String s1 = "foo";
    String s2 = firstNonNull(s1, "bar");
    assertSame(s1, s2);

    Long n1 = (long) 42;
    Long n2 = firstNonNull(null, n1);
    assertSame(n1, n2);
  }

  public void testFirstNonNull_throwsNullPointerException() throws Exception {
    try {
      firstNonNull(null, null);
      fail("expected NullPointerException");
    } catch (NullPointerException ignored) {
    }
  }

  public void testFirstNonNull_varArg() throws Exception {
    Integer input1 = 1;
    assertEquals(input1, firstNonNull(input1, null, null));

    Character input2 = '2';
    assertEquals(input2, firstNonNull(null, input2, null));

    Double input3 = 3.0;
    assertEquals(input3, firstNonNull(null, null, input3, null)); // the whole vararg is null

    Long input4 = 4L;
    assertEquals(input4, firstNonNull(null, null, input4, (Long) null)); // vararg contains a single null

    String input5 = "5";
    assertEquals(input5, firstNonNull(null, null, null, null, input5, null)); // find in vararg

    try {
      firstNonNull(null, null, null, null); // the whole vararg is null
      fail("expected NullPointerException");
    } catch (NullPointerException ignored) {
    }

    try {
      firstNonNull(null, null, null, null, null); // vararg contains only nulls
      fail("expected NullPointerException");
    } catch (NullPointerException ignored) {
    }
  }
}