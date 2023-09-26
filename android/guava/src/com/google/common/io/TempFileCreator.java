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
import static com.google.common.base.StandardSystemProperty.USER_NAME;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static java.nio.file.attribute.AclEntryFlag.DIRECTORY_INHERIT;
import static java.nio.file.attribute.AclEntryFlag.FILE_INHERIT;
import static java.nio.file.attribute.AclEntryType.ALLOW;
import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.j2objc.annotations.J2ObjCIncompatible;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
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
   *     of {@link Files#createTempDir()}, such as if the system does not support creating temporary
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

  /**
   * Creates the permissions normally used for Windows filesystems, looking up the user afresh, even
   * if previous calls have initialized the {@code PermissionSupplier} fields.
   *
   * <p>This lets us test the effects of different values of the {@code user.name} system property
   * without needing a separate VM or classloader.
   */
  @IgnoreJRERequirement // used only when Path is available (and only from tests)
  @VisibleForTesting
  static void testMakingUserPermissionsFromScratch() throws IOException {
    // All we're testing is whether it throws.
    FileAttribute<?> unused = JavaNioCreator.userPermissions().get();
  }

  @IgnoreJRERequirement // used only when Path is available
  private static final class JavaNioCreator extends TempFileCreator {
    @Override
    File createTempDir() {
      try {
        return java.nio.file.Files.createTempDirectory(
                Paths.get(JAVA_IO_TMPDIR.value()), /* prefix= */ null, directoryPermissions.get())
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
              filePermissions.get())
          .toFile();
    }

    @IgnoreJRERequirement // see enclosing class (whose annotation Animal Sniffer ignores here...)
    private interface PermissionSupplier {
      FileAttribute<?> get() throws IOException;
    }

    private static final PermissionSupplier filePermissions;
    private static final PermissionSupplier directoryPermissions;

    static {
      Set<String> views = FileSystems.getDefault().supportedFileAttributeViews();
      if (views.contains("posix")) {
        filePermissions = () -> asFileAttribute(PosixFilePermissions.fromString("rw-------"));
        directoryPermissions = () -> asFileAttribute(PosixFilePermissions.fromString("rwx------"));
      } else if (views.contains("acl")) {
        filePermissions = directoryPermissions = userPermissions();
      } else {
        filePermissions =
            directoryPermissions =
                () -> {
                  throw new IOException("unrecognized FileSystem type " + FileSystems.getDefault());
                };
      }
    }

    private static PermissionSupplier userPermissions() {
      try {
        UserPrincipal user =
            FileSystems.getDefault()
                .getUserPrincipalLookupService()
                .lookupPrincipalByName(getUsername());
        ImmutableList<AclEntry> acl =
            ImmutableList.of(
                AclEntry.newBuilder()
                    .setType(ALLOW)
                    .setPrincipal(user)
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                    .setFlags(DIRECTORY_INHERIT, FILE_INHERIT)
                    .build());
        FileAttribute<ImmutableList<AclEntry>> attribute =
            new FileAttribute<ImmutableList<AclEntry>>() {
              @Override
              public String name() {
                return "acl:acl";
              }

              @Override
              public ImmutableList<AclEntry> value() {
                return acl;
              }
            };
        return () -> attribute;
      } catch (IOException e) {
        // We throw a new exception each time so that the stack trace is right.
        return () -> {
          throw new IOException("Could not find user", e);
        };
      }
    }

    private static String getUsername() {
      /*
       * https://github.com/google/guava/issues/6634: ProcessHandle has more accurate information,
       * but that class isn't available under all environments that we support. We use it if
       * available and fall back if not.
       */
      String fromSystemProperty = requireNonNull(USER_NAME.value());

      try {
        Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
        Class<?> processHandleInfoClass = Class.forName("java.lang.ProcessHandle$Info");
        Class<?> optionalClass = Class.forName("java.util.Optional");
        /*
         * We don't *need* to use reflection to access Optional: It's available on all JDKs we
         * support, and Android code won't get this far, anyway, because ProcessHandle is
         * unavailable. But given how much other reflection we're using, we might as well use it
         * here, too, so that we don't need to also suppress an AndroidApiChecker error.
         */

        Method currentMethod = processHandleClass.getMethod("current");
        Method infoMethod = processHandleClass.getMethod("info");
        Method userMethod = processHandleInfoClass.getMethod("user");
        Method orElseMethod = optionalClass.getMethod("orElse", Object.class);

        Object current = currentMethod.invoke(null);
        Object info = infoMethod.invoke(current);
        Object user = userMethod.invoke(info);
        return (String) requireNonNull(orElseMethod.invoke(user, fromSystemProperty));
      } catch (ClassNotFoundException runningUnderAndroidOrJava8) {
        /*
         * I'm not sure that we could actually get here for *Android*: I would expect us to enter
         * the POSIX code path instead. And if we tried this code path, we'd have trouble unless we
         * were running under a new enough version of Android to support NIO.
         *
         * So this is probably just the "Windows Java 8" case. In that case, if we wanted *another*
         * layer of fallback before consulting the system property, we could try
         * com.sun.security.auth.module.NTSystem.
         *
         * But for now, we use the value from the system property as our best guess.
         */
        return fromSystemProperty;
      } catch (InvocationTargetException e) {
        throwIfUnchecked(e.getCause()); // in case it's an Error or something
        return fromSystemProperty; // should be impossible
      } catch (NoSuchMethodException shouldBeImpossible) {
        return fromSystemProperty;
      } catch (IllegalAccessException shouldBeImpossible) {
        /*
         * We don't merge these into `catch (ReflectiveOperationException ...)` or an equivalent
         * multicatch because ReflectiveOperationException isn't available under Android:
         * b/124188803
         */
        return fromSystemProperty;
      }
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
