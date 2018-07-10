/*
 * Copyright (C) 2006 The Guava Authors
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

package com.google.common.escape;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LessThan;

/**
 * An object that converts literal text into a format safe for inclusion in a particular context
 * (such as an XML document). Typically (but not always), the inverse process of "unescaping" the
 * text is performed automatically by the relevant parser.
 *
 * <p>For example, an XML escaper would convert the literal string {@code "Foo<Bar>"} into {@code
 * "Foo&lt;Bar&gt;"} to prevent {@code "<Bar>"} from being confused with an XML tag. When the
 * resulting XML document is parsed, the parser API will return this text as the original literal
 * string {@code "Foo<Bar>"}.
 *
 * <p>A {@code CharEscaper} instance is required to be stateless, and safe when used concurrently by
 * multiple threads.
 *
 * <p>Popular escapers are defined as constants in classes like {@link
 * com.google.common.html.HtmlEscapers} and {@link com.google.common.xml.XmlEscapers}. To create
 * your own escapers extend this class and implement the {@link #escape(char)} method.
 *
 * @author Sven Mawson
 * @since 15.0
 */
@Beta
@GwtCompatible
public abstract class CharEscaper extends Escaper {
  /** Constructor for use by subclasses. */
  protected CharEscaper() {}

  /**
   * Returns the escaped form of a given literal string.
   *
   * @param string the literal string to be escaped
   * @return the escaped form of {@code string}
   * @throws NullPointerException if {@code string} is null
   */
  @Override
  public String escape(String string) {
    checkNotNull(string); // GWT specific check (do not optimize)
    // Inlineable fast-path loop which hands off to escapeSlow() only if needed
    int length = string.length();
    for (int index = 0; index < length; index++) {
      if (escape(string.charAt(index)) != null) {
        return escapeSlow(string, index);
      }
    }
    return string;
  }

  /**
   * Returns the escaped form of the given character, or {@code null} if this character does not
   * need to be escaped. If an empty array is returned, this effectively strips the input character
   * from the resulting text.
   *
   * <p>If the character does not need to be escaped, this method should return {@code null}, rather
   * than a one-character array containing the character itself. This enables the escaping algorithm
   * to perform more efficiently.
   *
   * <p>An escaper is expected to be able to deal with any {@code char} value, so this method should
   * not throw any exceptions.
   *
   * @param c the character to escape if necessary
   * @return the replacement characters, or {@code null} if no escaping was needed
   */
  protected abstract char[] escape(char c);

  /**
   * Returns the escaped form of a given literal string, starting at the given index. This method is
   * called by the {@link #escape(String)} method when it discovers that escaping is required. It is
   * protected to allow subclasses to override the fastpath escaping function to inline their
   * escaping test. See {@link CharEscaperBuilder} for an example usage.
   *
   * @param s the literal string to be escaped
   * @param index the index to start escaping from
   * @return the escaped form of {@code string}
   * @throws NullPointerException if {@code string} is null
   */
  @SuppressWarnings(value = {"upperbound:assignment.type.incompatible",/*
   (1) Because of System.arraycopy() method, `rlen` is required to be @LTLengthOf(value={"r", "dest"}, offset={"-1", "destIndex - 1"}).
   Since r = escape(), can't annotate `escape()` return type as @LTLengthOf(value={"r", "dest"}, offset={"-1", "destIndex - 1"}).*/
          "upperbound:compound.assignment.type.incompatible"/*(2): `destIndex` is always @LTEqLengthOf("dest") because `dest` array
          will always be regrow when `destSize` is less than `sizeNeeded`*/})
  protected final String escapeSlow(String s, @IndexOrHigh("#1") int index) {
    int slen = s.length();

    // Get a destination buffer and setup some loop variables.
    char[] dest = Platform.charBufferFromThreadLocal();
    int destSize = dest.length;
    @LTEqLengthOf("dest") @LessThan("destSize + 1") int destIndex = 0;
    @LTEqLengthOf("s") int lastEscape = 0;

    // Loop through the rest of the string, replacing when needed into the
    // destination buffer, which gets grown as needed as well.
    for (; index < slen; index++) {

      // Get a replacement for the current character.
      char[] r = escape(s.charAt(index));

      // If no replacement is needed, just continue.
      if (r == null) {
        continue;
      }

      @LTLengthOf(value={"r", "dest"}, offset={"-1", "destIndex - 1"}) int rlen = r.length;//(1)
      int charsSkipped = index - lastEscape;

      // This is the size needed to add the replacement, not the full size
      // needed by the string. We only regrow when we absolutely must, and
      // when we do grow, grow enough to avoid excessive growing. Grow.
      int sizeNeeded = destIndex + charsSkipped + rlen;
      if (destSize < sizeNeeded) {
        destSize = sizeNeeded + DEST_PAD_MULTIPLIER * (slen - index);
        dest = growBuffer(dest, destIndex, destSize);
      }

      // If we have skipped any characters, we need to copy them now.
      if (charsSkipped > 0) {
        s.getChars(lastEscape, index, dest, destIndex);
        destIndex += charsSkipped;
      }

      // Copy the replacement string into the dest buffer as needed.
      if (rlen > 0) {
        System.arraycopy(r, 0, dest, destIndex, rlen);//(2)
        destIndex += rlen;
      }
      lastEscape = index + 1;
    }

    // Copy leftover characters if there are any.
    int charsLeft = slen - lastEscape;
    if (charsLeft > 0) {
      int sizeNeeded = destIndex + charsLeft;
      if (destSize < sizeNeeded) {

        // Regrow and copy, expensive! No padding as this is the final copy.
        dest = growBuffer(dest, destIndex, sizeNeeded);//(2)
      }
      s.getChars(lastEscape, slen, dest, destIndex);
      destIndex = sizeNeeded;
    }
    return new String(dest, 0, destIndex);
  }

  /**
   * Helper method to grow the character buffer as needed, this only happens once in a while so it's
   * ok if it's in a method call. If the index passed in is 0 then no copying will be done.
   */
  @SuppressWarnings("upperbound:argument.type.incompatible")//index should infer @LessThan("#3 + 1") as same as @LTEqLengthOf("copy")
  //this is a similar improvement to this issue: https://github.com/typetools/checker-framework/issues/2029
  private static char[] growBuffer(char[] dest, @LTEqLengthOf("#1") @LessThan("#3 + 1")  int index, int size) {
    if (size < 0) { // overflow - should be OutOfMemoryError but GWT/j2cl don't support it
      throw new AssertionError("Cannot increase internal buffer any further");
    }
    char[] copy = new char[size];
    if (index > 0) {
      System.arraycopy(dest, 0, copy, 0, index);
    }
    return copy;
  }

  /** The multiplier for padding to use when growing the escape buffer. */
  private static final int DEST_PAD_MULTIPLIER = 2;
}
