/*
 * Copyright (C) 2006 The Guava Authors
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

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for {@link CaseFormat}.
 *
 * @author Mike Bostock
 */
@GwtCompatible(emulated = true)
@NullUnmarked
public class CaseFormatTest extends TestCase {

  public void testIdentity() {
    for (CaseFormat from : CaseFormat.values()) {
      assertWithMessage("%s to %s", from, from).that(from.to(from, "foo")).isSameInstanceAs("foo");
      for (CaseFormat to : CaseFormat.values()) {
        assertWithMessage("%s to %s", from, to).that(from.to(to, "")).isEmpty();
        assertWithMessage("%s to %s", from, to).that(from.to(to, " ")).isEqualTo(" ");
      }
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullArguments() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(CaseFormat.class);
    for (CaseFormat format : CaseFormat.values()) {
      tester.testAllPublicInstanceMethods(format);
    }
  }

  public void testLowerHyphenToLowerHyphen() {
    assertThat(LOWER_HYPHEN.to(LOWER_HYPHEN, "foo")).isEqualTo("foo");
    assertThat(LOWER_HYPHEN.to(LOWER_HYPHEN, "foo-bar")).isEqualTo("foo-bar");
  }

  public void testLowerHyphenToLowerUnderscore() {
    assertThat(LOWER_HYPHEN.to(LOWER_UNDERSCORE, "foo")).isEqualTo("foo");
    assertThat(LOWER_HYPHEN.to(LOWER_UNDERSCORE, "foo-bar")).isEqualTo("foo_bar");
  }

  public void testLowerHyphenToLowerCamel() {
    assertThat(LOWER_HYPHEN.to(LOWER_CAMEL, "foo")).isEqualTo("foo");
    assertThat(LOWER_HYPHEN.to(LOWER_CAMEL, "foo-bar")).isEqualTo("fooBar");
  }

  public void testLowerHyphenToUpperCamel() {
    assertThat(LOWER_HYPHEN.to(UPPER_CAMEL, "foo")).isEqualTo("Foo");
    assertThat(LOWER_HYPHEN.to(UPPER_CAMEL, "foo-bar")).isEqualTo("FooBar");
  }

  public void testLowerHyphenToUpperUnderscore() {
    assertThat(LOWER_HYPHEN.to(UPPER_UNDERSCORE, "foo")).isEqualTo("FOO");
    assertThat(LOWER_HYPHEN.to(UPPER_UNDERSCORE, "foo-bar")).isEqualTo("FOO_BAR");
  }

  public void testLowerUnderscoreToLowerHyphen() {
    assertThat(LOWER_UNDERSCORE.to(LOWER_HYPHEN, "foo")).isEqualTo("foo");
    assertThat(LOWER_UNDERSCORE.to(LOWER_HYPHEN, "foo_bar")).isEqualTo("foo-bar");
  }

  public void testLowerUnderscoreToLowerUnderscore() {
    assertThat(LOWER_UNDERSCORE.to(LOWER_UNDERSCORE, "foo")).isEqualTo("foo");
    assertThat(LOWER_UNDERSCORE.to(LOWER_UNDERSCORE, "foo_bar")).isEqualTo("foo_bar");
  }

  public void testLowerUnderscoreToLowerCamel() {
    assertThat(LOWER_UNDERSCORE.to(LOWER_CAMEL, "foo")).isEqualTo("foo");
    assertThat(LOWER_UNDERSCORE.to(LOWER_CAMEL, "foo_bar")).isEqualTo("fooBar");
  }

  public void testLowerUnderscoreToUpperCamel() {
    assertThat(LOWER_UNDERSCORE.to(UPPER_CAMEL, "foo")).isEqualTo("Foo");
    assertThat(LOWER_UNDERSCORE.to(UPPER_CAMEL, "foo_bar")).isEqualTo("FooBar");
  }

  public void testLowerUnderscoreToUpperUnderscore() {
    assertThat(LOWER_UNDERSCORE.to(UPPER_UNDERSCORE, "foo")).isEqualTo("FOO");
    assertThat(LOWER_UNDERSCORE.to(UPPER_UNDERSCORE, "foo_bar")).isEqualTo("FOO_BAR");
  }

  public void testLowerCamelToLowerHyphen() {
    assertThat(LOWER_CAMEL.to(LOWER_HYPHEN, "foo")).isEqualTo("foo");
    assertThat(LOWER_CAMEL.to(LOWER_HYPHEN, "fooBar")).isEqualTo("foo-bar");
    assertThat(LOWER_CAMEL.to(LOWER_HYPHEN, "HTTP")).isEqualTo("h-t-t-p");
  }

  public void testLowerCamelToLowerUnderscore() {
    assertThat(LOWER_CAMEL.to(LOWER_UNDERSCORE, "foo")).isEqualTo("foo");
    assertThat(LOWER_CAMEL.to(LOWER_UNDERSCORE, "fooBar")).isEqualTo("foo_bar");
    assertThat(LOWER_CAMEL.to(LOWER_UNDERSCORE, "hTTP")).isEqualTo("h_t_t_p");
  }

  public void testLowerCamelToLowerCamel() {
    assertThat(LOWER_CAMEL.to(LOWER_CAMEL, "foo")).isEqualTo("foo");
    assertThat(LOWER_CAMEL.to(LOWER_CAMEL, "fooBar")).isEqualTo("fooBar");
  }

  public void testLowerCamelToUpperCamel() {
    assertThat(LOWER_CAMEL.to(UPPER_CAMEL, "foo")).isEqualTo("Foo");
    assertThat(LOWER_CAMEL.to(UPPER_CAMEL, "fooBar")).isEqualTo("FooBar");
    assertThat(LOWER_CAMEL.to(UPPER_CAMEL, "hTTP")).isEqualTo("HTTP");
  }

  public void testLowerCamelToUpperUnderscore() {
    assertThat(LOWER_CAMEL.to(UPPER_UNDERSCORE, "foo")).isEqualTo("FOO");
    assertThat(LOWER_CAMEL.to(UPPER_UNDERSCORE, "fooBar")).isEqualTo("FOO_BAR");
  }

  public void testUpperCamelToLowerHyphen() {
    assertThat(UPPER_CAMEL.to(LOWER_HYPHEN, "Foo")).isEqualTo("foo");
    assertThat(UPPER_CAMEL.to(LOWER_HYPHEN, "FooBar")).isEqualTo("foo-bar");
  }

  public void testUpperCamelToLowerUnderscore() {
    assertThat(UPPER_CAMEL.to(LOWER_UNDERSCORE, "Foo")).isEqualTo("foo");
    assertThat(UPPER_CAMEL.to(LOWER_UNDERSCORE, "FooBar")).isEqualTo("foo_bar");
  }

  public void testUpperCamelToLowerCamel() {
    assertThat(UPPER_CAMEL.to(LOWER_CAMEL, "Foo")).isEqualTo("foo");
    assertThat(UPPER_CAMEL.to(LOWER_CAMEL, "FooBar")).isEqualTo("fooBar");
    assertThat(UPPER_CAMEL.to(LOWER_CAMEL, "HTTP")).isEqualTo("hTTP");
  }

  public void testUpperCamelToUpperCamel() {
    assertThat(UPPER_CAMEL.to(UPPER_CAMEL, "Foo")).isEqualTo("Foo");
    assertThat(UPPER_CAMEL.to(UPPER_CAMEL, "FooBar")).isEqualTo("FooBar");
  }

  public void testUpperCamelToUpperUnderscore() {
    assertThat(UPPER_CAMEL.to(UPPER_UNDERSCORE, "Foo")).isEqualTo("FOO");
    assertThat(UPPER_CAMEL.to(UPPER_UNDERSCORE, "FooBar")).isEqualTo("FOO_BAR");
    assertThat(UPPER_CAMEL.to(UPPER_UNDERSCORE, "HTTP")).isEqualTo("H_T_T_P");
    assertThat(UPPER_CAMEL.to(UPPER_UNDERSCORE, "H_T_T_P")).isEqualTo("H__T__T__P");
  }

  public void testUpperUnderscoreToLowerHyphen() {
    assertThat(UPPER_UNDERSCORE.to(LOWER_HYPHEN, "FOO")).isEqualTo("foo");
    assertThat(UPPER_UNDERSCORE.to(LOWER_HYPHEN, "FOO_BAR")).isEqualTo("foo-bar");
  }

  public void testUpperUnderscoreToLowerUnderscore() {
    assertThat(UPPER_UNDERSCORE.to(LOWER_UNDERSCORE, "FOO")).isEqualTo("foo");
    assertThat(UPPER_UNDERSCORE.to(LOWER_UNDERSCORE, "FOO_BAR")).isEqualTo("foo_bar");
  }

  public void testUpperUnderscoreToLowerCamel() {
    assertThat(UPPER_UNDERSCORE.to(LOWER_CAMEL, "FOO")).isEqualTo("foo");
    assertThat(UPPER_UNDERSCORE.to(LOWER_CAMEL, "FOO_BAR")).isEqualTo("fooBar");
  }

  public void testUpperUnderscoreToUpperCamel() {
    assertThat(UPPER_UNDERSCORE.to(UPPER_CAMEL, "FOO")).isEqualTo("Foo");
    assertThat(UPPER_UNDERSCORE.to(UPPER_CAMEL, "FOO_BAR")).isEqualTo("FooBar");
    assertThat(UPPER_UNDERSCORE.to(UPPER_CAMEL, "H_T_T_P")).isEqualTo("HTTP");
  }

  public void testUpperUnderscoreToUpperUnderscore() {
    assertThat(UPPER_UNDERSCORE.to(UPPER_UNDERSCORE, "FOO")).isEqualTo("FOO");
    assertThat(UPPER_UNDERSCORE.to(UPPER_UNDERSCORE, "FOO_BAR")).isEqualTo("FOO_BAR");
  }

  public void testConverterToForward() {
    assertThat(UPPER_UNDERSCORE.converterTo(UPPER_CAMEL).convert("FOO_BAR")).isEqualTo("FooBar");
    assertThat(UPPER_UNDERSCORE.converterTo(LOWER_CAMEL).convert("FOO_BAR")).isEqualTo("fooBar");
    assertThat(UPPER_CAMEL.converterTo(UPPER_UNDERSCORE).convert("FooBar")).isEqualTo("FOO_BAR");
    assertThat(LOWER_CAMEL.converterTo(UPPER_UNDERSCORE).convert("fooBar")).isEqualTo("FOO_BAR");
  }

  public void testConverterToBackward() {
    assertThat(UPPER_UNDERSCORE.converterTo(UPPER_CAMEL).reverse().convert("FooBar"))
        .isEqualTo("FOO_BAR");
    assertThat(UPPER_UNDERSCORE.converterTo(LOWER_CAMEL).reverse().convert("fooBar"))
        .isEqualTo("FOO_BAR");
    assertThat(UPPER_CAMEL.converterTo(UPPER_UNDERSCORE).reverse().convert("FOO_BAR"))
        .isEqualTo("FooBar");
    assertThat(LOWER_CAMEL.converterTo(UPPER_UNDERSCORE).reverse().convert("FOO_BAR"))
        .isEqualTo("fooBar");
  }

  public void testConverter_nullConversions() {
    for (CaseFormat outer : CaseFormat.values()) {
      for (CaseFormat inner : CaseFormat.values()) {
        assertThat(outer.converterTo(inner).convert(null)).isNull();
        assertThat(outer.converterTo(inner).reverse().convert(null)).isNull();
      }
    }
  }

  public void testConverter_toString() {
    assertThat(LOWER_HYPHEN.converterTo(UPPER_CAMEL).toString())
        .isEqualTo("LOWER_HYPHEN.converterTo(UPPER_CAMEL)");
  }

  public void testConverter_serialization() {
    for (CaseFormat outer : CaseFormat.values()) {
      for (CaseFormat inner : CaseFormat.values()) {
        SerializableTester.reserializeAndAssert(outer.converterTo(inner));
      }
    }
  }
}
