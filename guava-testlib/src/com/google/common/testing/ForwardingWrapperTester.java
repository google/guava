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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
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
import java.lang.reflect.AccessibleObject;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Tester to ensure forwarding wrapper works by delegating calls to the corresponding method
 * with the same parameters forwarded and return value forwarded back or exception propagated as is.
 *
 * <p>For example: <pre>   {@code
 *   new ForwardingWrapperTester().testForwarding(Foo.class, new Function<Foo, Foo>() {
 *     public Foo apply(Foo foo) {
 *       return ForwardingFoo(foo);
 *     }
 *   });}</pre>
 *
 * @author Ben Yu
 * @since 14.0
 */
@Beta
public final class ForwardingWrapperTester {

  private boolean testsEquals = false;

  /**
   * Asks for {@link Object#equals} and {@link Object#hashCode} to be tested.
   * That is, forwarding wrappers of equal instances should be equal.
   */
  public ForwardingWrapperTester includingEquals() {
    this.testsEquals = true;
    return this;
  }

  /**
   * Tests that the forwarding wrapper returned by {@code wrapperFunction} properly forwards
   * method calls with parameters passed as is, return value returned as is, and exceptions
   * propagated as is.
   */
  public <T> void testForwarding(
      Class<T> interfaceType, Function<? super T, ? extends T> wrapperFunction) {
    checkNotNull(wrapperFunction);
    checkArgument(interfaceType.isInterface(), "%s isn't an interface", interfaceType);
    Method[] methods = getMostConcreteMethods(interfaceType);
    AccessibleObject.setAccessible(methods, true);
    for (Method method : methods) {
      // The interface could be package-private or private.
      // filter out equals/hashCode/toString
      if (method.getName().equals("equals")
          && method.getParameterTypes().length == 1
          && method.getParameterTypes()[0] == Object.class) {
        continue;
      }
      if (method.getName().equals("hashCode")
          && method.getParameterTypes().length == 0) {
        continue;
      }
      if (method.getName().equals("toString")
          && method.getParameterTypes().length == 0) {
        continue;
      }
      testSuccessfulForwarding(interfaceType, method, wrapperFunction);
      testExceptionPropagation(interfaceType, method, wrapperFunction);
    }
    if (testsEquals) {
      testEquals(interfaceType, wrapperFunction);
    }
    testToString(interfaceType, wrapperFunction);
  }

