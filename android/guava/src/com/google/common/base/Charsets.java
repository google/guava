/*
 * Copyright (C) 2007 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Contains constant definitions for the six standard {@link Charset} instances, which are
 * guaranteed to be supported by all Java platform implementations.
 *
 * <p>Assuming you're free to choose, note that <b>{@link #UTF_8} is widely preferred</b>.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/StringsExplained#charsets">{@code Charsets}</a>.
 *
 * @author Mike Bostock
 * @since 1.0
 */
@GwtCompatible(emulated = true)
public final class Charsets {

  /**
   * US-ASCII: seven-bit ASCII, the Basic Latin block of the Unicode character set (ISO646-US).
   *
   * @deprecated Use {@link StandardCharsets#US_ASCII} instead.
   */
  @Deprecated @J2ktIncompatible @GwtIncompatible // Charset not supported by GWT
  public static final Charset US_ASCII = StandardCharsets.US_ASCII;

  /**
   * ISO-8859-1: ISO Latin Alphabet Number 1 (ISO-LATIN-1).
   *
   * @deprecated Use {@link StandardCharsets#ISO_8859_1} instead.
   */
  @Deprecated public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;

  /**
   * UTF-8: eight-bit UCS Transformation Format.
   *
   * @deprecated Use {@link StandardCharsets#UTF_8} instead.
   */
  @Deprecated public static final Charset UTF_8 = StandardCharsets.UTF_8;

  /**
   * UTF-16BE: sixteen-bit UCS Transformation Format, big-endian byte order.
   *
   * @deprecated Use {@link StandardCharsets#UTF_16BE} instead.
   */
  @Deprecated @J2ktIncompatible @GwtIncompatible // Charset not supported by GWT
  public static final Charset UTF_16BE = StandardCharsets.UTF_16BE;

  /**
   * UTF-16LE: sixteen-bit UCS Transformation Format, little-endian byte order.
   *
   * @deprecated Use {@link StandardCharsets#UTF_16LE} instead.
   */
  @Deprecated @J2ktIncompatible @GwtIncompatible // Charset not supported by GWT
  public static final Charset UTF_16LE = StandardCharsets.UTF_16LE;

  /**
   * UTF-16: sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order
   * mark.
   *
   * @deprecated Use {@link StandardCharsets#UTF_16} instead.
   */
  @Deprecated @J2ktIncompatible @GwtIncompatible // Charset not supported by GWT
  public static final Charset UTF_16 = StandardCharsets.UTF_16;

  private Charsets() {}
}
