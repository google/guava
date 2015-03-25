/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.base;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;

/**
 * Extracts non-overlapping substrings from an input string, typically by
 * recognizing appearances of a <i>separator</i> sequence. This separator can be
 * specified as a single {@linkplain #on(char) character}, fixed {@linkplain
 * #on(String) string}, {@linkplain #onPattern regular expression} or {@link
 * #on(CharMatcher) CharMatcher} instance. Or, instead of using a separator at
 * all, a splitter can extract adjacent substrings of a given {@linkplain
 * #fixedLength fixed length}.
 *
 * <p>For example, this expression: <pre>   {@code
 *
 *   Splitter.on(',').split("foo,bar,qux")}</pre>
 *
 * ... produces an {@code Iterable} containing {@code "foo"}, {@code "bar"} and
 * {@code "qux"}, in that order.
 *
 * <p>By default, {@code Splitter}'s behavior is simplistic and unassuming. The
 * following expression: <pre>   {@code
 *
 *   Splitter.on(',').split(" foo,,,  bar ,")}</pre>
 *
 * ... yields the substrings {@code [" foo", "", "", "  bar ", ""]}. If this
 * is not the desired behavior, use configuration methods to obtain a <i>new</i>
 * splitter instance with modified behavior: <pre>   {@code
 *
 *   private static final Splitter MY_SPLITTER = Splitter.on(',')
 *       .trimResults()
 *       .omitEmptyStrings();}</pre>
 *
 * <p>Now {@code MY_SPLITTER.split("foo,,,  bar ,")} returns just {@code ["foo",
 * "bar"]}. Note that the order in which these configuration methods are called
 * is never significant.
 *
 * <p><b>Warning:</b> Splitter instances are immutable. Invoking a configuration
 * method has no effect on the receiving instance; you must store and use the
 * new splitter instance it returns instead. <pre>   {@code
 *
 *   // Do NOT do this
 *   Splitter splitter = Splitter.on('/');
 *   splitter.trimResults(); // does nothing!
 *   return splitter.split("wrong / wrong / wrong");}</pre>
 *
 * <p>For separator-based splitters that do not use {@code omitEmptyStrings}, an
 * input string containing {@code n} occurrences of the separator naturally
 * yields an iterable of size {@code n + 1}. So if the separator does not occur
 * anywhere in the input, a single substring is returned containing the entire
 * input. Consequently, all splitters split the empty string to {@code [""]}
 * (note: even fixed-length splitters).
 *
 * <p>Splitter instances are thread-safe immutable, and are therefore safe to
 * store as {@code static final} constants.
 *
 * <p>The {@link Joiner} class provides the inverse operation to splitting, but
 * note that a round-trip between the two should be assumed to be lossy.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/StringsExplained#Splitter">
 * {@code Splitter}</a>.
 *
 * @author Julien Silland
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 * @since 1.0
 */
@GwtCompatible(emulated = true)
public final class Splitter {
  private final CharMatcher trimmer;
  private final boolean omitEmptyStrings;
  private final Strategy strategy;
  private final int limit;

  private Splitter(Strategy strategy) {
    this(strategy, false, CharMatcher.NONE, Integer.MAX_VALUE);
  }

  private Splitter(Strategy strategy, boolean omitEmptyStrings,
      CharMatcher trimmer, int limit) {
    this.strategy = strategy;
    this.omitEmptyStrings = omitEmptyStrings;
    this.trimmer = trimmer;
    this.limit = limit;
  }

  /**
   * Returns a splitter that uses the given single-character separator. For
   * example, {@code Splitter.on(',').split("foo,,bar")} returns an iterable
   * containing {@code ["foo", "", "bar"]}.
   *
   * @param separator the character to recognize as a separator
   * @return a splitter, with default settings, that recognizes that separator
   */
  @CheckReturnValue
  public static Splitter on(char separator) {
    return on(CharMatcher.is(separator));
  }

