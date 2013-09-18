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

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A {@link SubscriberFindingStrategy} for collecting all event subscriber methods that are marked
 * with the {@link Subscribe} annotation.
 *
 * @author Cliff Biffle
 * @author Louis Wasserman
 */
class AnnotatedSubscriberFinder implements SubscriberFindingStrategy {
  /**
   * A thread-safe cache that contains the mapping from each class to all methods in that class and
   * all super-classes, that are annotated with {@code @Subscribe}. The cache is shared across all
   * instances of this class; this greatly improves performance if multiple EventBus instances are
   * created and objects of the same class are registered on all of them.
   */
  private static final LoadingCache<Class<?>, ImmutableList<Method>> subscriberMethodsCache =
      CacheBuilder.newBuilder()
          .weakKeys()
          .build(new CacheLoader<Class<?>, ImmutableList<Method>>() {
            @Override
            public ImmutableList<Method> load(Class<?> concreteClass) throws Exception {
              return getAnnotatedMethodsInternal(concreteClass);
            }
          });

  /**
   * {@inheritDoc}
   *
   * This implementation finds all methods marked with a {@link Subscribe} annotation.
   */
  @Override
  public Multimap<Class<?>, EventSubscriber> findAllSubscribers(Object listener) {
    Multimap<Class<?>, EventSubscriber> methodsInListener = HashMultimap.create();
    Class<?> clazz = listener.getClass();
    for (Method method : getAnnotatedMethods(clazz)) {
      Class<?>[] parameterTypes = method.getParameterTypes();
      Class<?> eventType = parameterTypes[0];
      EventSubscriber subscriber = makeSubscriber(listener, method);
      methodsInListener.put(eventType, subscriber);
    }
    return methodsInListener;
  }

  private static ImmutableList<Method> getAnnotatedMethods(Class<?> clazz) {
    try {
      return subscriberMethodsCache.getUnchecked(clazz);
    } catch (UncheckedExecutionException e) {
      throw Throwables.propagate(e.getCause());
    }
  }
  
  private static final class MethodIdentifier {
    private final String name;
    private final List<Class<?>> parameterTypes;
    
    MethodIdentifier(Method method) {
      this.name = method.getName();
      this.parameterTypes = Arrays.asList(method.getParameterTypes());
    }
    
    @Override
    public int hashCode() {
      return Objects.hashCode(name, parameterTypes);
    }
    
    @Override
    public boolean equals(@Nullable Object o) {
      if (o instanceof MethodIdentifier) {
        MethodIdentifier ident = (MethodIdentifier) o;
        return name.equals(ident.name) && parameterTypes.equals(ident.parameterTypes);
      }
      return false;
    }
  }

  private static ImmutableList<Method> getAnnotatedMethodsInternal(Class<?> clazz) {
    Set<? extends Class<?>> supers = TypeToken.of(clazz).getTypes().rawTypes();
    Map<MethodIdentifier, Method> identifiers = Maps.newHashMap();
    for (Class<?> superClazz : supers) {
      for (Method superClazzMethod : superClazz.getMethods()) {
        if (superClazzMethod.isAnnotationPresent(Subscribe.class)) {
          Class<?>[] parameterTypes = superClazzMethod.getParameterTypes();
          if (parameterTypes.length != 1) {
            throw new IllegalArgumentException("Method " + superClazzMethod
                + " has @Subscribe annotation, but requires " + parameterTypes.length
                + " arguments.  Event subscriber methods must require a single argument.");
          }
          
          MethodIdentifier ident = new MethodIdentifier(superClazzMethod);
          if (!identifiers.containsKey(ident)) {
            identifiers.put(ident, superClazzMethod);
          }
        }
      }
    }
    return ImmutableList.copyOf(identifiers.values());
  }

  /**
   * Creates an {@code EventSubscriber} for subsequently calling {@code method} on
   * {@code listener}.
   * Selects an EventSubscriber implementation based on the annotations on
   * {@code method}.
   *
   * @param listener  object bearing the event subscriber method.
   * @param method  the event subscriber method to wrap in an EventSubscriber.
   * @return an EventSubscriber that will call {@code method} on {@code listener}
   *         when invoked.
   */
  private static EventSubscriber makeSubscriber(Object listener, Method method) {
    EventSubscriber wrapper;
    if (methodIsDeclaredThreadSafe(method)) {
      wrapper = new EventSubscriber(listener, method);
    } else {
      wrapper = new SynchronizedEventSubscriber(listener, method);
    }
    return wrapper;
  }

  /**
   * Checks whether {@code method} is thread-safe, as indicated by the
   * {@link AllowConcurrentEvents} annotation.
   *
   * @param method  subscriber method to check.
   * @return {@code true} if {@code subscriber} is marked as thread-safe,
   *         {@code false} otherwise.
   */
  private static boolean methodIsDeclaredThreadSafe(Method method) {
    return method.getAnnotation(AllowConcurrentEvents.class) != null;
  }
}
