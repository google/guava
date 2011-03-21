/*
 * Copyright (C) 2007 The Guava Authors
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

import junit.framework.TestCase;

import java.nio.charset.Charset;

/**
 * Unit test for {@link Charsets}.
 *
 * @author Mike Bostock
 */
public class CharsetsTest extends TestCase {
  public void testUsAscii() {
    assertEquals(Charset.forName("US-ASCII"), Charsets.US_ASCII);
  }

  public void testIso88591() {
    assertEquals(Charset.forName("ISO-8859-1"), Charsets.ISO_8859_1);
  }

  public void testUtf8() {
    assertEquals(Charset.forName("UTF-8"), Charsets.UTF_8);
  }

  public void testUtf16be() {
    assertEquals(Charset.forName("UTF-16BE"), Charsets.UTF_16BE);
  }

  public void testUtf16le() {
    assertEquals(Charset.forName("UTF-16LE"), Charsets.UTF_16LE);
  }

  public void testUtf16() {
    assertEquals(Charset.forName("UTF-16"), Charsets.UTF_16);
  }
}
