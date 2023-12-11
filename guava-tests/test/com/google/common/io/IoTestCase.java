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

package com.google.common.io;

import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base test case class for I/O tests.
 *
 * @author Chris Nokleberg
 * @author Colin Decker
 */
public abstract class IoTestCase extends TestCase {

  private static final Logger logger = Logger.getLogger(IoTestCase.class.getName());

  static final String I18N =
      "\u00CE\u00F1\u0163\u00E9\u0072\u00F1\u00E5\u0163\u00EE\u00F6"
          + "\u00F1\u00E5\u013C\u00EE\u017E\u00E5\u0163\u00EE\u00F6\u00F1";

  static final String ASCII =
      " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ"
          + "[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

  private File testDir;
  private File tempDir;

  private final Set<File> filesToDelete = Sets.newHashSet();

  @Override
  protected void tearDown() {
    for (File file : filesToDelete) {
      if (file.exists()) {
        delete(file);
      }
    }
    filesToDelete.clear();
  }

  private File getTestDir() throws IOException {
    if (testDir != null) {
      return testDir;
    }

    URL testFileUrl = IoTestCase.class.getResource("testdata/i18n.txt");
    if (testFileUrl == null) {
      throw new RuntimeException("unable to locate testdata directory");
    }

    if (testFileUrl.getProtocol().equals("file")) {
      try {
        File testFile = new File(testFileUrl.toURI());
        testDir = testFile.getParentFile(); // the testdata directory
      } catch (Exception ignore) {
        // probably URISyntaxException or IllegalArgumentException
        // fall back to copying URLs to files in the testDir == null block below
      }
    }

    if (testDir == null) {
      // testdata resources aren't file:// urls, so create a directory to store them in and then
      // copy the resources to the filesystem as needed
      testDir = createTempDir();
    }

    return testDir;
  }

  /** Returns the file with the given name under the testdata directory. */
  protected final @Nullable File getTestFile(String name) throws IOException {
    File file = new File(getTestDir(), name);
    if (!file.exists()) {
      URL resourceUrl = IoTestCase.class.getResource("testdata/" + name);
      if (resourceUrl == null) {
        return null;
      }
      copy(resourceUrl, file);
    }

    return file;
  }

  /**
   * Creates a new temp dir for testing. The returned directory and all contents of it will be
   * deleted in the tear-down for this test.
   */
  protected final File createTempDir() throws IOException {
    File tempFile = File.createTempFile("IoTestCase", "");
    if (!tempFile.delete() || !tempFile.mkdir()) {
      throw new IOException("failed to create temp dir");
    }
    filesToDelete.add(tempFile);
    return tempFile;
  }

  /**
   * Gets a temp dir for testing. The returned directory and all contents of it will be deleted in
   * the tear-down for this test. Subsequent invocations of this method will return the same
   * directory.
   */
  protected final File getTempDir() throws IOException {
    if (tempDir == null) {
      tempDir = createTempDir();
    }

    return tempDir;
  }

  /**
   * Creates a new temp file in the temp directory returned by {@link #getTempDir()}. The file will
   * be deleted in the tear-down for this test.
   */
  protected final File createTempFile() throws IOException {
    return File.createTempFile("test", null, getTempDir());
  }

  /** Returns a byte array of length size that has values 0 .. size - 1. */
  static byte[] newPreFilledByteArray(int size) {
    return newPreFilledByteArray(0, size);
  }

  /** Returns a byte array of length size that has values offset .. offset + size - 1. */
  static byte[] newPreFilledByteArray(int offset, int size) {
    byte[] array = new byte[size];
    for (int i = 0; i < size; i++) {
      array[i] = (byte) (offset + i);
    }
    return array;
  }

  private static void copy(URL url, File file) throws IOException {
    InputStream in = url.openStream();
    try {
      OutputStream out = new FileOutputStream(file);
      try {
        byte[] buf = new byte[4096];
        for (int read = in.read(buf); read != -1; read = in.read(buf)) {
          out.write(buf, 0, read);
        }
      } finally {
        out.close();
      }
    } finally {
      in.close();
    }
  }

  @CanIgnoreReturnValue
  private boolean delete(File file) {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File f : files) {
          if (!delete(f)) {
            return false;
          }
        }
      }
    }

    if (!file.delete()) {
      logger.log(Level.WARNING, "couldn't delete file: {0}", new Object[] {file});
      return false;
    }

    return true;
  }
}
