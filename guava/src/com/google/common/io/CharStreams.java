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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides utility methods for working with character streams.
 *
 * <p>All method parameters must be non-null unless documented otherwise.
 *
 * <p>Some of the methods in this class take arguments with a generic type of
 * {@code Readable & Closeable}. A {@link java.io.Reader} implements both of
 * those interfaces. Similarly for {@code Appendable & Closeable} and
 * {@link java.io.Writer}.
 *
 * @author Chris Nokleberg
 * @author Bin Zhu
 * @author Colin Decker
 * @since 1.0
 */
@Beta
public final class CharStreams {
  private static final int BUF_SIZE = 0x800; // 2K chars (4K bytes)

  private CharStreams() {}

  /**
   * Returns a factory that will supply instances of {@link StringReader} that
   * read a string value.
   *
   * @param value the string to read
   * @return the factory
   */
  public static InputSupplier<StringReader> newReaderSupplier(
      final String value) {
    return CharStreams.asInputSupplier(asCharSource(value));
  }

  /**
   * Returns a {@link CharSource} that reads the given string value.
   *
   * @since 14.0
   */
  public static CharSource asCharSource(String string) {
    return new StringCharSource(string);
  }

  private static final class StringCharSource extends CharSource {

    private static final Splitter LINE_SPLITTER
        = Splitter.on(Pattern.compile("\r\n|\n|\r"));

    private final String string;

    private StringCharSource(String string) {
      this.string = checkNotNull(string);
    }

    @Override
    public Reader openStream() {
      return new StringReader(string);
    }

    @Override
    public String read() {
      return string;
    }

    /**
     * Returns an iterable over the lines in the string. If the string ends in
     * a newline, a final empty string is not included to match the behavior of
     * BufferedReader/LineReader.readLine().
     */
    private Iterable<String> lines() {
      return new Iterable<String>() {
        @Override
        public Iterator<String> iterator() {
          return new AbstractIterator<String>() {
            Iterator<String> lines = LINE_SPLITTER.split(string).iterator();

            @Override
            protected String computeNext() {
              if (lines.hasNext()) {
                String next = lines.next();
                // skip last line if it's empty
                if (lines.hasNext() || !next.isEmpty()) {
                  return next;
                }
              }
              return endOfData();
            }
          };
        }
      };
    }

    @Override
    public String readFirstLine() {
      Iterator<String> lines = lines().iterator();
      return lines.hasNext() ? lines.next() : null;
    }

    @Override
    public ImmutableList<String> readLines() {
      return ImmutableList.copyOf(lines());
    }

    @Override
    public String toString() {
      String limited = (string.length() <= 15)
          ? string
          : string.substring(0, 12) + "...";
      return "CharStreams.asCharSource(" + limited + ")";
    }
  }

  /**
   * Returns a factory that will supply instances of {@link InputStreamReader},
   * using the given {@link InputStream} factory and character set.
   *
   * @param in the factory that will be used to open input streams
   * @param charset the charset used to decode the input stream; see {@link
   *     Charsets} for helpful predefined constants
   * @return the factory
   */
  public static InputSupplier<InputStreamReader> newReaderSupplier(
      final InputSupplier<? extends InputStream> in, final Charset charset) {
    return CharStreams.asInputSupplier(
        ByteStreams.asByteSource(in).asCharSource(charset));
  }

  /**
   * Returns a factory that will supply instances of {@link OutputStreamWriter},
   * using the given {@link OutputStream} factory and character set.
   *
   * @param out the factory that will be used to open output streams
   * @param charset the charset used to encode the output stream; see {@link
   *     Charsets} for helpful predefined constants
   * @return the factory
   */
  public static OutputSupplier<OutputStreamWriter> newWriterSupplier(
      final OutputSupplier<? extends OutputStream> out, final Charset charset) {
    return CharStreams.asOutputSupplier(
        ByteStreams.asByteSink(out).asCharSink(charset));
  }

  /**
   * Writes a character sequence (such as a string) to an appendable
   * object from the given supplier.
   *
   * @param from the character sequence to write
   * @param to the output supplier
   * @throws IOException if an I/O error occurs
   */
  public static <W extends Appendable & Closeable> void write(CharSequence from,
      OutputSupplier<W> to) throws IOException {
    asCharSink(to).write(from);
  }

  /**
   * Opens {@link Readable} and {@link Appendable} objects from the
   * given factories, copies all characters between the two, and closes
   * them.
   *
   * @param from the input factory
   * @param to the output factory
   * @return the number of characters copied
   * @throws IOException if an I/O error occurs
   */
  public static <R extends Readable & Closeable,
      W extends Appendable & Closeable> long copy(InputSupplier<R> from,
      OutputSupplier<W> to) throws IOException {
    return asCharSource(from).copyTo(asCharSink(to));
  }

