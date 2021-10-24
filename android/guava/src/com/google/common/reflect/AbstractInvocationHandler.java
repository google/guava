/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.reflect;

import com.google.common.annotations.Beta;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract implementation of {@link InvocationHandler} that handles {@link Object#equals}, {@link
 * Object#hashCode} and {@link Object#toString}. For example:
 *
 * <pre>
 * class Unsupported extends AbstractInvocationHandler {
 *   protected Object handleInvocation(Object proxy, Method method, Object[] args) {
 *     throw new UnsupportedOperationException();
 *   }
 * }
 *
 * CharSequence unsupported = Reflection.newProxy(CharSequence.class, new Unsupported());
 * </pre>
 *
 * @author Ben Yu
 * @since 12.0
 */
@Beta
@ElementTypesAreNonnullByDefault
public abstract class AbstractInvocationHandler implements InvocationHandler {

  private static final Object[] NO_ARGS = {};

  /**
   * {@inheritDoc}
   *
   * <ul>
   *   <li>{@code proxy.hashCode()} delegates to {@link AbstractInvocationHandler#hashCode}
   *   <li>{@code proxy.toString()} delegates to {@link AbstractInvocationHandler#toString}
   *   <li>{@code proxy.equals(argument)} returns true if:
   *       <ul>
   *         <li>{@code proxy} and {@code argument} are of the same type
   *         <li>and {@link AbstractInvocationHandler#equals} returns true for the {@link
   *             InvocationHandler} of {@code argument}
   *       </ul>
   *   <li>other method calls are dispatched to {@link #handleInvocation}.
   * </ul>
   */
  @Override
  @CheckForNull
  public final Object invoke(Object proxy, Method method, @CheckForNull @Nullable Object[] args)
      throws Throwable {
    if (args == null) {
      args = NO_ARGS;
    }
    if (args.length == 0 && method.getName().equals("hashCode")) {
      return hashCode();
    }
    if (args.length == 1
        && method.getName().equals("equals")
        && method.getParameterTypes()[0] == Object.class) {
      Object arg = args[0];
      if (arg == null) {
        return false;
      }
      if (proxy == arg) {
        return true;
      }
      return isProxyOfSameInterfaces(arg, proxy.getClass())
          && equals(Proxy.getInvocationHandler(arg));
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
  @CheckForNull
  protected abstract Object handleInvocation(Object proxy, Method method, @Nullable Object[] args)
      throws Throwable;

  /**
   * By default delegates to {@link Object#equals} so instances are only equal if they are
   * identical. {@code proxy.equals(argument)} returns true if:
   *
   * <ul>
   *   <li>{@code proxy} and {@code argument} are of the same type
   *   <li>and this method returns true for the {@link InvocationHandler} of {@code argument}
   * </ul>
   *
   * <p>Subclasses can override this method to provide custom equality.
   */
  @Override
  public boolean equals(@CheckForNull Object obj) {
    return super.equals(obj);
  }

  /**
   * By default delegates to {@link Object#hashCode}. The dynamic proxies' {@code hashCode()} will
   * delegate to this method. Subclasses can override this method to provide custom equality.
   */
  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * By default delegates to {@link Object#toString}. The dynamic proxies' {@code toString()} will
   * delegate to this method. Subclasses can override this method to provide custom string
   * representation for the proxies.
   */
  @Override
  public String toString() {
    return super.toString();
  }

  private static boolean isProxyOfSameInterfaces(Object arg, Class<?> proxyClass) {
    return proxyClass.isInstance(arg)
        // Equal proxy instances should mostly be instance of proxyClass
        // Under some edge cases (such as the proxy of JDK types serialized and then deserialized)
        // the proxy type may not be the same.
        // We first check isProxyClass() so that the common case of comparing with non-proxy objects
        // is efficient.
        || (Proxy.isProxyClass(arg.getClass())
            && Arrays.equals(arg.getClass().getInterfaces(), proxyClass.getInterfaces()));
  }
}