  /**
   * Returns a splitter that considers any single character matched by the
   * given {@code CharMatcher} to be a separator. For example, {@code
   * Splitter.on(CharMatcher.anyOf(";,")).split("foo,;bar,quux")} returns an
   * iterable containing {@code ["foo", "", "bar", "quux"]}.
   *
   * @param separatorMatcher a {@link CharMatcher} that determines whether a
   *     character is a separator
   * @return a splitter, with default settings, that uses this matcher
   */
  @CheckReturnValue
  public static Splitter on(final CharMatcher separatorMatcher) {
    checkNotNull(separatorMatcher);

    return new Splitter(new Strategy() {
      @Override public SplittingIterator iterator(
          Splitter splitter, final CharSequence toSplit) {
        return new SplittingIterator(splitter, toSplit) {
          @Override int separatorStart(int start) {
            return separatorMatcher.indexIn(toSplit, start);
          }

          @Override int separatorEnd(int separatorPosition) {
            return separatorPosition + 1;
          }
        };
      }
    });
  }

  /**
   * Returns a splitter that uses the given fixed string as a separator. For
   * example, {@code Splitter.on(", ").split("foo, bar,baz")} returns an
   * iterable containing {@code ["foo", "bar,baz"]}.
   *
   * @param separator the literal, nonempty string to recognize as a separator
   * @return a splitter, with default settings, that recognizes that separator
   */
  @CheckReturnValue
  public static Splitter on(final String separator) {
    checkArgument(separator.length() != 0,
        "The separator may not be the empty string.");

    return new Splitter(new Strategy() {
      @Override public SplittingIterator iterator(
          Splitter splitter, CharSequence toSplit) {
        return new SplittingIterator(splitter, toSplit) {
          @Override public int separatorStart(int start) {
            int separatorLength = separator.length();

            positions:
            for (int p = start, last = toSplit.length() - separatorLength;
                p <= last; p++) {
              for (int i = 0; i < separatorLength; i++) {
                if (toSplit.charAt(i + p) != separator.charAt(i)) {
                  continue positions;
                }
              }
              return p;
            }
            return -1;
          }

          @Override public int separatorEnd(int separatorPosition) {
            return separatorPosition + separator.length();
          }
        };
      }
    });
  }

  /**
   * Returns a splitter that considers any subsequence matching {@code
   * pattern} to be a separator. For example, {@code
   * Splitter.on(Pattern.compile("\r?\n")).split(entireFile)} splits a string
   * into lines whether it uses DOS-style or UNIX-style line terminators.
   *
   * @param separatorPattern the pattern that determines whether a subsequence
   *     is a separator. This pattern may not match the empty string.
   * @return a splitter, with default settings, that uses this pattern
   * @throws IllegalArgumentException if {@code separatorPattern} matches the
   *     empty string
   */
  @CheckReturnValue
  @GwtIncompatible("java.util.regex")
  public static Splitter on(final Pattern separatorPattern) {
    checkNotNull(separatorPattern);
    checkArgument(!separatorPattern.matcher("").matches(),
        "The pattern may not match the empty string: %s", separatorPattern);

    return new Splitter(new Strategy() {
      @Override public SplittingIterator iterator(
          final Splitter splitter, CharSequence toSplit) {
        final Matcher matcher = separatorPattern.matcher(toSplit);
        return new SplittingIterator(splitter, toSplit) {
          @Override public int separatorStart(int start) {
            return matcher.find(start) ? matcher.start() : -1;
          }

          @Override public int separatorEnd(int separatorPosition) {
            return matcher.end();
          }
        };
      }
    });
  }

  /**
   * Returns a splitter that considers any subsequence matching a given
   * pattern (regular expression) to be a separator. For example, {@code
   * Splitter.onPattern("\r?\n").split(entireFile)} splits a string into lines
   * whether it uses DOS-style or UNIX-style line terminators. This is
   * equivalent to {@code Splitter.on(Pattern.compile(pattern))}.
   *
   * @param separatorPattern the pattern that determines whether a subsequence
   *     is a separator. This pattern may not match the empty string.
   * @return a splitter, with default settings, that uses this pattern
   * @throws java.util.regex.PatternSyntaxException if {@code separatorPattern}
   *     is a malformed expression
   * @throws IllegalArgumentException if {@code separatorPattern} matches the
   *     empty string
   */
  @CheckReturnValue
  @GwtIncompatible("java.util.regex")
  public static Splitter onPattern(String separatorPattern) {
    return on(Pattern.compile(separatorPattern));
  }

