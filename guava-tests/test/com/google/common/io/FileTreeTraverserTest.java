/*
 * Copyright (C) 2012 The Guava Authors
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

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;

/**
 * Tests for {@link Files#fileTreeViewer}.
 *
 * @author Colin Decker
 */

public class FileTreeTraverserTest extends TestCase {

  private File dir;

  @Override
  public void setUp() throws IOException {
    dir = Files.createTempDir();
  }

  @Override
  public void tearDown() throws IOException {
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }

    // we aren't creating any files in subdirs
    for (File file : files) {
      file.delete();
    }

    dir.delete();
  }

  public void testFileTreeViewer_emptyDir() throws IOException {
    assertDirChildren();
  }

  public void testFileTreeViewer_singleFile() throws IOException {
    File file = newFile("test");
    assertDirChildren(file);
  }

  public void testFileTreeViewer_singleDir() throws IOException {
    File file = newDir("test");
    assertDirChildren(file);
  }

  public void testFileTreeViewer_multipleFiles() throws IOException {
    File a = newFile("a");
    File b = newDir("b");
    File c = newFile("c");
    File d = newDir("d");
    assertDirChildren(a, b, c, d);
  }

  private File newDir(String name) throws IOException {
    File file = new File(dir, name);
    file.mkdir();
    return file;
  }

  private File newFile(String name) throws IOException {
    File file = new File(dir, name);
    file.createNewFile();
    return file;
  }

  private void assertDirChildren(File... files) {
    assertEquals(ImmutableSet.copyOf(files),
        ImmutableSet.copyOf(Files.fileTreeTraverser().children(dir)));
  }
}
