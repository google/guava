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
import static com.google.common.testing.NullPointerTester.isNullable;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Generates a dummy interface proxy that simply returns a dummy value for each method.
 *
 * @author Ben Yu
 */
@GwtIncompatible
@J2ktIncompatible
@NullMarked
abstract class DummyProxy {

  /**
   * Returns a new proxy for {@code interfaceType}. Proxies of the same interface are equal to each
   * other if the {@link DummyProxy} instance that created the proxies are equal.
   */
  final <T> T newProxy(TypeToken<T> interfaceType) {
    Set<Class<?>> interfaceClasses = new LinkedHashSet<>();
    Set<Class<? super T>> allInterfaceClasses = interfaceType.getTypes().interfaces().rawTypes();
    for (Class<? super T> itf : allInterfaceClasses) {
      Iterator<Class<?>> iterator = interfaceClasses.iterator();
      boolean addToSet = true;
      while (iterator.hasNext()) {
        Class<?> current = iterator.next();
        if (current == itf || itf.isAssignableFrom(current)) {
          // Skip any super interface of the ones that are already included.
          addToSet = false;
          break;
        }
      }
      if (addToSet) {
        interfaceClasses.add(itf);
      }
    }
    // Make the proxy serializable to work with SerializableTester
    interfaceClasses.add(Serializable.class);
    Object dummy =
        Proxy.newProxyInstance(
            interfaceClasses.iterator().next().getClassLoader(),
            interfaceClasses.toArray(new Class<?>[interfaceClasses.size()]),
            new DummyHandler(interfaceType));
    @SuppressWarnings("unchecked") // interfaceType is T
    T result = (T) dummy;
    return result;
  }

  /** Returns the dummy return value for {@code returnType}. */
  abstract <R> @Nullable R dummyReturnValue(TypeToken<R> returnType);

  private class DummyHandler extends AbstractInvocationHandler implements Serializable {
    private final TypeToken<?> interfaceType;

    DummyHandler(TypeToken<?> interfaceType) {
      this.interfaceType = interfaceType;
    }

    @Override
    protected @Nullable Object handleInvocation(
        Object proxy, Method method, @Nullable Object[] args) {
      Invokable<?, ?> invokable = interfaceType.method(method);
      ImmutableList<Parameter> params = invokable.getParameters();
      for (int i = 0; i < args.length; i++) {
        Parameter param = params.get(i);
        if (!isNullable(param)) {
          checkNotNull(args[i]);
        }
      }
      return dummyReturnValue(interfaceType.resolveType(method.getGenericReturnType()));
    }

    @Override
    public int hashCode() {
      return identity().hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof DummyHandler) {
        DummyHandler that = (DummyHandler) obj;
        return identity().equals(that.identity());
      } else {
        return false;
      }
    }

    private DummyProxy identity() {
      return DummyProxy.this;
    }

    @Override
    public String toString() {
      return "Dummy proxy for " + interfaceType;
    }

    // Since type variables aren't serializable, reduce the type down to raw type before
    // serialization.
    private Object writeReplace() {
      return new DummyHandler(TypeToken.of(interfaceType.getRawType()));
    }
  }
}
