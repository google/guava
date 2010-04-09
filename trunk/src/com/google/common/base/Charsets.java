/*
 * Copyright (C) 2007 Google Inc.
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

import java.nio.charset.Charset;

/**
 * Contains constant definitions for the six standard {@link Charset}
 * instances, which are guaranteed to be supported by all Java platform
 * implementations.
 *
 * @author Mike Bostock
 * @since 2009.09.15 <b>tentative</b>
 */
public final class Charsets {
  private Charsets() {}

  /**
   * US-ASCII: seven-bit ASCII, a.k.a. ISO646-US, a.k.a the Basic Latin block of
   * the Unicode character set.
   */
  public static final Charset US_ASCII = Charset.forName("US-ASCII");

  /**
   * ISO-8859-1. ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.
   */
  public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

  /**
   * UTF-8: eight-bit UCS Transformation Format.
   */
  public static final Charset UTF_8 = Charset.forName("UTF-8");

  /**
   * UTF-16BE: sixteen-bit UCS Transformation Format, big-endian byte order.
   */
  public static final Charset UTF_16BE = Charset.forName("UTF-16BE");

  /**
   * UTF-16LE: sixteen-bit UCS Transformation Format, little-endian byte order.
   */
  public static final Charset UTF_16LE = Charset.forName("UTF-16LE");

  /**
   * UTF-16: sixteen-bit UCS Transformation Format, byte order identified by an
   * optional byte-order mark.
   */
  public static final Charset UTF_16 = Charset.forName("UTF-16");

  /*
   * Please do not add new Charset references to this class, unless those
   * character encodings are part of the set required to be supported by all
   * Java platform implementations! Any Charsets initialized here may cause
   * unexpected delays when this class is loaded. See the Charset Javadocs for
   * the list of built-in character encodings.
   */
}
