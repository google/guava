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

import com.google.common.testing.GcFinalization;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Unit tests for {@link FileBackedOutputStream}.
 *
 * @author Chris Nokleberg
 */
public class FileBackedOutputStreamTest extends IoTestCase {

  public void testThreshold() throws Exception {
    testThreshold(0, 100, true, false);
    testThreshold(10, 100, true, false);
    testThreshold(100, 100, true, false);
    testThreshold(1000, 100, true, false);
    testThreshold(0, 100, false, false);
    testThreshold(10, 100, false, false);
    testThreshold(100, 100, false, false);
    testThreshold(1000, 100, false, false);
  }

  private void testThreshold(
      int fileThreshold, int dataSize, boolean singleByte, boolean resetOnFinalize)
      throws IOException {
    byte[] data = newPreFilledByteArray(dataSize);
    FileBackedOutputStream out = new FileBackedOutputStream(fileThreshold, resetOnFinalize);
    ByteSource source = out.asByteSource();
    int chunk1 = Math.min(dataSize, fileThreshold);
    int chunk2 = dataSize - chunk1;

    // Write just enough to not trip the threshold
    if (chunk1 > 0) {
      write(out, data, 0, chunk1, singleByte);
      assertTrue(ByteSource.wrap(data).slice(0, chunk1).contentEquals(source));
    }
    File file = out.getFile();
    assertNull(file);

    // Write data to go over the threshold
    if (chunk2 > 0) {
      write(out, data, chunk1, chunk2, singleByte);
      file = out.getFile();
      assertEquals(dataSize, file.length());
      assertTrue(file.exists());
    }
    out.close();

    // Check that source returns the right data
    assertTrue(Arrays.equals(data, source.read()));

    // Make sure that reset deleted the file
    out.reset();
    if (file != null) {
      assertFalse(file.exists());
    }
  }

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

  public void testThreshold_resetOnFinalize() throws Exception {
    testThreshold(0, 100, true, true);
    testThreshold(10, 100, true, true);
    testThreshold(100, 100, true, true);
    testThreshold(1000, 100, true, true);
    testThreshold(0, 100, false, true);
    testThreshold(10, 100, false, true);
    testThreshold(100, 100, false, true);
    testThreshold(1000, 100, false, true);
  }

  private static void write(OutputStream out, byte[] b, int off, int len, boolean singleByte)
      throws IOException {
    if (singleByte) {
      for (int i = off; i < off + len; i++) {
        out.write(b[i]);
      }
    } else {
      out.write(b, off, len);
    }
    out.flush(); // for coverage
  }

  // TODO(chrisn): only works if we ensure we have crossed file threshold

  public void testWriteErrorAfterClose() throws Exception {
    byte[] data = newPreFilledByteArray(100);
    FileBackedOutputStream out = new FileBackedOutputStream(50);
    ByteSource source = out.asByteSource();

    out.write(data);
    assertTrue(Arrays.equals(data, source.read()));

    out.close();
    try {
      out.write(42);
      fail("expected exception");
    } catch (IOException expected) {
    }

    // Verify that write had no effect
    assertTrue(Arrays.equals(data, source.read()));
    out.reset();
  }

  public void testReset() throws Exception {
    byte[] data = newPreFilledByteArray(100);
    FileBackedOutputStream out = new FileBackedOutputStream(Integer.MAX_VALUE);
    ByteSource source = out.asByteSource();

    out.write(data);
    assertTrue(Arrays.equals(data, source.read()));

    out.reset();
    assertTrue(Arrays.equals(new byte[0], source.read()));

    out.write(data);
    assertTrue(Arrays.equals(data, source.read()));

    out.close();
  }
}
