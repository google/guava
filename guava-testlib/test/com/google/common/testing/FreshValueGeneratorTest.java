/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.testing;

import com.google.common.base.CharMatcher;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.google.common.reflect.TypeToken;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Tests for {@link FreshValueGenerator}.
 *
 * @author Ben Yu
 */
public class FreshValueGeneratorTest extends TestCase {

  public void testFreshInstance() {
    assertFreshInstances(
        String.class, CharSequence.class,
        Appendable.class, StringBuffer.class, StringBuilder.class,
        Pattern.class, MatchResult.class,
        Number.class, int.class, Integer.class,
        long.class, Long.class,
        short.class, Short.class,
        byte.class, Byte.class,
        boolean.class, Boolean.class,
        char.class, Character.class,
        int[].class, Object[].class,
        UnsignedInteger.class, UnsignedLong.class,
        BigInteger.class, BigDecimal.class,
        Throwable.class, Error.class, Exception.class, RuntimeException.class,
        Charset.class, Locale.class, Currency.class,
        List.class, Map.Entry.class,
        Object.class,
        Equivalence.class, Predicate.class, Function.class,
        Comparable.class, Comparator.class, Ordering.class,
        Class.class, Type.class, TypeToken.class,
        TimeUnit.class, Ticker.class,
        Joiner.class, Splitter.class, CharMatcher.class,
        InputStream.class, ByteArrayInputStream.class,
        Reader.class, Readable.class, StringReader.class,
        OutputStream.class, ByteArrayOutputStream.class,
        Writer.class, StringWriter.class, File.class,
        Buffer.class, ByteBuffer.class, CharBuffer.class,
        ShortBuffer.class, IntBuffer.class, LongBuffer.class,
        FloatBuffer.class, DoubleBuffer.class);
  }

  public void testEnums() {
    assertEqualInstance(EmptyEnum.class, null);
    assertEqualInstance(OneConstantEnum.class, OneConstantEnum.CONSTANT1);
    assertFreshInstance(TwoConstantEnum.class);
  }

  public void testAddSampleInstances_twoInstances() {
    FreshValueGenerator generator = new FreshValueGenerator();
    generator.addSampleInstances(String.class, ImmutableList.of("a", "b"));
    assertEquals("a", generator.generate(String.class));
    assertEquals("b", generator.generate(String.class));
    assertEquals("a", generator.generate(String.class));
  }

  public void testAddSampleInstances_oneInstance() {
    FreshValueGenerator generator = new FreshValueGenerator();
    generator.addSampleInstances(String.class, ImmutableList.of("a"));
    assertEquals("a", generator.generate(String.class));
    assertEquals("a", generator.generate(String.class));
  }

  public void testAddSampleInstances_noInstance() {
    FreshValueGenerator generator = new FreshValueGenerator();
    generator.addSampleInstances(String.class, ImmutableList.<String>of());
    assertEquals(new FreshValueGenerator().generate(String.class),
        generator.generate(String.class));
  }

  public void testFreshCurrency() {
    FreshValueGenerator generator = new FreshValueGenerator();
    // repeat a few times to make sure we don't stumble upon a bad Locale
    assertNotNull(generator.generate(Currency.class));
    assertNotNull(generator.generate(Currency.class));
    assertNotNull(generator.generate(Currency.class));
  }

  public void testNulls() throws Exception {
    new ClassSanityTester()
        .setDefault(Method.class, FreshValueGeneratorTest.class.getDeclaredMethod("testNulls"))
        .testNulls(FreshValueGenerator.class);
  }

  private static void assertFreshInstances(Class<?>... types) {
    for (Class<?> type : types) {
      assertFreshInstance(type);
    }
  }

  private static <T> void assertFreshInstance(Class<T> type) {
    FreshValueGenerator generator = new FreshValueGenerator();
    T value1 = generator.generate(type);
    T value2 = generator.generate(type);
    assertNotNull("Null returned for " + type, value1);
    assertFalse("Equal instance " + value1 + " returned for " + type, value1.equals(value2));
  }

  private static <T> void assertEqualInstance(Class<T> type, T value) {
    FreshValueGenerator generator = new FreshValueGenerator();
    assertEquals(value, generator.generate(type));
    assertEquals(value, generator.generate(type));
  }

  private enum EmptyEnum {}

  private enum OneConstantEnum {
    CONSTANT1
  }

  private enum TwoConstantEnum {
    CONSTANT1, CONSTANT2
  }
}
