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
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import junit.framework.TestCase;

/**
 * Tests for {@link MoreFiles#fileTraverser()}.
 *
 * @author Jens Nyman
 */

public class MoreFilesFileTraverserTest extends TestCase {

  private Path rootDir;

  @Override
  public void setUp() throws IOException {
    rootDir = Jimfs.newFileSystem(Configuration.unix()).getPath("/tmp");
    Files.createDirectory(rootDir);
  }

  @Override
  public void tearDown() throws IOException {
    rootDir.getFileSystem().close();
  }

  public void testFileTraverser_emptyDirectory() throws Exception {
    assertThat(MoreFiles.fileTraverser().breadthFirst(rootDir)).containsExactly(rootDir);
  }

  public void testFileTraverser_nonExistingFile() throws Exception {
    Path file = rootDir.resolve("file-that-doesnt-exist");

    assertThat(MoreFiles.fileTraverser().breadthFirst(file)).containsExactly(file);
  }

  public void testFileTraverser_file() throws Exception {
    Path file = newFile("some-file");

    assertThat(MoreFiles.fileTraverser().breadthFirst(file)).containsExactly(file);
  }

  public void testFileTraverser_singleFile() throws Exception {
    Path file = newFile("some-file");

    assertThat(MoreFiles.fileTraverser().breadthFirst(rootDir)).containsExactly(rootDir, file);
  }

  public void testFileTraverser_singleDirectory() throws Exception {
    Path file = newDir("some-dir");

    assertThat(MoreFiles.fileTraverser().breadthFirst(rootDir)).containsExactly(rootDir, file);
  }

  public void testFileTraverser_multipleFilesAndDirectories() throws Exception {
    Path fileA = newFile("file-a");
    Path fileB = newFile("file-b");
    Path dir1 = newDir("dir-1");
    Path dir2 = newDir("dir-2");

    assertThat(MoreFiles.fileTraverser().breadthFirst(rootDir))
        .containsExactly(rootDir, fileA, fileB, dir1, dir2);
  }

  public void testFileTraverser_multipleDirectoryLayers_breadthFirstStartsWithTopLayer()
      throws Exception {
    Path fileA = newFile("file-a");
    Path dir1 = newDir("dir-1");
    newFile("dir-1/file-b");
    newFile("dir-1/dir-2");

    assertThat(Iterables.limit(MoreFiles.fileTraverser().breadthFirst(rootDir), 3))
        .containsExactly(rootDir, fileA, dir1);
  }

  public void testFileTraverser_multipleDirectoryLayers_traversalReturnsAll() throws Exception {
    Path fileA = newFile("file-a");
    Path dir1 = newDir("dir-1");
    Path fileB = newFile("dir-1/file-b");
    Path dir2 = newFile("dir-1/dir-2");

    assertThat(MoreFiles.fileTraverser().breadthFirst(rootDir))
        .containsExactly(rootDir, fileA, fileB, dir1, dir2);
  }

  @CanIgnoreReturnValue
  private Path newDir(String name) throws IOException {
    Path dir = rootDir.resolve(name);
    Files.createDirectory(dir);
    return dir;
  }

  @CanIgnoreReturnValue
  private Path newFile(String name) throws IOException {
    Path file = rootDir.resolve(name);
    MoreFiles.touch(file);
    return file;
  }
}
