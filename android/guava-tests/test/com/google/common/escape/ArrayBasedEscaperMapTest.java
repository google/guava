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

import static com.google.common.escape.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * @author David Beaumont
 */
@GwtCompatible
@NullUnmarked
public class ArrayBasedEscaperMapTest extends TestCase {
  public void testNullMap() {
    assertThrows(NullPointerException.class, () -> ArrayBasedEscaperMap.create(null));
  }

  public void testEmptyMap() {
    Map<Character, String> map = ImmutableMap.of();
    ArrayBasedEscaperMap fem = ArrayBasedEscaperMap.create(map);
    // Non-null array of zero length.
    assertThat(fem.getReplacementArray()).isEmpty();
  }

  public void testMapLength() {
    Map<Character, String> map =
        ImmutableMap.of(
            'a', "first",
            'z', "last");
    ArrayBasedEscaperMap fem = ArrayBasedEscaperMap.create(map);
    // Array length is highest character value + 1
    assertThat(fem.getReplacementArray()).hasLength('z' + 1);
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
    assertThat(replacementArray).hasLength(65536);
    // The final element should always be non-null.
    assertThat(replacementArray[replacementArray.length - 1]).isNotNull();
    // Exhaustively check all mappings (an int index avoids wrapping).
    for (int n = 0; n < replacementArray.length; n++) {
      char c = (char) n;
      String expected = map.get(c);
      if (expected == null) {
        assertThat(replacementArray[n]).isNull();
      } else {
        assertThat(new String(replacementArray[n])).isEqualTo(expected);
      }
    }
  }
}
