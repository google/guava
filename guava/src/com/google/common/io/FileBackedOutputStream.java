/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An {@link OutputStream} that starts buffering to a byte array, but switches to file buffering
 * once the data reaches a configurable size.
 *
 * <p>Temporary files created by this stream may live in the local filesystem until either:
 *
 * <ul>
 *   <li>{@link #reset} is called (removing the data in this stream and deleting the file), or...
 *   <li>this stream (or, more precisely, its {@link #asByteSource} view) is finalized during
 *       garbage collection, <strong>AND</strong> this stream was not constructed with {@linkplain
 *       #FileBackedOutputStream(int) the 1-arg constructor} or the {@linkplain
 *       #FileBackedOutputStream(int, boolean) 2-arg constructor} passing {@code false} in the
 *       second parameter.
 * </ul>
 *
 * <p>This class is thread-safe.
 *
 * @author Chris Nokleberg
 * @since 1.0
 */
@Beta
@GwtIncompatible
public final class FileBackedOutputStream extends OutputStream {
  private final int fileThreshold;
  private final boolean resetOnFinalize;
  private final ByteSource source;
  @Nullable private final File parentDirectory;

  @GuardedBy("this")
  private OutputStream out;

  @GuardedBy("this")
  private MemoryOutput memory;

  @GuardedBy("this")
  @Nullable
  private File file;

  /** ByteArrayOutputStream that exposes its internals. */
  private static class MemoryOutput extends ByteArrayOutputStream {
    byte[] getBuffer() {
      return buf;
    }

    int getCount() {
      return count;
    }
  }

  /** Returns the file holding the data (possibly null). */
  @VisibleForTesting
  synchronized File getFile() {
    return file;
  }

  /**
   * Creates a new instance that uses the given file threshold, and does not reset the data when the
   * {@link ByteSource} returned by {@link #asByteSource} is finalized.
   *
   * @param fileThreshold the number of bytes before the stream should switch to buffering to a file
   */
  public FileBackedOutputStream(int fileThreshold) {
    this(fileThreshold, false);
  }

  /**
   * Creates a new instance that uses the given file threshold, and optionally resets the data when
   * the {@link ByteSource} returned by {@link #asByteSource} is finalized.
   *
   * @param fileThreshold the number of bytes before the stream should switch to buffering to a file
   * @param resetOnFinalize if true, the {@link #reset} method will be called when the {@link
   *     ByteSource} returned by {@link #asByteSource} is finalized.
   */
  public FileBackedOutputStream(int fileThreshold, boolean resetOnFinalize) {
    this(fileThreshold, resetOnFinalize, null);
  }

  private FileBackedOutputStream(
      int fileThreshold, boolean resetOnFinalize, @Nullable File parentDirectory) {
    this.fileThreshold = fileThreshold;
    this.resetOnFinalize = resetOnFinalize;
    this.parentDirectory = parentDirectory;
    memory = new MemoryOutput();
    out = memory;

    if (resetOnFinalize) {
      source =
          new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
              return openInputStream();
            }

            @Override
            protected void finalize() {
              try {
                reset();
              } catch (Throwable t) {
                t.printStackTrace(System.err);
              }
            }
          };
    } else {
      source =
          new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
              return openInputStream();
            }
          };
    }
  }

  /**
   * Returns a readable {@link ByteSource} view of the data that has been written to this stream.
   *
   * @since 15.0
   */
  public ByteSource asByteSource() {
    return source;
  }

  private synchronized InputStream openInputStream() throws IOException {
    if (file != null) {
      return new FileInputStream(file);
    } else {
      return new ByteArrayInputStream(memory.getBuffer(), 0, memory.getCount());
    }
  }

  /**
   * Calls {@link #close} if not already closed, and then resets this object back to its initial
   * state, for reuse. If data was buffered to a file, it will be deleted.
   *
   * @throws IOException if an I/O error occurred while deleting the file buffer
   */
  public synchronized void reset() throws IOException {
    try {
      close();
    } finally {
      if (memory == null) {
        memory = new MemoryOutput();
      } else {
        memory.reset();
      }
      out = memory;
      if (file != null) {
        File deleteMe = file;
        file = null;
        if (!deleteMe.delete()) {
          throw new IOException("Could not delete: " + deleteMe);
        }
      }
    }
  }

  @Override
  public synchronized void write(int b) throws IOException {
    update(1);
    out.write(b);
  }

  @Override
  public synchronized void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public synchronized void write(byte[] b, int off, int len) throws IOException {
    update(len);
    out.write(b, off, len);
  }

  @Override
  public synchronized void close() throws IOException {
    out.close();
  }

  @Override
  public synchronized void flush() throws IOException {
    out.flush();
  }

  /**
   * Checks if writing {@code len} bytes would go over threshold, and switches to file buffering if
   * so.
   */
  @GuardedBy("this")
  private void update(int len) throws IOException {
    if (file == null && (memory.getCount() + len > fileThreshold)) {
      File temp = File.createTempFile("FileBackedOutputStream", null, parentDirectory);
      if (resetOnFinalize) {
        // Finalizers are not guaranteed to be called on system shutdown;
        // this is insurance.
        temp.deleteOnExit();
      }
      FileOutputStream transfer = new FileOutputStream(temp);
      transfer.write(memory.getBuffer(), 0, memory.getCount());
      transfer.flush();

      // We've successfully transferred the data; switch to writing to file
      out = transfer;
      file = temp;
      memory = null;
    }
  }
}
