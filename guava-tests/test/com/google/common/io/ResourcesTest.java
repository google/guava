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

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.NullPointerTester;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestSuite;

/**
 * Unit test for {@link Resources}.
 *
 * @author Chris Nokleberg
 */

public class ResourcesTest extends IoTestCase {

  @AndroidIncompatible // wouldn't run anyway, but strip the source entirely because of b/230620681
  public static TestSuite suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        ByteSourceTester.tests(
            "Resources.asByteSource[URL]", SourceSinkFactories.urlByteSourceFactory(), true));
    suite.addTest(
        CharSourceTester.tests(
            "Resources.asCharSource[URL, Charset]",
            SourceSinkFactories.urlCharSourceFactory(),
            false));
    suite.addTestSuite(ResourcesTest.class);
    return suite;
  }

  public void testToString() throws IOException {
    URL resource = getClass().getResource("testdata/i18n.txt");
    assertEquals(I18N, Resources.toString(resource, Charsets.UTF_8));
    assertThat(Resources.toString(resource, Charsets.US_ASCII)).isNotEqualTo(I18N);
  }

  public void testToByteArray() throws IOException {
    URL resource = getClass().getResource("testdata/i18n.txt");
    assertThat(Resources.toByteArray(resource)).isEqualTo(I18N.getBytes(Charsets.UTF_8));
  }

  public void testReadLines() throws IOException {
    // TODO(chrisn): Check in a better resource
    URL resource = getClass().getResource("testdata/i18n.txt");
    assertEquals(ImmutableList.of(I18N), Resources.readLines(resource, Charsets.UTF_8));
  }

  public void testReadLines_withLineProcessor() throws IOException {
    URL resource = getClass().getResource("testdata/alice_in_wonderland.txt");
    LineProcessor<List<String>> collectAndLowercaseAndTrim =
        new LineProcessor<List<String>>() {
          List<String> collector = new ArrayList<>();

          @Override
          public boolean processLine(String line) {
            collector.add(whitespace().trimFrom(line));
            return true;
          }

          @Override
          public List<String> getResult() {
            return collector;
          }
        };
    List<String> result =
        Resources.readLines(resource, Charsets.US_ASCII, collectAndLowercaseAndTrim);
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
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class, () -> Resources.getResource("no such resource"));
    assertThat(e).hasMessageThat().isEqualTo("resource no such resource not found.");
  }

  public void testGetResource() {
    assertNotNull(Resources.getResource("com/google/common/io/testdata/i18n.txt"));
  }

  public void testGetResource_relativePath_notFound() {
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> Resources.getResource(getClass(), "com/google/common/io/testdata/i18n.txt"));
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "resource com/google/common/io/testdata/i18n.txt"
                + " relative to com.google.common.io.ResourcesTest not found.");
  }

  public void testGetResource_relativePath() {
    assertNotNull(Resources.getResource(getClass(), "testdata/i18n.txt"));
  }

  public void testGetResource_contextClassLoader() throws IOException {
    // Check that we can find a resource if it is visible to the context class
    // loader, even if it is not visible to the loader of the Resources class.

    File tempFile = createTempFile();
    PrintWriter writer = new PrintWriter(tempFile, "UTF-8");
    writer.println("rud a chur ar an méar fhada");
    writer.close();

    // First check that we can't find it without setting the context loader.
    // This is a sanity check that the test doesn't spuriously pass because
    // the resource is visible to the system class loader.
    assertThrows(IllegalArgumentException.class, () -> Resources.getResource(tempFile.getName()));

    // Now set the context loader to one that should find the resource.
    URL baseUrl = tempFile.getParentFile().toURI().toURL();
    URLClassLoader loader = new URLClassLoader(new URL[] {baseUrl});
    ClassLoader oldContextLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(loader);
      URL url = Resources.getResource(tempFile.getName());
      String text = Resources.toString(url, Charsets.UTF_8);
      assertEquals("rud a chur ar an méar fhada" + System.lineSeparator(), text);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContextLoader);
    }
  }

  public void testGetResource_contextClassLoaderNull() {
    ClassLoader oldContextLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(null);
      assertNotNull(Resources.getResource("com/google/common/io/testdata/i18n.txt"));
      assertThrows(IllegalArgumentException.class, () -> Resources.getResource("no such resource"));
    } finally {
      Thread.currentThread().setContextClassLoader(oldContextLoader);
    }
  }

  @AndroidIncompatible // .class files aren't available
  public void testNulls() {
    new NullPointerTester()
        .setDefault(URL.class, classfile(ResourcesTest.class))
        .testAllPublicStaticMethods(Resources.class);
  }

  private static URL classfile(Class<?> c) {
    return c.getResource(c.getSimpleName() + ".class");
  }
}
