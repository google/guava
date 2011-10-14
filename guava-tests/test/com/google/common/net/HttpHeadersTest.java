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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Tests for the HttpHeaders class.
 *
 * @author Kurt Aflred Kluever
 */
public class HttpHeadersTest extends TestCase {
  public void testConstantNameMatchesString() throws Exception {
    for (Field field : HttpHeaders.class.getDeclaredFields()) {
      /*
       * Coverage mode generates synthetic fields.  If we ever add private
       * fields, they will cause similar problems, and we may want to switch
       * this check to isAccessible().
       */
      if (!field.isSynthetic()) {
        assertEquals(upperToHttpHeaderName(field.getName()), field.get(null));
      }
    }
  }

  private static final ImmutableSet<String> UPPERCASE_ACRONYMS = ImmutableSet.of(
      "ID", "DNT", "GFE", "IP", "MD5", "P3P", "TE", "UID", "URL", "WWW", "XSS");

  private static final Splitter SPLITTER = Splitter.on('_');
  private static final Joiner JOINER = Joiner.on('-');

  private static String upperToHttpHeaderName(String constantName) {
    // Special case some of the weird HTTP Header names...
    if (constantName.equals("ETAG")) {
      return "ETag";
    }

    List<String> parts = Lists.newArrayList();
    for (String part : SPLITTER.split(constantName)) {
      if (!UPPERCASE_ACRONYMS.contains(part)) {
        part = part.charAt(0) + Ascii.toLowerCase(part.substring(1));
      }
      parts.add(part);
    }
    return JOINER.join(parts);
  }
}
