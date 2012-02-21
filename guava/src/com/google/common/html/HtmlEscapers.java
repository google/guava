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

package com.google.common.html;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.escape.ArrayBasedCharEscaper;
import com.google.common.escape.ArrayBasedEscaperMap;
import com.google.common.escape.ArrayBasedUnicodeEscaper;
import com.google.common.escape.CharEscaper;
import com.google.common.escape.Escapers;
import com.google.common.escape.UnicodeEscaper;

import java.util.HashMap;
import java.util.Map;

//TODO(user): Add better class docs once the refactoring is done.
/**
 * A factory class for obtaining escaper instances suitable for working with
 * HTML.
 *
 * @author Sven Mawson
 * @author David Beaumont
 * @since 12.0
 */
@Beta
@GwtCompatible
public final class HtmlEscapers {
  private HtmlEscapers() { }

  // For each xxxEscaper() method, please add links to external reference pages
  // that are considered authoritative for the behavior of that escaper.

  /**
   * Returns an {@link Escaper} instance that escapes Unicode code points as
   * specified by <a href="http://www.w3.org/TR/html4/">HTML 4.01</a>. In
   * particular see the sections on
   * <a href="http://www.w3.org/TR/html4/charset.html">character sets and
   * encodings</a> and <a href="http://www.w3.org/TR/html4/sgml/entities.html">
   * character entity references</a>.
   *
   * <p>Unsafe code points are escaped to their associated entity name, such as
   * {@code &quot;} if possible, otherwise they are escaped using their decimal
   * value, such as {@code &#12345;}.
   */
  public static UnicodeEscaper htmlEscaper() {
    // TODO(user): Update callers and return Escaper.
    return HtmlEscaperHolder.HTML_UNICODE_ESCAPER;
  }

