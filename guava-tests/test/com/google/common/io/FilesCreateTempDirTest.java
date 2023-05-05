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

import static com.google.common.truth.Truth.assertThat;

import java.io.File;

/**
 * Unit test for {@link Files#createTempDir}.
 *
 * @author Chris Nokleberg
 */

public class FilesCreateTempDirTest extends IoTestCase {
  public void testCreateTempDir() {
    File temp = Files.createTempDir();
    assertTrue(temp.exists());
    assertTrue(temp.isDirectory());
    assertThat(temp.listFiles()).isEmpty();
    assertTrue(temp.delete());
  }
}
