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
import static com.google.common.base.Throwables.throwIfUnchecked;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tester to ensure forwarding wrapper works by delegating calls to the corresponding method with
 * the same parameters forwarded and return value forwarded back or exception propagated as is.
 *
 * <p>For example:
 *
 * <pre>{@code
 * new ForwardingWrapperTester().testForwarding(Foo.class, new Function<Foo, Foo>() {
 *   public Foo apply(Foo foo) {
 *     return new ForwardingFoo(foo);
 *   }
 * });
 * }</pre>
 *
 * @author Ben Yu
 * @since 14.0
 */
@GwtIncompatible
public final class ForwardingWrapperTester {

  private boolean testsEquals = false;

  /**
   * Asks for {@link Object#equals} and {@link Object#hashCode} to be tested. That is, forwarding
   * wrappers of equal instances should be equal.
   */
  public ForwardingWrapperTester includingEquals() {
    this.testsEquals = true;
    return this;
  }

  /**
   * Tests that the forwarding wrapper returned by {@code wrapperFunction} properly forwards method
   * calls with parameters passed as is, return value returned as is, and exceptions propagated as
   * is.
   */
  public <T> void testForwarding(
      Class<T> interfaceType, Function<? super T, ? extends T> wrapperFunction) {
    checkNotNull(wrapperFunction);
    checkArgument(interfaceType.isInterface(), "%s isn't an interface", interfaceType);
    Method[] methods = getMostConcreteMethods(interfaceType);
    AccessibleObject.setAccessible(methods, true);
    for (Method method : methods) {
      // Under java 8, interfaces can have default methods that aren't abstract.
      // No need to verify them.
      // Can't check isDefault() for JDK 7 compatibility.
      if (!Modifier.isAbstract(method.getModifiers())) {
        continue;
      }
      // The interface could be package-private or private.
      // filter out equals/hashCode/toString
      if (method.getName().equals("equals")
          && method.getParameterTypes().length == 1
          && method.getParameterTypes()[0] == Object.class) {
        continue;
      }
      if (method.getName().equals("hashCode") && method.getParameterTypes().length == 0) {
        continue;
      }
      if (method.getName().equals("toString") && method.getParameterTypes().length == 0) {
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
        throwIfUnchecked(e);
        throw new RuntimeException(e);
      }
    }
    return methods;
  }

  private static <T> void testSuccessfulForwarding(
      Class<T> interfaceType, Method method, Function<? super T, ? extends T> wrapperFunction) {
    new InteractionTester<T>(interfaceType, method).testInteraction(wrapperFunction);
  }

  private static <T> void testExceptionPropagation(
      Class<T> interfaceType, Method method, Function<? super T, ? extends T> wrapperFunction) {
    RuntimeException exception = new RuntimeException();
    T proxy =
        Reflection.newProxy(
            interfaceType,
            new AbstractInvocationHandler() {
              @Override
              protected Object handleInvocation(Object p, Method m, Object[] args)
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
    T instance = generator.newFreshProxy(interfaceType);
    new EqualsTester()
        .addEqualityGroup(wrapperFunction.apply(instance), wrapperFunction.apply(instance))
        .addEqualityGroup(wrapperFunction.apply(generator.newFreshProxy(interfaceType)))
        // TODO: add an overload to EqualsTester to print custom error message?
        .testEquals();
  }

  private static <T> void testToString(
      Class<T> interfaceType, Function<? super T, ? extends T> wrapperFunction) {
    T proxy = new FreshValueGenerator().newFreshProxy(interfaceType);
    assertEquals(
        "toString() isn't properly forwarded",
        proxy.toString(),
        wrapperFunction.apply(proxy).toString());
  }

  private static Object[] getParameterValues(Method method) {
    FreshValueGenerator paramValues = new FreshValueGenerator();
    List<Object> passedArgs = Lists.newArrayList();
    for (Class<?> paramType : method.getParameterTypes()) {
      passedArgs.add(paramValues.generateFresh(paramType));
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
      this.returnValue = new FreshValueGenerator().generateFresh(method.getReturnType());
    }

    @Override
    protected Object handleInvocation(Object p, Method calledMethod, Object[] args)
        throws Throwable {
      assertEquals(method, calledMethod);
      assertEquals(method + " invoked more than once.", 0, called.get());
      for (int i = 0; i < passedArgs.length; i++) {
        assertEquals(
            "Parameter #" + i + " of " + method + " not forwarded", passedArgs[i], args[i]);
      }
      called.getAndIncrement();
      return returnValue;
    }

    void testInteraction(Function<? super T, ? extends T> wrapperFunction) {
      T proxy = Reflection.newProxy(interfaceType, this);
      T wrapper = wrapperFunction.apply(proxy);
      boolean isPossibleChainingCall = interfaceType.isAssignableFrom(method.getReturnType());
      try {
        Object actualReturnValue = method.invoke(wrapper, passedArgs);
        // If we think this might be a 'chaining' call then we allow the return value to either
        // be the wrapper or the returnValue.
        if (!isPossibleChainingCall || wrapper != actualReturnValue) {
          assertEquals(
              "Return value of " + method + " not forwarded", returnValue, actualReturnValue);
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw Throwables.propagate(e.getCause());
      }
      assertEquals("Failed to forward to " + method, 1, called.get());
    }

    @Override
    public String toString() {
      return "dummy " + interfaceType.getSimpleName();
    }
  }
}
