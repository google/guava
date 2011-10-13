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

package com.google.common.net;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;

/**
 * A factory for {@code Escaper} instances suitable for escaping strings so they
 * can be safely included in URIs or particular sections of URIs.
 *
 * <p>For more information on URI escaping, see
 * <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
 *
 * @author David Beaumont
 * @since 11.0
 */
@Beta
@GwtCompatible
public final class UriEscapers {
  private UriEscapers() { }

  // For each xxxEscaper() method, please add links to external reference pages
  // that are considered authoritative for the behavior of that escaper.

  // TODO(user): Remove the 'plusForSpace' boolean in favor of an enum.
  // As 'plusForSpace' is mostly not the right thing to use, we should consider
  // having an enum to give it an explicit name and associated documentation.

  /**
   * Returns an {@link Escaper} instance that escapes Java chars so they can be
   * safely included in URIs. For details on escaping URIs, see section 2.4 of
   * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>.
   *
   * <p>When encoding a String, the following rules apply:
   * <ul>
   * <li>The alphanumeric characters "a" through "z", "A" through "Z" and "0"
   *     through "9" remain the same.
   * <li>The special characters ".", "-", "*", and "_" remain the same.
   * <li>The space character " " is converted into a plus sign "+".
   * <li>All other characters are converted into one or more bytes using UTF-8
   *     encoding and each byte is then represented by the 3-character string
   *     "%XY", where "XY" is the two-digit, uppercase, hexadecimal
   *     representation of the byte value.
   * <ul>
   *
   * <p><b>Note</b>: Unlike other escapers, URI escapers produce uppercase
   * hexadecimal sequences. From <a href="http://www.ietf.org/rfc/rfc3986.txt">
   * RFC 3986</a>:<br>
   * <i>"URI producers and normalizers should use uppercase hexadecimal digits
   * for all percent-encodings."</i>
   *
   *
   * <p>This method is equivalent to {@code uriEscaper(true)}.
   */
  public static Escaper uriEscaper() {
    return uriEscaper(true);
  }

  /**
   * Returns a {@link Escaper} instance that escapes Java characters so they can
   * be safely included in URIs. For details on escaping URIs, see section 2.4
   * of <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>.
   *
   * <p>When encoding a String, the following rules apply:
   * <ul>
   * <li>The alphanumeric characters "a" through "z", "A" through "Z" and "0"
   *     through "9" remain the same.
   * <li>The special characters ".", "-", "*", and "_" remain the same.
   * <li>If {@code plusForSpace} was specified, the space character " " is
   *     converted into a plus sign "+". Otherwise it is converted into "%20".
   * <li>All other characters are converted into one or more bytes using UTF-8
   *     encoding and each byte is then represented by the 3-character string
   *     "%XY", where "XY" is the two-digit, uppercase, hexadecimal
   *     representation of the byte value.
   * </ul>
   *
   * <p>The need to use {@code plusForSpace} is limited to a small set of use
   * cases relating to URL encoded forms and should be avoided otherwise. For
   * more information on when it may be appropriate to use {@code plusForSpace},
   * see <a href="http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1">
   * "Forms in HTML documents".</a>
   *
   * <p><b>Note</b>: Unlike other escapers, URI escapers produce uppercase
   * hexadecimal sequences. From <a href="http://www.ietf.org/rfc/rfc3986.txt">
   * RFC 3986</a>:<br>
   * <i>"URI producers and normalizers should use uppercase hexadecimal digits
   * for all percent-encodings."</i>
   *
   * @param plusForSpace if {@code true} space is escaped to {@code +} otherwise
   *        it is escaped to {@code %20}. Although common, the escaping of
   *        spaces as plus signs has a very ambiguous status in the relevant
   *        specifications. You should prefer {@code %20} unless you are doing
   *        exact character-by-character comparisons of URLs and backwards
   *        compatibility requires you to use plus signs.
   *
   * @see #uriEscaper()
   */
  public static Escaper uriEscaper(boolean plusForSpace) {
    return plusForSpace ? URI_ESCAPER : URI_ESCAPER_NO_PLUS;
  }

  /**
   * A string of safe characters that mimics the behavior of
   * {@link java.net.URLEncoder}.
   */
  public static final String URI_SAFECHARS_JAVA = "-_.*";

  private static final Escaper URI_ESCAPER =
      new PercentEscaper(URI_SAFECHARS_JAVA, true);

  private static final Escaper URI_ESCAPER_NO_PLUS =
      new PercentEscaper(URI_SAFECHARS_JAVA, false);

