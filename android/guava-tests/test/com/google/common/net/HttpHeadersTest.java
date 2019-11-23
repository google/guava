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
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.lang.reflect.Field;
import java.util.List;
import junit.framework.TestCase;

/**
 * Tests for the HttpHeaders class.
 *
 * @author Kurt Alfred Kluever
 */
public class HttpHeadersTest extends TestCase {

  public void testConstantNameMatchesString() throws Exception {
    // Special case some of the weird HTTP Header names...
    ImmutableBiMap<String, String> specialCases =
        ImmutableBiMap.<String, String>builder()
            .put("CDN_LOOP", "CDN-Loop")
            .put("ETAG", "ETag")
            .put("SOURCE_MAP", "SourceMap")
            .put("SEC_WEBSOCKET_ACCEPT", "Sec-WebSocket-Accept")
            .put("SEC_WEBSOCKET_EXTENSIONS", "Sec-WebSocket-Extensions")
            .put("SEC_WEBSOCKET_KEY", "Sec-WebSocket-Key")
            .put("SEC_WEBSOCKET_PROTOCOL", "Sec-WebSocket-Protocol")
            .put("SEC_WEBSOCKET_VERSION", "Sec-WebSocket-Version")
            .put("X_WEBKIT_CSP", "X-WebKit-CSP")
            .put("X_WEBKIT_CSP_REPORT_ONLY", "X-WebKit-CSP-Report-Only")
            .build();
    ImmutableSet<String> uppercaseAcronyms =
        ImmutableSet.of(
            "ID", "DNT", "DNS", "HTTP2", "IP", "MD5", "P3P", "TE", "UID", "URL", "WWW", "XSS");
    assertConstantNameMatchesString(HttpHeaders.class, specialCases, uppercaseAcronyms);
  }

  // Visible for other tests to use
  static void assertConstantNameMatchesString(
      Class<?> clazz,
      ImmutableBiMap<String, String> specialCases,
      ImmutableSet<String> uppercaseAcronyms)
      throws IllegalAccessException {
    for (Field field : relevantFields(clazz)) {
      assertEquals(
          upperToHttpHeaderName(field.getName(), specialCases, uppercaseAcronyms), field.get(null));
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

  private static final Splitter SPLITTER = Splitter.on('_');
  private static final Joiner JOINER = Joiner.on('-');

  private static String upperToHttpHeaderName(
      String constantName,
      ImmutableBiMap<String, String> specialCases,
      ImmutableSet<String> uppercaseAcronyms) {
    if (specialCases.containsKey(constantName)) {
      return specialCases.get(constantName);
    }
    List<String> parts = Lists.newArrayList();
    for (String part : SPLITTER.split(constantName)) {
      if (!uppercaseAcronyms.contains(part)) {
        part = part.charAt(0) + Ascii.toLowerCase(part.substring(1));
      }
      parts.add(part);
    }
    return JOINER.join(parts);
  }
}