  /**
   * Opens a {@link Readable} object from the supplier, copies all characters
   * to the {@link Appendable} object, and closes the input. Does not close
   * or flush the output.
   *
   * @param from the input factory
   * @param to the object to write to
   * @return the number of characters copied
   * @throws IOException if an I/O error occurs
   */
  public static <R extends Readable & Closeable> long copy(
      InputSupplier<R> from, Appendable to) throws IOException {
    return asCharSource(from).copyTo(to);
  }

  /**
   * Copies all characters between the {@link Readable} and {@link Appendable}
   * objects. Does not close or flush either object.
   *
   * @param from the object to read from
   * @param to the object to write to
   * @return the number of characters copied
   * @throws IOException if an I/O error occurs
   */
  public static long copy(Readable from, Appendable to) throws IOException {
    checkNotNull(from);
    checkNotNull(to);
    CharBuffer buf = CharBuffer.allocate(BUF_SIZE);
    long total = 0;
    while (from.read(buf) != -1) {
      buf.flip();
      to.append(buf);
      total += buf.remaining();
      buf.clear();
    }
    return total;
  }

  /**
   * Reads all characters from a {@link Readable} object into a {@link String}.
   * Does not close the {@code Readable}.
   *
   * @param r the object to read from
   * @return a string containing all the characters
   * @throws IOException if an I/O error occurs
   */
  public static String toString(Readable r) throws IOException {
    return toStringBuilder(r).toString();
  }

  /**
   * Returns the characters from a {@link Readable} & {@link Closeable} object
   * supplied by a factory as a {@link String}.
   *
   * @param supplier the factory to read from
   * @return a string containing all the characters
   * @throws IOException if an I/O error occurs
   */
  public static <R extends Readable & Closeable> String toString(
      InputSupplier<R> supplier) throws IOException {
    return asCharSource(supplier).read();
  }

  /**
   * Reads all characters from a {@link Readable} object into a new
   * {@link StringBuilder} instance. Does not close the {@code Readable}.
   *
   * @param r the object to read from
   * @return a {@link StringBuilder} containing all the characters
   * @throws IOException if an I/O error occurs
   */
  private static StringBuilder toStringBuilder(Readable r) throws IOException {
    StringBuilder sb = new StringBuilder();
    copy(r, sb);
    return sb;
  }

  /**
   * Reads the first line from a {@link Readable} & {@link Closeable} object
   * supplied by a factory. The line does not include line-termination
   * characters, but does include other leading and trailing whitespace.
   *
   * @param supplier the factory to read from
   * @return the first line, or null if the reader is empty
   * @throws IOException if an I/O error occurs
   */
  public static <R extends Readable & Closeable> String readFirstLine(
      InputSupplier<R> supplier) throws IOException {
    return asCharSource(supplier).readFirstLine();
  }

