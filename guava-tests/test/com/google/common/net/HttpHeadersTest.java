/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.net;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Field;
import junit.framework.TestCase;

/**
 * Tests for the HttpHeaders class.
 *
 * @author Kurt Alfred Kluever
 */
public class HttpHeadersTest extends TestCase {

  public void testConstantNameMatchesString() throws Exception {
    // Special case some of the weird HTTP Header names...
    ImmutableBiMap<String, String> specialCases = ImmutableBiMap.of("ETAG", "ETag",
        "X_WEBKIT_CSP", "X-WebKit-CSP", "X_WEBKIT_CSP_REPORT_ONLY", "X-WebKit-CSP-Report-Only");
    assertConstantNameMatchesString(HttpHeaders.class, specialCases);
  }

  // Visible for other tests to use
  static void assertConstantNameMatchesString(
      Class<?> clazz, ImmutableBiMap<String, String> specialCases)
      throws IllegalAccessException {
    for (Field field : relevantFields(clazz)) {
      assertEquals(upperToHttpHeaderName(field.getName(), specialCases), field.get(null));
    }
  }

  // Visible for other tests to use
  static ImmutableSet<Field> relevantFields(Class<?> cls) {
    ImmutableSet.Builder<Field> builder = ImmutableSet.builder();
    for (Field field : cls.getDeclaredFields()) {
      /*
       * Coverage mode generates synthetic fields.  If we ever add private
       * fields, they will cause similar problems, and we may want to switch
       * this check to isAccessible().
       */
      if (!field.isSynthetic() && field.getType() == String.class) {
        builder.add(field);
      }
    }
    return builder.build();
  }

  private static String upperToHttpHeaderName(
      String constantName, ImmutableBiMap<String, String> specialCases) {
    if (specialCases.containsKey(constantName)) {
      return Ascii.toLowerCase(specialCases.get(constantName));
    }
    return Ascii.toLowerCase(constantName).replace('_', '-');
  }
}