  /**
   * Returns a splitter that divides strings into pieces of the given length.
   * For example, {@code Splitter.fixedLength(2).split("abcde")} returns an
   * iterable containing {@code ["ab", "cd", "e"]}. The last piece can be
   * smaller than {@code length} but will never be empty.
   *
   * <p><b>Exception:</b> for consistency with separator-based splitters, {@code
   * split("")} does not yield an empty iterable, but an iterable containing
   * {@code ""}. This is the only case in which {@code
   * Iterables.size(split(input))} does not equal {@code
   * IntMath.divide(input.length(), length, CEILING)}. To avoid this behavior,
   * use {@code omitEmptyStrings}.
   *
   * @param length the desired length of pieces after splitting, a positive
   *     integer
   * @return a splitter, with default settings, that can split into fixed sized
   *     pieces
   * @throws IllegalArgumentException if {@code length} is zero or negative
   */
  @CheckReturnValue
  public static Splitter fixedLength(final int length) {
    checkArgument(length > 0, "The length may not be less than 1");

    return new Splitter(new Strategy() {
      @Override public SplittingIterator iterator(
          final Splitter splitter, CharSequence toSplit) {
        return new SplittingIterator(splitter, toSplit) {
          @Override public int separatorStart(int start) {
            int nextChunkStart = start + length;
            return (nextChunkStart < toSplit.length() ? nextChunkStart : -1);
          }

          @Override public int separatorEnd(int separatorPosition) {
            return separatorPosition;
          }
        };
      }
    });
  }

  /**
   * Returns a splitter that behaves equivalently to {@code this} splitter, but
   * automatically omits empty strings from the results. For example, {@code
   * Splitter.on(',').omitEmptyStrings().split(",a,,,b,c,,")} returns an
   * iterable containing only {@code ["a", "b", "c"]}.
   *
   * <p>If either {@code trimResults} option is also specified when creating a
   * splitter, that splitter always trims results first before checking for
   * emptiness. So, for example, {@code
   * Splitter.on(':').omitEmptyStrings().trimResults().split(": : : ")} returns
   * an empty iterable.
   *
   * <p>Note that it is ordinarily not possible for {@link #split(CharSequence)}
   * to return an empty iterable, but when using this option, it can (if the
   * input sequence consists of nothing but separators).
   *
   * @return a splitter with the desired configuration
   */
  @CheckReturnValue
  public Splitter omitEmptyStrings() {
    return new Splitter(strategy, true, trimmer, limit);
  }

  /**
   * Returns a splitter that behaves equivalently to {@code this} splitter but
   * stops splitting after it reaches the limit.
   * The limit defines the maximum number of items returned by the iterator, or
   * the maximum size of the list returned by {@link #splitToList}.
   *
   * <p>For example,
   * {@code Splitter.on(',').limit(3).split("a,b,c,d")} returns an iterable
   * containing {@code ["a", "b", "c,d"]}.  When omitting empty strings, the
   * omitted strings do no count.  Hence,
   * {@code Splitter.on(',').limit(3).omitEmptyStrings().split("a,,,b,,,c,d")}
   * returns an iterable containing {@code ["a", "b", "c,d"}.
   * When trim is requested, all entries, including the last are trimmed.  Hence
   * {@code Splitter.on(',').limit(3).trimResults().split(" a , b , c , d ")}
   * results in @{code ["a", "b", "c , d"]}.
   *
   * @param limit the maximum number of items returned
   * @return a splitter with the desired configuration
   * @since 9.0
   */
  @CheckReturnValue
  public Splitter limit(int limit) {
    checkArgument(limit > 0, "must be greater than zero: %s", limit);
    return new Splitter(strategy, omitEmptyStrings, trimmer, limit);
  }

  /**
   * Returns a splitter that behaves equivalently to {@code this} splitter, but
   * automatically removes leading and trailing {@linkplain
   * CharMatcher#WHITESPACE whitespace} from each returned substring; equivalent
   * to {@code trimResults(CharMatcher.WHITESPACE)}. For example, {@code
   * Splitter.on(',').trimResults().split(" a, b ,c ")} returns an iterable
   * containing {@code ["a", "b", "c"]}.
   *
   * @return a splitter with the desired configuration
   */
  @CheckReturnValue
  public Splitter trimResults() {
    return trimResults(CharMatcher.WHITESPACE);
  }

