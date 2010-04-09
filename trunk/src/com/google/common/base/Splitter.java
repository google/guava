/*
 * Copyright (C) 2009 Google Inc.
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
import static com.google.common.base.Preconditions.checkState;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An object that divides strings (or other instances of {@code CharSequence})
 * into substrings, by recognizing a <i>separator</i> (a.k.a. "delimiter")
 * which can be expressed as a single character, literal string, regular
 * expression, {@code CharMatcher}, or by using a fixed substring length. This
 * class provides the complementary functionality to {@link Joiner}.
 *
 * <p>Here is the most basic example of {@code Splitter} usage: <pre>   {@code
 *
 *   Splitter.on(',').split("foo,bar")}</pre>
 *
 * This invocation returns an {@code Iterable<String>} containing {@code "foo"}
 * and {@code "bar"}, in that order.
 *
 * <p>By default {@code Splitter}'s behavior is very simplistic: <pre>   {@code
 *
 *   Splitter.on(',').split("foo,,bar,  quux")}</pre>
 *
 * This returns an iterable containing {@code ["foo", "", "bar", "  quux"]}.
 * Notice that the splitter does not assume that you want empty strings removed,
 * or that you wish to trim whitespace. If you want features like these, simply
 * ask for them: <pre> {@code
 *
 *   private static final Splitter MY_SPLITTER = Splitter.on(',')
 *       .trimResults()
 *       .omitEmptyStrings();}</pre>
 *
 * Now {@code MY_SPLITTER.split("foo, ,bar,  quux,")} returns an iterable
 * containing just {@code ["foo", "bar", "quux"]}. Note that the order in which
 * the configuration methods are called is never significant; for instance,
 * trimming is always applied first before checking for an empty result,
 * regardless of the order in which the {@link #trimResults()} and
 * {@link #omitEmptyStrings()} methods were invoked.
 *
 * <p><b>Warning: splitter instances are always immutable</b>; a configuration
 * method such as {@code omitEmptyStrings} has no effect on the instance it
 * is invoked on! You must store and use the new splitter instance returned by
 * the method. This makes splitters thread-safe, and safe to store as {@code
 * static final} constants (as illustrated above). <pre>   {@code
 *
 *   // Bad! Do not do this!
 *   Splitter splitter = Splitter.on('/');
 *   splitter.trimResults(); // does nothing!
 *   return splitter.split("wrong / wrong / wrong");}</pre>
 *
 * The separator recognized by the splitter does not have to be a single
 * literal character as in the examples above. See the methods {@link
 * #on(String)}, {@link #on(Pattern)} and {@link #on(CharMatcher)} for examples
 * of other ways to specify separators.
 *
 * <p><b>Note:</b> this class does not mimic any of the quirky behaviors of
 * similar JDK methods; for instance, it does not silently discard trailing
 * separators, as does {@link String#split(String)}, nor does it have a default
 * behavior of using five particular whitespace characters as separators, like
 * {@link StringTokenizer}.
 *
 * @author Julien Silland
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @since 2009.09.15 <b>tentative</b>
 */
public final class Splitter {
  private final CharMatcher trimmer;
  private final boolean omitEmptyStrings;
  private final Strategy strategy;

  private Splitter(Strategy strategy) {
    this(strategy, false, CharMatcher.NONE);
  }

  private Splitter(Strategy strategy, boolean omitEmptyStrings,
      CharMatcher trimmer) {
    this.strategy = strategy;
    this.omitEmptyStrings = omitEmptyStrings;
    this.trimmer = trimmer;
  }

