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

package com.google.common.escape;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import junit.framework.TestCase;

/** @author David Beaumont */
@GwtCompatible
public class ArrayBasedEscaperMapTest extends TestCase {
  public void testNullMap() {
    try {
      ArrayBasedEscaperMap.create(null);
      fail("expected exception did not occur");
    } catch (NullPointerException e) {
      // pass
    }
  }

  public void testEmptyMap() {
    Map<Character, String> map = ImmutableMap.of();
    ArrayBasedEscaperMap fem = ArrayBasedEscaperMap.create(map);
    // Non-null array of zero length.
    assertEquals(0, fem.getReplacementArray().length);
  }

  public void testMapLength() {
    Map<Character, String> map =
        ImmutableMap.of(
            'a', "first",
            'z', "last");
    ArrayBasedEscaperMap fem = ArrayBasedEscaperMap.create(map);
    // Array length is highest character value + 1
    assertEquals('z' + 1, fem.getReplacementArray().length);
  }

  public void testMapping() {
    Map<Character, String> map =
        ImmutableMap.of(
            '\0', "zero",
            'a', "first",
            'b', "second",
            'z', "last",
            '\uFFFF', "biggest");
    ArrayBasedEscaperMap fem = ArrayBasedEscaperMap.create(map);
    char[][] replacementArray = fem.getReplacementArray();
    // Array length is highest character value + 1
    assertEquals(65536, replacementArray.length);
    // The final element should always be non-null.
    assertNotNull(replacementArray[replacementArray.length - 1]);
    // Exhaustively check all mappings (an int index avoids wrapping).
    for (int n = 0; n < replacementArray.length; ++n) {
      char c = (char) n;
      if (replacementArray[n] != null) {
        assertEquals(map.get(c), new String(replacementArray[n]));
      } else {
        assertFalse(map.containsKey(c));
      }
    }
  }
}