  // A lazy initialization holder for HTML_ESCAPER. This is used because the
  // size of an escaper map is proportional to the highest valued character it
  // contains, which in this case is \u2666 (nearly 10,000).
  private static class HtmlEscaperHolder {
    private static final ArrayBasedEscaperMap replacementMap;
    static {
      Map<Character, String> map = new HashMap<Character, String>();
      map.put('"',      "&quot;");
      map.put('\'',     "&#39;");
      map.put('&',      "&amp;");
      map.put('<',      "&lt;");
      map.put('>',      "&gt;");
      map.put('\u00A0', "&nbsp;");
      map.put('\u00A1', "&iexcl;");
      map.put('\u00A2', "&cent;");
      map.put('\u00A3', "&pound;");
      map.put('\u00A4', "&curren;");
      map.put('\u00A5', "&yen;");
      map.put('\u00A6', "&brvbar;");
      map.put('\u00A7', "&sect;");
      map.put('\u00A8', "&uml;");
      map.put('\u00A9', "&copy;");
      map.put('\u00AA', "&ordf;");
      map.put('\u00AB', "&laquo;");
      map.put('\u00AC', "&not;");
      map.put('\u00AD', "&shy;");
      map.put('\u00AE', "&reg;");
      map.put('\u00AF', "&macr;");
      map.put('\u00B0', "&deg;");
      map.put('\u00B1', "&plusmn;");
      map.put('\u00B2', "&sup2;");
      map.put('\u00B3', "&sup3;");
      map.put('\u00B4', "&acute;");
      map.put('\u00B5', "&micro;");
      map.put('\u00B6', "&para;");
      map.put('\u00B7', "&middot;");
      map.put('\u00B8', "&cedil;");
      map.put('\u00B9', "&sup1;");
      map.put('\u00BA', "&ordm;");
      map.put('\u00BB', "&raquo;");
      map.put('\u00BC', "&frac14;");
      map.put('\u00BD', "&frac12;");
      map.put('\u00BE', "&frac34;");
      map.put('\u00BF', "&iquest;");
      map.put('\u00C0', "&Agrave;");
      map.put('\u00C1', "&Aacute;");
      map.put('\u00C2', "&Acirc;");
      map.put('\u00C3', "&Atilde;");
      map.put('\u00C4', "&Auml;");
      map.put('\u00C5', "&Aring;");
      map.put('\u00C6', "&AElig;");
      map.put('\u00C7', "&Ccedil;");
      map.put('\u00C8', "&Egrave;");
      map.put('\u00C9', "&Eacute;");
      map.put('\u00CA', "&Ecirc;");
      map.put('\u00CB', "&Euml;");
      map.put('\u00CC', "&Igrave;");
      map.put('\u00CD', "&Iacute;");
      map.put('\u00CE', "&Icirc;");
      map.put('\u00CF', "&Iuml;");
      map.put('\u00D0', "&ETH;");
      map.put('\u00D1', "&Ntilde;");
      map.put('\u00D2', "&Ograve;");
      map.put('\u00D3', "&Oacute;");
      map.put('\u00D4', "&Ocirc;");
      map.put('\u00D5', "&Otilde;");
      map.put('\u00D6', "&Ouml;");
      map.put('\u00D7', "&times;");
      map.put('\u00D8', "&Oslash;");
      map.put('\u00D9', "&Ugrave;");
      map.put('\u00DA', "&Uacute;");
      map.put('\u00DB', "&Ucirc;");
      map.put('\u00DC', "&Uuml;");
      map.put('\u00DD', "&Yacute;");
      map.put('\u00DE', "&THORN;");
      map.put('\u00DF', "&szlig;");
      map.put('\u00E0', "&agrave;");
      map.put('\u00E1', "&aacute;");
      map.put('\u00E2', "&acirc;");
      map.put('\u00E3', "&atilde;");
      map.put('\u00E4', "&auml;");
      map.put('\u00E5', "&aring;");
      map.put('\u00E6', "&aelig;");
      map.put('\u00E7', "&ccedil;");
      map.put('\u00E8', "&egrave;");
      map.put('\u00E9', "&eacute;");
      map.put('\u00EA', "&ecirc;");
      map.put('\u00EB', "&euml;");
      map.put('\u00EC', "&igrave;");
      map.put('\u00ED', "&iacute;");
      map.put('\u00EE', "&icirc;");
      map.put('\u00EF', "&iuml;");
      map.put('\u00F0', "&eth;");
      map.put('\u00F1', "&ntilde;");
      map.put('\u00F2', "&ograve;");
      map.put('\u00F3', "&oacute;");
      map.put('\u00F4', "&ocirc;");
      map.put('\u00F5', "&otilde;");
      map.put('\u00F6', "&ouml;");
      map.put('\u00F7', "&divide;");
      map.put('\u00F8', "&oslash;");
      map.put('\u00F9', "&ugrave;");
      map.put('\u00FA', "&uacute;");
      map.put('\u00FB', "&ucirc;");
      map.put('\u00FC', "&uuml;");
      map.put('\u00FD', "&yacute;");
      map.put('\u00FE', "&thorn;");
      map.put('\u00FF', "&yuml;");
      map.put('\u0152', "&OElig;");
      map.put('\u0153', "&oelig;");
      map.put('\u0160', "&Scaron;");
      map.put('\u0161', "&scaron;");
      map.put('\u0178', "&Yuml;");
      map.put('\u0192', "&fnof;");
      map.put('\u02C6', "&circ;");
      map.put('\u02DC', "&tilde;");
      map.put('\u0391', "&Alpha;");
      map.put('\u0392', "&Beta;");
      map.put('\u0393', "&Gamma;");
      map.put('\u0394', "&Delta;");
      map.put('\u0395', "&Epsilon;");
      map.put('\u0396', "&Zeta;");
      map.put('\u0397', "&Eta;");
      map.put('\u0398', "&Theta;");
      map.put('\u0399', "&Iota;");
      map.put('\u039A', "&Kappa;");
      map.put('\u039B', "&Lambda;");
      map.put('\u039C', "&Mu;");
      map.put('\u039D', "&Nu;");
      map.put('\u039E', "&Xi;");
      map.put('\u039F', "&Omicron;");
      map.put('\u03A0', "&Pi;");
      map.put('\u03A1', "&Rho;");
      map.put('\u03A3', "&Sigma;");
      map.put('\u03A4', "&Tau;");
      map.put('\u03A5', "&Upsilon;");
      map.put('\u03A6', "&Phi;");
      map.put('\u03A7', "&Chi;");
      map.put('\u03A8', "&Psi;");
      map.put('\u03A9', "&Omega;");
      map.put('\u03B1', "&alpha;");
      map.put('\u03B2', "&beta;");
      map.put('\u03B3', "&gamma;");
      map.put('\u03B4', "&delta;");
      map.put('\u03B5', "&epsilon;");
      map.put('\u03B6', "&zeta;");
      map.put('\u03B7', "&eta;");
      map.put('\u03B8', "&theta;");
      map.put('\u03B9', "&iota;");
      map.put('\u03BA', "&kappa;");
      map.put('\u03BB', "&lambda;");
      map.put('\u03BC', "&mu;");
      map.put('\u03BD', "&nu;");
      map.put('\u03BE', "&xi;");
      map.put('\u03BF', "&omicron;");
      map.put('\u03C0', "&pi;");
      map.put('\u03C1', "&rho;");
      map.put('\u03C2', "&sigmaf;");
      map.put('\u03C3', "&sigma;");
      map.put('\u03C4', "&tau;");
      map.put('\u03C5', "&upsilon;");
      map.put('\u03C6', "&phi;");
      map.put('\u03C7', "&chi;");
      map.put('\u03C8', "&psi;");
      map.put('\u03C9', "&omega;");
      map.put('\u03D1', "&thetasym;");
      map.put('\u03D2', "&upsih;");
      map.put('\u03D6', "&piv;");
      map.put('\u2002', "&ensp;");
      map.put('\u2003', "&emsp;");
      map.put('\u2009', "&thinsp;");
      map.put('\u200C', "&zwnj;");
      map.put('\u200D', "&zwj;");
      map.put('\u200E', "&lrm;");
      map.put('\u200F', "&rlm;");
      map.put('\u2013', "&ndash;");
      map.put('\u2014', "&mdash;");
      map.put('\u2018', "&lsquo;");
      map.put('\u2019', "&rsquo;");
      map.put('\u201A', "&sbquo;");
      map.put('\u201C', "&ldquo;");
      map.put('\u201D', "&rdquo;");
      map.put('\u201E', "&bdquo;");
      map.put('\u2020', "&dagger;");
      map.put('\u2021', "&Dagger;");
      map.put('\u2022', "&bull;");
      map.put('\u2026', "&hellip;");
      map.put('\u2030', "&permil;");
      map.put('\u2032', "&prime;");
      map.put('\u2033', "&Prime;");
      map.put('\u2039', "&lsaquo;");
      map.put('\u203A', "&rsaquo;");
      map.put('\u203E', "&oline;");
      map.put('\u2044', "&frasl;");
      map.put('\u20AC', "&euro;");
      map.put('\u2111', "&image;");
      map.put('\u2118', "&weierp;");
      map.put('\u211C', "&real;");
      map.put('\u2122', "&trade;");
      map.put('\u2135', "&alefsym;");
      map.put('\u2190', "&larr;");
      map.put('\u2191', "&uarr;");
      map.put('\u2192', "&rarr;");
      map.put('\u2193', "&darr;");
      map.put('\u2194', "&harr;");
      map.put('\u21B5', "&crarr;");
      map.put('\u21D0', "&lArr;");
      map.put('\u21D1', "&uArr;");
      map.put('\u21D2', "&rArr;");
      map.put('\u21D3', "&dArr;");
      map.put('\u21D4', "&hArr;");
      map.put('\u2200', "&forall;");
      map.put('\u2202', "&part;");
      map.put('\u2203', "&exist;");
      map.put('\u2205', "&empty;");
      map.put('\u2207', "&nabla;");
      map.put('\u2208', "&isin;");
      map.put('\u2209', "&notin;");
      map.put('\u220B', "&ni;");
      map.put('\u220F', "&prod;");
      map.put('\u2211', "&sum;");
      map.put('\u2212', "&minus;");
      map.put('\u2217', "&lowast;");
      map.put('\u221A', "&radic;");
      map.put('\u221D', "&prop;");
      map.put('\u221E', "&infin;");
      map.put('\u2220', "&ang;");
      map.put('\u2227', "&and;");
      map.put('\u2228', "&or;");
      map.put('\u2229', "&cap;");
      map.put('\u222A', "&cup;");
      map.put('\u222B', "&int;");
      map.put('\u2234', "&there4;");
      map.put('\u223C', "&sim;");
      map.put('\u2245', "&cong;");
      map.put('\u2248', "&asymp;");
      map.put('\u2260', "&ne;");
      map.put('\u2261', "&equiv;");
      map.put('\u2264', "&le;");
      map.put('\u2265', "&ge;");
      map.put('\u2282', "&sub;");
      map.put('\u2283', "&sup;");
      map.put('\u2284', "&nsub;");
      map.put('\u2286', "&sube;");
      map.put('\u2287', "&supe;");
      map.put('\u2295', "&oplus;");
      map.put('\u2297', "&otimes;");
      map.put('\u22A5', "&perp;");
      map.put('\u22C5', "&sdot;");
      map.put('\u2308', "&lceil;");
      map.put('\u2309', "&rceil;");
      map.put('\u230A', "&lfloor;");
      map.put('\u230B', "&rfloor;");
      map.put('\u2329', "&lang;");
      map.put('\u232A', "&rang;");
      map.put('\u25CA', "&loz;");
      map.put('\u2660', "&spades;");
      map.put('\u2663', "&clubs;");
      map.put('\u2665', "&hearts;");
      map.put('\u2666', "&diams;");
      replacementMap = ArrayBasedEscaperMap.create(map);
    }