  /**
   * Returns an {@link Escaper} instance that escapes Java chars so they can be
   * safely included in URI path segments. For details on escaping URIs, see
   * section 2.4 of <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
   *
   * <p>When encoding a String, the following rules apply:
   * <ul>
   * <li>The alphanumeric characters "a" through "z", "A" through "Z" and "0"
   *     through "9" remain the same.
   * <li>The unreserved characters ".", "-", "~", and "_" remain the same.
   * <li>The general delimiters "@" and ":" remain the same.
   * <li>The subdelimiters "!", "$", "&amp;", "'", "(", ")", "*", ",", ";",
   *     and "=" remain the same.
   * <li>The space character " " is converted into %20.
   * <li>All other characters are converted into one or more bytes using UTF-8
   *     encoding and each byte is then represented by the 3-character string
   *     "%XY", where "XY" is the two-digit, uppercase, hexadecimal
   *     representation of the byte value.
   * </ul>
   *
   * <p><b>Note</b>: Unlike other escapers, URI escapers produce uppercase
   * hexadecimal sequences. From <a href="http://www.ietf.org/rfc/rfc3986.txt">
   * RFC 3986</a>:<br>
   * <i>"URI producers and normalizers should use uppercase hexadecimal digits
   * for all percent-encodings."</i>
   *
   * <p>This method differs from {@link #uriPathEscaperRfcCompliant} by
   * escaping "+" characters into "%2D".
   */
  public static Escaper uriPathEscaper() {
    return URI_PATH_ESCAPER;
  }

  /**
   * Returns an {@link Escaper} instance that escapes Java chars so they can be
   * safely included in URI path segments. For details on escaping URIs, see
   * section 2.4 of <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
   *
   * <p>When encoding a String, the following rules apply:
   * <ul>
   * <li>The alphanumeric characters "a" through "z", "A" through "Z" and "0"
   *     through "9" remain the same.
   * <li>The unreserved characters ".", "-", "~", and "_" remain the same.
   * <li>The general delimiters "@" and ":" remain the same.
   * <li>The subdelimiters "!", "$", "&amp;", "'", "(", ")", "*", "+", ",", ";",
   *     and "=" remain the same.
   * <li>If {@code escapePlus} was specified, the plus character "+" is
   *     converted into "%2B", otherwise it remains the same.
   * <li>The space character " " is converted into %20.
   * <li>All other characters are converted into one or more bytes using UTF-8
   *     encoding and each byte is then represented by the 3-character string
   *     "%XY", where "XY" is the two-digit, uppercase, hexadecimal
   *     representation of the byte value.
   * </ul>
   *
   * <p><b>Note</b>: Unlike other escapers, URI escapers produce uppercase
   * hexadecimal sequences. From <a href="http://www.ietf.org/rfc/rfc3986.txt">
   * RFC 3986</a>:<br>
   * <i>"URI producers and normalizers should use uppercase hexadecimal digits
   * for all percent-encodings."</i>
   *
   * <p>This method differs from {@link #uriPathEscaper} by not escaping
   * "+" characters, treating them the same as the other sub-delimiters
   * described by RFC 3986. Note that the version that does escape "+" characters
   * is in common use and therefore the use of this method should be done
   * carefully to avoid compatibility problems, especially when comparing URLs.
   */
  public static Escaper uriPathEscaperRfcCompliant() {
    return URI_PATH_ESCAPER_RFC_COMPLIANT;
  }

  /**
   * The set of path characters as defined by RFC 3986 excluding the plus
   * character {@code '+'}. This set of characters is used as the basis for
   * most of the 'safe' strings for URIs.
   */
  public static final String URI_SAFECHARS_PATH =
      "-._~" +        // Unreserved characters.
      "!$'()*,;&=" +  // The subdelim characters (excluding '+').
      "@:";           // The gendelim characters permitted in paths.

  private static final Escaper URI_PATH_ESCAPER =
      new PercentEscaper(URI_SAFECHARS_PATH, false);

  private static final Escaper URI_PATH_ESCAPER_RFC_COMPLIANT =
      new PercentEscaper(URI_SAFECHARS_PATH + "+", false);

