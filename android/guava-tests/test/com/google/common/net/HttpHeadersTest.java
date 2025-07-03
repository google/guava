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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for the HttpHeaders class.
 *
 * @author Kurt Alfred Kluever
 */
@NullUnmarked
public class HttpHeadersTest extends TestCase {

  public void testConstantNameMatchesString() throws Exception {
    // Special case some of the weird HTTP Header names...
    ImmutableBiMap<String, String> specialCases =
        ImmutableBiMap.<String, String>builder()
            .put("CDN_LOOP", "CDN-Loop")
            .put("ETAG", "ETag")
            .put("SOURCE_MAP", "SourceMap")
            .put("SEC_CH_UA_WOW64", "Sec-CH-UA-WoW64")
            .put("SEC_WEBSOCKET_ACCEPT", "Sec-WebSocket-Accept")
            .put("SEC_WEBSOCKET_EXTENSIONS", "Sec-WebSocket-Extensions")
            .put("SEC_WEBSOCKET_KEY", "Sec-WebSocket-Key")
            .put("SEC_WEBSOCKET_PROTOCOL", "Sec-WebSocket-Protocol")
            .put("SEC_WEBSOCKET_VERSION", "Sec-WebSocket-Version")
            .put("X_WEBKIT_CSP", "X-WebKit-CSP")
            .put("X_WEBKIT_CSP_REPORT_ONLY", "X-WebKit-CSP-Report-Only")
            .buildOrThrow();
    ImmutableSet<String> uppercaseAcronyms =
        ImmutableSet.of(
            "CH", "ID", "DNT", "DNS", "DPR", "ECT", "GPC", "HTTP2", "IP", "MD5", "P3P", "RTT", "TE",
            "UA", "UID", "URL", "WWW", "XSS");

    for (Field field : httpHeadersFields()) {
      assertEquals(
          upperToHttpHeaderName(field.getName(), specialCases, uppercaseAcronyms), field.get(null));
    }
  }

  // Tests that there are no duplicate HTTP header names
  public void testNoDuplicateFields() throws Exception {
    ImmutableList.Builder<String> httpHeaders = ImmutableList.builder();
    for (Field field : httpHeadersFields()) {
      httpHeaders.add((String) field.get(null));
    }
    assertThat(httpHeaders.build()).containsNoDuplicates();
  }

  private static ImmutableSet<Field> httpHeadersFields() {
    ImmutableSet.Builder<Field> builder = ImmutableSet.builder();
    for (Field field : HttpHeaders.class.getDeclaredFields()) {
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
      String constantName,
      ImmutableBiMap<String, String> specialCases,
      ImmutableSet<String> uppercaseAcronyms) {
    if (specialCases.containsKey(constantName)) {
      return specialCases.get(constantName);
    }
    List<String> parts = new ArrayList<>();
    for (String part : Splitter.on('_').split(constantName)) {
      if (!uppercaseAcronyms.contains(part)) {
        part = part.charAt(0) + Ascii.toLowerCase(part.substring(1));
      }
      parts.add(part);
    }
    return Joiner.on('-').join(parts);
  }
}
