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
import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;
import static com.google.common.base.StandardSystemProperty.OS_NAME;
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
      assertThat(temp.exists()).isTrue();
      assertThat(temp.isDirectory()).isTrue();
      assertThat(temp.listFiles()).isEmpty();
      File child = new File(temp, "child");
      assertThat(child.createNewFile()).isTrue();
      assertThat(child.delete()).isTrue();

      if (!isAndroid() && !isWindows()) {
        PosixFileAttributes attributes =
            java.nio.file.Files.getFileAttributeView(temp.toPath(), PosixFileAttributeView.class)
                .readAttributes();
        assertThat(attributes.permissions())
            .containsExactly(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE);
      }
    } finally {
      assertThat(temp.delete()).isTrue();
    }
  }

  public void testBogusSystemPropertiesUsername() {
    /*
     * Only under Windows (or hypothetically when running with some other non-POSIX, ACL-based
     * filesystem) do we look up the username. Thus, this test doesn't test anything interesting
     * under most environments.
     *
     * Under Windows, we test that:
     *
     * - Under Java 9+, createTempDir() succeeds because it can look up the *real* username, rather
     * than relying on the one from the system property.
     *
     * - Under Java 8, createTempDir() fails because it falls back to the bogus username from the
     * system property.
     *
     * However: Note that we don't actually run our CI on Windows under Java 8, at least as of this
     * writing.
     */
    boolean isJava8OnWindows = JAVA_SPECIFICATION_VERSION.value().equals("1.8") && isWindows();

    String save = System.getProperty("user.name");
    System.setProperty("user.name", "-this-is-definitely-not-the-username-we-are-running-as//?");
    try {
      TempFileCreator.testMakingUserPermissionsFromScratch();
      assertThat(isJava8OnWindows).isFalse();
    } catch (IOException expectedIfJavaWindows8) {
      assertThat(isJava8OnWindows).isTrue();
    } finally {
      System.setProperty("user.name", save);
    }
  }

  private static boolean isAndroid() {
    return System.getProperty("java.runtime.name", "").contains("Android");
  }

  private static boolean isWindows() {
    return OS_NAME.value().startsWith("Windows");
  }
}