  /**
   * Returns an {@link Escaper} instance that escapes Java chars so they can be
   * safely included in URI query string segments. When the query string
   * consists of a sequence of name=value pairs separated by &amp;, the names
   * and values should be individually encoded. If you escape an entire query
   * string in one pass with this escaper, then the "=" and "&amp;" characters
   * used as separators will also be escaped.
   *
   * <p>This escaper is also suitable for escaping fragment identifiers.
   *
   * <p>For details on escaping URIs, see
   * section 2.4 of <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
   *
   * <p>When encoding a String, the following rules apply:
   * <ul>
   * <li>The alphanumeric characters "a" through "z", "A" through "Z" and "0"
   *     through "9" remain the same.
   * <li>The unreserved characters ".", "-", "~", and "_" remain the same.
   * <li>The general delimiters "@" and ":" remain the same.
   * <li>The path delimiters "/" and "?" remain the same.
   * <li>The subdelimiters "!", "$", "'", "(", ")", "*", ",", and ";",
   *     remain the same.
   * <li>The space character " " is converted into %20.
   * <li>The equals sign "=" is converted into %3D.
   * <li>The ampersand "&amp;" is converted into %26.
   * <li>All other characters are converted into one or more bytes using UTF-8
   *     encoding and each byte is then represented by the 3-character string
   *     "%XY", where "XY" is the two-digit, uppercase, hexadecimal
   *     representation of the byte value.
   * </ul>
   *
   * <p>The need to use {@code plusForSpace} is limited to a small set of use
   * cases relating to URL encoded forms and should be avoided otherwise. For
   * more information on when it may be appropriate to use {@code plusForSpace},
   * see <a href="http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1">
   * "Forms in HTML documents".</a>
   *
   * <p><b>Note</b>: Unlike other escapers, URI escapers produce uppercase
   * hexadecimal sequences. From <a href="http://www.ietf.org/rfc/rfc3986.txt">
   * RFC 3986</a>:<br>
   * <i>"URI producers and normalizers should use uppercase hexadecimal digits
   * for all percent-encodings."</i>
   *
   * @param plusForSpace if {@code true} space is escaped to {@code +} otherwise
   *        it is escaped to {@code %20}. Although common, the escaping of
   *        spaces as plus signs has a very ambiguous status in the relevant
   *        specifications. You should prefer {@code %20} unless you are doing
   *        exact character-by-character comparisons of URLs and backwards
   *        compatibility requires you to use plus signs.
   */
  public static Escaper uriQueryStringEscaper(boolean plusForSpace) {
    return plusForSpace ? QUERY_STRING_ESCAPER : QUERY_STRING_ESCAPER_NO_PLUS;
  }

  /**
   * A string of characters that do not need to be escaped when used in the
   * key/value arguments that make up the query parameters of an HTTP URL. Note
   * that some of these characters do need to be escaped when used in other
   * parts of the URI.
   */
  public static final String URI_SAFECHARS_QUERY_STRING =
      "-._~" +      // Unreserved characters
      "!$'()*,;" +  // The subdelim characters (excluding '+', '&' and '=').
      "@:/?";       // The gendelim characters permitted in query parameters.

  private static final Escaper QUERY_STRING_ESCAPER =
      new PercentEscaper(URI_SAFECHARS_QUERY_STRING, true);

  private static final Escaper QUERY_STRING_ESCAPER_NO_PLUS =
      new PercentEscaper(URI_SAFECHARS_QUERY_STRING, false);

  /**
   * Returns an {@link Escaper} instance which ensures that any invalid Unicode
   * code points in a URI are correctly percent escaped. This escaper is
   * suitable for applying additional escaping to partially escaped URIs and
   * <em>will not</em> correctly escape data during the creation of a URI.
   *
   * <p>This escaper is special in that it <em>does not</em> escape the
   * {@code %} character and as such does not have a well defined inverse.
   * It assumes that a minimal amount of percent-escaping has already been
   * applied to the input, at least ensuring that {@code %} itself is escaped.
   *
   * <p>For details on escaping URIs, see
   * section 2.4 of <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
   *
   * <p>When encoding a String, the following rules apply:
   * <ul>
   * <li>The alphanumeric characters "a" through "z", "A" through "Z" and "0"
   *     through "9" remain the same.
   * <li>The unreserved characters ".", "-", "~", and "_" remain the same.
   * <li>The general delimiters ":", "/", "?", "#", "[", "]" and "@" remain the
   *     same.
   * <li>The subdelimiters "!", "$", "&", "'", "(", ")", "*", "+", ",", ";", and
   *     "=" remain the same.
   * <li><em>The percent character "%" remains the same.</em>
   * <li>All other characters are converted into one or more bytes using UTF-8
   *     encoding and each byte is then represented by the 3-character string
   *     "%XY", where "XY" is the two-digit, uppercase, hexadecimal
   *     representation of the byte value.
   * </ul>
   *
   * <p>This escaper is useful in cases where we have a partially escaped (but
   * not ambiguous) URI such as:
   * <pre>{@code http://example.com/foo|bar%26baz}</pre>
   * If we re-escape the string representation of the complete URI we get:
   * <pre>{@code http://example.com/foo%7Cbar%26baz}</pre>
   *
   * <p>Note that this escaper only canonicalizes with respect to percent
   * escaping and in general <em>will not</em> make the string representations
   * of URIs comparable in a meaningful way.
   *
   * <p>This escaper is idempotent and escaping an already validly escaped URI
   * will have no effect.
   */
  public static Escaper canonicalizingEscaper() {
    return CANONICALIZING_ESCAPER;
  }

  private static final String ALL_VALID_URI_CHARS =
      "-._~" +         // Unreserved characters.
      ":/?#[]@" +      // Gendelim chars.
      "!$&'()*+,;=" +  // Subdelim chars.
      "%";             // The percent character itself!

  private static final Escaper CANONICALIZING_ESCAPER =
      new PercentEscaper(ALL_VALID_URI_CHARS, false);
}
