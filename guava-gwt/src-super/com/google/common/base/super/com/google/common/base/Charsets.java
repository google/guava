/*
 * Copyright (C) 2007 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;

import java.nio.charset.Charset;

/**
 * Contains constant definitions for the six standard {@link Charset} instances, which are
 * guaranteed to be supported by all Java platform implementations.
 *
 * <p>Assuming you're free to choose, note that <b>{@link #UTF_8} is widely preferred</b>.
 *
 * <p>See the Guava User Guide article on <a
 * href="http://code.google.com/p/guava-libraries/wiki/StringsExplained#Charsets">
 * {@code Charsets}</a>.
 *
 * @author Mike Bostock
 * @since 1.0
 */
@GwtCompatible(emulated = true)
public final class Charsets {
  private Charsets() {}

  /**
   * UTF-8: eight-bit UCS Transformation Format.
   *
   */
  public static final Charset UTF_8 = Charset.forName("UTF-8");

  /*
   * Please do not add new Charset references to this class, unless those character encodings are
   * part of the set required to be supported by all Java platform implementations! Any Charsets
   * initialized here may cause unexpected delays when this class is loaded. See the Charset
   * Javadocs for the list of built-in character encodings.
   */
}
