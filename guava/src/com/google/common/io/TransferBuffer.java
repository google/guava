package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Supplier;
import java.io.Closeable;
import java.lang.ref.SoftReference;
import java.util.Arrays;

/**
 * Provides byte- or char-Arrays to be used as method-local transfer buffers, to e.g. move data from
 * an {@link java.io.InputStream} to an {@link java.io.OutputStream}.
 *
 * TransferBuffers must not be stored and must be closed after use:
 *
 * <pre>
 * {@code
 * try (TransferBuffer<byte[]> buffer = TransferBuffer.getByteArrayTransferBuffer()) {
 *  byte[] array = buffer.getArray();
 *  int read;
 *  do {
 *    read = inputStream.read(array);
 *    outputStream.write(array);
 *  } while (read != 0);
 * }
 * }
 * </pre>
 *
 * @author Bernd Hopp
 * @since 29
 */
@Beta
@GwtIncompatible
final class TransferBuffer<T> implements Closeable, Supplier<T> {

  // 8K bytes
  private static final int BYTE_BUFFER_SIZE = 0x2000;
  // 2K chars (4K bytes)
  private static final int CHAR_BUFFER_SIZE = 0x800;

  private static final ThreadLocal<SoftReference<TransferBuffer<byte[]>>> byteArrayBufferHolder = new ThreadLocal<>();
  private static final ThreadLocal<SoftReference<TransferBuffer<char[]>>> charArrayBufferHolder = new ThreadLocal<>();

  private final T array;
  private boolean inUse = true; //transferBuffers will always be used right after creation

  private TransferBuffer(T array) {
    this.array = array;
  }

  /**
   * Returns a {@link TransferBuffer} of byte[], which is guaranteed to be thread-local
   */
  static TransferBuffer<byte[]> getByteArrayTransferBuffer() {
    final SoftReference<TransferBuffer<byte[]>> bufferReference = byteArrayBufferHolder.get();

    if (bufferReference == null) {
      return createAndCacheByteBuffer();
    }

    TransferBuffer<byte[]> buffer = bufferReference.get();

    if (buffer == null) {
      return createAndCacheByteBuffer();
    }

    /*
    it could - in somewhat obscure scenarios - be possible that a single thread needs more than
    one buffer simultaneously. For example, let's assume there is a class called 'ConcatenatingInputStream' which
    subclasses java.io.InputStream. The implementation uses ByteStreams#copy(InputStream, OutputStream) to move data between streams,
    which means that the ConcatenatingInputStream#read()-method will use the TransferBuffer for that thread.
    If an instance of ConcatenatingInputStream is now being passed to ByteStreams#copy(InputStream, OutputStream),
    this would result in a double-use of the buffer, similar to 'use-after-free'-flaws in languages with manual
    memory allocation. In order to avoid this, a private flag 'inUse' was introduced to mark the buffer as being currently under use.
    If this flag is set on the current TransferBuffer, a new 'throwaway'-buffer is being created, which will
    not be stored thread-locally afterwards.
    */
    if (buffer.isInUse()) {
      return new TransferBuffer<>(new byte[BYTE_BUFFER_SIZE]);
    }

    Arrays.fill(buffer.get(), (byte) 0);
    buffer.setInUse();
    return buffer;
  }

  private static TransferBuffer<byte[]> createAndCacheByteBuffer() {
    TransferBuffer<byte[]> buffer = new TransferBuffer<>(new byte[BYTE_BUFFER_SIZE]);
    SoftReference<TransferBuffer<byte[]>> bufferReference = new SoftReference<>(buffer);
    byteArrayBufferHolder.set(bufferReference);
    return buffer;
  }

  /**
   * Returns a {@link TransferBuffer} of char[], which is guaranteed to be thread-local
   */
  static TransferBuffer<char[]> getCharArrayTransferBuffer() {
    final SoftReference<TransferBuffer<char[]>> bufferReference = charArrayBufferHolder.get();

    if (bufferReference == null) {
      return createAndCacheCharBuffer();
    }

    TransferBuffer<char[]> buffer = bufferReference.get();

    if (buffer == null) {
      return createAndCacheCharBuffer();
    }

    //see comment in getByteArrayTransferBuffer()
    if (buffer.isInUse()) {
      return new TransferBuffer<>(new char[CHAR_BUFFER_SIZE]);
    }

    Arrays.fill(buffer.get(), (char) 0);
    buffer.setInUse();

    return buffer;
  }

  private static TransferBuffer<char[]> createAndCacheCharBuffer() {
    TransferBuffer<char[]> buffer = new TransferBuffer<>(new char[CHAR_BUFFER_SIZE]);
    SoftReference<TransferBuffer<char[]>> bufferReference = new SoftReference<>(buffer);
    charArrayBufferHolder.set(bufferReference);
    return buffer;
  }

  @Override
  public void close() {
    inUse = false;
  }

  private void setInUse() {
    inUse = true;
  }

  private boolean isInUse() {
    return inUse;
  }

  @Override
  public T get() {
    return array;
  }
}
