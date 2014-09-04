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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * A readable source of characters, such as a text file. Unlike a {@link Reader}, a
 * {@code CharSource} is not an open, stateful stream of characters that can be read and closed.
 * Instead, it is an immutable <i>supplier</i> of {@code Reader} instances.
 *
 * <p>{@code CharSource} provides two kinds of methods:
 * <ul>
 *   <li><b>Methods that return a reader:</b> These methods should return a <i>new</i>, independent
 *   instance each time they are called. The caller is responsible for ensuring that the returned
 *   reader is closed.
 *   <li><b>Convenience methods:</b> These are implementations of common operations that are
 *   typically implemented by opening a reader using one of the methods in the first category,
 *   doing something and finally closing the reader that was opened.
 * </ul>
 *
 * <p>Several methods in this class, such as {@link #readLines()}, break the contents of the
 * source into lines. Like {@link BufferedReader}, these methods break lines on any of {@code \n},
 * {@code \r} or {@code \r\n}, do not include the line separator in each line and do not consider
 * there to be an empty line at the end if the contents are terminated with a line separator.
 *
 * <p>Any {@link ByteSource} containing text encoded with a specific {@linkplain Charset character
 * encoding} may be viewed as a {@code CharSource} using {@link ByteSource#asCharSource(Charset)}.
 *
 * @since 14.0
 * @author Colin Decker
 */
public abstract class CharSource {

  /**
   * Constructor for use by subclasses.
   */
  protected CharSource() {}

  /**
   * Opens a new {@link Reader} for reading from this source. This method should return a new,
   * independent reader each time it is called.
   *
   * <p>The caller is responsible for ensuring that the returned reader is closed.
   *
   * @throws IOException if an I/O error occurs in the process of opening the reader
   */
  public abstract Reader openStream() throws IOException;

  /**
   * Opens a new {@link BufferedReader} for reading from this source. This method should return a
   * new, independent reader each time it is called.
   *
   * <p>The caller is responsible for ensuring that the returned reader is closed.
   *
   * @throws IOException if an I/O error occurs in the process of opening the reader
   */
  public BufferedReader openBufferedStream() throws IOException {
    Reader reader = openStream();
    return (reader instanceof BufferedReader)
        ? (BufferedReader) reader
        : new BufferedReader(reader);
  }

  /**
   * Appends the contents of this source to the given {@link Appendable} (such as a {@link Writer}).
   * Does not close {@code appendable} if it is {@code Closeable}.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source or
   *     writing to {@code appendable}
   */
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
   * @throws IOException if an I/O error occurs in the process of reading from this source or
   *     writing to {@code sink}
   */
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
   * @throws IOException if an I/O error occurs in the process of reading from this source
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
   * Reads the first link of this source as a string. Returns {@code null} if this source is empty.
   *
   * <p>Like {@link BufferedReader}, this method breaks lines on any of {@code \n}, {@code \r} or
   * {@code \r\n}, does not include the line separator in the returned line and does not consider
   * there to be an extra empty line at the end if the content is terminated with a line separator.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source
   */
  @Nullable public String readFirstLine() throws IOException {
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
   * <p>Like {@link BufferedReader}, this method breaks lines on any of {@code \n}, {@code \r} or
   * {@code \r\n}, does not include the line separator in the returned lines and does not consider
   * there to be an extra empty line at the end if the content is terminated with a line separator.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source
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
   * Reads lines of text from this source, processing each line as it is read using the given
   * {@link LineProcessor processor}. Stops when all lines have been processed or the processor
   * returns {@code false} and returns the result produced by the processor.
   *
   * <p>Like {@link BufferedReader}, this method breaks lines on any of {@code \n}, {@code \r} or
   * {@code \r\n}, does not include the line separator in the lines passed to the {@code processor}
   * and does not consider there to be an extra empty line at the end if the content is terminated
   * with a line separator.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source or if
   *     {@code processor} throws an {@code IOException}
   * @since 16.0
   */
  @Beta
  public <T> T readLines(LineProcessor<T> processor) throws IOException {
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
   * Returns whether the source has zero chars. The default implementation is to open a stream and
   * check for EOF.
   *
   * @throws IOException if an I/O error occurs
   * @since 15.0
   */
  public boolean isEmpty() throws IOException {
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
   * <p>Only one underlying stream will be open at a time. Closing the  concatenated stream will
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
   * <p>Note: The input {@code Iterator} will be copied to an {@code ImmutableList} when this
   * method is called. This will fail if the iterator is infinite and may cause problems if the
   * iterator eagerly fetches data for each source when iterated (rather than producing sources
   * that only load data through their streams). Prefer using the {@link #concat(Iterable)}
   * overload if possible.
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
    return new CharSequenceCharSource(charSequence);
  }

  /**
   * Returns an immutable {@link CharSource} that contains no characters.
   *
   * @since 15.0
   */
  public static CharSource empty() {
    return EmptyCharSource.INSTANCE;
  }

  private static class CharSequenceCharSource extends CharSource {

    private static final Splitter LINE_SPLITTER
        = Splitter.on(Pattern.compile("\r\n|\n|\r"));

    private final CharSequence seq;

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
    public <T> T readLines(LineProcessor<T> processor) throws IOException {
      for (String line : lines()) {
        if (!processor.processLine(line)) {
          break;
        }
      }
      return processor.getResult();
    }

    @Override
    public String toString() {
      return "CharSource.wrap(" + truncate(seq, 30, "...") + ")";
    }

    /**
     * Truncates the given character sequence to the given maximum length. If the length of the
     * sequence is greater than {@code maxLength}, the returned string will be exactly
     * {@code maxLength} chars in length and will end with the given {@code truncationIndicator}.
     * Otherwise, the sequence will be returned as a string with no changes to the content.
     *
     * <p>Examples:
     *
     * <pre>   {@code
     *   truncate("foobar", 7, "..."); // returns "foobar"
     *   truncate("foobar", 5, "..."); // returns "fo..." }</pre>
     *
     * <p><b>Note:</b> This method <i>may</i> work with certain non-ASCII text but is not safe for
     * use with arbitrary Unicode text. It is mostly intended for use with text that is known to be
     * safe for use with it (such as all-ASCII text) and for simple debugging text. When using this
     * method, consider the following:
     *
     * <ul>
     *   <li>it may split surrogate pairs</li>
     *   <li>it may split characters and combining characters</li>
     *   <li>it does not consider word boundaries</li>
     *   <li>if truncating for display to users, there are other considerations that must be taken
     *   into account</li>
     *   <li>the appropriate truncation indicator may be locale-dependent</li>
     *   <li>it is safe to use non-ASCII characters in the truncation indicator</li>
     * </ul>
     *
     *
     * @throws IllegalArgumentException if {@code maxLength} is less than the length of
     *     {@code truncationIndicator}
     */
    /*
     * <p>TODO(user, cpovirk): Use Ascii.truncate once it is available in our internal copy of
     * guava_jdk5.
     */
    private static String truncate(CharSequence seq, int maxLength, String truncationIndicator) {
      checkNotNull(seq);

      // length to truncate the sequence to, not including the truncation indicator
      int truncationLength = maxLength - truncationIndicator.length();

      // in this worst case, this allows a maxLength equal to the length of the truncationIndicator,
      // meaning that a string will be truncated to just the truncation indicator itself
      checkArgument(truncationLength >= 0,
          "maxLength (%s) must be >= length of the truncation indicator (%s)",
          maxLength, truncationIndicator.length());

      if (seq.length() <= maxLength) {
        String string = seq.toString();
        if (string.length() <= maxLength) {
          return string;
        }
        // if the length of the toString() result was > maxLength for some reason, truncate that
        seq = string;
      }

      return new StringBuilder(maxLength)
          .append(seq, 0, truncationLength)
          .append(truncationIndicator)
          .toString();
    }
  }

  private static final class EmptyCharSource extends CharSequenceCharSource {

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
    public String toString() {
      return "CharSource.concat(" + sources + ")";
    }
  }
}
