/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.io.FileBackedOutputStreamTest.write;

import com.google.common.testing.GcFinalization;
import java.io.File;

/**
 * Android-incompatible tests for {@link FileBackedOutputStream}.
 *
 * @author Chris Nokleberg
 */
@AndroidIncompatible // Finalization probably just doesn't happen fast enough?
public class FileBackedOutputStreamAndroidIncompatibleTest extends IoTestCase {

  public void testFinalizeDeletesFile() throws Exception {
    byte[] data = newPreFilledByteArray(100);
    FileBackedOutputStream out = new FileBackedOutputStream(0, true);

    write(out, data, 0, 100, true);
    final File file = out.getFile();
    assertEquals(100, file.length());
    assertTrue(file.exists());
    out.close();

    // Make sure that finalize deletes the file
    out = null;

    // times out and throws RuntimeException on failure
    GcFinalization.awaitDone(
        new GcFinalization.FinalizationPredicate() {
          @Override
          public boolean isDone() {
            return !file.exists();
          }
        });
  }
}
