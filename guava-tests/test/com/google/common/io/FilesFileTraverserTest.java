/*
 * Copyright (C) 2017 The Guava Authors
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import junit.framework.TestCase;

/**
 * Tests for {@link Files#fileTraverser()}.
 *
 * @author Jens Nyman
 */

public class FilesFileTraverserTest extends TestCase {

  private File rootDir;

  @Override
  public void setUp() throws IOException {
    rootDir = Files.createTempDir();
  }

  @Override
  public void tearDown() throws IOException {
    // delete rootDir and its contents
    java.nio.file.Files.walkFileTree(
        rootDir.toPath(),
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            java.nio.file.Files.deleteIfExists(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) {
              return FileVisitResult.TERMINATE;
            }
            java.nio.file.Files.deleteIfExists(dir);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  public void testFileTraverser_emptyDirectory() throws Exception {
    assertThat(Files.fileTraverser().breadthFirst(rootDir)).containsExactly(rootDir);
  }

  public void testFileTraverser_nonExistingFile() throws Exception {
    File file = new File(rootDir, "file-that-doesnt-exist");

    assertThat(Files.fileTraverser().breadthFirst(file)).containsExactly(file);
  }

  public void testFileTraverser_file() throws Exception {
    File file = newFile("some-file");

    assertThat(Files.fileTraverser().breadthFirst(file)).containsExactly(file);
  }

  public void testFileTraverser_singleFile() throws Exception {
    File file = newFile("some-file");

    assertThat(Files.fileTraverser().breadthFirst(rootDir)).containsExactly(rootDir, file);
  }

  public void testFileTraverser_singleDirectory() throws Exception {
    File file = newDir("some-dir");

    assertThat(Files.fileTraverser().breadthFirst(rootDir)).containsExactly(rootDir, file);
  }

  public void testFileTraverser_multipleFilesAndDirectories() throws Exception {
    File fileA = newFile("file-a");
    File fileB = newFile("file-b");
    File dir1 = newDir("dir-1");
    File dir2 = newDir("dir-2");

    assertThat(Files.fileTraverser().breadthFirst(rootDir))
        .containsExactly(rootDir, fileA, fileB, dir1, dir2);
  }

  public void testFileTraverser_multipleDirectoryLayers_breadthFirstStartsWithTopLayer()
      throws Exception {
    File fileA = newFile("file-a");
    File dir1 = newDir("dir-1");
    newFile("dir-1/file-b");
    newFile("dir-1/dir-2");

    assertThat(Iterables.limit(Files.fileTraverser().breadthFirst(rootDir), 3))
        .containsExactly(rootDir, fileA, dir1);
  }

  public void testFileTraverser_multipleDirectoryLayers_traversalReturnsAll() throws Exception {
    File fileA = newFile("file-a");
    File dir1 = newDir("dir-1");
    File fileB = newFile("dir-1/file-b");
    File dir2 = newFile("dir-1/dir-2");

    assertThat(Files.fileTraverser().breadthFirst(rootDir))
        .containsExactly(rootDir, fileA, fileB, dir1, dir2);
  }

  @CanIgnoreReturnValue
  private File newDir(String name) {
    File file = new File(rootDir, name);
    file.mkdir();
    return file;
  }

  @CanIgnoreReturnValue
  private File newFile(String name) throws IOException {
    File file = new File(rootDir, name);
    file.createNewFile();
    return file;
  }
}
