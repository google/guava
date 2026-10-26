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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.nio.charset.Charset;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for {@link Charsets}.
 *
 * @author Mike Bostock
 */
@GwtCompatible
@NullUnmarked
public class CharsetsTest extends TestCase {

  @SuppressWarnings("TruthConstantAsserts") // We are testing our constant definition.
  @J2ktIncompatible
  @GwtIncompatible // Non-UTF-8 Charset
  public void testUsAscii() {
    assertThat(Charsets.US_ASCII).isEqualTo(Charset.forName("US-ASCII"));
  }

  @SuppressWarnings("TruthConstantAsserts") // We are testing our constant definition.
  @J2ktIncompatible
  @GwtIncompatible // Non-UTF-8 Charset
  public void testIso88591() {
    assertThat(Charsets.ISO_8859_1).isEqualTo(Charset.forName("ISO-8859-1"));
  }

  @SuppressWarnings("TruthConstantAsserts") // We are testing our constant definition.
  public void testUtf8() {
    assertThat(Charsets.UTF_8).isEqualTo(Charset.forName("UTF-8"));
  }

  @SuppressWarnings("TruthConstantAsserts") // We are testing our constant definition.
  @J2ktIncompatible
  @GwtIncompatible // Non-UTF-8 Charset
  public void testUtf16be() {
    assertThat(Charsets.UTF_16BE).isEqualTo(Charset.forName("UTF-16BE"));
  }

  @SuppressWarnings("TruthConstantAsserts") // We are testing our constant definition.
  @J2ktIncompatible
  @GwtIncompatible // Non-UTF-8 Charset
  public void testUtf16le() {
    assertThat(Charsets.UTF_16LE).isEqualTo(Charset.forName("UTF-16LE"));
  }

  @SuppressWarnings("TruthConstantAsserts") // We are testing our constant definition.
  @J2ktIncompatible
  @GwtIncompatible // Non-UTF-8 Charset
  public void testUtf16() {
    assertThat(Charsets.UTF_16).isEqualTo(Charset.forName("UTF-16"));
  }

  @J2ktIncompatible
  @GwtIncompatible // Non-UTF-8 Charset
  public void testWhyUsAsciiIsDangerous() {
    byte[] b1 = "朝日新聞".getBytes(Charsets.US_ASCII);
    byte[] b2 = "聞朝日新".getBytes(Charsets.US_ASCII);
    byte[] b3 = "????".getBytes(Charsets.US_ASCII);
    byte[] b4 = "ニュース".getBytes(Charsets.US_ASCII);
    byte[] b5 = "スューー".getBytes(Charsets.US_ASCII);
    // Assert they are all equal (using the transitive property)
    assertThat(b1).isEqualTo(b2);
    assertThat(b2).isEqualTo(b3);
    assertThat(b3).isEqualTo(b4);
    assertThat(b4).isEqualTo(b5);
  }
}