  /**
   * Returns a splitter that uses the given single-character separator. For
   * example, {@code Splitter.on(',').split("foo,,bar")} returns an iterable
   * containing {@code ["foo", "", "bar"]}.
   *
   * @param separator the character to recognize as a separator
   * @return a splitter, with default settings, that recognizes that separator
   */
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
  public static Splitter on(final CharMatcher separatorMatcher) {
    checkNotNull(separatorMatcher);

    return new Splitter(new Strategy() {
      /*@Override*/ public SplittingIterator iterator(
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
   * example, {@code Splitter.on(", ").split("foo, bar, baz,qux")} returns an
   * iterable containing {@code ["foo", "bar", "baz,qux"]}.
   *
   * @param separator the literal, nonempty string to recognize as a separator
   * @return a splitter, with default settings, that recognizes that separator
   */
  public static Splitter on(final String separator) {
    checkArgument(separator.length() != 0,
        "The separator may not be the empty string.");

    return new Splitter(new Strategy() {
      /*@Override*/ public SplittingIterator iterator(
          Splitter splitter, CharSequence toSplit) {
        return new SplittingIterator(splitter, toSplit) {
          @Override public int separatorStart(int start) {
            int delimeterLength = separator.length();

            positions:
            for (int p = start, last = toSplit.length() - delimeterLength;
                p <= last; p++) {
              for (int i = 0; i < delimeterLength; i++) {
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
  public static Splitter on(final Pattern separatorPattern) {
    checkNotNull(separatorPattern);
    checkArgument(!separatorPattern.matcher("").matches(),
        "The pattern may not match the empty string: %s", separatorPattern);

    return new Splitter(new Strategy() {
      /*@Override*/ public SplittingIterator iterator(
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
   * @throws PatternSyntaxException if {@code separatorPattern} is a malformed
   *     expression
   * @throws IllegalArgumentException if {@code separatorPattern} matches the
   *     empty string
   */
  public static Splitter onPattern(String separatorPattern) {
    return on(Pattern.compile(separatorPattern));
  }

  /**
   * Returns a splitter that divides strings into pieces of the given length.
   * For example, {@code Splitter.atEach(2).split("abcde")} returns an
   * iterable containing {@code ["ab", "cd", "e"]}. The last piece can be
   * smaller than {@code length} but will never be empty.
   *
   * @param length the desired length of pieces after splitting
   * @return a splitter, with default settings, that can split into fixed sized
   *     pieces
   */
  public static Splitter fixedLength(final int length) {
    checkArgument(length > 0, "The length may not be less than 1");

    return new Splitter(new Strategy() {
      /*@Override*/ public SplittingIterator iterator(
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
  public Splitter omitEmptyStrings() {
    return new Splitter(strategy, true, trimmer);
  }

  /**
   * Returns a splitter that behaves equivalently to {@code this} splitter, but
   * automatically removes leading and trailing {@linkplain
   * CharMatcher#WHITESPACE whitespace} from each returned substring; equivalent
   * to {@code trimResults(CharMatcher.WHITESPACE)}. For example, {@code
   * Splitter.on(',').trimResults().split(" a, b  ,c  ")} returns an iterable
   * containing {@code ["a", "b", "c"]}.
   *
   * @return a splitter with the desired configuration
   */
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
  public Splitter trimResults(CharMatcher trimmer) {
    checkNotNull(trimmer);
    return new Splitter(strategy, omitEmptyStrings, trimmer);
  }

  /**
   * Splits the {@link CharSequence} passed in parameter.
   *
   * @param sequence the sequence of characters to split
   * @return an iteration over the segments split from the parameter.
   */
  public Iterable<String> split(final CharSequence sequence) {
    checkNotNull(sequence);

    return new Iterable<String>() {
      /*@Override*/ public Iterator<String> iterator() {
        return strategy.iterator(Splitter.this, sequence);
      }
    };
  }

  private interface Strategy {
    Iterator<String> iterator(Splitter splitter, CharSequence toSplit);
  }

  private abstract static class SplittingIterator
      extends AbstractIterator<String> {
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

    protected SplittingIterator(Splitter splitter, CharSequence toSplit) {
      this.trimmer = splitter.trimmer;
      this.omitEmptyStrings = splitter.omitEmptyStrings;
      this.toSplit = toSplit;
    }

    @Override protected String computeNext() {
      while (offset != -1) {
        int start = offset;
        int end;

        int separatorPosition = separatorStart(offset);
        if (separatorPosition == -1) {
          end = toSplit.length();
          offset = -1;
        } else {
          end = separatorPosition;
          offset = separatorEnd(separatorPosition);
        }

        while (start < end && trimmer.matches(toSplit.charAt(start))) {
          start++;
        }
        while (end > start && trimmer.matches(toSplit.charAt(end - 1))) {
          end--;
        }

        if (omitEmptyStrings && start == end) {
          continue;
        }

        return toSplit.subSequence(start, end).toString();
      }
      return endOfData();
    }
  }

  /*
   * Copied from common.collect.AbstractIterator. TODO: un-fork once these
   * packages have been combined into a single library.
   */
  private static abstract class AbstractIterator<T> implements Iterator<T> {
    State state = State.NOT_READY;

    enum State {
      READY, NOT_READY, DONE, FAILED,
    }

    T next;

    protected abstract T computeNext();

    protected final T endOfData() {
      state = State.DONE;
      return null;
    }

    public final boolean hasNext() {
      checkState(state != State.FAILED);
      switch (state) {
        case DONE:
          return false;
        case READY:
          return true;
        default:
      }
      return tryToComputeNext();
    }

    boolean tryToComputeNext() {
      state = State.FAILED; // temporary pessimism
      next = computeNext();
      if (state != State.DONE) {
        state = State.READY;
        return true;
      }
      return false;
    }

    public final T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      state = State.NOT_READY;
      return next;
    }

    /*@Override*/ public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
