/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.base.Charsets.UTF_16;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.net.MediaType.ANY_APPLICATION_TYPE;
import static com.google.common.net.MediaType.ANY_AUDIO_TYPE;
import static com.google.common.net.MediaType.ANY_IMAGE_TYPE;
import static com.google.common.net.MediaType.ANY_TEXT_TYPE;
import static com.google.common.net.MediaType.ANY_TYPE;
import static com.google.common.net.MediaType.ANY_VIDEO_TYPE;
import static com.google.common.net.MediaType.HTML_UTF_8;
import static com.google.common.net.MediaType.JPEG;
import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import junit.framework.TestCase;

/**
 * Tests for {@link MediaType}.
 *
 * @author Gregory Kick
 */
@GwtCompatible(emulated = true)
public class MediaTypeTest extends TestCase {
  @GwtIncompatible // reflection
  public void testParse_useConstants() throws Exception {
    for (MediaType constant : getConstants()) {
      assertSame(constant, MediaType.parse(constant.toString()));
    }
  }

  @GwtIncompatible // reflection
  public void testCreate_useConstants() throws Exception {
    for (MediaType constant : getConstants()) {
      assertSame(
          constant,
          MediaType.create(constant.type(), constant.subtype())
              .withParameters(constant.parameters()));
    }
  }

  @GwtIncompatible // reflection
  public void testConstants_charset() throws Exception {
    for (Field field : getConstantFields()) {
      Optional<Charset> charset = ((MediaType) field.get(null)).charset();
      if (field.getName().endsWith("_UTF_8")) {
        assertThat(charset).hasValue(UTF_8);
      } else {
        assertThat(charset).isAbsent();
      }
    }
  }

  @GwtIncompatible // reflection
  public void testConstants_areUnique() {
    assertThat(getConstants()).containsNoDuplicates();
  }

  @GwtIncompatible // reflection
  private static FluentIterable<Field> getConstantFields() {
    return FluentIterable.from(asList(MediaType.class.getDeclaredFields()))
        .filter(
            new Predicate<Field>() {
              @Override
              public boolean apply(Field input) {
                int modifiers = input.getModifiers();
                return isPublic(modifiers)
                    && isStatic(modifiers)
                    && isFinal(modifiers)
                    && MediaType.class.equals(input.getType());
              }
            });
  }

