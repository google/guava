/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.base;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * Static methods pertaining to ASCII characters (those in the range of values {@code 0x00} through
 * {@code 0x7F}), and to strings containing such characters.
 *
 * <p>ASCII utilities also exist in other classes of this package:
 * <ul>
 * <!-- TODO(kevinb): how can we make this not produce a warning when building gwt javadoc? -->
 * <li>{@link Charsets#US_ASCII} specifies the {@code Charset} of ASCII characters.
 * <li>{@link CharMatcher#ascii} matches ASCII characters and provides text processing methods which
 * operate only on the ASCII characters of a string.
 * </ul>
 *
 * @author Craig Berry
 * @author Gregory Kick
 * @since 7.0
 */
@GwtCompatible
public final class Ascii {

  private Ascii() {}

  /* The ASCII control characters, per RFC 20. */
  /**
   * Null ('\0'): The all-zeros character which may serve to accomplish time fill and media fill.
   * Normally used as a C string terminator.
   *
   * <p>Although RFC 20 names this as "Null", note that it is distinct from the C/C++ "NULL"
   * pointer.
   *
   * @since 8.0
   */
  public static final byte NUL = 0;

  /**
   * Start of Heading: A communication control character used at the beginning of a sequence of
   * characters which constitute a machine-sensible address or routing information. Such a sequence
   * is referred to as the "heading." An STX character has the effect of terminating a heading.
   *
   * @since 8.0
   */
  public static final byte SOH = 1;

  /**
   * Start of Text: A communication control character which precedes a sequence of characters that
   * is to be treated as an entity and entirely transmitted through to the ultimate destination.
   * Such a sequence is referred to as "text." STX may be used to terminate a sequence of characters
   * started by SOH.
   *
   * @since 8.0
   */
  public static final byte STX = 2;

  /**
   * End of Text: A communication control character used to terminate a sequence of characters
   * started with STX and transmitted as an entity.
   *
   * @since 8.0
   */
  public static final byte ETX = 3;

  /**
   * End of Transmission: A communication control character used to indicate the conclusion of a
   * transmission, which may have contained one or more texts and any associated headings.
   *
   * @since 8.0
   */
  public static final byte EOT = 4;

  /**
   * Enquiry: A communication control character used in data communication systems as a request for
   * a response from a remote station. It may be used as a "Who Are You" (WRU) to obtain
   * identification, or may be used to obtain station status, or both.
   *
   * @since 8.0
   */
  public static final byte ENQ = 5;

  /**
   * Acknowledge: A communication control character transmitted by a receiver as an affirmative
   * response to a sender.
   *
   * @since 8.0
   */
  public static final byte ACK = 6;

  /**
   * Bell ('\a'): A character for use when there is a need to call for human attention. It may
   * control alarm or attention devices.
   *
   * @since 8.0
   */
  public static final byte BEL = 7;

  /**
   * Backspace ('\b'): A format effector which controls the movement of the printing position one
   * printing space backward on the same printing line. (Applicable also to display devices.)
   *
   * @since 8.0
   */
  public static final byte BS = 8;

  /**
   * Horizontal Tabulation ('\t'): A format effector which controls the movement of the printing
   * position to the next in a series of predetermined positions along the printing line.
   * (Applicable also to display devices and the skip function on punched cards.)
   *
   * @since 8.0
   */
  public static final byte HT = 9;

  /**
   * Line Feed ('\n'): A format effector which controls the movement of the printing position to the
   * next printing line. (Applicable also to display devices.) Where appropriate, this character may
   * have the meaning "New Line" (NL), a format effector which controls the movement of the printing
   * point to the first printing position on the next printing line. Use of this convention requires
   * agreement between sender and recipient of data.
   *
   * @since 8.0
   */
  public static final byte LF = 10;

  /**
   * Alternate name for {@link #LF}. ({@code LF} is preferred.)
   *
   * @since 8.0
   */
  public static final byte NL = 10;

  /**
   * Vertical Tabulation ('\v'): A format effector which controls the movement of the printing
   * position to the next in a series of predetermined printing lines. (Applicable also to display
   * devices.)
   *
   * @since 8.0
   */
  public static final byte VT = 11;

  /**
   * Form Feed ('\f'): A format effector which controls the movement of the printing position to the
   * first pre-determined printing line on the next form or page. (Applicable also to display
   * devices.)
   *
   * @since 8.0
   */
  public static final byte FF = 12;

  /**
   * Carriage Return ('\r'): A format effector which controls the movement of the printing position
   * to the first printing position on the same printing line. (Applicable also to display devices.)
   *
   * @since 8.0
   */
  public static final byte CR = 13;

  /**
   * Shift Out: A control character indicating that the code combinations which follow shall be
   * interpreted as outside of the character set of the standard code table until a Shift In
   * character is reached.
   *
   * @since 8.0
   */
  public static final byte SO = 14;

  /**
   * Shift In: A control character indicating that the code combinations which follow shall be
   * interpreted according to the standard code table.
   *
   * @since 8.0
   */
  public static final byte SI = 15;

  /**
   * Data Link Escape: A communication control character which will change the meaning of a limited
   * number of contiguously following characters. It is used exclusively to provide supplementary
   * controls in data communication networks.
   *
   * @since 8.0
   */
  public static final byte DLE = 16;

  /**
   * Device Control 1. Characters for the control of ancillary devices associated with data
   * processing or telecommunication systems, more especially switching devices "on" or "off." (If a
   * single "stop" control is required to interrupt or turn off ancillary devices, DC4 is the
   * preferred assignment.)
   *
   * @since 8.0
   */
  public static final byte DC1 = 17; // aka XON

  /**
   * Transmission On: Although originally defined as DC1, this ASCII control character is now better
   * known as the XON code used for software flow control in serial communications. The main use is
   * restarting the transmission after the communication has been stopped by the XOFF control code.
   *
   * @since 8.0
   */
  public static final byte XON = 17; // aka DC1

  /**
   * Device Control 2. Characters for the control of ancillary devices associated with data
   * processing or telecommunication systems, more especially switching devices "on" or "off." (If a
   * single "stop" control is required to interrupt or turn off ancillary devices, DC4 is the
   * preferred assignment.)
   *
   * @since 8.0
   */
  public static final byte DC2 = 18;

  /**
   * Device Control 3. Characters for the control of ancillary devices associated with data
   * processing or telecommunication systems, more especially switching devices "on" or "off." (If a
   * single "stop" control is required to interrupt or turn off ancillary devices, DC4 is the
   * preferred assignment.)
   *
   * @since 8.0
   */
  public static final byte DC3 = 19; // aka XOFF

  /**
   * Transmission off. See {@link #XON} for explanation.
   *
   * @since 8.0
   */
  public static final byte XOFF = 19; // aka DC3

  /**
   * Device Control 4. Characters for the control of ancillary devices associated with data
   * processing or telecommunication systems, more especially switching devices "on" or "off." (If a
   * single "stop" control is required to interrupt or turn off ancillary devices, DC4 is the
   * preferred assignment.)
   *
   * @since 8.0
   */
  public static final byte DC4 = 20;

  /**
   * Negative Acknowledge: A communication control character transmitted by a receiver as a negative
   * response to the sender.
   *
   * @since 8.0
   */
  public static final byte NAK = 21;

  /**
   * Synchronous Idle: A communication control character used by a synchronous transmission system
   * in the absence of any other character to provide a signal from which synchronism may be
   * achieved or retained.
   *
   * @since 8.0
   */
  public static final byte SYN = 22;

  /**
   * End of Transmission Block: A communication control character used to indicate the end of a
   * block of data for communication purposes. ETB is used for blocking data where the block
   * structure is not necessarily related to the processing format.
   *
   * @since 8.0
   */
  public static final byte ETB = 23;

  /**
   * Cancel: A control character used to indicate that the data with which it is sent is in error or
   * is to be disregarded.
   *
   * @since 8.0
   */
  public static final byte CAN = 24;

  /**
   * End of Medium: A control character associated with the sent data which may be used to identify
   * the physical end of the medium, or the end of the used, or wanted, portion of information
   * recorded on a medium. (The position of this character does not necessarily correspond to the
   * physical end of the medium.)
   *
   * @since 8.0
   */
  public static final byte EM = 25;

  /**
   * Substitute: A character that may be substituted for a character which is determined to be
   * invalid or in error.
   *
   * @since 8.0
   */
  public static final byte SUB = 26;

  /**
   * Escape: A control character intended to provide code extension (supplementary characters) in
   * general information interchange. The Escape character itself is a prefix affecting the
   * interpretation of a limited number of contiguously following characters.
   *
   * @since 8.0
   */
  public static final byte ESC = 27;

  /**
   * File Separator: These four information separators may be used within data in optional fashion,
   * except that their hierarchical relationship shall be: FS is the most inclusive, then GS, then
   * RS, and US is least inclusive. (The content and length of a File, Group, Record, or Unit are
   * not specified.)
   *
   * @since 8.0
   */
  public static final byte FS = 28;

  /**
   * Group Separator: These four information separators may be used within data in optional fashion,
   * except that their hierarchical relationship shall be: FS is the most inclusive, then GS, then
   * RS, and US is least inclusive. (The content and length of a File, Group, Record, or Unit are
   * not specified.)
   *
   * @since 8.0
   */
  public static final byte GS = 29;

  /**
   * Record Separator: These four information separators may be used within data in optional
   * fashion, except that their hierarchical relationship shall be: FS is the most inclusive, then
   * GS, then RS, and US is least inclusive. (The content and length of a File, Group, Record, or
   * Unit are not specified.)
   *
   * @since 8.0
   */
  public static final byte RS = 30;

  /**
   * Unit Separator: These four information separators may be used within data in optional fashion,
   * except that their hierarchical relationship shall be: FS is the most inclusive, then GS, then
   * RS, and US is least inclusive. (The content and length of a File, Group, Record, or Unit are
   * not specified.)
   *
   * @since 8.0
   */
  public static final byte US = 31;

  /**
   * Space: A normally non-printing graphic character used to separate words. It is also a format
   * effector which controls the movement of the printing position, one printing position forward.
   * (Applicable also to display devices.)
   *
   * @since 8.0
   */
  public static final byte SP = 32;

  /**
   * Alternate name for {@link #SP}.
   *
   * @since 8.0
   */
  public static final byte SPACE = 32;

  /**
   * Delete: This character is used primarily to "erase" or "obliterate" erroneous or unwanted
   * characters in perforated tape.
   *
   * @since 8.0
   */
  public static final byte DEL = 127;

  /**
   * The minimum value of an ASCII character.
   *
   * @since 9.0 (was type {@code int} before 12.0)
   */
  public static final char MIN = 0;

  /**
   * The maximum value of an ASCII character.
   *
   * @since 9.0 (was type {@code int} before 12.0)
   */
  public static final char MAX = 127;

  /**
   * Returns a copy of the input string in which all {@linkplain #isUpperCase(char) uppercase ASCII
   * characters} have been converted to lowercase. All other characters are copied without
   * modification.
   */
  public static String toLowerCase(String string) {
    int length = string.length();
    for (int i = 0; i < length; i++) {
      if (isUpperCase(string.charAt(i))) {
        char[] chars = string.toCharArray();
        for (; i < length; i++) {
          char c = chars[i];
          if (isUpperCase(c)) {
            chars[i] = (char) (c ^ 0x20);
          }
        }
        return String.valueOf(chars);
      }
    }
    return string;
  }

  /**
   * Returns a copy of the input character sequence in which all {@linkplain #isUpperCase(char)
   * uppercase ASCII characters} have been converted to lowercase. All other characters are copied
   * without modification.
   *
   * @since 14.0
   */
  public static String toLowerCase(CharSequence chars) {
    if (chars instanceof String) {
      return toLowerCase((String) chars);
    }
    int length = chars.length();
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(toLowerCase(chars.charAt(i)));
    }
    return builder.toString();
  }

  /**
   * If the argument is an {@linkplain #isUpperCase(char) uppercase ASCII character} returns the
   * lowercase equivalent. Otherwise returns the argument.
   */
  public static char toLowerCase(char c) {
    return isUpperCase(c) ? (char) (c ^ 0x20) : c;
  }

  /**
   * Returns a copy of the input string in which all {@linkplain #isLowerCase(char) lowercase ASCII
   * characters} have been converted to uppercase. All other characters are copied without
   * modification.
   */
  public static String toUpperCase(String string) {
    int length = string.length();
    for (int i = 0; i < length; i++) {
      if (isLowerCase(string.charAt(i))) {
        char[] chars = string.toCharArray();
        for (; i < length; i++) {
          char c = chars[i];
          if (isLowerCase(c)) {
            chars[i] = (char) (c & 0x5f);
          }
        }
        return String.valueOf(chars);
      }
    }
    return string;
  }

  /**
   * Returns a copy of the input character sequence in which all {@linkplain #isLowerCase(char)
   * lowercase ASCII characters} have been converted to uppercase. All other characters are copied
   * without modification.
   *
   * @since 14.0
   */
  public static String toUpperCase(CharSequence chars) {
    if (chars instanceof String) {
      return toUpperCase((String) chars);
    }
    int length = chars.length();
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(toUpperCase(chars.charAt(i)));
    }
    return builder.toString();
  }

  /**
   * If the argument is a {@linkplain #isLowerCase(char) lowercase ASCII character} returns the
   * uppercase equivalent. Otherwise returns the argument.
   */
  public static char toUpperCase(char c) {
    return isLowerCase(c) ? (char) (c & 0x5f) : c;
  }

  /**
   * Indicates whether {@code c} is one of the twenty-six lowercase ASCII alphabetic characters
   * between {@code 'a'} and {@code 'z'} inclusive. All others (including non-ASCII characters)
   * return {@code false}.
   */
  public static boolean isLowerCase(char c) {
    // Note: This was benchmarked against the alternate expression "(char)(c - 'a') < 26" (Nov '13)
    // and found to perform at least as well, or better.
    return (c >= 'a') && (c <= 'z');
  }

  /**
   * Indicates whether {@code c} is one of the twenty-six uppercase ASCII alphabetic characters
   * between {@code 'A'} and {@code 'Z'} inclusive. All others (including non-ASCII characters)
   * return {@code false}.
   */
  public static boolean isUpperCase(char c) {
    return (c >= 'A') && (c <= 'Z');
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
   *   Ascii.truncate("foobar", 7, "..."); // returns "foobar"
   *   Ascii.truncate("foobar", 5, "..."); // returns "fo..." }</pre>
   *
   * <p><b>Note:</b> This method <i>may</i> work with certain non-ASCII text but is not safe for use
   * with arbitrary Unicode text. It is mostly intended for use with text that is known to be safe
   * for use with it (such as all-ASCII text) and for simple debugging text. When using this method,
   * consider the following:
   *
   * <ul>
   * <li>it may split surrogate pairs
   * <li>it may split characters and combining characters
   * <li>it does not consider word boundaries
   * <li>if truncating for display to users, there are other considerations that must be taken into
   *     account
   * <li>the appropriate truncation indicator may be locale-dependent
   * <li>it is safe to use non-ASCII characters in the truncation indicator
   * </ul>
   *
   *
   * @throws IllegalArgumentException if {@code maxLength} is less than the length of
   *     {@code truncationIndicator}
   * @since 16.0
   */
  @Beta
  public static String truncate(CharSequence seq, int maxLength, String truncationIndicator) {
    checkNotNull(seq);

    // length to truncate the sequence to, not including the truncation indicator
    int truncationLength = maxLength - truncationIndicator.length();

    // in this worst case, this allows a maxLength equal to the length of the truncationIndicator,
    // meaning that a string will be truncated to just the truncation indicator itself
    checkArgument(
        truncationLength >= 0,
        "maxLength (%s) must be >= length of the truncation indicator (%s)",
        maxLength,
        truncationIndicator.length());

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

  /**
   * Indicates whether the contents of the given character sequences {@code s1} and {@code s2} are
   * equal, ignoring the case of any ASCII alphabetic characters between {@code 'a'} and {@code 'z'}
   * or {@code 'A'} and {@code 'Z'} inclusive.
   *
   * <p>This method is significantly faster than {@link String#equalsIgnoreCase} and should be used
   * in preference if at least one of the parameters is known to contain only ASCII characters.
   *
   * <p>Note however that this method does not always behave identically to expressions such as:
   * <ul>
   * <li>{@code string.toUpperCase().equals("UPPER CASE ASCII")}
   * <li>{@code string.toLowerCase().equals("lower case ascii")}
   * </ul>
   * <p>due to case-folding of some non-ASCII characters (which does not occur in
   * {@link String#equalsIgnoreCase}). However in almost all cases that ASCII strings are used, the
   * author probably wanted the behavior provided by this method rather than the subtle and
   * sometimes surprising behavior of {@code toUpperCase()} and {@code toLowerCase()}.
   *
   * @since 16.0
   */
  @Beta
  public static boolean equalsIgnoreCase(CharSequence s1, CharSequence s2) {
    // Calling length() is the null pointer check (so do it before we can exit early).
    int length = s1.length();
    if (s1 == s2) {
      return true;
    }
    if (length != s2.length()) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      char c1 = s1.charAt(i);
      char c2 = s2.charAt(i);
      if (c1 == c2) {
        continue;
      }
      int alphaIndex = getAlphaIndex(c1);
      // This was also benchmarked using '&' to avoid branching (but always evaluate the rhs),
      // however this showed no obvious improvement.
      if (alphaIndex < 26 && alphaIndex == getAlphaIndex(c2)) {
        continue;
      }
      return false;
    }
    return true;
  }

  /**
   * Returns the non-negative index value of the alpha character {@code c}, regardless of case. Ie,
   * 'a'/'A' returns 0 and 'z'/'Z' returns 25. Non-alpha characters return a value of 26 or greater.
   */
  private static int getAlphaIndex(char c) {
    // Fold upper-case ASCII to lower-case and make zero-indexed and unsigned (by casting to char).
    return (char) ((c | 0x20) - 'a');
  }
}