    public static final HtmlCharEscaper HTML_CHAR_ESCAPER =
        new HtmlCharEscaper(replacementMap);

    public static final HtmlContentEscaper HTML_UNICODE_ESCAPER =
        new HtmlContentEscaper(replacementMap);
  }

  /**
   * Returns an {@link Escaper} instance that escapes special characters in a
   * string so it can safely be included in an HTML document in either element
   * content or attribute values. This escaper only escapes the following five
   * ASCII characters {@code '"&<>}.
   *
   * <p><b>Note</b>: This escaper only performs minimal escaping to make content
   * structurally compatible with HTML. Specifically it does not perform entity
   * replacement (symbolic or numeric) and will output non-ASCII characters.
   */
  public static CharEscaper htmlContentEscaper() {
    // TODO(user): Update callers and return Escaper (remove cast below).
    return HTML_CONTENT_ESCAPER;
  }

  private static final CharEscaper HTML_CONTENT_ESCAPER =
      (CharEscaper) Escapers.builder()
          .addEscape('"', "&quot;")
          // Note: "&apos;" is not defined in HTML 4.01.
          .addEscape('\'', "&#39;")
          .addEscape('&', "&amp;")
          .addEscape('<', "&lt;")
          .addEscape('>', "&gt;")
          .build();

