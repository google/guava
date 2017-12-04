/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.xml;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

/**
 * {@code Escaper} instances suitable for strings to be included in XML attribute values and
 * elements' text contents. When possible, avoid manual escaping by using templating systems and
 * high-level APIs that provide autoescaping. For example, consider <a
 * href="http://www.xom.nu/">XOM</a> or <a href="http://www.jdom.org/">JDOM</a>.
 *
 * <p><b>Note:</b> Currently the escapers provided by this class do not escape any characters
 * outside the ASCII character range. Unlike HTML escaping the XML escapers will not escape
 * non-ASCII characters to their numeric entity replacements. These XML escapers provide the minimal
 * level of escaping to ensure that the output can be safely included in a Unicode XML document.
 *
 *
 * <p>For details on the behavior of the escapers in this class, see sections <a
 * href="http://www.w3.org/TR/2008/REC-xml-20081126/#charsets">2.2</a> and <a
 * href="http://www.w3.org/TR/2008/REC-xml-20081126/#syntax">2.4</a> of the XML specification.
 *
 * @author Alex Matevossian
 * @author David Beaumont
 * @since 15.0
 */
@Beta
@GwtCompatible
public class XmlEscapers {
  private XmlEscapers() {}

  private static final char MIN_ASCII_CONTROL_CHAR = 0x00;
  private static final char MAX_ASCII_CONTROL_CHAR = 0x1F;

  // For each xxxEscaper() method, please add links to external reference pages
  // that are considered authoritative for the behavior of that escaper.

  /**
   * Returns an {@link Escaper} instance that escapes special characters in a string so it can
   * safely be included in an XML document as element content. See section <a
   * href="http://www.w3.org/TR/2008/REC-xml-20081126/#syntax">2.4</a> of the XML specification.
   *
   * <p><b>Note:</b> Double and single quotes are not escaped, so it is <b>not safe</b> to use this
   * escaper to escape attribute values. Use {@link #xmlContentEscaper} if the output can appear in
   * element content or {@link #xmlAttributeEscaper} in attribute values.
   *
   * <p>This escaper substitutes {@code 0xFFFD} for non-whitespace control characters and the
   * character values {@code 0xFFFE} and {@code 0xFFFF} which are not permitted in XML. For more
   * detail see section <a href="http://www.w3.org/TR/2008/REC-xml-20081126/#charsets">2.2</a> of
   * the XML specification.
   *
   * <p>This escaper does not escape non-ASCII characters to their numeric character references
   * (NCR). Any non-ASCII characters appearing in the input will be preserved in the output.
   * Specifically "\r" (carriage return) is preserved in the output, which may result in it being
   * silently converted to "\n" when the XML is parsed.
   *
   * <p>This escaper does not treat surrogate pairs specially and does not perform Unicode
   * validation on its input.
   */
  public static Escaper xmlContentEscaper() {
    return XML_CONTENT_ESCAPER;
  }

  /**
   * Returns an {@link Escaper} instance that escapes special characters in a string so it can
   * safely be included in XML document as an attribute value. See section <a
   * href="http://www.w3.org/TR/2008/REC-xml-20081126/#AVNormalize">3.3.3</a> of the XML
   * specification.
   *
   * <p>This escaper substitutes {@code 0xFFFD} for non-whitespace control characters and the
   * character values {@code 0xFFFE} and {@code 0xFFFF} which are not permitted in XML. For more
   * detail see section <a href="http://www.w3.org/TR/2008/REC-xml-20081126/#charsets">2.2</a> of
   * the XML specification.
   *
   * <p>This escaper does not escape non-ASCII characters to their numeric character references
   * (NCR). However, horizontal tab {@code '\t'}, line feed {@code '\n'} and carriage return {@code
   * '\r'} are escaped to a corresponding NCR {@code "&#x9;"}, {@code "&#xA;"}, and {@code "&#xD;"}
   * respectively. Any other non-ASCII characters appearing in the input will be preserved in the
   * output.
   *
   * <p>This escaper does not treat surrogate pairs specially and does not perform Unicode
   * validation on its input.
   */
  public static Escaper xmlAttributeEscaper() {
    return XML_ATTRIBUTE_ESCAPER;
  }

  private static final Escaper XML_ESCAPER;
  private static final Escaper XML_CONTENT_ESCAPER;
  private static final Escaper XML_ATTRIBUTE_ESCAPER;

  static {
    Escapers.Builder builder = Escapers.builder();
    // The char values \uFFFE and \uFFFF are explicitly not allowed in XML
    // (Unicode code points above \uFFFF are represented via surrogate pairs
    // which means they are treated as pairs of safe characters).
    builder.setSafeRange(Character.MIN_VALUE, '\uFFFD');
    // Unsafe characters are replaced with the Unicode replacement character.
    builder.setUnsafeReplacement("\uFFFD");

    /*
     * Except for \n, \t, and \r, all ASCII control characters are replaced with the Unicode
     * replacement character.
     *
     * Implementation note: An alternative to the following would be to make a map that simply
     * replaces the allowed ASCII whitespace characters with themselves and to set the minimum safe
     * character to 0x20. However this would slow down the escaping of simple strings that contain
     * \t, \n, or \r.
     */
    for (char c = MIN_ASCII_CONTROL_CHAR; c <= MAX_ASCII_CONTROL_CHAR; c++) {
      if (c != '\t' && c != '\n' && c != '\r') {
        builder.addEscape(c, "\uFFFD");
      }
    }

    // Build the content escaper first and then add quote escaping for the
    // general escaper.
    builder.addEscape('&', "&amp;");
    builder.addEscape('<', "&lt;");
    builder.addEscape('>', "&gt;");
    XML_CONTENT_ESCAPER = builder.build();
    builder.addEscape('\'', "&apos;");
    builder.addEscape('"', "&quot;");
    XML_ESCAPER = builder.build();
    builder.addEscape('\t', "&#x9;");
    builder.addEscape('\n', "&#xA;");
    builder.addEscape('\r', "&#xD;");
    XML_ATTRIBUTE_ESCAPER = builder.build();
  }
}
