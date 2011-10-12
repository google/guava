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

import junit.framework.TestCase;

import java.lang.reflect.Field;

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

  private static String upperToHttpHeaderName(String constantName) {
    // Special case some of the weird HTTP Header names...
    if (constantName.equals("TE")) {
      return "TE";
    } else if (constantName.equals("CONTENT_MD5")) {
      return "Content-MD5";
    } else if (constantName.equals("ETAG")) {
      return "ETag";
    } else if (constantName.equals("P3P")) {
      return "P3P";
    } else if (constantName.equals("WWW_AUTHENTICATE")) {
      return "WWW-Authenticate";
    } else if (constantName.equals("X_XSS_PROTECTION")) {
      return "X-XSS-Protection";
    } else if (constantName.equals("X_USER_IP")) {
      return "X-User-IP";
    } else if (constantName.equals("DNT")) {
      return "DNT";
    } else if (constantName.equals("LAST_EVENT_ID")) {
      return "Last-Event-ID";
    }

    boolean toLower = false;
    StringBuilder builder = new StringBuilder(constantName.length());
    for (char c : constantName.replace('_', '-').toCharArray()) {
      builder.append(toLower ? Ascii.toLowerCase(c) : c);
      toLower = (c != '-');
    }
    return builder.toString();
  }
}