  /**
   * Returns a splitter that behaves equivalently to {@code this} splitter, but
   * removes all leading or trailing characters matching the given {@code
   * CharMatcher} from each returned substring. For example, {@code
   * Splitter.on(',').trimResults(CharMatcher.is('_')).split("_a ,_b_ ,c__")}
   * returns an iterable containing {@code ["a ", "b_ ", "c"]}.
   *
   * @param trimmer a {@link CharMatcher} that determines whether a character
   *     should be removed from the beginning/end of a subsequence
   * @return a splitter with the desired configuration
   */
  // TODO(kevinb): throw if a trimmer was already specified!
  @CheckReturnValue
  public Splitter trimResults(CharMatcher trimmer) {
    checkNotNull(trimmer);
    return new Splitter(strategy, omitEmptyStrings, trimmer, limit);
  }

  /**
   * Splits {@code sequence} into string components and makes them available
   * through an {@link Iterator}, which may be lazily evaluated. If you want
   * an eagerly computed {@link List}, use {@link #splitToList(CharSequence)}.
   *
   * @param sequence the sequence of characters to split
   * @return an iteration over the segments split from the parameter.
   */
  @CheckReturnValue
  public Iterable<String> split(final CharSequence sequence) {
    checkNotNull(sequence);

    return new Iterable<String>() {
      @Override public Iterator<String> iterator() {
        return splittingIterator(sequence);
      }
      @Override public String toString() {
        return Joiner.on(", ")
            .appendTo(new StringBuilder().append('['), this)
            .append(']')
            .toString();
      }
    };
  }

  private Iterator<String> splittingIterator(CharSequence sequence) {
    return strategy.iterator(this, sequence);
  }

