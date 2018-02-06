/*
 * Copyright (C) 2013 The Guava Authors
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

import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;
import static com.google.common.jimfs.Feature.SECURE_DIRECTORY_STREAM;
import static com.google.common.jimfs.Feature.SYMBOLIC_LINKS;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import com.google.common.collect.ObjectArrays;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Feature;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@link MoreFiles}.
 *
 * <p>Note: {@link MoreFiles#fileTraverser()} is tested in {@link MoreFilesFileTraverserTest}.
 *
 * @author Colin Decker
 */

public class MoreFilesTest extends TestCase {

  public static TestSuite suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        ByteSourceTester.tests(
            "MoreFiles.asByteSource[Path]", SourceSinkFactories.pathByteSourceFactory(), true));
    suite.addTest(
        ByteSinkTester.tests(
            "MoreFiles.asByteSink[Path]", SourceSinkFactories.pathByteSinkFactory()));
    suite.addTest(
        ByteSinkTester.tests(
            "MoreFiles.asByteSink[Path, APPEND]",
            SourceSinkFactories.appendingPathByteSinkFactory()));
    suite.addTest(
        CharSourceTester.tests(
            "MoreFiles.asCharSource[Path, Charset]",
            SourceSinkFactories.pathCharSourceFactory(),
            false));
    suite.addTest(
        CharSinkTester.tests(
            "MoreFiles.asCharSink[Path, Charset]", SourceSinkFactories.pathCharSinkFactory()));
    suite.addTest(
        CharSinkTester.tests(
            "MoreFiles.asCharSink[Path, Charset, APPEND]",
            SourceSinkFactories.appendingPathCharSinkFactory()));
    suite.addTestSuite(MoreFilesTest.class);
    return suite;
  }

  private static final FileSystem FS = FileSystems.getDefault();

  private static Path root() {
    return FS.getRootDirectories().iterator().next();
  }

  private Path tempDir;

  @Override
  protected void setUp() throws Exception {
    tempDir = Files.createTempDirectory("MoreFilesTest");
  }

  @Override
  protected void tearDown() throws Exception {
    if (tempDir != null) {
      // delete tempDir and its contents
      Files.walkFileTree(
          tempDir,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              Files.deleteIfExists(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              if (exc != null) {
                return FileVisitResult.TERMINATE;
              }
              Files.deleteIfExists(dir);
              return FileVisitResult.CONTINUE;
            }
          });
    }
  }

  private Path createTempFile() throws IOException {
    return Files.createTempFile(tempDir, "test", ".test");
  }

  public void testByteSource_size_ofDirectory() throws IOException {
    try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path dir = fs.getPath("dir");
      Files.createDirectory(dir);

      ByteSource source = MoreFiles.asByteSource(dir);

      assertThat(source.sizeIfKnown()).isAbsent();

      try {
        source.size();
        fail();
      } catch (IOException expected) {
      }
    }
  }

  public void testByteSource_size_ofSymlinkToDirectory() throws IOException {
    try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path dir = fs.getPath("dir");
      Files.createDirectory(dir);
      Path link = fs.getPath("link");
      Files.createSymbolicLink(link, dir);

      ByteSource source = MoreFiles.asByteSource(link);

      assertThat(source.sizeIfKnown()).isAbsent();

      try {
        source.size();
        fail();
      } catch (IOException expected) {
      }
    }
  }

  public void testByteSource_size_ofSymlinkToRegularFile() throws IOException {
    try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path file = fs.getPath("file");
      Files.write(file, new byte[10]);
      Path link = fs.getPath("link");
      Files.createSymbolicLink(link, file);

      ByteSource source = MoreFiles.asByteSource(link);

      assertEquals(10L, (long) source.sizeIfKnown().get());
      assertEquals(10L, source.size());
    }
  }

  public void testByteSource_size_ofSymlinkToRegularFile_nofollowLinks() throws IOException {
    try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path file = fs.getPath("file");
      Files.write(file, new byte[10]);
      Path link = fs.getPath("link");
      Files.createSymbolicLink(link, file);

      ByteSource source = MoreFiles.asByteSource(link, NOFOLLOW_LINKS);

      assertThat(source.sizeIfKnown()).isAbsent();

      try {
        source.size();
        fail();
      } catch (IOException expected) {
      }
    }
  }

  public void testEqual() throws IOException {
    try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path fooPath = fs.getPath("foo");
      Path barPath = fs.getPath("bar");
      MoreFiles.asCharSink(fooPath, UTF_8).write("foo");
      MoreFiles.asCharSink(barPath, UTF_8).write("barbar");

      assertThat(MoreFiles.equal(fooPath, barPath)).isFalse();
      assertThat(MoreFiles.equal(fooPath, fooPath)).isTrue();
      assertThat(MoreFiles.asByteSource(fooPath).contentEquals(MoreFiles.asByteSource(fooPath)))
          .isTrue();

      Path fooCopy = Files.copy(fooPath, fs.getPath("fooCopy"));
      assertThat(Files.isSameFile(fooPath, fooCopy)).isFalse();
      assertThat(MoreFiles.equal(fooPath, fooCopy)).isTrue();

      MoreFiles.asCharSink(fooCopy, UTF_8).write("boo");
      assertThat(MoreFiles.asByteSource(fooPath).size())
          .isEqualTo(MoreFiles.asByteSource(fooCopy).size());
      assertThat(MoreFiles.equal(fooPath, fooCopy)).isFalse();

      // should also assert that a Path that erroneously reports a size 0 can still be compared,
      // not sure how to do that with the Path API
    }
  }

  public void testEqual_links() throws IOException {
    try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path fooPath = fs.getPath("foo");
      MoreFiles.asCharSink(fooPath, UTF_8).write("foo");

      Path fooSymlink = fs.getPath("symlink");
      Files.createSymbolicLink(fooSymlink, fooPath);

      Path fooHardlink = fs.getPath("hardlink");
      Files.createLink(fooHardlink, fooPath);

      assertThat(MoreFiles.equal(fooPath, fooSymlink)).isTrue();
      assertThat(MoreFiles.equal(fooPath, fooHardlink)).isTrue();
      assertThat(MoreFiles.equal(fooSymlink, fooHardlink)).isTrue();
    }
  }

  public void testTouch() throws IOException {
    Path temp = createTempFile();
    assertTrue(Files.exists(temp));
    Files.delete(temp);
    assertFalse(Files.exists(temp));
    MoreFiles.touch(temp);
    assertTrue(Files.exists(temp));
    MoreFiles.touch(temp);
    assertTrue(Files.exists(temp));
  }

  public void testTouchTime() throws IOException {
    Path temp = createTempFile();
    assertTrue(Files.exists(temp));
    Files.setLastModifiedTime(temp, FileTime.fromMillis(0));
    assertEquals(0, Files.getLastModifiedTime(temp).toMillis());
    MoreFiles.touch(temp);
    assertThat(Files.getLastModifiedTime(temp).toMillis()).isNotEqualTo(0);
  }

  public void testCreateParentDirectories_root() throws IOException {
    Path root = root();
    assertNull(root.getParent());
    assertNull(root.toRealPath().getParent());
    MoreFiles.createParentDirectories(root); // test that there's no exception
  }

  public void testCreateParentDirectories_relativePath() throws IOException {
    Path path = FS.getPath("nonexistent.file");
    assertNull(path.getParent());
    assertNotNull(path.toAbsolutePath().getParent());
    MoreFiles.createParentDirectories(path); // test that there's no exception
  }

  public void testCreateParentDirectories_noParentsNeeded() throws IOException {
    Path path = tempDir.resolve("nonexistent.file");
    assertTrue(Files.exists(path.getParent()));
    MoreFiles.createParentDirectories(path); // test that there's no exception
  }

  public void testCreateParentDirectories_oneParentNeeded() throws IOException {
    Path path = tempDir.resolve("parent/nonexistent.file");
    Path parent = path.getParent();
    assertFalse(Files.exists(parent));
    MoreFiles.createParentDirectories(path);
    assertTrue(Files.exists(parent));
  }

  public void testCreateParentDirectories_multipleParentsNeeded() throws IOException {
    Path path = tempDir.resolve("grandparent/parent/nonexistent.file");
    Path parent = path.getParent();
    Path grandparent = parent.getParent();
    assertFalse(Files.exists(grandparent));
    assertFalse(Files.exists(parent));

    MoreFiles.createParentDirectories(path);
    assertTrue(Files.exists(parent));
    assertTrue(Files.exists(grandparent));
  }

  public void testCreateParentDirectories_noPermission() {
    Path file = root().resolve("parent/nonexistent.file");
    Path parent = file.getParent();
    assertFalse(Files.exists(parent));
    try {
      MoreFiles.createParentDirectories(file);
      // Cleanup in case parent creation was [erroneously] successful.
      Files.delete(parent);
      fail("expected exception");
    } catch (IOException expected) {
    }
  }

  public void testCreateParentDirectories_nonDirectoryParentExists() throws IOException {
    Path parent = createTempFile();
    assertTrue(Files.isRegularFile(parent));
    Path file = parent.resolve("foo");
    try {
      MoreFiles.createParentDirectories(file);
      fail();
    } catch (IOException expected) {
    }
  }

  public void testCreateParentDirectories_symlinkParentExists() throws IOException {
    Path symlink = tempDir.resolve("linkToDir");
    Files.createSymbolicLink(symlink, root());
    Path file = symlink.resolve("foo");
    MoreFiles.createParentDirectories(file);
  }

  public void testGetFileExtension() {
    assertEquals("txt", MoreFiles.getFileExtension(FS.getPath(".txt")));
    assertEquals("txt", MoreFiles.getFileExtension(FS.getPath("blah.txt")));
    assertEquals("txt", MoreFiles.getFileExtension(FS.getPath("blah..txt")));
    assertEquals("txt", MoreFiles.getFileExtension(FS.getPath(".blah.txt")));
    assertEquals("txt", MoreFiles.getFileExtension(root().resolve("tmp/blah.txt")));
    assertEquals("gz", MoreFiles.getFileExtension(FS.getPath("blah.tar.gz")));
    assertEquals("", MoreFiles.getFileExtension(root()));
    assertEquals("", MoreFiles.getFileExtension(FS.getPath(".")));
    assertEquals("", MoreFiles.getFileExtension(FS.getPath("..")));
    assertEquals("", MoreFiles.getFileExtension(FS.getPath("...")));
    assertEquals("", MoreFiles.getFileExtension(FS.getPath("blah")));
    assertEquals("", MoreFiles.getFileExtension(FS.getPath("blah.")));
    assertEquals("", MoreFiles.getFileExtension(FS.getPath(".blah.")));
    assertEquals("", MoreFiles.getFileExtension(root().resolve("foo.bar/blah")));
    assertEquals("", MoreFiles.getFileExtension(root().resolve("foo/.bar/blah")));
  }

  public void testGetNameWithoutExtension() {
    assertEquals("", MoreFiles.getNameWithoutExtension(FS.getPath(".txt")));
    assertEquals("blah", MoreFiles.getNameWithoutExtension(FS.getPath("blah.txt")));
    assertEquals("blah.", MoreFiles.getNameWithoutExtension(FS.getPath("blah..txt")));
    assertEquals(".blah", MoreFiles.getNameWithoutExtension(FS.getPath(".blah.txt")));
    assertEquals("blah", MoreFiles.getNameWithoutExtension(root().resolve("tmp/blah.txt")));
    assertEquals("blah.tar", MoreFiles.getNameWithoutExtension(FS.getPath("blah.tar.gz")));
    assertEquals("", MoreFiles.getNameWithoutExtension(root()));
    assertEquals("", MoreFiles.getNameWithoutExtension(FS.getPath(".")));
    assertEquals(".", MoreFiles.getNameWithoutExtension(FS.getPath("..")));
    assertEquals("..", MoreFiles.getNameWithoutExtension(FS.getPath("...")));
    assertEquals("blah", MoreFiles.getNameWithoutExtension(FS.getPath("blah")));
    assertEquals("blah", MoreFiles.getNameWithoutExtension(FS.getPath("blah.")));
    assertEquals(".blah", MoreFiles.getNameWithoutExtension(FS.getPath(".blah.")));
    assertEquals("blah", MoreFiles.getNameWithoutExtension(root().resolve("foo.bar/blah")));
    assertEquals("blah", MoreFiles.getNameWithoutExtension(root().resolve("foo/.bar/blah")));
  }

  public void testPredicates() throws IOException {
    Path file = createTempFile();
    Path dir = tempDir.resolve("dir");
    Files.createDirectory(dir);

    assertTrue(MoreFiles.isDirectory().apply(dir));
    assertFalse(MoreFiles.isRegularFile().apply(dir));

    assertFalse(MoreFiles.isDirectory().apply(file));
    assertTrue(MoreFiles.isRegularFile().apply(file));

    Path symlinkToDir = tempDir.resolve("symlinkToDir");
    Path symlinkToFile = tempDir.resolve("symlinkToFile");

    Files.createSymbolicLink(symlinkToDir, dir);
    Files.createSymbolicLink(symlinkToFile, file);

    assertTrue(MoreFiles.isDirectory().apply(symlinkToDir));
    assertFalse(MoreFiles.isRegularFile().apply(symlinkToDir));

    assertFalse(MoreFiles.isDirectory().apply(symlinkToFile));
    assertTrue(MoreFiles.isRegularFile().apply(symlinkToFile));

    assertFalse(MoreFiles.isDirectory(NOFOLLOW_LINKS).apply(symlinkToDir));
    assertFalse(MoreFiles.isRegularFile(NOFOLLOW_LINKS).apply(symlinkToFile));
  }

  /**
   * Creates a new file system for testing that supports the given features in addition to
   * supporting symbolic links. The file system is created initially having the following file
   * structure:
   *
   * <pre>
   *   /
   *      work/
   *         dir/
   *            a
   *            b/
   *               g
   *               h -> ../a
   *               i/
   *                  j/
   *                     k
   *                     l/
   *            c
   *            d -> b/i
   *            e/
   *            f -> /dontdelete
   *      dontdelete/
   *         a
   *         b/
   *         c
   *      symlinktodir -> work/dir
   * </pre>
   */
  static FileSystem newTestFileSystem(Feature... supportedFeatures) throws IOException {
    FileSystem fs =
        Jimfs.newFileSystem(
            Configuration.unix()
                .toBuilder()
                .setSupportedFeatures(ObjectArrays.concat(SYMBOLIC_LINKS, supportedFeatures))
                .build());
    Files.createDirectories(fs.getPath("dir/b/i/j/l"));
    Files.createFile(fs.getPath("dir/a"));
    Files.createFile(fs.getPath("dir/c"));
    Files.createSymbolicLink(fs.getPath("dir/d"), fs.getPath("b/i"));
    Files.createDirectory(fs.getPath("dir/e"));
    Files.createSymbolicLink(fs.getPath("dir/f"), fs.getPath("/dontdelete"));
    Files.createFile(fs.getPath("dir/b/g"));
    Files.createSymbolicLink(fs.getPath("dir/b/h"), fs.getPath("../a"));
    Files.createFile(fs.getPath("dir/b/i/j/k"));
    Files.createDirectory(fs.getPath("/dontdelete"));
    Files.createFile(fs.getPath("/dontdelete/a"));
    Files.createDirectory(fs.getPath("/dontdelete/b"));
    Files.createFile(fs.getPath("/dontdelete/c"));
    Files.createSymbolicLink(fs.getPath("/symlinktodir"), fs.getPath("work/dir"));
    return fs;
  }

  public void testDirectoryDeletion_basic() throws IOException {
    for (DirectoryDeleteMethod method : EnumSet.allOf(DirectoryDeleteMethod.class)) {
      try (FileSystem fs = newTestFileSystem(SECURE_DIRECTORY_STREAM)) {
        Path dir = fs.getPath("dir");
        assertEquals(6, MoreFiles.listFiles(dir).size());

        method.delete(dir);
        method.assertDeleteSucceeded(dir);

        assertEquals(
            "contents of /dontdelete deleted by delete method " + method,
            3,
            MoreFiles.listFiles(fs.getPath("/dontdelete")).size());
      }
    }
  }

  public void testDirectoryDeletion_emptyDir() throws IOException {
    for (DirectoryDeleteMethod method : EnumSet.allOf(DirectoryDeleteMethod.class)) {
      try (FileSystem fs = newTestFileSystem(SECURE_DIRECTORY_STREAM)) {
        Path emptyDir = fs.getPath("dir/e");
        assertEquals(0, MoreFiles.listFiles(emptyDir).size());

        method.delete(emptyDir);
        method.assertDeleteSucceeded(emptyDir);
      }
    }
  }

  public void testDeleteRecursively_symlinkToDir() throws IOException {
    try (FileSystem fs = newTestFileSystem(SECURE_DIRECTORY_STREAM)) {
      Path symlink = fs.getPath("/symlinktodir");
      Path dir = fs.getPath("dir");

      assertEquals(6, MoreFiles.listFiles(dir).size());

      MoreFiles.deleteRecursively(symlink);

      assertFalse(Files.exists(symlink));
      assertTrue(Files.exists(dir));
      assertEquals(6, MoreFiles.listFiles(dir).size());
    }
  }

  public void testDeleteDirectoryContents_symlinkToDir() throws IOException {
    try (FileSystem fs = newTestFileSystem(SECURE_DIRECTORY_STREAM)) {
      Path symlink = fs.getPath("/symlinktodir");
      Path dir = fs.getPath("dir");

      assertEquals(6, MoreFiles.listFiles(symlink).size());

      MoreFiles.deleteDirectoryContents(symlink);

      assertTrue(Files.exists(symlink, NOFOLLOW_LINKS));
      assertTrue(Files.exists(symlink));
      assertTrue(Files.exists(dir));
      assertEquals(0, MoreFiles.listFiles(symlink).size());
    }
  }

  public void testDirectoryDeletion_sdsNotSupported_fails() throws IOException {
    for (DirectoryDeleteMethod method : EnumSet.allOf(DirectoryDeleteMethod.class)) {
      try (FileSystem fs = newTestFileSystem()) {
        Path dir = fs.getPath("dir");
        assertEquals(6, MoreFiles.listFiles(dir).size());

        try {
          method.delete(dir);
          fail("expected InsecureRecursiveDeleteException");
        } catch (InsecureRecursiveDeleteException expected) {
        }

        assertTrue(Files.exists(dir));
        assertEquals(6, MoreFiles.listFiles(dir).size());
      }
    }
  }

  public void testDirectoryDeletion_sdsNotSupported_allowInsecure() throws IOException {
    for (DirectoryDeleteMethod method : EnumSet.allOf(DirectoryDeleteMethod.class)) {
      try (FileSystem fs = newTestFileSystem()) {
        Path dir = fs.getPath("dir");
        assertEquals(6, MoreFiles.listFiles(dir).size());

        method.delete(dir, ALLOW_INSECURE);
        method.assertDeleteSucceeded(dir);

        assertEquals(
            "contents of /dontdelete deleted by delete method " + method,
            3,
            MoreFiles.listFiles(fs.getPath("/dontdelete")).size());
      }
    }
  }

  public void testDeleteRecursively_symlinkToDir_sdsNotSupported_allowInsecure()
      throws IOException {
    try (FileSystem fs = newTestFileSystem()) {
      Path symlink = fs.getPath("/symlinktodir");
      Path dir = fs.getPath("dir");

      assertEquals(6, MoreFiles.listFiles(dir).size());

      MoreFiles.deleteRecursively(symlink, ALLOW_INSECURE);

      assertFalse(Files.exists(symlink));
      assertTrue(Files.exists(dir));
      assertEquals(6, MoreFiles.listFiles(dir).size());
    }
  }

  public void testDeleteDirectoryContents_symlinkToDir_sdsNotSupported_allowInsecure()
      throws IOException {
    try (FileSystem fs = newTestFileSystem()) {
      Path symlink = fs.getPath("/symlinktodir");
      Path dir = fs.getPath("dir");

      assertEquals(6, MoreFiles.listFiles(dir).size());

      MoreFiles.deleteDirectoryContents(symlink, ALLOW_INSECURE);
      assertEquals(0, MoreFiles.listFiles(dir).size());
    }
  }

  /**
   * This test attempts to create a situation in which one thread is constantly changing a file from
   * being a real directory to being a symlink to another directory. It then calls
   * deleteDirectoryContents thousands of times on a directory whose subtree contains the file
   * that's switching between directory and symlink to try to ensure that under no circumstance does
   * deleteDirectoryContents follow the symlink to the other directory and delete that directory's
   * contents.
   *
   * <p>We can only test this with a file system that supports SecureDirectoryStream, because it's
   * not possible to protect against this if the file system doesn't.
   */
  public void testDirectoryDeletion_directorySymlinkRace() throws IOException {
    for (DirectoryDeleteMethod method : EnumSet.allOf(DirectoryDeleteMethod.class)) {
      try (FileSystem fs = newTestFileSystem(SECURE_DIRECTORY_STREAM)) {
        Path dirToDelete = fs.getPath("dir/b/i");
        Path changingFile = dirToDelete.resolve("j/l");
        Path symlinkTarget = fs.getPath("/dontdelete");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        startDirectorySymlinkSwitching(changingFile, symlinkTarget, executor);

        try {
          for (int i = 0; i < 5000; i++) {
            try {
              Files.createDirectories(changingFile);
              Files.createFile(dirToDelete.resolve("j/k"));
            } catch (FileAlreadyExistsException expected) {
              // if a file already exists, that's fine... just continue
            }

            try {
              method.delete(dirToDelete);
            } catch (FileSystemException expected) {
              // the delete method may or may not throw an exception, but if it does that's fine
              // and expected
            }

            // this test is mainly checking that the contents of /dontdelete aren't deleted under
            // any circumstances
            assertEquals(3, MoreFiles.listFiles(symlinkTarget).size());

            Thread.yield();
          }
        } finally {
          executor.shutdownNow();
        }
      }
    }
  }

  public void testDeleteRecursively_nonDirectoryFile() throws IOException {
    try (FileSystem fs = newTestFileSystem(SECURE_DIRECTORY_STREAM)) {
      Path file = fs.getPath("dir/a");
      assertTrue(Files.isRegularFile(file, NOFOLLOW_LINKS));

      MoreFiles.deleteRecursively(file);

      assertFalse(Files.exists(file, NOFOLLOW_LINKS));

      Path symlink = fs.getPath("/symlinktodir");
      assertTrue(Files.isSymbolicLink(symlink));

      Path realSymlinkTarget = symlink.toRealPath();
      assertTrue(Files.isDirectory(realSymlinkTarget, NOFOLLOW_LINKS));

      MoreFiles.deleteRecursively(symlink);

      assertFalse(Files.exists(symlink, NOFOLLOW_LINKS));
      assertTrue(Files.isDirectory(realSymlinkTarget, NOFOLLOW_LINKS));
    }
  }

  /**
   * Starts a new task on the given executor that switches (deletes and replaces) a file between
   * being a directory and being a symlink. The given {@code file} is the file that should switch
   * between being a directory and being a symlink, while the given {@code target} is the target the
   * symlink should have.
   */
  private static void startDirectorySymlinkSwitching(
      final Path file, final Path target, ExecutorService executor) {
    @SuppressWarnings("unused") // go/futurereturn-lsc
    Future<?> possiblyIgnoredError =
        executor.submit(
            new Runnable() {
              @Override
              public void run() {
                boolean createSymlink = false;
                while (!Thread.interrupted()) {
                  try {
                    // trying to switch between a real directory and a symlink (dir -> /a)
                    if (Files.deleteIfExists(file)) {
                      if (createSymlink) {
                        Files.createSymbolicLink(file, target);
                      } else {
                        Files.createDirectory(file);
                      }
                      createSymlink = !createSymlink;
                    }
                  } catch (IOException tolerated) {
                    // it's expected that some of these will fail
                  }

                  Thread.yield();
                }
              }
            });
  }

  /** Enum defining the two MoreFiles methods that delete directory contents. */
  private enum DirectoryDeleteMethod {
    DELETE_DIRECTORY_CONTENTS {
      @Override
      public void delete(Path path, RecursiveDeleteOption... options) throws IOException {
        MoreFiles.deleteDirectoryContents(path, options);
      }

      @Override
      public void assertDeleteSucceeded(Path path) throws IOException {
        assertEquals(
            "contents of directory " + path + " not deleted with delete method " + this,
            0,
            MoreFiles.listFiles(path).size());
      }
    },
    DELETE_RECURSIVELY {
      @Override
      public void delete(Path path, RecursiveDeleteOption... options) throws IOException {
        MoreFiles.deleteRecursively(path, options);
      }

      @Override
      public void assertDeleteSucceeded(Path path) throws IOException {
        assertFalse("file " + path + " not deleted with delete method " + this, Files.exists(path));
      }
    };

    public abstract void delete(Path path, RecursiveDeleteOption... options) throws IOException;

    public abstract void assertDeleteSucceeded(Path path) throws IOException;
  }
}
