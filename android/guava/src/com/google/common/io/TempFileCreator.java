/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.io;

import static com.google.common.base.StandardSystemProperty.JAVA_IO_TMPDIR;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.j2objc.annotations.J2ObjCIncompatible;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Creates temporary files and directories whose permissions are restricted to the current user or,
 * in the case of Android, the current app. If that is not possible (as is the case under the very
 * old Android Ice Cream Sandwich release), then this class throws an exception instead of creating
 * a file or directory that would be more accessible.
 */
@J2ktIncompatible
@GwtIncompatible
@J2ObjCIncompatible
@ElementTypesAreNonnullByDefault
abstract class TempFileCreator {
  static final TempFileCreator INSTANCE = pickSecureCreator();

  /**
   * @throws IllegalStateException if the directory could not be created (to implement the contract
   *     of {@link Files#createTempDir()}
   * @throws UnsupportedOperationException if the system does not support creating temporary
   *     directories securely
   */
  abstract File createTempDir();

  abstract File createTempFile(String prefix) throws IOException;

  private static TempFileCreator pickSecureCreator() {
    try {
      Class.forName("java.nio.file.Path");
      return new JavaNioCreator();
    } catch (ClassNotFoundException runningUnderAndroid) {
      // Try another way.
    }

    try {
      int version = (int) Class.forName("android.os.Build$VERSION").getField("SDK_INT").get(null);
      int jellyBean =
          (int) Class.forName("android.os.Build$VERSION_CODES").getField("JELLY_BEAN").get(null);
      /*
       * I assume that this check can't fail because JELLY_BEAN will be present only if we're
       * running under Jelly Bean or higher. But it seems safest to check.
       */
      if (version < jellyBean) {
        return new ThrowingCreator();
      }

      // Don't merge these catch() blocks, let alone use ReflectiveOperationException directly:
      // b/65343391
    } catch (NoSuchFieldException e) {
      // The JELLY_BEAN field doesn't exist because we're running on a version before Jelly Bean :)
      return new ThrowingCreator();
    } catch (ClassNotFoundException e) {
      // Should be impossible, but we want to return *something* so that class init succeeds.
      return new ThrowingCreator();
    } catch (IllegalAccessException e) {
      // ditto
      return new ThrowingCreator();
    }

    // Android isolates apps' temporary directories since Jelly Bean:
    // https://github.com/google/guava/issues/4011#issuecomment-770020802
    // So we can create files there with any permissions and still get security from the isolation.
    return new JavaIoCreator();
  }

  @IgnoreJRERequirement // used only when Path is available
  private static final class JavaNioCreator extends TempFileCreator {
    private static final FileAttribute<Set<PosixFilePermission>> RWX_USER_ONLY =
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
    private static final FileAttribute<Set<PosixFilePermission>> RW_USER_ONLY =
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    @Override
    File createTempDir() {
      try {
        return java.nio.file.Files.createTempDirectory(
                Paths.get(JAVA_IO_TMPDIR.value()), /* prefix= */ null, RWX_USER_ONLY)
            .toFile();
      } catch (IOException e) {
        throw new IllegalStateException("Failed to create directory", e);
      }
    }

    @Override
    File createTempFile(String prefix) throws IOException {
      return java.nio.file.Files.createTempFile(
              Paths.get(JAVA_IO_TMPDIR.value()),
              /* prefix= */ prefix,
              /* suffix= */ null,
              RW_USER_ONLY)
          .toFile();
    }
  }

  private static final class JavaIoCreator extends TempFileCreator {
    @Override
    File createTempDir() {
      File baseDir = new File(JAVA_IO_TMPDIR.value());
      @SuppressWarnings("GoodTime") // reading system time without TimeSource
      String baseName = System.currentTimeMillis() + "-";

      for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
        File tempDir = new File(baseDir, baseName + counter);
        if (tempDir.mkdir()) {
          return tempDir;
        }
      }
      throw new IllegalStateException(
          "Failed to create directory within "
              + TEMP_DIR_ATTEMPTS
              + " attempts (tried "
              + baseName
              + "0 to "
              + baseName
              + (TEMP_DIR_ATTEMPTS - 1)
              + ')');
    }

    @Override
    File createTempFile(String prefix) throws IOException {
      return File.createTempFile(
          /* prefix= */ prefix,
          /* suffix= */ null,
          /* directory= */ null /* defaults to java.io.tmpdir */);
    }

    /** Maximum loop count when creating temp directories. */
    private static final int TEMP_DIR_ATTEMPTS = 10000;
  }

  private static final class ThrowingCreator extends TempFileCreator {
    private static final String MESSAGE =
        "Guava cannot securely create temporary files or directories under SDK versions before"
            + " Jelly Bean. You can create one yourself, either in the insecure default directory"
            + " or in a more secure directory, such as context.getCacheDir(). For more information,"
            + " see the Javadoc for Files.createTempDir().";

    @Override
    File createTempDir() {
      throw new IllegalStateException(MESSAGE);
    }

    @Override
    File createTempFile(String prefix) throws IOException {
      throw new IOException(MESSAGE);
    }
  }

  private TempFileCreator() {}
}
