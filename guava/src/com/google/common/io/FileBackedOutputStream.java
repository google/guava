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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static com.google.common.collect.Sets.newConcurrentHashSet;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.channels.Channels.newOutputStream;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.FinalizablePhantomReference;
import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.io.TempFileCreator.TempFileAndChannel;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.google.j2objc.annotations.J2ObjCIncompatible;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * An {@link OutputStream} that starts buffering to a byte array, but switches to file buffering
 * once the data reaches a configurable size.
 *
 * <p>When this stream creates a temporary file, it restricts the file's permissions to the current
 * user or, in the case of Android, the current app. If that is not possible (as is the case under
 * the very old Android Ice Cream Sandwich release), then this stream throws an exception instead of
 * creating a file that would be more accessible. (This behavior is new in Guava 32.0.0. Previous
 * versions would create a file that is more accessible, as discussed in <a
 * href="https://nvd.nist.gov/vuln/detail/cve-2023-2976">CVE-2023-2976</a>.
 *
 * <p>Temporary files created by this stream may live in the local filesystem until either:
 *
 * <ul>
 *   <li>{@link #reset} is called (removing the data in this stream and deleting the file), or...
 *   <li>this stream (or, more precisely, its {@link #asByteSource} view) is garbage collected,
 *       <strong>AND</strong> this stream was constructed with {@linkplain
 *       #FileBackedOutputStream(int, boolean) the two-arg constructor} with {@code true} for the
 *       second parameter. Relying on this second approach is discouraged.
 * </ul>
 *
 * <p>This class is thread-safe.
 *
 * @author Chris Nokleberg
 * @since 1.0
 */
@Beta
@J2ktIncompatible
@GwtIncompatible
@J2ObjCIncompatible
public final class FileBackedOutputStream extends OutputStream {
  private final FbosByteSource byteSource;
  private final State state;

  /** ByteArrayOutputStream that exposes its internals. */
  private static final class MemoryOutput extends ByteArrayOutputStream {
    byte[] getBuffer() {
      return buf;
    }

    int getCount() {
      return count;
    }
  }

  /** Returns the file holding the data (possibly null). */
  @VisibleForTesting
  @Nullable File getFile() {
    return state.getFile();
  }

  /**
   * Creates a new instance that uses the given file threshold, and does not reset the data when the
   * {@link ByteSource} returned by {@link #asByteSource} is garbage collected.
   *
   * @param fileThreshold the number of bytes before the stream should switch to buffering to a file
   * @throws IllegalArgumentException if {@code fileThreshold} is negative
   */
  public FileBackedOutputStream(int fileThreshold) {
    this(fileThreshold, false);
  }

  /**
   * Creates a new instance that uses the given file threshold, and optionally resets the data when
   * the {@link ByteSource} returned by {@link #asByteSource} is garbage collected. Prefer to {@link
   * #reset} the stream explicitly, rather than rely on garbage collection: If you call {@link
   * #reset} explicitly, you can call it more promptly, and you can appropriately handle any
   * exception that results.
   *
   * @param fileThreshold the number of bytes before the stream should switch to buffering to a file
   * @param resetWhenGarbageCollected if true, the {@link #reset} method will be called when the
   *     {@link ByteSource} returned by {@link #asByteSource} is garbage collected.
   * @throws IllegalArgumentException if {@code fileThreshold} is negative
   */
  public FileBackedOutputStream(int fileThreshold, boolean resetWhenGarbageCollected) {
    checkArgument(
        fileThreshold >= 0, "fileThreshold must be non-negative, but was %s", fileThreshold);
    this.state = new State(fileThreshold, resetWhenGarbageCollected);
    this.byteSource = new FbosByteSource(state);

    if (resetWhenGarbageCollected) {
      FinalizableReference.register(byteSource);
    }
  }

  private static final class FinalizableReference extends FinalizablePhantomReference<ByteSource> {
    static final FinalizableReferenceQueue referenceQueue = new FinalizableReferenceQueue();
    static final Set<FinalizableReference> references = newConcurrentHashSet();

    static void register(FbosByteSource referent) {
      try {
        references.add(new FinalizableReference(referent));
      } finally {
        FileBackedOutputStream.reachabilityFence(referent);
      }
    }

    final State state;

    FinalizableReference(FbosByteSource referent) {
      super(referent, referenceQueue);
      this.state = referent.state;
    }

    @Override
    public void finalizeReferent() {
      references.remove(this);
      try {
        state.reset();
      } catch (Throwable t) {
        t.printStackTrace(System.err);
      }
    }
  }

  private static final class FbosByteSource extends ByteSource {
    final State state;

    FbosByteSource(State state) {
      this.state = state;
    }

    @Override
    public InputStream openStream() throws IOException {
      return state.openInputStream();
    }
  }

  /**
   * Returns a readable {@link ByteSource} view of the data that has been written to this stream.
   *
   * @since 15.0
   */
  public ByteSource asByteSource() {
    return byteSource;
  }

  /**
   * Calls {@link #close} if not already closed, and then resets this object back to its initial
   * state, for reuse. If data was buffered to a file, it will be deleted.
   *
   * @throws IOException if an I/O error occurred while deleting the file buffer
   */
  public void reset() throws IOException {
    try {
      state.reset();
    } finally {
      reachabilityFence(byteSource);
    }
  }

  @Override
  public void write(int b) throws IOException {
    try {
      state.write(b);
    } finally {
      reachabilityFence(byteSource);
    }
  }

  @Override
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    try {
      state.write(b, off, len);
    } finally {
      reachabilityFence(byteSource);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      state.close();
    } finally {
      reachabilityFence(byteSource);
    }
  }

  @Override
  public void flush() throws IOException {
    try {
      state.flush();
    } finally {
      reachabilityFence(byteSource);
    }
  }

  /**
   * Per-instance state, extracted to a separate object so that {@link FinalizableReference} can
   * still access it after the {@link FileBackedOutputStream} and {@link ByteSource} have been
   * garbage collected.
   */
  private static final class State {
    final int fileThreshold;
    final boolean resetWhenGarbageCollected;

    @GuardedBy("this")
    @Nullable MemoryOutput memory;

    @GuardedBy("this")
    @Nullable TempFileAndChannel fileAndChannel;

    @GuardedBy("this")
    OutputStream out;

    State(int fileThreshold, boolean resetWhenGarbageCollected) {
      this.fileThreshold = fileThreshold;
      this.resetWhenGarbageCollected = resetWhenGarbageCollected;
      this.memory = new MemoryOutput();
      this.out = memory;
    }

    synchronized @Nullable File getFile() {
      return fileAndChannel != null ? fileAndChannel.file : null;
    }

    synchronized InputStream openInputStream() throws IOException {
      if (fileAndChannel != null) {
        return new ChannelInputStream(fileAndChannel.channel);
      } else {
        // requireNonNull is safe because we always have either `fileAndChannel` or `memory`.
        requireNonNull(memory);
        return new ByteArrayInputStream(memory.getBuffer(), 0, memory.getCount());
      }
    }

    synchronized void reset() throws IOException {
      /*
       * We don't need to call this.close(): It does nothing but update `out`, and we handle
       * updating `out` here.
       */

      if (memory == null) {
        memory = new MemoryOutput();
      } else {
        memory.reset();
      }
      out = memory;

      try (TempFileAndChannel closeMe = fileAndChannel) {
        fileAndChannel = null;
      }
    }

    synchronized void write(int b) throws IOException {
      update(1);
      out.write(b);
    }

    synchronized void write(byte[] b, int off, int len) throws IOException {
      update(len);
      out.write(b, off, len);
    }

    synchronized void close() {
      /*
       * It would make sense to switch to CLOSED_OUTPUT_STREAM whether we've spilled to a file or
       * not. But that would be a difference from our initial behavior of allowing writes after
       * close() in the non-spilled case, thanks to behavior inherited from ByteArrayOutputStream.
       *
       * TODO(cpovirk): Maybe change that behavior, which is fragile.
       */
      if (fileAndChannel != null) {
        // Do not close the FileChannel; it must stay open for subsequent reads via asByteSource().
        out = CLOSED_OUTPUT_STREAM;
        /*
         * We don't even need to set a bit that marks the old OutputStream as closed, since we
         * control all access to that stream (and we have now cut off that access).
         */
      }
    }

    synchronized void flush() throws IOException {
      out.flush();
    }

    /**
     * Checks if writing {@code len} bytes would go over threshold, and switches to file buffering
     * if so.
     */
    @GuardedBy("this")
    void update(int len) throws IOException {
      if (memory != null && (memory.getCount() + len > fileThreshold)) {
        TempFileAndChannel temp = TempFileCreator.INSTANCE.openTempFile("FileBackedOutputStream");
        if (resetWhenGarbageCollected) {
          // References are not guaranteed to be collected on system shutdown; this is insurance.
          temp.file.deleteOnExit();
        }
        try {
          OutputStream transfer = newOutputStream(temp.channel);
          transfer.write(memory.getBuffer(), 0, memory.getCount());
          transfer.flush();
          // We've successfully transferred the data; switch to writing to file.
          out = transfer;
        } catch (IOException e) {
          try {
            temp.close();
          } catch (IOException closeException) {
            e.addSuppressed(closeException);
          }
          throw e;
        }

        fileAndChannel = temp;
        memory = null;
      }
    }
  }

  private static final OutputStream CLOSED_OUTPUT_STREAM =
      new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          throw new IOException("Stream closed");
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
          checkPositionIndexes(off, off + len, b.length);
          throw new IOException("Stream closed");
        }

        @Override
        public void flush() throws IOException {
          throw new IOException("Stream closed");
        }
      };

  private static final class ChannelInputStream extends InputStream {
    final FileChannel channel;

    @GuardedBy("this")
    long position;

    @GuardedBy("this")
    boolean closed;

    ChannelInputStream(FileChannel channel) {
      this.channel = channel;
    }

    @Override
    public synchronized int read() throws IOException {
      byte[] b = new byte[1];
      return read(b, 0, 1) == 1 ? b[0] & 0xFF : -1;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
      ByteBuffer buf = ByteBuffer.wrap(b, off, len);
      checkOpen();
      if (len == 0) {
        return 0;
      }
      int read = channel.read(buf, position);
      if (read > 0) {
        position += read;
      }
      return read;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
      checkOpen();
      if (n <= 0) {
        return 0;
      }
      long available = channel.size() - position;
      if (available <= 0) {
        return 0;
      }
      long skipped = min(n, available);
      position += skipped;
      return skipped;
    }

    @Override
    public synchronized int available() throws IOException {
      checkOpen();
      return Ints.saturatedCast(max(0, channel.size() - position));
    }

    @Override
    public synchronized void close() {
      // Do not close the FileChannel; it must stay open for subsequent reads via asByteSource().
      closed = true;
    }

    @GuardedBy("this")
    void checkOpen() throws IOException {
      if (closed) {
        throw new IOException("Stream closed");
      }
    }
  }

  /** Ensures that {@code o} is not garbage collected until this point in the code. */
  @VisibleForTesting
  // We call the method only after checking that it's present.
  @IgnoreJRERequirement
  @SuppressWarnings({
    "Java8ApiChecker",
    "NewApi",
    // This method is a helper, which we call from a `finally` block, as recommended.
    "ReachabilityFenceUsage",
  })
  static void reachabilityFence(@Nullable Object o) {
    if (IS_REACHABILITY_FENCE_METHOD_USABLE) {
      Reference.reachabilityFence(o);
    }
  }

  private static final boolean IS_REACHABILITY_FENCE_METHOD_USABLE =
      computeIsReachabilityFenceMethodUsable();

  private static boolean computeIsReachabilityFenceMethodUsable() {
    try {
      Method method = Reference.class.getMethod("reachabilityFence", Object.class);
      method.invoke(null, FileBackedOutputStream.class); // to make sure the method is accessible
      return true;
    } catch (NoSuchMethodException | IllegalAccessException probablyBeforeJava9OrAndroid28) {
      /*
       * It's theoretically possible for Reference.reachabilityFence to exist under older VMs in an
       * inaccessible form.
       */
      return false;
    } catch (InvocationTargetException e) {
      /*
       * It's theoretically possible for Reference.reachabilityFence to exist under older VMs but
       * not work. (Under Android in particular, we really should check the API Level instead of
       * probing for methods....) But it's hard to imagine how reachabilityFence in particular could
       * exist but throw, so we propagate anything that's thrown, presumably an unchecked Exception
       * or Error.
       */
      throwIfUnchecked(e.getCause());
      throw new AssertionError(e.getCause());
    }
  }
}
