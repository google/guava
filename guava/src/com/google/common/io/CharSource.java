/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ascii;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.MustBeClosed;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A readable source of characters, such as a text file. Unlike a {@link Reader}, a {@code
 * CharSource} is not an open, stateful stream of characters that can be read and closed. Instead,
 * it is an immutable <i>supplier</i> of {@code Reader} instances.
 *
 * <p>{@code CharSource} provides two kinds of methods:
 *
 * <ul>
 *   <li><b>Methods that return a reader:</b> These methods should return a <i>new</i>, independent
 *       instance each time they are called. The caller is responsible for ensuring that the
 *       returned reader is closed.
 *   <li><b>Convenience methods:</b> These are implementations of common operations that are
 *       typically implemented by opening a reader using one of the methods in the first category,
 *       doing something and finally closing the reader that was opened.
 * </ul>
 *
 * <p>Several methods in this class, such as {@link #readLines()}, break the contents of the source
 * into lines. Like {@link BufferedReader}, these methods break lines on any of {@code \n}, {@code
 * \r} or {@code \r\n}, do not include the line separator in each line and do not consider there to
 * be an empty line at the end if the contents are terminated with a line separator.
 *
 * <p>Any {@link ByteSource} containing text encoded with a specific {@linkplain Charset character
 * encoding} may be viewed as a {@code CharSource} using {@link ByteSource#asCharSource(Charset)}.
 *
 * <p><b>Note:</b> In general, {@code CharSource} is intended to be used for "file-like" sources
 * that provide readers that are:
 *
 * <ul>
 *   <li><b>Finite:</b> Many operations, such as {@link #length()} and {@link #read()}, will either
 *       block indefinitely or fail if the source creates an infinite reader.
 *   <li><b>Non-destructive:</b> A <i>destructive</i> reader will consume or otherwise alter the
 *       source as they are read from it. A source that provides such readers will not be reusable,
 *       and operations that read from the stream (including {@link #length()}, in some
 *       implementations) will prevent further operations from completing as expected.
 * </ul>
 *
 * @since 14.0
 * @author Colin Decker
 */
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public abstract class CharSource {

  /** Constructor for use by subclasses. */
  protected CharSource() {}

  /**
   * Returns a {@link ByteSource} view of this char source that encodes chars read from this source
   * as bytes using the given {@link Charset}.
   *
   * <p>If {@link ByteSource#asCharSource} is called on the returned source with the same charset,
   * the default implementation of this method will ensure that the original {@code CharSource} is
   * returned, rather than round-trip encoding. Subclasses that override this method should behave
   * the same way.
   *
   * @since 20.0
   */
  @Beta
  public ByteSource asByteSource(Charset charset) {
    return new AsByteSource(charset);
  }

  /**
   * Opens a new {@link Reader} for reading from this source. This method returns a new, independent
   * reader each time it is called.
   *
   * <p>The caller is responsible for ensuring that the returned reader is closed.
   *
   * @throws IOException if an I/O error occurs while opening the reader
   */
  public abstract Reader openStream() throws IOException;

  /**
   * Opens a new {@link BufferedReader} for reading from this source. This method returns a new,
   * independent reader each time it is called.
   *
   * <p>The caller is responsible for ensuring that the returned reader is closed.
   *
   * @throws IOException if an I/O error occurs while of opening the reader
   */
  public BufferedReader openBufferedStream() throws IOException {
    Reader reader = openStream();
    return (reader instanceof BufferedReader)
        ? (BufferedReader) reader
        : new BufferedReader(reader);
  }

  /**
   * Opens a new {@link Stream} for reading text one line at a time from this source. This method
   * returns a new, independent stream each time it is called.
   *
   * <p>The returned stream is lazy and only reads from the source in the terminal operation. If an
   * I/O error occurs while the stream is reading from the source or when the stream is closed, an
   * {@link UncheckedIOException} is thrown.
   *
   * <p>Like {@link BufferedReader#readLine()}, this method considers a line to be a sequence of
   * text that is terminated by (but does not include) one of {@code \r\n}, {@code \r} or {@code
   * \n}. If the source's content does not end in a line termination sequence, it is treated as if
   * it does.
   *
   * <p>The caller is responsible for ensuring that the returned stream is closed. For example:
   *
   * <pre>{@code
   * try (Stream<String> lines = source.lines()) {
   *   lines.map(...)
   *      .filter(...)
   *      .forEach(...);
   * }
   * }</pre>
   *
   * @throws IOException if an I/O error occurs while opening the stream
   * @since 22.0
   */
  @Beta
  @MustBeClosed
  public Stream<String> lines() throws IOException {
    BufferedReader reader = openBufferedStream();
    return reader
        .lines()
        .onClose(
            () -> {
              try {
                reader.close();
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
  }

  /**
   * Returns the size of this source in chars, if the size can be easily determined without actually
   * opening the data stream.
   *
   * <p>The default implementation returns {@link Optional#absent}. Some sources, such as a {@code
   * CharSequence}, may return a non-absent value. Note that in such cases, it is <i>possible</i>
   * that this method will return a different number of chars than would be returned by reading all
   * of the chars.
   *
   * <p>Additionally, for mutable sources such as {@code StringBuilder}s, a subsequent read may
   * return a different number of chars if the contents are changed.
   *
   * @since 19.0
   */
  @Beta
  public Optional<Long> lengthIfKnown() {
    return Optional.absent();
  }

  /**
   * Returns the length of this source in chars, even if doing so requires opening and traversing an
   * entire stream. To avoid a potentially expensive operation, see {@link #lengthIfKnown}.
   *
   * <p>The default implementation calls {@link #lengthIfKnown} and returns the value if present. If
   * absent, it will fall back to a heavyweight operation that will open a stream, {@link
   * Reader#skip(long) skip} to the end of the stream, and return the total number of chars that
   * were skipped.
   *
   * <p>Note that for sources that implement {@link #lengthIfKnown} to provide a more efficient
   * implementation, it is <i>possible</i> that this method will return a different number of chars
   * than would be returned by reading all of the chars.
   *
   * <p>In either case, for mutable sources such as files, a subsequent read may return a different
   * number of chars if the contents are changed.
   *
   * @throws IOException if an I/O error occurs while reading the length of this source
   * @since 19.0
   */
  @Beta
  public long length() throws IOException {
    Optional<Long> lengthIfKnown = lengthIfKnown();
    if (lengthIfKnown.isPresent()) {
      return lengthIfKnown.get();
    }

    Closer closer = Closer.create();
    try {
      Reader reader = closer.register(openStream());
      return countBySkipping(reader);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  private long countBySkipping(Reader reader) throws IOException {
    long count = 0;
    long read;
    while ((read = reader.skip(Long.MAX_VALUE)) != 0) {
      count += read;
    }
    return count;
  }

  /**
   * Appends the contents of this source to the given {@link Appendable} (such as a {@link Writer}).
   * Does not close {@code appendable} if it is {@code Closeable}.
   *
   * @return the number of characters copied
   * @throws IOException if an I/O error occurs while reading from this source or writing to {@code
   *     appendable}
   */
  @CanIgnoreReturnValue
  public long copyTo(Appendable appendable) throws IOException {
    checkNotNull(appendable);

    Closer closer = Closer.create();
    try {
      Reader reader = closer.register(openStream());
      return CharStreams.copy(reader, appendable);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Copies the contents of this source to the given sink.
   *
   * @return the number of characters copied
   * @throws IOException if an I/O error occurs while reading from this source or writing to {@code
   *     sink}
   */
  @CanIgnoreReturnValue
  public long copyTo(CharSink sink) throws IOException {
    checkNotNull(sink);

    Closer closer = Closer.create();
    try {
      Reader reader = closer.register(openStream());
      Writer writer = closer.register(sink.openStream());
      return CharStreams.copy(reader, writer);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Reads the contents of this source as a string.
   *
   * @throws IOException if an I/O error occurs while reading from this source
   */
  public String read() throws IOException {
    Closer closer = Closer.create();
    try {
      Reader reader = closer.register(openStream());
      return CharStreams.toString(reader);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Reads the first line of this source as a string. Returns {@code null} if this source is empty.
   *
   * <p>Like {@link BufferedReader#readLine()}, this method considers a line to be a sequence of
   * text that is terminated by (but does not include) one of {@code \r\n}, {@code \r} or {@code
   * \n}. If the source's content does not end in a line termination sequence, it is treated as if
   * it does.
   *
   * @throws IOException if an I/O error occurs while reading from this source
   */
  @CheckForNull
  public String readFirstLine() throws IOException {
    Closer closer = Closer.create();
    try {
      BufferedReader reader = closer.register(openBufferedStream());
      return reader.readLine();
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Reads all the lines of this source as a list of strings. The returned list will be empty if
   * this source is empty.
   *
   * <p>Like {@link BufferedReader#readLine()}, this method considers a line to be a sequence of
   * text that is terminated by (but does not include) one of {@code \r\n}, {@code \r} or {@code
   * \n}. If the source's content does not end in a line termination sequence, it is treated as if
   * it does.
   *
   * @throws IOException if an I/O error occurs while reading from this source
   */
  public ImmutableList<String> readLines() throws IOException {
    Closer closer = Closer.create();
    try {
      BufferedReader reader = closer.register(openBufferedStream());
      List<String> result = Lists.newArrayList();
      String line;
      while ((line = reader.readLine()) != null) {
        result.add(line);
      }
      return ImmutableList.copyOf(result);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Reads lines of text from this source, processing each line as it is read using the given {@link
   * LineProcessor processor}. Stops when all lines have been processed or the processor returns
   * {@code false} and returns the result produced by the processor.
   *
   * <p>Like {@link BufferedReader#readLine()}, this method considers a line to be a sequence of
   * text that is terminated by (but does not include) one of {@code \r\n}, {@code \r} or {@code
   * \n}. If the source's content does not end in a line termination sequence, it is treated as if
   * it does.
   *
   * @throws IOException if an I/O error occurs while reading from this source or if {@code
   *     processor} throws an {@code IOException}
   * @since 16.0
   */
  @Beta
  @CanIgnoreReturnValue // some processors won't return a useful result
  @ParametricNullness
  public <T extends @Nullable Object> T readLines(LineProcessor<T> processor) throws IOException {
    checkNotNull(processor);

    Closer closer = Closer.create();
    try {
      Reader reader = closer.register(openStream());
      return CharStreams.readLines(reader, processor);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Reads all lines of text from this source, running the given {@code action} for each line as it
   * is read.
   *
   * <p>Like {@link BufferedReader#readLine()}, this method considers a line to be a sequence of
   * text that is terminated by (but does not include) one of {@code \r\n}, {@code \r} or {@code
   * \n}. If the source's content does not end in a line termination sequence, it is treated as if
   * it does.
   *
   * @throws IOException if an I/O error occurs while reading from this source or if {@code action}
   *     throws an {@code UncheckedIOException}
   * @since 22.0
   */
  @Beta
  public void forEachLine(Consumer<? super String> action) throws IOException {
    try (Stream<String> lines = lines()) {
      // The lines should be ordered regardless in most cases, but use forEachOrdered to be sure
      lines.forEachOrdered(action);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  /**
   * Returns whether the source has zero chars. The default implementation first checks {@link
   * #lengthIfKnown}, returning true if it's known to be zero and false if it's known to be
   * non-zero. If the length is not known, it falls back to opening a stream and checking for EOF.
   *
   * <p>Note that, in cases where {@code lengthIfKnown} returns zero, it is <i>possible</i> that
   * chars are actually available for reading. This means that a source may return {@code true} from
   * {@code isEmpty()} despite having readable content.
   *
   * @throws IOException if an I/O error occurs
   * @since 15.0
   */
  public boolean isEmpty() throws IOException {
    Optional<Long> lengthIfKnown = lengthIfKnown();
    if (lengthIfKnown.isPresent()) {
      return lengthIfKnown.get() == 0L;
    }
    Closer closer = Closer.create();
    try {
      Reader reader = closer.register(openStream());
      return reader.read() == -1;
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Concatenates multiple {@link CharSource} instances into a single source. Streams returned from
   * the source will contain the concatenated data from the streams of the underlying sources.
   *
   * <p>Only one underlying stream will be open at a time. Closing the concatenated stream will
   * close the open underlying stream.
   *
   * @param sources the sources to concatenate
   * @return a {@code CharSource} containing the concatenated data
   * @since 15.0
   */
  public static CharSource concat(Iterable<? extends CharSource> sources) {
    return new ConcatenatedCharSource(sources);
  }

  /**
   * Concatenates multiple {@link CharSource} instances into a single source. Streams returned from
   * the source will contain the concatenated data from the streams of the underlying sources.
   *
   * <p>Only one underlying stream will be open at a time. Closing the concatenated stream will
   * close the open underlying stream.
   *
   * <p>Note: The input {@code Iterator} will be copied to an {@code ImmutableList} when this method
   * is called. This will fail if the iterator is infinite and may cause problems if the iterator
   * eagerly fetches data for each source when iterated (rather than producing sources that only
   * load data through their streams). Prefer using the {@link #concat(Iterable)} overload if
   * possible.
   *
   * @param sources the sources to concatenate
   * @return a {@code CharSource} containing the concatenated data
   * @throws NullPointerException if any of {@code sources} is {@code null}
   * @since 15.0
   */
  public static CharSource concat(Iterator<? extends CharSource> sources) {
    return concat(ImmutableList.copyOf(sources));
  }

  /**
   * Concatenates multiple {@link CharSource} instances into a single source. Streams returned from
   * the source will contain the concatenated data from the streams of the underlying sources.
   *
   * <p>Only one underlying stream will be open at a time. Closing the concatenated stream will
   * close the open underlying stream.
   *
   * @param sources the sources to concatenate
   * @return a {@code CharSource} containing the concatenated data
   * @throws NullPointerException if any of {@code sources} is {@code null}
   * @since 15.0
   */
  public static CharSource concat(CharSource... sources) {
    return concat(ImmutableList.copyOf(sources));
  }

  /**
   * Returns a view of the given character sequence as a {@link CharSource}. The behavior of the
   * returned {@code CharSource} and any {@code Reader} instances created by it is unspecified if
   * the {@code charSequence} is mutated while it is being read, so don't do that.
   *
   * @since 15.0 (since 14.0 as {@code CharStreams.asCharSource(String)})
   */
  public static CharSource wrap(CharSequence charSequence) {
    return charSequence instanceof String
        ? new StringCharSource((String) charSequence)
        : new CharSequenceCharSource(charSequence);
  }

  /**
   * Returns an immutable {@link CharSource} that contains no characters.
   *
   * @since 15.0
   */
  public static CharSource empty() {
    return EmptyCharSource.INSTANCE;
  }

  /** A byte source that reads chars from this source and encodes them as bytes using a charset. */
  private final class AsByteSource extends ByteSource {

    final Charset charset;

    AsByteSource(Charset charset) {
      this.charset = checkNotNull(charset);
    }

    @Override
    public CharSource asCharSource(Charset charset) {
      if (charset.equals(this.charset)) {
        return CharSource.this;
      }
      return super.asCharSource(charset);
    }

    @Override
    public InputStream openStream() throws IOException {
      return new ReaderInputStream(CharSource.this.openStream(), charset, 8192);
    }

    @Override
    public String toString() {
      return CharSource.this.toString() + ".asByteSource(" + charset + ")";
    }
  }

  private static class CharSequenceCharSource extends CharSource {

    private static final Splitter LINE_SPLITTER = Splitter.onPattern("\r\n|\n|\r");

    protected final CharSequence seq;

    protected CharSequenceCharSource(CharSequence seq) {
      this.seq = checkNotNull(seq);
    }

    @Override
    public Reader openStream() {
      return new CharSequenceReader(seq);
    }

    @Override
    public String read() {
      return seq.toString();
    }

    @Override
    public boolean isEmpty() {
      return seq.length() == 0;
    }

    @Override
    public long length() {
      return seq.length();
    }

    @Override
    public Optional<Long> lengthIfKnown() {
      return Optional.of((long) seq.length());
    }

    /**
     * Returns an iterator over the lines in the string. If the string ends in a newline, a final
     * empty string is not included, to match the behavior of BufferedReader/LineReader.readLine().
     */
    private Iterator<String> linesIterator() {
      return new AbstractIterator<String>() {
        Iterator<String> lines = LINE_SPLITTER.split(seq).iterator();

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

    @Override
    public Stream<String> lines() {
      return Streams.stream(linesIterator());
    }

    @Override
    @CheckForNull
    public String readFirstLine() {
      Iterator<String> lines = linesIterator();
      return lines.hasNext() ? lines.next() : null;
    }

    @Override
    public ImmutableList<String> readLines() {
      return ImmutableList.copyOf(linesIterator());
    }

    @Override
    @ParametricNullness
    public <T extends @Nullable Object> T readLines(LineProcessor<T> processor) throws IOException {
      Iterator<String> lines = linesIterator();
      while (lines.hasNext()) {
        if (!processor.processLine(lines.next())) {
          break;
        }
      }
      return processor.getResult();
    }

    @Override
    public String toString() {
      return "CharSource.wrap(" + Ascii.truncate(seq, 30, "...") + ")";
    }
  }

  /**
   * Subclass specialized for string instances.
   *
   * <p>Since Strings are immutable and built into the jdk we can optimize some operations
   *
   * <ul>
   *   <li>use {@link StringReader} instead of {@link CharSequenceReader}. It is faster since it can
   *       use {@link String#getChars(int, int, char[], int)} instead of copying characters one by
   *       one with {@link CharSequence#charAt(int)}.
   *   <li>use {@link Appendable#append(CharSequence)} in {@link #copyTo(Appendable)} and {@link
   *       #copyTo(CharSink)}. We know this is correct since strings are immutable and so the length
   *       can't change, and it is faster because many writers and appendables are optimized for
   *       appending string instances.
   * </ul>
   */
  private static class StringCharSource extends CharSequenceCharSource {
    protected StringCharSource(String seq) {
      super(seq);
    }

    @Override
    public Reader openStream() {
      return new StringReader((String) seq);
    }

    @Override
    public long copyTo(Appendable appendable) throws IOException {
      appendable.append(seq);
      return seq.length();
    }

    @Override
    public long copyTo(CharSink sink) throws IOException {
      checkNotNull(sink);
      Closer closer = Closer.create();
      try {
        Writer writer = closer.register(sink.openStream());
        writer.write((String) seq);
        return seq.length();
      } catch (Throwable e) {
        throw closer.rethrow(e);
      } finally {
        closer.close();
      }
    }
  }

  private static final class EmptyCharSource extends StringCharSource {

    private static final EmptyCharSource INSTANCE = new EmptyCharSource();

    private EmptyCharSource() {
      super("");
    }

    @Override
    public String toString() {
      return "CharSource.empty()";
    }
  }

  private static final class ConcatenatedCharSource extends CharSource {

    private final Iterable<? extends CharSource> sources;

    ConcatenatedCharSource(Iterable<? extends CharSource> sources) {
      this.sources = checkNotNull(sources);
    }

    @Override
    public Reader openStream() throws IOException {
      return new MultiReader(sources.iterator());
    }

    @Override
    public boolean isEmpty() throws IOException {
      for (CharSource source : sources) {
        if (!source.isEmpty()) {
          return false;
        }
      }
      return true;
    }

    @Override
    public Optional<Long> lengthIfKnown() {
      long result = 0L;
      for (CharSource source : sources) {
        Optional<Long> lengthIfKnown = source.lengthIfKnown();
        if (!lengthIfKnown.isPresent()) {
          return Optional.absent();
        }
        result += lengthIfKnown.get();
      }
      return Optional.of(result);
    }

    @Override
    public long length() throws IOException {
      long result = 0L;
      for (CharSource source : sources) {
        result += source.length();
      }
      return result;
    }

    @Override
    public String toString() {
      return "CharSource.concat(" + sources + ")";
    }
  }
}
