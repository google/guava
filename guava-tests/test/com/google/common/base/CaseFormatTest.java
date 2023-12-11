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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import junit.framework.TestCase;

/**
 * Unit test for {@link CaseFormat}.
 *
 * @author Mike Bostock
 */
@GwtCompatible(emulated = true)
public class CaseFormatTest extends TestCase {

  public void testIdentity() {
    for (CaseFormat from : CaseFormat.values()) {
      assertSame(from + " to " + from, "foo", from.to(from, "foo"));
      for (CaseFormat to : CaseFormat.values()) {
        assertEquals(from + " to " + to, "", from.to(to, ""));
        assertEquals(from + " to " + to, " ", from.to(to, " "));
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
    assertEquals("foo", LOWER_HYPHEN.to(LOWER_HYPHEN, "foo"));
    assertEquals("foo-bar", LOWER_HYPHEN.to(LOWER_HYPHEN, "foo-bar"));
  }

  public void testLowerHyphenToLowerUnderscore() {
    assertEquals("foo", LOWER_HYPHEN.to(LOWER_UNDERSCORE, "foo"));
    assertEquals("foo_bar", LOWER_HYPHEN.to(LOWER_UNDERSCORE, "foo-bar"));
  }

  public void testLowerHyphenToLowerCamel() {
    assertEquals("foo", LOWER_HYPHEN.to(LOWER_CAMEL, "foo"));
    assertEquals("fooBar", LOWER_HYPHEN.to(LOWER_CAMEL, "foo-bar"));
  }

  public void testLowerHyphenToUpperCamel() {
    assertEquals("Foo", LOWER_HYPHEN.to(UPPER_CAMEL, "foo"));
    assertEquals("FooBar", LOWER_HYPHEN.to(UPPER_CAMEL, "foo-bar"));
  }

  public void testLowerHyphenToUpperUnderscore() {
    assertEquals("FOO", LOWER_HYPHEN.to(UPPER_UNDERSCORE, "foo"));
    assertEquals("FOO_BAR", LOWER_HYPHEN.to(UPPER_UNDERSCORE, "foo-bar"));
  }

  public void testLowerUnderscoreToLowerHyphen() {
    assertEquals("foo", LOWER_UNDERSCORE.to(LOWER_HYPHEN, "foo"));
    assertEquals("foo-bar", LOWER_UNDERSCORE.to(LOWER_HYPHEN, "foo_bar"));
  }

  public void testLowerUnderscoreToLowerUnderscore() {
    assertEquals("foo", LOWER_UNDERSCORE.to(LOWER_UNDERSCORE, "foo"));
    assertEquals("foo_bar", LOWER_UNDERSCORE.to(LOWER_UNDERSCORE, "foo_bar"));
  }

  public void testLowerUnderscoreToLowerCamel() {
    assertEquals("foo", LOWER_UNDERSCORE.to(LOWER_CAMEL, "foo"));
    assertEquals("fooBar", LOWER_UNDERSCORE.to(LOWER_CAMEL, "foo_bar"));
  }

  public void testLowerUnderscoreToUpperCamel() {
    assertEquals("Foo", LOWER_UNDERSCORE.to(UPPER_CAMEL, "foo"));
    assertEquals("FooBar", LOWER_UNDERSCORE.to(UPPER_CAMEL, "foo_bar"));
  }

  public void testLowerUnderscoreToUpperUnderscore() {
    assertEquals("FOO", LOWER_UNDERSCORE.to(UPPER_UNDERSCORE, "foo"));
    assertEquals("FOO_BAR", LOWER_UNDERSCORE.to(UPPER_UNDERSCORE, "foo_bar"));
  }

  public void testLowerCamelToLowerHyphen() {
    assertEquals("foo", LOWER_CAMEL.to(LOWER_HYPHEN, "foo"));
    assertEquals("foo-bar", LOWER_CAMEL.to(LOWER_HYPHEN, "fooBar"));
    assertEquals("h-t-t-p", LOWER_CAMEL.to(LOWER_HYPHEN, "HTTP"));
  }

  public void testLowerCamelToLowerUnderscore() {
    assertEquals("foo", LOWER_CAMEL.to(LOWER_UNDERSCORE, "foo"));
    assertEquals("foo_bar", LOWER_CAMEL.to(LOWER_UNDERSCORE, "fooBar"));
    assertEquals("h_t_t_p", LOWER_CAMEL.to(LOWER_UNDERSCORE, "hTTP"));
  }

  public void testLowerCamelToLowerCamel() {
    assertEquals("foo", LOWER_CAMEL.to(LOWER_CAMEL, "foo"));
    assertEquals("fooBar", LOWER_CAMEL.to(LOWER_CAMEL, "fooBar"));
  }

  public void testLowerCamelToUpperCamel() {
    assertEquals("Foo", LOWER_CAMEL.to(UPPER_CAMEL, "foo"));
    assertEquals("FooBar", LOWER_CAMEL.to(UPPER_CAMEL, "fooBar"));
    assertEquals("HTTP", LOWER_CAMEL.to(UPPER_CAMEL, "hTTP"));
  }

  public void testLowerCamelToUpperUnderscore() {
    assertEquals("FOO", LOWER_CAMEL.to(UPPER_UNDERSCORE, "foo"));
    assertEquals("FOO_BAR", LOWER_CAMEL.to(UPPER_UNDERSCORE, "fooBar"));
  }

  public void testUpperCamelToLowerHyphen() {
    assertEquals("foo", UPPER_CAMEL.to(LOWER_HYPHEN, "Foo"));
    assertEquals("foo-bar", UPPER_CAMEL.to(LOWER_HYPHEN, "FooBar"));
  }

  public void testUpperCamelToLowerUnderscore() {
    assertEquals("foo", UPPER_CAMEL.to(LOWER_UNDERSCORE, "Foo"));
    assertEquals("foo_bar", UPPER_CAMEL.to(LOWER_UNDERSCORE, "FooBar"));
  }

  public void testUpperCamelToLowerCamel() {
    assertEquals("foo", UPPER_CAMEL.to(LOWER_CAMEL, "Foo"));
    assertEquals("fooBar", UPPER_CAMEL.to(LOWER_CAMEL, "FooBar"));
    assertEquals("hTTP", UPPER_CAMEL.to(LOWER_CAMEL, "HTTP"));
  }

  public void testUpperCamelToUpperCamel() {
    assertEquals("Foo", UPPER_CAMEL.to(UPPER_CAMEL, "Foo"));
    assertEquals("FooBar", UPPER_CAMEL.to(UPPER_CAMEL, "FooBar"));
  }

  public void testUpperCamelToUpperUnderscore() {
    assertEquals("FOO", UPPER_CAMEL.to(UPPER_UNDERSCORE, "Foo"));
    assertEquals("FOO_BAR", UPPER_CAMEL.to(UPPER_UNDERSCORE, "FooBar"));
    assertEquals("H_T_T_P", UPPER_CAMEL.to(UPPER_UNDERSCORE, "HTTP"));
    assertEquals("H__T__T__P", UPPER_CAMEL.to(UPPER_UNDERSCORE, "H_T_T_P"));
  }

  public void testUpperUnderscoreToLowerHyphen() {
    assertEquals("foo", UPPER_UNDERSCORE.to(LOWER_HYPHEN, "FOO"));
    assertEquals("foo-bar", UPPER_UNDERSCORE.to(LOWER_HYPHEN, "FOO_BAR"));
  }

  public void testUpperUnderscoreToLowerUnderscore() {
    assertEquals("foo", UPPER_UNDERSCORE.to(LOWER_UNDERSCORE, "FOO"));
    assertEquals("foo_bar", UPPER_UNDERSCORE.to(LOWER_UNDERSCORE, "FOO_BAR"));
  }

  public void testUpperUnderscoreToLowerCamel() {
    assertEquals("foo", UPPER_UNDERSCORE.to(LOWER_CAMEL, "FOO"));
    assertEquals("fooBar", UPPER_UNDERSCORE.to(LOWER_CAMEL, "FOO_BAR"));
  }

  public void testUpperUnderscoreToUpperCamel() {
    assertEquals("Foo", UPPER_UNDERSCORE.to(UPPER_CAMEL, "FOO"));
    assertEquals("FooBar", UPPER_UNDERSCORE.to(UPPER_CAMEL, "FOO_BAR"));
    assertEquals("HTTP", UPPER_UNDERSCORE.to(UPPER_CAMEL, "H_T_T_P"));
  }

  public void testUpperUnderscoreToUpperUnderscore() {
    assertEquals("FOO", UPPER_UNDERSCORE.to(UPPER_UNDERSCORE, "FOO"));
    assertEquals("FOO_BAR", UPPER_UNDERSCORE.to(UPPER_UNDERSCORE, "FOO_BAR"));
  }

  public void testConverterToForward() {
    assertEquals("FooBar", UPPER_UNDERSCORE.converterTo(UPPER_CAMEL).convert("FOO_BAR"));
    assertEquals("fooBar", UPPER_UNDERSCORE.converterTo(LOWER_CAMEL).convert("FOO_BAR"));
    assertEquals("FOO_BAR", UPPER_CAMEL.converterTo(UPPER_UNDERSCORE).convert("FooBar"));
    assertEquals("FOO_BAR", LOWER_CAMEL.converterTo(UPPER_UNDERSCORE).convert("fooBar"));
  }

  public void testConverterToBackward() {
    assertEquals("FOO_BAR", UPPER_UNDERSCORE.converterTo(UPPER_CAMEL).reverse().convert("FooBar"));
    assertEquals("FOO_BAR", UPPER_UNDERSCORE.converterTo(LOWER_CAMEL).reverse().convert("fooBar"));
    assertEquals("FooBar", UPPER_CAMEL.converterTo(UPPER_UNDERSCORE).reverse().convert("FOO_BAR"));
    assertEquals("fooBar", LOWER_CAMEL.converterTo(UPPER_UNDERSCORE).reverse().convert("FOO_BAR"));
  }

  public void testConverter_nullConversions() {
    for (CaseFormat outer : CaseFormat.values()) {
      for (CaseFormat inner : CaseFormat.values()) {
        assertNull(outer.converterTo(inner).convert(null));
        assertNull(outer.converterTo(inner).reverse().convert(null));
      }
    }
  }

  public void testConverter_toString() {
    assertEquals(
        "LOWER_HYPHEN.converterTo(UPPER_CAMEL)", LOWER_HYPHEN.converterTo(UPPER_CAMEL).toString());
  }

  public void testConverter_serialization() {
    for (CaseFormat outer : CaseFormat.values()) {
      for (CaseFormat inner : CaseFormat.values()) {
        SerializableTester.reserializeAndAssert(outer.converterTo(inner));
      }
    }
  }
}