  /** Returns the most concrete public methods from {@code type}. */
  private static Method[] getMostConcreteMethods(Class<?> type) {
    Method[] methods = type.getMethods();
    for (int i = 0; i < methods.length; i++) {
      try {
        methods[i] = type.getMethod(methods[i].getName(), methods[i].getParameterTypes());
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    return methods;
  }

  private static <T> void testSuccessfulForwarding(
      Class<T> interfaceType,  Method method, Function<? super T, ? extends T> wrapperFunction) {
    new InteractionTester<T>(interfaceType, method).testInteraction(wrapperFunction);
  }

  private static <T> void testExceptionPropagation(
      Class<T> interfaceType, Method method, Function<? super T, ? extends T> wrapperFunction) {
    final RuntimeException exception = new RuntimeException();
    T proxy = Reflection.newProxy(interfaceType, new AbstractInvocationHandler() {
      @Override protected Object handleInvocation(Object p, Method m, Object[] args)
          throws Throwable {
        throw exception;
      }
    });
    T wrapper = wrapperFunction.apply(proxy);
    try {
      method.invoke(wrapper, getParameterValues(method));
      fail(method + " failed to throw exception as is.");
    } catch (InvocationTargetException e) {
      if (exception != e.getCause()) {
        throw new RuntimeException(e);
      }
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  private static <T> void testEquals(
      Class<T> interfaceType, Function<? super T, ? extends T> wrapperFunction) {
    FreshValueGenerator generator = new FreshValueGenerator();
    T instance = generator.newProxy(interfaceType);
    new EqualsTester()
        .addEqualityGroup(wrapperFunction.apply(instance), wrapperFunction.apply(instance))
        .addEqualityGroup(wrapperFunction.apply(generator.newProxy(interfaceType)))
        // TODO: add an overload to EqualsTester to print custom error message?
        .testEquals();
  }

  private static <T> void testToString(
      Class<T> interfaceType, Function<? super T, ? extends T> wrapperFunction) {
    T proxy = new FreshValueGenerator().newProxy(interfaceType);
    assertEquals("toString() isn't properly forwarded",
        proxy.toString(), wrapperFunction.apply(proxy).toString());
  }

  private static Object[] getParameterValues(Method method) {
    FreshValueGenerator paramValues = new FreshValueGenerator();
    final List<Object> passedArgs = Lists.newArrayList();
    for (Class<?> paramType : method.getParameterTypes()) {
      passedArgs.add(paramValues.generate(paramType));
    }
    return passedArgs.toArray();
  }

  /** Tests a single interaction against a method. */
  private static final class InteractionTester<T> extends AbstractInvocationHandler {

    private final Class<T> interfaceType;
    private final Method method;
    private final Object[] passedArgs;
    private final Object returnValue;
    private final AtomicInteger called = new AtomicInteger();

    InteractionTester(Class<T> interfaceType, Method method) {
      this.interfaceType = interfaceType;
      this.method = method;
      this.passedArgs = getParameterValues(method);
      this.returnValue = new FreshValueGenerator().generate(method.getReturnType());
    }

    @Override protected Object handleInvocation(Object p, Method calledMethod, Object[] args)
        throws Throwable {
      assertEquals(method, calledMethod);
      assertEquals(method + " invoked more than once.", 0, called.get());
      for (int i = 0; i < passedArgs.length; i++) {
        assertEquals("Parameter #" + i + " of " + method + " not forwarded",
            passedArgs[i], args[i]);
      }
      called.getAndIncrement();
      return returnValue;
    }

    void testInteraction(Function<? super T, ? extends T> wrapperFunction) {
      T proxy = Reflection.newProxy(interfaceType, this);
      T wrapper = wrapperFunction.apply(proxy);
      try {
        assertEquals("Return value of " + method + " not forwarded", returnValue,
            method.invoke(wrapper, passedArgs));
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw Throwables.propagate(e.getCause());
      }
      assertEquals("Failed to forward to " + method, 1, called.get());
    }

    @Override public String toString() {
      return "dummy " + interfaceType.getSimpleName();
    }
  }

  /** Generates fresh instances of types that are different from each other. */
  @VisibleForTesting static final class FreshValueGenerator {

    private final AtomicInteger differentiator = new AtomicInteger();

    <T> T generate(Class<T> type) {
      if (type.isInterface()) {
        // always create a new proxy
        return newProxy(type);
      }
      for (Method method : FreshValueGenerator.class.getDeclaredMethods()) {
        if (method.isAnnotationPresent(Generates.class)) {
          if (Primitives.wrap(type).isAssignableFrom(Primitives.wrap(method.getReturnType()))) {
            try {
              @SuppressWarnings("unchecked") // protected by isAssignableFrom
              T result = (T) method.invoke(this);
              return result;
            } catch (Exception e) {
              throw Throwables.propagate(e);
            }
          }
        }
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
      return Iterables.get(instances, freshInt() % instances.size());
    }

    private <T> T newProxy(final Class<T> interfaceType) {
      return Reflection.newProxy(interfaceType, new AbstractInvocationHandler() {
        @Override protected Object handleInvocation(Object proxy, Method method, Object[] args) {
          throw new UnsupportedOperationException();
        }

        final String string = paramString(interfaceType, freshInt());
        @Override public String toString() {
          return string;
        }
      });
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
      return nextInstance(Locale.getAvailableLocales(), Locale.ENGLISH);
    }

    @SuppressWarnings("unused")
    @Generates private Currency freshCurrency() {
      return Currency.getInstance(freshLocale());
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
}
