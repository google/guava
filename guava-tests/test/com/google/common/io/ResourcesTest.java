/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.base.CharMatcher.WHITESPACE;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link Resources}.
 *
 * @author Chris Nokleberg
 */
public class ResourcesTest extends IoTestCase {

  public void testUrlSupplier() throws IOException {
    try {
      Resources.newInputStreamSupplier(null);
      fail("expected NPE");
    } catch (NullPointerException expected) {
      // expected
    }

    URL url = getClass().getResource("/com/google/common/io/Resources.class");
    byte[] data = ByteStreams.toByteArray(
        Resources.newInputStreamSupplier(url));
    assertEquals(0xCAFEBABE,
        new DataInputStream(new ByteArrayInputStream(data)).readInt());
  }

  public void testToString() throws IOException {
    URL resource = getClass().getResource("testdata/i18n.txt");
    assertEquals(I18N, Resources.toString(resource, Charsets.UTF_8));
    ASSERT.that(Resources.toString(resource, Charsets.US_ASCII))
        .isNotEqualTo(I18N);
  }
  
  public void testToToByteArray() throws IOException {
    URL url = getClass().getResource("/com/google/common/io/Resources.class");
    byte[] data = Resources.toByteArray(url);
    assertEquals(0xCAFEBABE,
        new DataInputStream(new ByteArrayInputStream(data)).readInt());
  }

  public void testReadLines() throws IOException {
    // TODO(chrisn): Check in a better resource
    URL resource = getClass().getResource("testdata/i18n.txt");
    assertEquals(ImmutableList.of(I18N),
        Resources.readLines(resource, Charsets.UTF_8));
  }

  public void testReadLines_withLineProcessor() throws IOException {
    URL resource = getClass().getResource("testdata/alice_in_wonderland.txt");
    LineProcessor<List<String>> collectAndLowercaseAndTrim =
        new LineProcessor<List<String>>() {
          List<String> collector = new ArrayList<String>();
          @Override
          public boolean processLine(String line) {
            collector.add(WHITESPACE.trimFrom(line));
            return true;
          }

          @Override
          public List<String> getResult() {
            return collector;
          }
        };
    List<String> result = Resources.readLines(resource, Charsets.US_ASCII,
        collectAndLowercaseAndTrim);
    assertEquals(3600, result.size());
    assertEquals("ALICE'S ADVENTURES IN WONDERLAND", result.get(0));
    assertEquals("THE END", result.get(result.size() - 1));
  }

  public void testCopyToOutputStream() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    URL resource = getClass().getResource("testdata/i18n.txt");
    Resources.copy(resource, out);
    assertEquals(I18N, out.toString("UTF-8"));
  }
  
  public void testGetResource_notFound() {
    try {
      Resources.getResource("no such resource");
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("resource no such resource not found.", e.getMessage());
    }
  }
  
  public void testGetResource() {
    assertNotNull(
        Resources.getResource("com/google/common/io/testdata/i18n.txt"));
  }
  
  public void testGetResource_relativePath_notFound() {
    try {
      Resources.getResource(
          getClass(), "com/google/common/io/testdata/i18n.txt");
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("resource com/google/common/io/testdata/i18n.txt" +
          " relative to com.google.common.io.ResourcesTest not found.",
          e.getMessage());
    }
  }
  
  public void testGetResource_relativePath() {
    assertNotNull(Resources.getResource(getClass(), "testdata/i18n.txt"));
  }
}
