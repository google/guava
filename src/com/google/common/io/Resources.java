/*
 * Copyright (C) 2007 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Provides utility methods for working with resources in the classpath.
 * Note that even those these methods use {@link URL} parameters, they
 * are usually not appropriate for HTTP or other non-classpath resources.
 *
 * <p>All method parameters must be non-null unless documented otherwise.
 *
 * @author Chris Nokleberg
 * @author Ben Yu
 * @since 2009.09.15 <b>tentative</b>
 */
public final class Resources {

  /**
   * Returns a factory that will supply instances of {@link InputStream} that
   * read from the given URL.
   *
   * @param url the URL to read from
   * @return the factory
   */
  public static InputSupplier<InputStream> newInputStreamSupplier(
      final URL url) {
    checkNotNull(url);
    return new InputSupplier<InputStream>() {
      public InputStream getInput() throws IOException {
        return url.openStream();
      }
    };
  }

  /**
   * Returns a factory that will supply instances of
   * {@link InputStreamReader} that read a URL using the given character set.
   *
   * @param url the URL to read from
   * @param charset the character set used when reading the URL contents
   * @return the factory
   */
  public static InputSupplier<InputStreamReader> newReaderSupplier(
      URL url, Charset charset) {
    return CharStreams.newReaderSupplier(newInputStreamSupplier(url), charset);
  }

  /**
   * Reads all bytes from a URL into a byte array.
   *
   * @param url the URL to read from
   * @return a byte array containing all the bytes from the URL
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(URL url) throws IOException {
    return ByteStreams.toByteArray(newInputStreamSupplier(url));
  }

  /**
   * Reads all characters from a URL into a {@link String}, using the given
   * character set.
   *
   * @param url the URL to read from
   * @param charset the character set used when reading the URL
   * @return a string containing all the characters from the URL
   * @throws IOException if an I/O error occurs.
   */
  public static String toString(URL url, Charset charset) throws IOException {
    return CharStreams.toString(newReaderSupplier(url, charset));
  }

  /**
   * Streams lines from a URL, stopping when our callback returns false, or we
   * have read all of the lines.
   *
   * @param url the URL to read from
   * @param charset the character set used when reading the URL
   * @param callback the LineProcessor to use to handle the lines
   * @return the output of processing the lines
   * @throws IOException if an I/O error occurs
   */
  public static <T> T readLines(URL url, Charset charset,
      LineProcessor<T> callback) throws IOException {
    return CharStreams.readLines(newReaderSupplier(url, charset), callback);
  }

  /**
   * Reads all of the lines from a URL. The lines do not include
   * line-termination characters, but do include other leading and trailing
   * whitespace.
   *
   * @param url the URL to read from
   * @param charset the character set used when writing the file
   * @return a mutable {@link List} containing all the lines
   * @throws IOException if an I/O error occurs
   */
  public static List<String> readLines(URL url, Charset charset)
      throws IOException {
    return CharStreams.readLines(newReaderSupplier(url, charset));
  }

  /**
   * Copies all bytes from a URL to an output stream.
   *
   * @param from the URL to read from
   * @param to the output stream
   * @throws IOException if an I/O error occurs
   */
  public static void copy(URL from, OutputStream to) throws IOException {
    ByteStreams.copy(newInputStreamSupplier(from), to);
  }
  
  /**
   * Returns a {@code URL} pointing to {@code resourceName} if the resource is
   * found in the class path. {@code Resources.class.getClassLoader()} is used
   * to locate the resource.
   * 
   * @throws IllegalArgumentException if resource is not found
   */
  public static URL getResource(String resourceName) {
    URL url = Resources.class.getClassLoader().getResource(resourceName);
    checkArgument(url != null, "resource %s not found.", resourceName);
    return url;
  }

  /**
   * Returns a {@code URL} pointing to {@code resourceName} that is relative to
   * {@code contextClass}, if the resource is found in the class path. 
   * 
   * @throws IllegalArgumentException if resource is not found
   */
  public static URL getResource(Class<?> contextClass, String resourceName) {
    URL url = contextClass.getResource(resourceName);
    checkArgument(url != null, "resource %s relative to %s not found.",
        resourceName, contextClass.getName());
    return url;
  }
}