  private static class HtmlCharEscaper extends ArrayBasedCharEscaper {
    public HtmlCharEscaper(ArrayBasedEscaperMap replacementMap) {
      super(replacementMap, Character.MIN_VALUE, '~');
    }

    @Override protected char[] escapeUnsafe(char c) {
      return escapeDecimal(c);
    }
  }

  private static class HtmlContentEscaper extends ArrayBasedUnicodeEscaper {
    public HtmlContentEscaper(ArrayBasedEscaperMap replacementMap) {
      super(replacementMap, Integer.MIN_VALUE, '~', null);
    }

    @Override protected char[] escapeUnsafe(int cp) {
      return escapeDecimal(cp);
    }
  }

  // Escapes the given character or code point value as an HTML decimal escape
  // sequence of the form {@code &#nnnn;} with up to 7 decimal digits.
  // The given values must be in the range [0x7F, 0x10FFFF].
  // While the output is the same as String.valueOf(value).toCharArray(), this
  // implementation is designed to be high-performance and minimize allocations.
  private static char[] escapeDecimal(int value) {
    // Calculate the index in the output array of the units column of the value.
    int unitsIndex;
    // Converting to decimal is a pain but (as of 06/2009) not all supported
    // browsers cope with the hex representation &#xhhhh;
    if (value < 10000) {
      // 3 and 4 length decimal sequences
      unitsIndex = (value < 1000) ? 4 : 5;
    } else if (value < 1000000) {
      // 5 and 6 length decimal sequences
      unitsIndex = (value < 100000) ? 6 : 7;
    } else {
      // 7 length decimal sequences
      unitsIndex = 8;
    }
    // Leave space for the ';' that follows after the units column.
    char[] result = new char[unitsIndex + 2];
    result[0] = '&';
    result[1] = '#';
    result[unitsIndex + 1] = ';';
    // Loop in reverse to actually output the characters.
    for (; unitsIndex > 1; unitsIndex--) {
      result[unitsIndex] = (char) ('0' + (value % 10));
      value /= 10;
    }
    return result;
  }
}
