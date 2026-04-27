/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;
import com.google.common.testing.EqualsTester;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/** Unit tests for {@link Converter}. */
@GwtCompatible
@NullUnmarked
public class ConverterTest extends TestCase {

  private static final Converter<String, Long> STR_TO_LONG =
      new Converter<String, Long>() {
        @Override
        protected Long doForward(String object) {
          return Long.valueOf(object);
        }

        @Override
        protected String doBackward(Long object) {
          return String.valueOf(object);
        }

        @Override
        public String toString() {
          return "string2long";
        }
      };

  private static final Long LONG_VAL = 12345L;
  private static final String STR_VAL = "12345";

  private static final ImmutableList<String> STRINGS = ImmutableList.of("123", "456");
  private static final ImmutableList<Long> LONGS = ImmutableList.of(123L, 456L);

  public void testConverter() {
    assertEquals(LONG_VAL, STR_TO_LONG.convert(STR_VAL));
    assertThat(STR_TO_LONG.reverse().convert(LONG_VAL)).isEqualTo(STR_VAL);

    Iterable<Long> convertedValues = STR_TO_LONG.convertAll(STRINGS);
    assertEquals(LONGS, ImmutableList.copyOf(convertedValues));
  }

  public void testConvertAllIsView() {
    List<String> mutableList = newArrayList("789", "123");
    Iterable<Long> convertedValues = STR_TO_LONG.convertAll(mutableList);
    assertEquals(ImmutableList.of(789L, 123L), ImmutableList.copyOf(convertedValues));

    Iterator<Long> iterator = convertedValues.iterator();
    iterator.next();
    iterator.remove();
    assertEquals(ImmutableList.of("123"), mutableList);
  }

  public void testReverse() {
    Converter<Long, String> reverseConverter = STR_TO_LONG.reverse();

    assertThat(reverseConverter.convert(LONG_VAL)).isEqualTo(STR_VAL);
    assertEquals(LONG_VAL, reverseConverter.reverse().convert(STR_VAL));

    Iterable<String> convertedValues = reverseConverter.convertAll(LONGS);
    assertEquals(STRINGS, ImmutableList.copyOf(convertedValues));

    assertThat(reverseConverter.reverse()).isSameInstanceAs(STR_TO_LONG);

    assertThat(reverseConverter.toString()).isEqualTo("string2long.reverse()");

    new EqualsTester()
        .addEqualityGroup(STR_TO_LONG, STR_TO_LONG.reverse().reverse())
        .addEqualityGroup(STR_TO_LONG.reverse(), STR_TO_LONG.reverse())
        .testEquals();
  }

  public void testReverseReverse() {
    Converter<String, Long> converter = STR_TO_LONG;
    assertEquals(converter, converter.reverse().reverse());
  }

  // We need to test that apply() does in fact behave like convert().
  @SuppressWarnings("InlineMeInliner")
  public void testApply() {
    assertEquals(LONG_VAL, STR_TO_LONG.apply(STR_VAL));
  }

  private static class StringWrapper {
    private final String value;

    StringWrapper(String value) {
      this.value = value;
    }
  }

  @GwtIncompatible // J2CL generics problem
  public void testAndThen() {
    Converter<StringWrapper, String> first =
        new Converter<StringWrapper, String>() {
          @Override
          protected String doForward(StringWrapper object) {
            return object.value;
          }

          @Override
          protected StringWrapper doBackward(String object) {
            return new StringWrapper(object);
          }

          @Override
          public String toString() {
            return "StringWrapper";
          }
        };

    Converter<StringWrapper, Long> converter = first.andThen(STR_TO_LONG);

    assertEquals(LONG_VAL, converter.convert(new StringWrapper(STR_VAL)));
    assertThat(converter.reverse().convert(LONG_VAL).value).isEqualTo(STR_VAL);

    assertThat(converter.toString()).isEqualTo("StringWrapper.andThen(string2long)");

    new EqualsTester()
        .addEqualityGroup(first.andThen(STR_TO_LONG), first.andThen(STR_TO_LONG))
        .testEquals();
  }

  @GwtIncompatible // J2CL generics problem
  public void testIdentityConverter() {
    Converter<String, String> stringIdentityConverter = Converter.identity();

    assertThat(stringIdentityConverter.reverse()).isSameInstanceAs(stringIdentityConverter);
    assertThat(stringIdentityConverter.andThen(STR_TO_LONG)).isSameInstanceAs(STR_TO_LONG);

    assertThat(stringIdentityConverter.convert(STR_VAL)).isSameInstanceAs(STR_VAL);
    assertThat(stringIdentityConverter.reverse().convert(STR_VAL)).isSameInstanceAs(STR_VAL);

    assertThat(stringIdentityConverter.toString()).isEqualTo("Converter.identity()");

    assertThat(Converter.identity()).isSameInstanceAs(Converter.identity());
  }

  public void testFrom() {
    Function<String, Integer> forward = Integer::parseInt;
    Function<Object, String> backward = toStringFunction();

    Converter<String, Number> converter = Converter.from(forward, backward);

    assertThat(converter.convert(null)).isNull();
    assertThat(converter.reverse().convert(null)).isNull();

    assertEquals((Integer) 5, converter.convert("5"));
    assertThat(converter.reverse().convert(5)).isEqualTo("5");
  }

  // Null-passthrough violates our nullness annotations, so we don't support it under J2KT.
  @J2ktIncompatible
  public void testNullIsPassedThrough() {
    Converter<String, String> nullsArePassed = sillyConverter(false);
    assertThat(nullsArePassed.convert("foo")).isEqualTo("forward");
    assertThat(nullsArePassed.convert(null)).isEqualTo("forward");
    assertThat(nullsArePassed.reverse().convert("foo")).isEqualTo("backward");
    assertThat(nullsArePassed.reverse().convert(null)).isEqualTo("backward");
  }

  public void testNullIsNotPassedThrough() {
    Converter<String, String> nullsAreHandled = sillyConverter(true);
    assertThat(nullsAreHandled.convert("foo")).isEqualTo("forward");
    assertThat(nullsAreHandled.convert(null)).isNull();
    assertThat(nullsAreHandled.reverse().convert("foo")).isEqualTo("backward");
    assertThat(nullsAreHandled.reverse().convert(null)).isNull();
  }

  private static Converter<String, String> sillyConverter(boolean handleNullAutomatically) {
    return new Converter<String, String>(handleNullAutomatically) {
      @Override
      protected String doForward(String string) {
        return "forward";
      }

      @Override
      protected String doBackward(String string) {
        return "backward";
      }
    };
  }

  public void testSerialization_identity() {
    Converter<String, String> identityConverter = Converter.identity();
    reserializeAndAssert(identityConverter);
  }

  public void testSerialization_reverse() {
    Converter<Long, String> reverseConverter = Longs.stringConverter().reverse();
    reserializeAndAssert(reverseConverter);
  }

  @GwtIncompatible // J2CL generics problem
  public void testSerialization_andThen() {
    Converter<String, Long> converterA = Longs.stringConverter();
    Converter<Long, String> reverseConverter = Longs.stringConverter().reverse();
    Converter<String, String> composedConverter = converterA.andThen(reverseConverter);
    reserializeAndAssert(composedConverter);
  }

  public void testSerialization_from() {
    Converter<String, String> dumb = Converter.from(toStringFunction(), toStringFunction());
    reserializeAndAssert(dumb);
  }
}
