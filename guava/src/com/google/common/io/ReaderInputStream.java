/*
 * Copyright (C) 2015 The Guava Authors
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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.primitives.UnsignedBytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

/**
 * An {@link InputStream} that converts characters from a {@link Reader} into bytes using an
 * arbitrary Charset.
 *
 * <p>This is an alternative to copying the data to an {@code OutputStream} via a {@code Writer},
 * which is necessarily blocking. By implementing an {@code InputStream} it allows consumers to
 * "pull" as much data as they can handle, which is more convenient when dealing with flow
 * controlled, async APIs.
 *
 * @author Chris Nokleberg
 */
@GwtIncompatible
final class ReaderInputStream extends InputStream {
  private final Reader reader;
  private final CharsetEncoder encoder;
  private final byte[] singleByte = new byte[1];

  /**
   * charBuffer holds characters that have been read from the Reader but not encoded yet. The buffer
   * is perpetually "flipped" (unencoded characters between position and limit).
   */
  private CharBuffer charBuffer;

  /**
   * byteBuffer holds encoded characters that have not yet been sent to the caller of the input
   * stream. When encoding it is "unflipped" (encoded bytes between 0 and position) and when
   * draining it is flipped (undrained bytes between position and limit).
   */
  private ByteBuffer byteBuffer;

  /** Whether we've finished reading the reader. */
  private boolean endOfInput;
  /** Whether we're copying encoded bytes to the caller's buffer. */
  private boolean draining;
  /** Whether we've successfully flushed the encoder. */
  private boolean doneFlushing;

  /**
   * Creates a new input stream that will encode the characters from {@code reader} into bytes using
   * the given character set. Malformed input and unmappable characters will be replaced.
   *
   * @param reader input source
   * @param charset character set used for encoding chars to bytes
   * @param bufferSize size of internal input and output buffers
   * @throws IllegalArgumentException if bufferSize is non-positive
   */
  ReaderInputStream(Reader reader, Charset charset, int bufferSize) {
    this(
        reader,
        charset
            .newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE),
        bufferSize);
  }

  /**
   * Creates a new input stream that will encode the characters from {@code reader} into bytes using
   * the given character set encoder.
   *
   * @param reader input source
   * @param encoder character set encoder used for encoding chars to bytes
   * @param bufferSize size of internal input and output buffers
   * @throws IllegalArgumentException if bufferSize is non-positive
   */
  ReaderInputStream(Reader reader, CharsetEncoder encoder, int bufferSize) {
    this.reader = checkNotNull(reader);
    this.encoder = checkNotNull(encoder);
    checkArgument(bufferSize > 0, "bufferSize must be positive: %s", bufferSize);
    encoder.reset();

    charBuffer = CharBuffer.allocate(bufferSize);
    charBuffer.flip();

    byteBuffer = ByteBuffer.allocate(bufferSize);
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  @Override
  public int read() throws IOException {
    return (read(singleByte) == 1) ? UnsignedBytes.toInt(singleByte[0]) : -1;
  }

  // TODO(chrisn): Consider trying to encode/flush directly to the argument byte
  // buffer when possible.
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    // Obey InputStream contract.
    checkPositionIndexes(off, off + len, b.length);
    if (len == 0) {
      return 0;
    }

    // The rest of this method implements the process described by the CharsetEncoder javadoc.
    int totalBytesRead = 0;
    boolean doneEncoding = endOfInput;

    DRAINING:
    while (true) {
      // We stay in draining mode until there are no bytes left in the output buffer. Then we go
      // back to encoding/flushing.
      if (draining) {
        totalBytesRead += drain(b, off + totalBytesRead, len - totalBytesRead);
        if (totalBytesRead == len || doneFlushing) {
          return (totalBytesRead > 0) ? totalBytesRead : -1;
        }
        draining = false;
        byteBuffer.clear();
      }

      while (true) {
        // We call encode until there is no more input. The last call to encode will have endOfInput
        // == true. Then there is a final call to flush.
        CoderResult result;
        if (doneFlushing) {
          result = CoderResult.UNDERFLOW;
        } else if (doneEncoding) {
          result = encoder.flush(byteBuffer);
        } else {
          result = encoder.encode(charBuffer, byteBuffer, endOfInput);
        }

        if (result.isOverflow()) {
          // Not enough room in output buffer--drain it, creating a bigger buffer if necessary.
          startDraining(true);
          continue DRAINING;
        } else if (result.isUnderflow()) {
          // If encoder underflows, it means either:
          // a) the final flush() succeeded; next drain (then done)
          // b) we encoded all of the input; next flush
          // c) we ran of out input to encode; next read more input
          if (doneEncoding) { // (a)
            doneFlushing = true;
            startDraining(false);
            continue DRAINING;
          } else if (endOfInput) { // (b)
            doneEncoding = true;
          } else { // (c)
            readMoreChars();
          }
        } else if (result.isError()) {
          // Only reach here if a CharsetEncoder with non-REPLACE settings is used.
          result.throwException();
          return 0; // Not called.
        }
      }
    }
  }

  /** Returns a new CharBuffer identical to buf, except twice the capacity. */
  private static CharBuffer grow(CharBuffer buf) {
    char[] copy = Arrays.copyOf(buf.array(), buf.capacity() * 2);
    CharBuffer bigger = CharBuffer.wrap(copy);
    bigger.position(buf.position());
    bigger.limit(buf.limit());
    return bigger;
  }

  /** Handle the case of underflow caused by needing more input characters. */
  private void readMoreChars() throws IOException {
    // Possibilities:
    // 1) array has space available on right hand side (between limit and capacity)
    // 2) array has space available on left hand side (before position)
    // 3) array has no space available
    //
    // In case 2 we shift the existing chars to the left, and in case 3 we create a bigger
    // array, then they both become case 1.

    if (availableCapacity(charBuffer) == 0) {
      if (charBuffer.position() > 0) {
        // (2) There is room in the buffer. Move existing bytes to the beginning.
        charBuffer.compact().flip();
      } else {
        // (3) Entire buffer is full, need bigger buffer.
        charBuffer = grow(charBuffer);
      }
    }

    // (1) Read more characters into free space at end of array.
    int limit = charBuffer.limit();
    int numChars = reader.read(charBuffer.array(), limit, availableCapacity(charBuffer));
    if (numChars == -1) {
      endOfInput = true;
    } else {
      charBuffer.limit(limit + numChars);
    }
  }

  /** Returns the number of elements between the limit and capacity. */
  private static int availableCapacity(Buffer buffer) {
    return buffer.capacity() - buffer.limit();
  }

  /**
   * Flips the buffer output buffer so we can start reading bytes from it. If we are starting to
   * drain because there was overflow, and there aren't actually any characters to drain, then the
   * overflow must be due to a small output buffer.
   */
  private void startDraining(boolean overflow) {
    byteBuffer.flip();
    if (overflow && byteBuffer.remaining() == 0) {
      byteBuffer = ByteBuffer.allocate(byteBuffer.capacity() * 2);
    } else {
      draining = true;
    }
  }

  /**
   * Copy as much of the byte buffer into the output array as possible, returning the (positive)
   * number of characters copied.
   */
  private int drain(byte[] b, int off, int len) {
    int remaining = Math.min(len, byteBuffer.remaining());
    byteBuffer.get(b, off, remaining);
    return remaining;
  }
}
