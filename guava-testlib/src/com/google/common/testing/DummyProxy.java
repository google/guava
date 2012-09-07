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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Generates a dummy interface proxy that simply returns a dummy value for each method.
 *
 * @author Ben Yu
 */
abstract class DummyProxy {

  final <T> T newProxy(final TypeToken<T> interfaceType) {
    Set<Class<? super T>> interfaceClasses = interfaceType.getTypes().interfaces().rawTypes();
    Object dummy = Proxy.newProxyInstance(
        interfaceClasses.iterator().next().getClassLoader(),
        interfaceClasses.toArray(new Class<?>[interfaceClasses.size()]),
        new AbstractInvocationHandler() {
          @Override protected Object handleInvocation(
              Object proxy, Method method, Object[] args) {
            Invokable<?, ?> invokable = interfaceType.method(method);
            ImmutableList<Parameter> params = invokable.getParameters();
            for (int i = 0; i < args.length; i++) {
              Parameter param = params.get(i);
              if (!param.isAnnotationPresent(Nullable.class)) {
                Preconditions.checkNotNull(args[i]);
              }
            }
            return dummyReturnValue(interfaceType.resolveType(method.getGenericReturnType()));
          }
          @Override public String toString() {
            return "Dummy proxy for " + interfaceType;
          }
        });
    @SuppressWarnings("unchecked") // interfaceType is T
    T result = (T) dummy;
    return result;
  }

  /** Returns the dummy return value for {@code returnType}. */
  abstract <R> R dummyReturnValue(TypeToken<R> returnType);
}
