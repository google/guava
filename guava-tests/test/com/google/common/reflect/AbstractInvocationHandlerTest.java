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

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Tests for {@link AbstractInvocationHandler}.
 *
 * @author Ben Yu
 */
public class AbstractInvocationHandlerTest extends TestCase {

  private final ImmutableList<String> delegate = ImmutableList.of("one", "two");
  private final DelegatingInvocationHandler handler = new DelegatingInvocationHandler(delegate);

  public void testDelegate() {
    assertEquals(delegate, ImmutableList.copyOf(newDelegatingProxy()));
  }

  public void testToString() {
    assertEquals(handler.toString(), newDelegatingProxy().toString());
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(newDelegatingProxy())
        // Actually, this violates List#equals contract.
        // But whatever, no one is going to proxy List (hopefully).
        .addEqualityGroup(newDelegatingProxy())
        .testEquals();
  }

  @SuppressWarnings("unchecked") // proxy of List<String>
  private List<String> newDelegatingProxy() {
    return Reflection.newProxy(List.class, handler);
  }

  private static class DelegatingInvocationHandler extends AbstractInvocationHandler {
    private final Object delegate;

    public DelegatingInvocationHandler(Object delegate) {
      this.delegate = delegate;
    }

    @Override protected Object handleInvocation(Object proxy, Method method, Object[] args)
        throws Throwable {
      return method.invoke(delegate, args);
    }

    @Override public String toString() {
      return "some arbitrary string";
    }
  }
}