  @GwtIncompatible // reflection
  private static FluentIterable<MediaType> getConstants() {
    return getConstantFields()
        .transform(
            new Function<Field, MediaType>() {
              @Override
              public MediaType apply(Field input) {
                try {
                  return (MediaType) input.get(null);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              }
            });
  }

  public void testCreate_invalidType() {
    try {
      MediaType.create("te><t", "plaintext");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreate_invalidSubtype() {
    try {
      MediaType.create("text", "pl@intext");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreate_wildcardTypeDeclaredSubtype() {
    try {
      MediaType.create("*", "text");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreateApplicationType() {
    MediaType newType = MediaType.createApplicationType("yams");
    assertEquals("application", newType.type());
    assertEquals("yams", newType.subtype());
  }

  public void testCreateAudioType() {
    MediaType newType = MediaType.createAudioType("yams");
    assertEquals("audio", newType.type());
    assertEquals("yams", newType.subtype());
  }

  public void testCreateImageType() {
    MediaType newType = MediaType.createImageType("yams");
    assertEquals("image", newType.type());
    assertEquals("yams", newType.subtype());
  }

  public void testCreateTextType() {
    MediaType newType = MediaType.createTextType("yams");
    assertEquals("text", newType.type());
    assertEquals("yams", newType.subtype());
  }

  public void testCreateVideoType() {
    MediaType newType = MediaType.createVideoType("yams");
    assertEquals("video", newType.type());
    assertEquals("yams", newType.subtype());
  }

  public void testGetType() {
    assertEquals("text", MediaType.parse("text/plain").type());
    assertEquals("application", MediaType.parse("application/atom+xml; charset=utf-8").type());
  }

  public void testGetSubtype() {
    assertEquals("plain", MediaType.parse("text/plain").subtype());
    assertEquals("atom+xml", MediaType.parse("application/atom+xml; charset=utf-8").subtype());
  }

  private static final ImmutableListMultimap<String, String> PARAMETERS =
      ImmutableListMultimap.of("a", "1", "a", "2", "b", "3");

  public void testGetParameters() {
    assertEquals(ImmutableListMultimap.of(), MediaType.parse("text/plain").parameters());
    assertEquals(
        ImmutableListMultimap.of("charset", "utf-8"),
        MediaType.parse("application/atom+xml; charset=utf-8").parameters());
    assertEquals(PARAMETERS, MediaType.parse("application/atom+xml; a=1; a=2; b=3").parameters());
  }

  public void testWithoutParameters() {
    assertSame(MediaType.parse("image/gif"), MediaType.parse("image/gif").withoutParameters());
    assertEquals(
        MediaType.parse("image/gif"), MediaType.parse("image/gif; foo=bar").withoutParameters());
  }

  public void testWithParameters() {
    assertEquals(
        MediaType.parse("text/plain; a=1; a=2; b=3"),
        MediaType.parse("text/plain").withParameters(PARAMETERS));
    assertEquals(
        MediaType.parse("text/plain; a=1; a=2; b=3"),
        MediaType.parse("text/plain; a=1; a=2; b=3").withParameters(PARAMETERS));
  }

  public void testWithParameters_invalidAttribute() {
    MediaType mediaType = MediaType.parse("text/plain");
    ImmutableListMultimap<String, String> parameters =
        ImmutableListMultimap.of("a", "1", "@", "2", "b", "3");
    try {
      mediaType.withParameters(parameters);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testWithParameter() {
    assertEquals(
        MediaType.parse("text/plain; a=1"), MediaType.parse("text/plain").withParameter("a", "1"));
    assertEquals(
        MediaType.parse("text/plain; a=1"),
        MediaType.parse("text/plain; a=1; a=2").withParameter("a", "1"));
    assertEquals(
        MediaType.parse("text/plain; a=3"),
        MediaType.parse("text/plain; a=1; a=2").withParameter("a", "3"));
    assertEquals(
        MediaType.parse("text/plain; a=1; a=2; b=3"),
        MediaType.parse("text/plain; a=1; a=2").withParameter("b", "3"));
  }

  public void testWithParameter_invalidAttribute() {
    MediaType mediaType = MediaType.parse("text/plain");
    try {
      mediaType.withParameter("@", "2");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testWithParametersIterable() {
    assertEquals(
        MediaType.parse("text/plain"),
        MediaType.parse("text/plain; a=1; a=2").withParameters("a", ImmutableSet.<String>of()));
    assertEquals(
        MediaType.parse("text/plain; a=1"),
        MediaType.parse("text/plain").withParameters("a", ImmutableSet.of("1")));
    assertEquals(
        MediaType.parse("text/plain; a=1"),
        MediaType.parse("text/plain; a=1; a=2").withParameters("a", ImmutableSet.of("1")));
    assertEquals(
        MediaType.parse("text/plain; a=1; a=3"),
        MediaType.parse("text/plain; a=1; a=2").withParameters("a", ImmutableSet.of("1", "3")));
    assertEquals(
        MediaType.parse("text/plain; a=1; a=2; b=3; b=4"),
        MediaType.parse("text/plain; a=1; a=2").withParameters("b", ImmutableSet.of("3", "4")));
  }

  public void testWithParametersIterable_invalidAttribute() {
    MediaType mediaType = MediaType.parse("text/plain");
    try {
      mediaType.withParameters("@", ImmutableSet.of("2"));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testWithParametersIterable_nullValue() {
    MediaType mediaType = MediaType.parse("text/plain");
    try {
      mediaType.withParameters("a", Arrays.asList((String) null));
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testWithCharset() {
    assertEquals(
        MediaType.parse("text/plain; charset=utf-8"),
        MediaType.parse("text/plain").withCharset(UTF_8));
    assertEquals(
        MediaType.parse("text/plain; charset=utf-8"),
        MediaType.parse("text/plain; charset=utf-16").withCharset(UTF_8));
  }

  public void testHasWildcard() {
    assertFalse(PLAIN_TEXT_UTF_8.hasWildcard());
    assertFalse(JPEG.hasWildcard());
    assertTrue(ANY_TYPE.hasWildcard());
    assertTrue(ANY_APPLICATION_TYPE.hasWildcard());
    assertTrue(ANY_AUDIO_TYPE.hasWildcard());
    assertTrue(ANY_IMAGE_TYPE.hasWildcard());
    assertTrue(ANY_TEXT_TYPE.hasWildcard());
    assertTrue(ANY_VIDEO_TYPE.hasWildcard());
  }

  public void testIs() {
    assertTrue(PLAIN_TEXT_UTF_8.is(ANY_TYPE));
    assertTrue(JPEG.is(ANY_TYPE));
    assertTrue(ANY_TEXT_TYPE.is(ANY_TYPE));
    assertTrue(PLAIN_TEXT_UTF_8.is(ANY_TEXT_TYPE));
    assertTrue(PLAIN_TEXT_UTF_8.withoutParameters().is(ANY_TEXT_TYPE));
    assertFalse(JPEG.is(ANY_TEXT_TYPE));
    assertTrue(PLAIN_TEXT_UTF_8.is(PLAIN_TEXT_UTF_8));
    assertTrue(PLAIN_TEXT_UTF_8.is(PLAIN_TEXT_UTF_8.withoutParameters()));
    assertFalse(PLAIN_TEXT_UTF_8.withoutParameters().is(PLAIN_TEXT_UTF_8));
    assertFalse(PLAIN_TEXT_UTF_8.is(HTML_UTF_8));
    assertFalse(PLAIN_TEXT_UTF_8.withParameter("charset", "UTF-16").is(PLAIN_TEXT_UTF_8));
    assertFalse(PLAIN_TEXT_UTF_8.is(PLAIN_TEXT_UTF_8.withParameter("charset", "UTF-16")));
  }

  public void testParse_empty() {
    try {
      MediaType.parse("");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testParse_badInput() {
    try {
      MediaType.parse("/");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("te<t/plain");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/pl@in");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/plain;");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/plain; ");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/plain; a");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/plain; a=");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/plain; a=@");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/plain; a=\"@");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/plain; a=1;");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/plain; a=1; ");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/plain; a=1; b");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/plain; a=1; b=");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MediaType.parse("text/plain; a=\u2025");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testGetCharset() {
    assertThat(MediaType.parse("text/plain").charset()).isAbsent();
    assertThat(MediaType.parse("text/plain; charset=utf-8").charset()).hasValue(UTF_8);
  }

  @GwtIncompatible // Non-UTF-8 Charset
  public void testGetCharset_utf16() {
    assertThat(MediaType.parse("text/plain; charset=utf-16").charset()).hasValue(UTF_16);
  }

  public void testGetCharset_tooMany() {
    MediaType mediaType = MediaType.parse("text/plain; charset=utf-8; charset=utf-16");
    try {
      mediaType.charset();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testGetCharset_illegalCharset() {
    MediaType mediaType = MediaType.parse("text/plain; charset=\"!@#$%^&*()\"");
    try {
      mediaType.charset();
      fail();
    } catch (IllegalCharsetNameException expected) {
    }
  }

  public void testGetCharset_unsupportedCharset() {
    MediaType mediaType = MediaType.parse("text/plain; charset=utf-wtf");
    try {
      mediaType.charset();
      fail();
    } catch (UnsupportedCharsetException expected) {
    }
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            MediaType.create("text", "plain"),
            MediaType.create("TEXT", "PLAIN"),
            MediaType.parse("text/plain"),
            MediaType.parse("TEXT/PLAIN"),
            MediaType.create("text", "plain").withParameter("a", "1").withoutParameters())
        .addEqualityGroup(
            MediaType.create("text", "plain").withCharset(UTF_8),
            MediaType.create("text", "plain").withParameter("CHARSET", "UTF-8"),
            MediaType.create("text", "plain")
                .withParameters(ImmutableMultimap.of("charset", "utf-8")),
            MediaType.parse("text/plain;charset=utf-8"),
            MediaType.parse("text/plain; charset=utf-8"),
            MediaType.parse("text/plain;  charset=utf-8"),
            MediaType.parse("text/plain; \tcharset=utf-8"),
            MediaType.parse("text/plain; \r\n\tcharset=utf-8"),
            MediaType.parse("text/plain; CHARSET=utf-8"),
            MediaType.parse("text/plain; charset=\"utf-8\""),
            MediaType.parse("text/plain; charset=\"\\u\\tf-\\8\""),
            MediaType.parse("text/plain; charset=UTF-8"),
            MediaType.parse("text/plain ; charset=utf-8"))
        .addEqualityGroup(MediaType.parse("text/plain; charset=utf-8; charset=utf-8"))
        .addEqualityGroup(
            MediaType.create("text", "plain").withParameter("a", "value"),
            MediaType.create("text", "plain").withParameter("A", "value"))
        .addEqualityGroup(
            MediaType.create("text", "plain").withParameter("a", "VALUE"),
            MediaType.create("text", "plain").withParameter("A", "VALUE"))
        .addEqualityGroup(
            MediaType.create("text", "plain")
                .withParameters(ImmutableListMultimap.of("a", "1", "a", "2")),
            MediaType.create("text", "plain")
                .withParameters(ImmutableListMultimap.of("a", "2", "a", "1")))
        .addEqualityGroup(MediaType.create("text", "csv"))
        .addEqualityGroup(MediaType.create("application", "atom+xml"))
        .testEquals();
  }

  @GwtIncompatible // Non-UTF-8 Charset
  public void testEquals_nonUtf8Charsets() {
    new EqualsTester()
        .addEqualityGroup(MediaType.create("text", "plain"))
        .addEqualityGroup(MediaType.create("text", "plain").withCharset(UTF_8))
        .addEqualityGroup(MediaType.create("text", "plain").withCharset(UTF_16))
        .testEquals();
  }

  @GwtIncompatible // com.google.common.testing.NullPointerTester
  public void testNullPointer() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicConstructors(MediaType.class);
    tester.testAllPublicStaticMethods(MediaType.class);
    tester.testAllPublicInstanceMethods(MediaType.parse("text/plain"));
  }

  public void testToString() {
    assertEquals("text/plain", MediaType.create("text", "plain").toString());
    assertEquals(
        "text/plain; something=\"cr@zy\"; something-else=\"crazy with spaces\"",
        MediaType.create("text", "plain")
            .withParameter("something", "cr@zy")
            .withParameter("something-else", "crazy with spaces")
            .toString());
  }
}
