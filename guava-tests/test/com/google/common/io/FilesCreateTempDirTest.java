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

import static com.google.common.base.StandardSystemProperty.JAVA_IO_TMPDIR;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import junit.framework.TestCase;

/**
 * Unit test for {@link Files#createTempDir}.
 *
 * @author Chris Nokleberg
 */

@SuppressWarnings("deprecation") // tests of a deprecated method
public class FilesCreateTempDirTest extends TestCase {
  public void testCreateTempDir() throws IOException {
    if (JAVA_IO_TMPDIR.value().equals("/sdcard")) {
      assertThrows(IllegalStateException.class, Files::createTempDir);
      return;
    }
    File temp = Files.createTempDir();
    try {
      assertTrue(temp.exists());
      assertTrue(temp.isDirectory());
      assertThat(temp.listFiles()).isEmpty();

      if (isAndroid()) {
        return;
      }
      PosixFileAttributes attributes =
          java.nio.file.Files.getFileAttributeView(temp.toPath(), PosixFileAttributeView.class)
              .readAttributes();
      assertThat(attributes.permissions()).containsExactly(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE);
    } finally {
      assertTrue(temp.delete());
    }
  }

  private static boolean isAndroid() {
    return System.getProperty("java.runtime.name", "").contains("Android");
  }
}
