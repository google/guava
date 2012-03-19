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

import com.google.common.annotations.Beta;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

/**
 * Abstract implementation of {@link InvocationHandler} that handles {@link Object#equals},
 * {@link Object#hashCode} and {@link Object#toString}.
 *
 * @author Ben Yu
 * @since 12.0
 */
@Beta
public abstract class AbstractInvocationHandler implements InvocationHandler {

  private static final Object[] NO_ARGS = {};

  /**
   * {@inheritDoc}
   * 
   * <p>{@link Object#equals}, {@link Object#hashCode} are implemented according to referential
   * equality (the default behavior of {@link Object}). {@link Object#toString} delegates to
   * {@link #toString} that can be overridden by subclasses.
   */
  @Override public final Object invoke(Object proxy, Method method, @Nullable Object[] args)
      throws Throwable {
    if (args == null) {
      args = NO_ARGS;
    }
    if (args.length == 0 && method.getName().equals("hashCode")) {
      return System.identityHashCode(proxy);
    }
    if (args.length == 1
        && method.getName().equals("equals")
        && method.getParameterTypes()[0] == Object.class) {
      return proxy == args[0];
    }
    if (args.length == 0 && method.getName().equals("toString")) {
      return toString();
    }
    return handleInvocation(proxy, method, args);
  }

  /**
   * {@link #invoke} delegates to this method upon any method invocation on the proxy instance,
   * except {@link Object#equals}, {@link Object#hashCode} and {@link Object#toString}. The result
   * will be returned as the proxied method's return value.
   * 
   * <p>Unlike {@link #invoke}, {@code args} will never be null. When the method has no parameter,
   * an empty array is passed in.
   */
  protected abstract Object handleInvocation(Object proxy, Method method, Object[] args)
      throws Throwable;

  /**
   * The dynamic proxies' {@link Object#toString} will delegate to this method. Subclasses can
   * override this to provide custom string representation of the proxies.
   */
  @Override public String toString() {
    return super.toString();
  }
}
