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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Equivalence;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.base.Ticker;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Generates fresh instances of types that are different from each other (if possible).
 *
 * @author Ben Yu
 */
class FreshValueGenerator {

  private final AtomicInteger differentiator = new AtomicInteger(1);
  private final ListMultimap<Class<?>, Object> sampleInstances = ArrayListMultimap.create();

  <T> void addSampleInstances(Class<T> type, Iterable<? extends T> instances) {
    sampleInstances.putAll(checkNotNull(type), checkNotNull(instances));
  }

  final <T> T generate(Class<T> type) {
    List<Object> samples = sampleInstances.get(type);
    @SuppressWarnings("unchecked") // sampleInstances is always registered by type.
    T sample = (T) nextInstance(samples, null);
    if (sample != null) {
      return sample;
    }
    for (Method method : FreshValueGenerator.class.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Generates.class)) {
        if (Primitives.wrap(type).isAssignableFrom(Primitives.wrap(method.getReturnType()))) {
          try {
            @SuppressWarnings("unchecked") // protected by isAssignableFrom
            T result = (T) method.invoke(this);
            return result;
          } catch (InvocationTargetException e) {
            Throwables.propagate(e.getCause());
          } catch (Exception e) {
            throw Throwables.propagate(e);
          }
        }
      }
    }
    if (type.isInterface()) {
      // always create a new proxy
      return newProxy(type);
    }
    if (type.isEnum()) {
      return nextInstance(type.getEnumConstants(), null);
    }
    return ArbitraryInstances.get(type);
  }

  private <T> T nextInstance(T[] instances, T defaultValue) {
    return nextInstance(Arrays.asList(instances), defaultValue);
  }

  private <T> T nextInstance(Collection<T> instances, T defaultValue) {
    if (instances.isEmpty()) {
      return defaultValue;
    }
    // freshInt() is 1-based.
    return Iterables.get(instances, (freshInt() - 1) % instances.size());
  }

  final <T> T newProxy(final Class<T> interfaceType) {
    final int identity = freshInt();
    return Reflection.newProxy(interfaceType, new AbstractInvocationHandler() {
      @Override protected Object handleInvocation(Object proxy, Method method, Object[] args) {
        return interfaceMethodCalled(interfaceType, method);
      }
      @Override public String toString() {
        return paramString(interfaceType, identity);
      }
    });
  }

  /** Subclasses can override to provide different return value for proxied interface methods. */
  Object interfaceMethodCalled(
      @SuppressWarnings("unused") Class<?> interfaceType,
      @SuppressWarnings("unused") Method method) {
    throw new UnsupportedOperationException();
  }

  private static String paramString(Class<?> type, int i) {
    return type.getSimpleName() + '@' + i;
  }

  /** Annotates a method to be the instance supplier of a certain type. */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  private @interface Generates {}

  @Generates private Class<?> freshClass() {
    return nextInstance(
        ImmutableList.of(
            int.class, long.class, void.class,
            Object.class, Object[].class, Iterable.class),
        Object.class);
  }

  @Generates private int freshInt() {
    return differentiator.getAndIncrement();
  }

  @Generates private long freshLong() {
    return freshInt();
  }

  @SuppressWarnings("unused")
  @Generates private float freshFloat() {
    return freshInt();
  }

  @SuppressWarnings("unused")
  @Generates private double freshDouble() {
    return freshInt();
  }

  @SuppressWarnings("unused")
  @Generates private short freshShort() {
    return (short) freshInt();
  }

  @SuppressWarnings("unused")
  @Generates private byte freshByte() {
    return (byte) freshInt();
  }

  @SuppressWarnings("unused")
  @Generates private char freshChar() {
    return freshString().charAt(0);
  }

  @SuppressWarnings("unused")
  @Generates private boolean freshBoolean() {
    return freshInt() % 2 == 0;
  }

  @SuppressWarnings("unused")
  @Generates private UnsignedInteger freshUnsignedInteger() {
    return UnsignedInteger.asUnsigned(freshInt());
  }

  @SuppressWarnings("unused")
  @Generates private UnsignedLong freshUnsignedLong() {
    return UnsignedLong.asUnsigned(freshLong());
  }

  @SuppressWarnings("unused")
  @Generates private BigInteger freshBigInteger() {
    return BigInteger.valueOf(freshInt());
  }

  @SuppressWarnings("unused")
  @Generates private BigDecimal freshBigDecimal() {
    return BigDecimal.valueOf(freshInt());
  }

  @Generates private String freshString() {
    return Integer.toString(freshInt());
  }

  @SuppressWarnings("unused")
  @Generates private Pattern freshPattern() {
    return Pattern.compile(freshString());
  }

  @SuppressWarnings("unused")
  @Generates private Charset freshCharset() {
    return nextInstance(Charset.availableCharsets().values(), Charsets.UTF_8);
  }

  @Generates private Locale freshLocale() {
    return nextInstance(Locale.getAvailableLocales(), Locale.US);
  }

  @SuppressWarnings("unused")
  @Generates private Currency freshCurrency() {
    for (Set<Locale> uselessLocales = Sets.newHashSet(); ; ) {
      Locale locale = freshLocale();
      if (uselessLocales.contains(locale)) { // exhausted all locales
        return Currency.getInstance(Locale.US);
      }
      try {
        return Currency.getInstance(locale);
      } catch (IllegalArgumentException e) {
        uselessLocales.add(locale);
      }
    }
  }

  // common.base
  @SuppressWarnings("unused")
  @Generates private Joiner freshJoiner() {
    return Joiner.on(freshString());
  }

  @SuppressWarnings("unused")
  @Generates private Splitter freshSplitter() {
    return Splitter.on(freshString());
  }

  @SuppressWarnings("unused")
  @Generates private <T> Equivalence<T> freshEquivalence() {
    return new Equivalence<T>() {
      @Override protected boolean doEquivalent(T a, T b) {
        return false;
      }
      @Override protected int doHash(T t) {
        return 0;
      }
      final String string = paramString(Equivalence.class, freshInt());
      @Override public String toString() {
        return string;
      }
    };
  }

  @SuppressWarnings("unused")
  @Generates private CharMatcher freshCharMatcher() {
    return new CharMatcher() {
      @Override public boolean matches(char c) {
        return false;
      }
      final String string = paramString(CharMatcher.class, freshInt());
      @Override public String toString() {
        return string;
      }
    };
  }

  @SuppressWarnings("unused")
  @Generates private Ticker freshTicker() {
    return new Ticker() {
      @Override public long read() {
        return 0;
      }
      final String string = paramString(Ticker.class, freshInt());
      @Override public String toString() {
        return string;
      }
    };
  }

  // common.collect
  @SuppressWarnings("unused")
  @Generates private <T> Ordering<T> freshOrdering() {
    return new Ordering<T>() {
      @Override public int compare(T left, T right) {
        return 0;
      }
      final String string = paramString(Ordering.class, freshInt());
      @Override public String toString() {
        return string;
      }
    };
  }

  // common.reflect
  @SuppressWarnings("unused")
  @Generates private TypeToken<?> freshTypeToken() {
    return TypeToken.of(freshClass());
  }

  // io types
  @SuppressWarnings("unused")
  @Generates private File freshFile() {
    return new File(freshString());
  }

  @SuppressWarnings("unused")
  @Generates private static ByteArrayInputStream freshInputStream() {
    return new ByteArrayInputStream(new byte[0]);
  }

  @SuppressWarnings("unused")
  @Generates private StringReader freshStringReader() {
    return new StringReader(freshString());
  }

  @SuppressWarnings("unused")
  @Generates private CharBuffer freshCharBuffer() {
    return CharBuffer.allocate(freshInt());
  }

  @SuppressWarnings("unused")
  @Generates private ByteBuffer freshByteBuffer() {
    return ByteBuffer.allocate(freshInt());
  }

  @SuppressWarnings("unused")
  @Generates private ShortBuffer freshShortBuffer() {
    return ShortBuffer.allocate(freshInt());
  }

  @SuppressWarnings("unused")
  @Generates private IntBuffer freshIntBuffer() {
    return IntBuffer.allocate(freshInt());
  }

  @SuppressWarnings("unused")
  @Generates private LongBuffer freshLongBuffer() {
    return LongBuffer.allocate(freshInt());
  }

  @SuppressWarnings("unused")
  @Generates private FloatBuffer freshFloatBuffer() {
    return FloatBuffer.allocate(freshInt());
  }

  @SuppressWarnings("unused")
  @Generates private DoubleBuffer freshDoubleBuffer() {
    return DoubleBuffer.allocate(freshInt());
  }
}