  /**
   * Reads all of the lines from a {@link Readable} & {@link Closeable} object
   * supplied by a factory. The lines do not include line-termination
   * characters, but do include other leading and trailing whitespace.
   *
   * @param supplier the factory to read from
   * @return a mutable {@link List} containing all the lines
   * @throws IOException if an I/O error occurs
   */
  public static <R extends Readable & Closeable> List<String> readLines(
      InputSupplier<R> supplier) throws IOException {
    Closer closer = Closer.create();
    try {
      R r = closer.register(supplier.getInput());
      return readLines(r);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Reads all of the lines from a {@link Readable} object. The lines do
   * not include line-termination characters, but do include other
   * leading and trailing whitespace.
   *
   * <p>Does not close the {@code Readable}. If reading files or resources you
   * should use the {@link Files#readLines} and {@link Resources#readLines}
   * methods.
   *
   * @param r the object to read from
   * @return a mutable {@link List} containing all the lines
   * @throws IOException if an I/O error occurs
   */
  public static List<String> readLines(Readable r) throws IOException {
    List<String> result = new ArrayList<String>();
    LineReader lineReader = new LineReader(r);
    String line;
    while ((line = lineReader.readLine()) != null) {
      result.add(line);
    }
    return result;
  }

  /**
   * Streams lines from a {@link Readable} object, stopping when the processor
   * returns {@code false} or all lines have been read and returning the result
   * produced by the processor. Does not close {@code readable}. Note that this
   * method may not fully consume the contents of {@code readable} if the
   * processor stops processing early.
   *
   * @throws IOException if an I/O error occurs
   * @since 14.0
   */
  public static <T> T readLines(
      Readable readable, LineProcessor<T> processor) throws IOException {
    checkNotNull(readable);
    checkNotNull(processor);

    LineReader lineReader = new LineReader(readable);
    String line;
    while ((line = lineReader.readLine()) != null) {
      if (!processor.processLine(line)) {
        break;
      }
    }
    return processor.getResult();
  }

  /**
   * Streams lines from a {@link Readable} and {@link Closeable} object
   * supplied by a factory, stopping when our callback returns false, or we
   * have read all of the lines.
   *
   * @param supplier the factory to read from
   * @param callback the LineProcessor to use to handle the lines
   * @return the output of processing the lines
   * @throws IOException if an I/O error occurs
   */
  public static <R extends Readable & Closeable, T> T readLines(
      InputSupplier<R> supplier, LineProcessor<T> callback) throws IOException {
    checkNotNull(supplier);
    checkNotNull(callback);

    Closer closer = Closer.create();
    try {
      R r = closer.register(supplier.getInput());
      return readLines(r, callback);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Joins multiple {@link Reader} suppliers into a single supplier.
   * Reader returned from the supplier will contain the concatenated data
   * from the readers of the underlying suppliers.
   *
   * <p>Reading from the joined reader will throw a {@link NullPointerException}
   * if any of the suppliers are null or return null.
   *
   * <p>Only one underlying reader will be open at a time. Closing the
   * joined reader will close the open underlying reader.
   *
   * @param suppliers the suppliers to concatenate
   * @return a supplier that will return a reader containing the concatenated
   *     data
   */
  public static InputSupplier<Reader> join(
      final Iterable<? extends InputSupplier<? extends Reader>> suppliers) {
    checkNotNull(suppliers);
    return new InputSupplier<Reader>() {
      @Override public Reader getInput() throws IOException {
        return new MultiReader(suppliers.iterator());
      }
    };
  }

  /** Varargs form of {@link #join(Iterable)}. */
  public static InputSupplier<Reader> join(
      InputSupplier<? extends Reader>... suppliers) {
    return join(Arrays.asList(suppliers));
  }

  /**
   * Discards {@code n} characters of data from the reader. This method
   * will block until the full amount has been skipped. Does not close the
   * reader.
   *
   * @param reader the reader to read from
   * @param n the number of characters to skip
   * @throws EOFException if this stream reaches the end before skipping all
   *     the bytes
   * @throws IOException if an I/O error occurs
   */
  public static void skipFully(Reader reader, long n) throws IOException {
    checkNotNull(reader);
    while (n > 0) {
      long amt = reader.skip(n);
      if (amt == 0) {
        // force a blocking read
        if (reader.read() == -1) {
          throw new EOFException();
        }
        n--;
      } else {
        n -= amt;
      }
    }
  }

  /**
   * Returns a Writer that sends all output to the given {@link Appendable}
   * target. Closing the writer will close the target if it is {@link
   * Closeable}, and flushing the writer will flush the target if it is {@link
   * java.io.Flushable}.
   *
   * @param target the object to which output will be sent
   * @return a new Writer object, unless target is a Writer, in which case the
   *     target is returned
   */
  public static Writer asWriter(Appendable target) {
    if (target instanceof Writer) {
      return (Writer) target;
    }
    return new AppendableWriter(target);
  }

  // TODO(user): Remove these once Input/OutputSupplier methods are removed

  static <R extends Readable & Closeable> Reader asReader(final R readable) {
    checkNotNull(readable);
    if (readable instanceof Reader) {
      return (Reader) readable;
    }
    return new Reader() {
      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        return read(CharBuffer.wrap(cbuf, off, len));
      }

      @Override
      public int read(CharBuffer target) throws IOException {
        return readable.read(target);
      }

      @Override
      public void close() throws IOException {
        readable.close();
      }
    };
  }

  static <R extends Reader> InputSupplier<R> asInputSupplier(
      final CharSource source) {
    checkNotNull(source);
    return new InputSupplier<R>() {
      @Override
      public R getInput() throws IOException {
        return (R) source.openStream();
      }
    };
  }

  static <W extends Writer> OutputSupplier<W> asOutputSupplier(
      final CharSink sink) {
    checkNotNull(sink);
    return new OutputSupplier<W>() {
      @Override
      public W getOutput() throws IOException {
        return (W) sink.openStream();
      }
    };
  }

  static <R extends Readable & Closeable> CharSource asCharSource(
      final InputSupplier<R> supplier) {
    checkNotNull(supplier);
    return new CharSource() {
      @Override
      public Reader openStream() throws IOException {
        return asReader(supplier.getInput());
      }
    };
  }

  static <W extends Appendable & Closeable> CharSink asCharSink(
      final OutputSupplier<W> supplier) {
    checkNotNull(supplier);
    return new CharSink() {
      @Override
      public Writer openStream() throws IOException {
        return asWriter(supplier.getOutput());
      }
    };
  }
}
