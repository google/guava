/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.eventbus;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * A {@link HandlerFindingStrategy} for collecting all event handler methods that are marked with
 * the {@link Subscribe} annotation.
 *
 * @author Cliff Biffle
 * @author Louis Wasserman
 */
class AnnotatedHandlerFinder implements HandlerFindingStrategy {
  /**
   * {@inheritDoc}
   *
   * This implementation finds all methods marked with a {@link Subscribe} annotation.
   */
  @Override
  public Multimap<Class<?>, EventHandler> findAllHandlers(Object listener) {
    Multimap<Class<?>, EventHandler> methodsInListener = HashMultimap.create();
    Class<?> clazz = listener.getClass();
    Set<? extends Class<?>> supers = TypeToken.of(clazz).getTypes().rawTypes();

    for (Method method : clazz.getMethods()) {
      /*
       * Iterate over each distinct method of {@code clazz}, checking if it is annotated with
       * @Subscribe by any of the superclasses or superinterfaces that declare it.
       */
      for (Class<?> c : supers) {
        try {
          Method m = c.getMethod(method.getName(), method.getParameterTypes());
          if (m.isAnnotationPresent(Subscribe.class)) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
              throw new IllegalArgumentException("Method " + method
                  + " has @Subscribe annotation, but requires " + parameterTypes.length
                  + " arguments.  Event handler methods must require a single argument.");
            }
            Class<?> eventType = parameterTypes[0];
            EventHandler handler = makeHandler(listener, method);

            methodsInListener.put(eventType, handler);
            break;
          }
        } catch (NoSuchMethodException ignored) {
          // Move on.
        }
      }
    }
    return methodsInListener;
  }

  /**
   * Creates an {@code EventHandler} for subsequently calling {@code method} on
   * {@code listener}.
   * Selects an EventHandler implementation based on the annotations on
   * {@code method}.
   *
   * @param listener  object bearing the event handler method.
   * @param method  the event handler method to wrap in an EventHandler.
   * @return an EventHandler that will call {@code method} on {@code listener}
   *         when invoked.
   */
  private static EventHandler makeHandler(Object listener, Method method) {
    EventHandler wrapper;
    if (methodIsDeclaredThreadSafe(method)) {
      wrapper = new EventHandler(listener, method);
    } else {
      wrapper = new SynchronizedEventHandler(listener, method);
    }
    return wrapper;
  }

  /**
   * Checks whether {@code method} is thread-safe, as indicated by the
   * {@link AllowConcurrentEvents} annotation.
   *
   * @param method  handler method to check.
   * @return {@code true} if {@code handler} is marked as thread-safe,
   *         {@code false} otherwise.
   */
  private static boolean methodIsDeclaredThreadSafe(Method method) {
    return method.getAnnotation(AllowConcurrentEvents.class) != null;
  }
}