  /**
   * Splits {@code sequence} into string components and returns them as
   * an immutable list. If you want an {@link Iterable} which may be lazily
   * evaluated, use {@link #split(CharSequence)}.
   *
   * @param sequence the sequence of characters to split
   * @return an immutable list of the segments split from the parameter
   * @since 15.0
   */
  @CheckReturnValue
  @Beta
  public List<String> splitToList(CharSequence sequence) {
    checkNotNull(sequence);

    Iterator<String> iterator = splittingIterator(sequence);
    List<String> result = new ArrayList<String>();

    while (iterator.hasNext()) {
      result.add(iterator.next());
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Returns a {@code MapSplitter} which splits entries based on this splitter,
   * and splits entries into keys and values using the specified separator.
   *
   * @since 10.0
   */
  @CheckReturnValue
  @Beta
  public MapSplitter withKeyValueSeparator(String separator) {
    return withKeyValueSeparator(on(separator));
  }

  /**
   * Returns a {@code MapSplitter} which splits entries based on this splitter,
   * and splits entries into keys and values using the specified separator.
   *
   * @since 14.0
   */
  @CheckReturnValue
  @Beta
  public MapSplitter withKeyValueSeparator(char separator) {
    return withKeyValueSeparator(on(separator));
  }

  /**
   * Returns a {@code MapSplitter} which splits entries based on this splitter,
   * and splits entries into keys and values using the specified key-value
   * splitter.
   *
   * @since 10.0
   */
  @CheckReturnValue
  @Beta
  public MapSplitter withKeyValueSeparator(Splitter keyValueSplitter) {
    return new MapSplitter(this, keyValueSplitter);
  }

  /**
   * An object that splits strings into maps as {@code Splitter} splits
   * iterables and lists. Like {@code Splitter}, it is thread-safe and
   * immutable.
   *
   * @since 10.0
   */
  @Beta
  public static final class MapSplitter {
    private static final String INVALID_ENTRY_MESSAGE =
        "Chunk [%s] is not a valid entry";
    private final Splitter outerSplitter;
    private final Splitter entrySplitter;

    private MapSplitter(Splitter outerSplitter, Splitter entrySplitter) {
      this.outerSplitter = outerSplitter; // only "this" is passed
      this.entrySplitter = checkNotNull(entrySplitter);
    }

    /**
     * Splits {@code sequence} into substrings, splits each substring into
     * an entry, and returns an unmodifiable map with each of the entries. For
     * example, <code>
     * Splitter.on(';').trimResults().withKeyValueSeparator("=>")
     * .split("a=>b ; c=>b")
     * </code> will return a mapping from {@code "a"} to {@code "b"} and
     * {@code "c"} to {@code b}.
     *
     * <p>The returned map preserves the order of the entries from
     * {@code sequence}.
     *
     * @throws IllegalArgumentException if the specified sequence does not split
     *         into valid map entries, or if there are duplicate keys
     */
    @CheckReturnValue
    public Map<String, String> split(CharSequence sequence) {
      Map<String, String> map = new LinkedHashMap<String, String>();
      for (String entry : outerSplitter.split(sequence)) {
        Iterator<String> entryFields = entrySplitter.splittingIterator(entry);

        checkArgument(entryFields.hasNext(), INVALID_ENTRY_MESSAGE, entry);
        String key = entryFields.next();
        checkArgument(!map.containsKey(key), "Duplicate key [%s] found.", key);

        checkArgument(entryFields.hasNext(), INVALID_ENTRY_MESSAGE, entry);
        String value = entryFields.next();
        map.put(key, value);

        checkArgument(!entryFields.hasNext(), INVALID_ENTRY_MESSAGE, entry);
      }
      return Collections.unmodifiableMap(map);
    }
  }

  private interface Strategy {
    Iterator<String> iterator(Splitter splitter, CharSequence toSplit);
  }

  private abstract static class SplittingIterator extends AbstractIterator<String> {
    final CharSequence toSplit;
    final CharMatcher trimmer;
    final boolean omitEmptyStrings;

    /**
     * Returns the first index in {@code toSplit} at or after {@code start}
     * that contains the separator.
     */
    abstract int separatorStart(int start);

    /**
     * Returns the first index in {@code toSplit} after {@code
     * separatorPosition} that does not contain a separator. This method is only
     * invoked after a call to {@code separatorStart}.
     */
    abstract int separatorEnd(int separatorPosition);

    int offset = 0;
    int limit;

    protected SplittingIterator(Splitter splitter, CharSequence toSplit) {
      this.trimmer = splitter.trimmer;
      this.omitEmptyStrings = splitter.omitEmptyStrings;
      this.limit = splitter.limit;
      this.toSplit = toSplit;
    }

    @Override protected String computeNext() {
      /*
       * The returned string will be from the end of the last match to the
       * beginning of the next one. nextStart is the start position of the
       * returned substring, while offset is the place to start looking for a
       * separator.
       */
      int nextStart = offset;
      while (offset != -1) {
        int start = nextStart;
        int end;

        int separatorPosition = separatorStart(offset);
        if (separatorPosition == -1) {
          end = toSplit.length();
          offset = -1;
        } else {
          end = separatorPosition;
          offset = separatorEnd(separatorPosition);
        }
        if (offset == nextStart) {
          /*
           * This occurs when some pattern has an empty match, even if it
           * doesn't match the empty string -- for example, if it requires
           * lookahead or the like. The offset must be increased to look for
           * separators beyond this point, without changing the start position
           * of the next returned substring -- so nextStart stays the same.
           */
          offset++;
          if (offset >= toSplit.length()) {
            offset = -1;
          }
          continue;
        }

        while (start < end && trimmer.matches(toSplit.charAt(start))) {
          start++;
        }
        while (end > start && trimmer.matches(toSplit.charAt(end - 1))) {
          end--;
        }

        if (omitEmptyStrings && start == end) {
          // Don't include the (unused) separator in next split string.
          nextStart = offset;
          continue;
        }

        if (limit == 1) {
          // The limit has been reached, return the rest of the string as the
          // final item.  This is tested after empty string removal so that
          // empty strings do not count towards the limit.
          end = toSplit.length();
          offset = -1;
          // Since we may have changed the end, we need to trim it again.
          while (end > start && trimmer.matches(toSplit.charAt(end - 1))) {
            end--;
          }
        } else {
          limit--;
        }

        return toSplit.subSequence(start, end).toString();
      }
      return endOfData();
    }
  }
}
