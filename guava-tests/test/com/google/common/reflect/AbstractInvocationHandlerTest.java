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

package com.google.common.reflect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Tests for {@link AbstractInvocationHandler}.
 *
 * @author Ben Yu
 */
public class AbstractInvocationHandlerTest extends TestCase {

  private static final ImmutableList<String> LIST1 = ImmutableList.of("one", "two");
  private static final ImmutableList<String> LIST2 = ImmutableList.of("three");

  public void testDelegate() {
    assertEquals(LIST1, ImmutableList.copyOf(newDelegatingList(LIST1)));
    assertEquals(LIST1, ImmutableList.copyOf(newDelegatingListWithEquals(LIST1)));
  }

  public void testToString() {
    List<String> proxy = newDelegatingList(LIST1);
    assertEquals(Proxy.getInvocationHandler(proxy).toString(), proxy.toString());
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(newDelegatingList(LIST1))
        // Actually, this violates List#equals contract.
        // But whatever, no one is going to proxy List (hopefully).
        .addEqualityGroup(newDelegatingList(LIST1))
        .addEqualityGroup(newDelegatingList(LIST2))
        .addEqualityGroup(newDelegatingListWithEquals(LIST1), newDelegatingListWithEquals(LIST1))
        .addEqualityGroup(
            newDelegatingListWithEquals(LIST2),
            newProxyWithSubHandler1(LIST2), // Makes sure type of handler doesn't affect equality
            newProxyWithSubHandler2(LIST2))
        .addEqualityGroup(newDelegatingIterableWithEquals(LIST2)) // different interface
        .testEquals();
  }

  @SuppressWarnings("unchecked") // proxy of List<String>
  private static List<String> newDelegatingList(List<String> delegate) {
    return Reflection.newProxy(List.class, new DelegatingInvocationHandler(delegate));
  }

  @SuppressWarnings("unchecked") // proxy of List<String>
  private static List<String> newDelegatingListWithEquals(List<String> delegate) {
    return Reflection.newProxy(List.class, new DelegatingInvocationHandlerWithEquals(delegate));
  }

  @SuppressWarnings("unchecked") // proxy of Iterable<String>
  private static Iterable<String> newDelegatingIterableWithEquals(Iterable<String> delegate) {
    return Reflection.newProxy(Iterable.class, new DelegatingInvocationHandlerWithEquals(delegate));
  }

  @SuppressWarnings("unchecked") // proxy of List<String>
  private static List<String> newProxyWithSubHandler1(List<String> delegate) {
    return Reflection.newProxy(List.class, new SubHandler1(delegate));
  }

  @SuppressWarnings("unchecked") // proxy of List<String>
  private static List<String> newProxyWithSubHandler2(List<String> delegate) {
    return Reflection.newProxy(List.class, new SubHandler2(delegate));
  }

  private static class DelegatingInvocationHandler extends AbstractInvocationHandler {
    final Object delegate;

    DelegatingInvocationHandler(Object delegate) {
      this.delegate = checkNotNull(delegate);
    }

    @Override protected Object handleInvocation(Object proxy, Method method, Object[] args)
        throws Throwable {
      return method.invoke(delegate, args);
    }

    @Override public String toString() {
      return "some arbitrary string";
    }
  }

  private static class DelegatingInvocationHandlerWithEquals extends DelegatingInvocationHandler {

    DelegatingInvocationHandlerWithEquals(Object delegate) {
      super(delegate);
    }

    @Override public boolean equals(Object obj) {
      if (obj instanceof DelegatingInvocationHandlerWithEquals) {
        DelegatingInvocationHandlerWithEquals that = (DelegatingInvocationHandlerWithEquals) obj;
        return delegate.equals(that.delegate);
      } else {
        return false;
      }
    }

    @Override public int hashCode() {
      return delegate.hashCode();
    }
  }

  private static class SubHandler1 extends DelegatingInvocationHandlerWithEquals {
    SubHandler1(Object delegate) {
      super(delegate);
    }
  }

  private static class SubHandler2 extends DelegatingInvocationHandlerWithEquals {
    SubHandler2(Object delegate) {
      super(delegate);
    }
  }
}
