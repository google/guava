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

package com.google.common.escape;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * An {@link Escaper} that converts literal text into a format safe for
 * inclusion in a particular context (such as an XML document). Typically (but
 * not always), the inverse process of "unescaping" the text is performed
 * automatically by the relevant parser.
 *
 * <p>For example, an XML escaper would convert the literal string {@code
 * "Foo<Bar>"} into {@code "Foo&lt;Bar&gt;"} to prevent {@code "<Bar>"} from
 * being confused with an XML tag. When the resulting XML document is parsed,
 * the parser API will return this text as the original literal string {@code
 * "Foo<Bar>"}.
 *
 * <p><b>Note:</b> This class is similar to {@link CharEscaper} but with one
 * very important difference. A CharEscaper can only process Java
 * <a href="http://en.wikipedia.org/wiki/UTF-16">UTF16</a> characters in
 * isolation and may not cope when it encounters surrogate pairs. This class
 * facilitates the correct escaping of all Unicode characters.
 *
 * <p>As there are important reasons, including potential security issues, to
 * handle Unicode correctly if you are considering implementing a new escaper
 * you should favor using UnicodeEscaper wherever possible.
 *
 * <p>A {@code UnicodeEscaper} instance is required to be stateless, and safe
 * when used concurrently by multiple threads.
 *
 * <p>Popular escapers are defined as constants in classes like {@link
 * com.google.common.html.HtmlEscapers} and {@link com.google.common.xml.XmlEscapers}.
 * To create your own escapers extend this class and implement the {@link #escape(int)}
 * method.
 *
 * @author David Beaumont
 * @since 15.0
 */
@Beta
@GwtCompatible
public abstract class UnicodeEscaper extends Escaper {
  /** The amount of padding (chars) to use when growing the escape buffer. */
  private static final int DEST_PAD = 32;

  /** Constructor for use by subclasses. */
  protected UnicodeEscaper() {}

  /**
   * Returns the escaped form of the given Unicode code point, or {@code null}
   * if this code point does not need to be escaped. When called as part of an
   * escaping operation, the given code point is guaranteed to be in the range
   * {@code 0 <= cp <= Character#MAX_CODE_POINT}.
   *
   * <p>If an empty array is returned, this effectively strips the input
   * character from the resulting text.
   *
   * <p>If the character does not need to be escaped, this method should return
   * {@code null}, rather than an array containing the character representation
   * of the code point. This enables the escaping algorithm to perform more
   * efficiently.
   *
   * <p>If the implementation of this method cannot correctly handle a
   * particular code point then it should either throw an appropriate runtime
   * exception or return a suitable replacement character. It must never
   * silently discard invalid input as this may constitute a security risk.
   *
   * @param cp the Unicode code point to escape if necessary
   * @return the replacement characters, or {@code null} if no escaping was
   *     needed
   */
  protected abstract char[] escape(int cp);

  /**
   * Scans a sub-sequence of characters from a given {@link CharSequence},
   * returning the index of the next character that requires escaping.
   *
   * <p><b>Note:</b> When implementing an escaper, it is a good idea to override
   * this method for efficiency. The base class implementation determines
   * successive Unicode code points and invokes {@link #escape(int)} for each of
   * them. If the semantics of your escaper are such that code points in the
   * supplementary range are either all escaped or all unescaped, this method
   * can be implemented more efficiently using {@link CharSequence#charAt(int)}.
   *
   * <p>Note however that if your escaper does not escape characters in the
   * supplementary range, you should either continue to validate the correctness
   * of any surrogate characters encountered or provide a clear warning to users
   * that your escaper does not validate its input.
   *
   * <p>See {@link com.google.common.net.PercentEscaper} for an example.
   *
   * @param csq a sequence of characters
   * @param start the index of the first character to be scanned
   * @param end the index immediately after the last character to be scanned
   * @throws IllegalArgumentException if the scanned sub-sequence of {@code csq}
   *     contains invalid surrogate pairs
   */
  protected int nextEscapeIndex(CharSequence csq, int start, int end) {
    int index = start;
    while (index < end) {
      int cp = codePointAt(csq, index, end);
      if (cp < 0 || escape(cp) != null) {
        break;
      }
      index += Character.isSupplementaryCodePoint(cp) ? 2 : 1;
    }
    return index;
  }

  /**
   * Returns the escaped form of a given literal string.
   *
   * <p>If you are escaping input in arbitrary successive chunks, then it is not
   * generally safe to use this method. If an input string ends with an
   * unmatched high surrogate character, then this method will throw
   * {@link IllegalArgumentException}. You should ensure your input is valid <a
   * href="http://en.wikipedia.org/wiki/UTF-16">UTF-16</a> before calling this
   * method.
   *
   * <p><b>Note:</b> When implementing an escaper it is a good idea to override
   * this method for efficiency by inlining the implementation of
   * {@link #nextEscapeIndex(CharSequence, int, int)} directly. Doing this for
   * {@link com.google.common.net.PercentEscaper} more than doubled the
   * performance for unescaped strings (as measured by {@link
   * CharEscapersBenchmark}).
   *
   * @param string the literal string to be escaped
   * @return the escaped form of {@code string}
   * @throws NullPointerException if {@code string} is null
   * @throws IllegalArgumentException if invalid surrogate characters are
   *         encountered
   */
  @Override
  public String escape(String string) {
    checkNotNull(string);
    int end = string.length();
    int index = nextEscapeIndex(string, 0, end);
    return index == end ? string : escapeSlow(string, index);
  }

  /**
   * Returns the escaped form of a given literal string, starting at the given
   * index.  This method is called by the {@link #escape(String)} method when it
   * discovers that escaping is required.  It is protected to allow subclasses
   * to override the fastpath escaping function to inline their escaping test.
   * See {@link CharEscaperBuilder} for an example usage.
   *
   * <p>This method is not reentrant and may only be invoked by the top level
   * {@link #escape(String)} method.
   *
   * @param s the literal string to be escaped
   * @param index the index to start escaping from
   * @return the escaped form of {@code string}
   * @throws NullPointerException if {@code string} is null
   * @throws IllegalArgumentException if invalid surrogate characters are
   *         encountered
   */
  protected final String escapeSlow(String s, int index) {
    int end = s.length();

    // Get a destination buffer and setup some loop variables.
    char[] dest = Platform.charBufferFromThreadLocal();
    int destIndex = 0;
    int unescapedChunkStart = 0;

    while (index < end) {
      int cp = codePointAt(s, index, end);
      if (cp < 0) {
        throw new IllegalArgumentException(
            "Trailing high surrogate at end of input");
      }
      // It is possible for this to return null because nextEscapeIndex() may
      // (for performance reasons) yield some false positives but it must never
      // give false negatives.
      char[] escaped = escape(cp);
      int nextIndex = index + (Character.isSupplementaryCodePoint(cp) ? 2 : 1);
      if (escaped != null) {
        int charsSkipped = index - unescapedChunkStart;

        // This is the size needed to add the replacement, not the full
        // size needed by the string.  We only regrow when we absolutely must.
        int sizeNeeded = destIndex + charsSkipped + escaped.length;
        if (dest.length < sizeNeeded) {
          int destLength = sizeNeeded + (end - index) + DEST_PAD;
          dest = growBuffer(dest, destIndex, destLength);
        }
        // If we have skipped any characters, we need to copy them now.
        if (charsSkipped > 0) {
          s.getChars(unescapedChunkStart, index, dest, destIndex);
          destIndex += charsSkipped;
        }
        if (escaped.length > 0) {
          System.arraycopy(escaped, 0, dest, destIndex, escaped.length);
          destIndex += escaped.length;
        }
        // If we dealt with an escaped character, reset the unescaped range.
        unescapedChunkStart = nextIndex;
      }
      index = nextEscapeIndex(s, nextIndex, end);
    }

    // Process trailing unescaped characters - no need to account for escaped
    // length or padding the allocation.
    int charsSkipped = end - unescapedChunkStart;
    if (charsSkipped > 0) {
      int endIndex = destIndex + charsSkipped;
      if (dest.length < endIndex) {
        dest = growBuffer(dest, destIndex, endIndex);
      }
      s.getChars(unescapedChunkStart, end, dest, destIndex);
      destIndex = endIndex;
    }
    return new String(dest, 0, destIndex);
  }

  /**
   * Returns the Unicode code point of the character at the given index.
   *
   * <p>Unlike {@link Character#codePointAt(CharSequence, int)} or
   * {@link String#codePointAt(int)} this method will never fail silently when
   * encountering an invalid surrogate pair.
   *
   * <p>The behaviour of this method is as follows:
   * <ol>
   * <li>If {@code index >= end}, {@link IndexOutOfBoundsException} is thrown.
   * <li><b>If the character at the specified index is not a surrogate, it is
   *     returned.</b>
   * <li>If the first character was a high surrogate value, then an attempt is
   *     made to read the next character.
   *     <ol>
   *     <li><b>If the end of the sequence was reached, the negated value of
   *         the trailing high surrogate is returned.</b>
   *     <li><b>If the next character was a valid low surrogate, the code point
   *         value of the high/low surrogate pair is returned.</b>
   *     <li>If the next character was not a low surrogate value, then
   *         {@link IllegalArgumentException} is thrown.
   *     </ol>
   * <li>If the first character was a low surrogate value,
   *     {@link IllegalArgumentException} is thrown.
   * </ol>
   *
   * @param seq the sequence of characters from which to decode the code point
   * @param index the index of the first character to decode
   * @param end the index beyond the last valid character to decode
   * @return the Unicode code point for the given index or the negated value of
   *         the trailing high surrogate character at the end of the sequence
   */
  protected static int codePointAt(CharSequence seq, int index, int end) {
    checkNotNull(seq);
    if (index < end) {
      char c1 = seq.charAt(index++);
      if (c1 < Character.MIN_HIGH_SURROGATE ||
          c1 > Character.MAX_LOW_SURROGATE) {
        // Fast path (first test is probably all we need to do)
        return c1;
      } else if (c1 <= Character.MAX_HIGH_SURROGATE) {
        // If the high surrogate was the last character, return its inverse
        if (index == end) {
          return -c1;
        }
        // Otherwise look for the low surrogate following it
        char c2 = seq.charAt(index);
        if (Character.isLowSurrogate(c2)) {
          return Character.toCodePoint(c1, c2);
        }
        throw new IllegalArgumentException(
            "Expected low surrogate but got char '" + c2 +
            "' with value " + (int) c2 + " at index " + index +
            " in '" + seq + "'");
      } else {
        throw new IllegalArgumentException(
            "Unexpected low surrogate character '" + c1 +
            "' with value " + (int) c1 + " at index " + (index - 1) +
            " in '" + seq + "'");
      }
    }
    throw new IndexOutOfBoundsException("Index exceeds specified range");
  }

  /**
   * Helper method to grow the character buffer as needed, this only happens
   * once in a while so it's ok if it's in a method call.  If the index passed
   * in is 0 then no copying will be done.
   */
  private static char[] growBuffer(char[] dest, int index, int size) {
    char[] copy = new char[size];
    if (index > 0) {
      System.arraycopy(dest, 0, copy, 0, index);
    }
    return copy;
  }
}
