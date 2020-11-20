/*
 * Copyright (C) 2005 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Static utilities relating to Java reflection.
 *
 * @since 12.0
 */
@Beta
public final class Reflection {

  /**
   * Returns the package name of {@code clazz} according to the Java Language Specification (section
   * 6.7). Unlike {@link Class#getPackage}, this method only parses the class name, without
   * attempting to define the {@link Package} and hence load files.
   */
  public static String getPackageName(Class<?> clazz) {
    return getPackageName(clazz.getName());
  }

  /**
   * Returns the package name of {@code classFullName} according to the Java Language Specification
   * (section 6.7). Unlike {@link Class#getPackage}, this method only parses the class name, without
   * attempting to define the {@link Package} and hence load files.
   */
  public static String getPackageName(String classFullName) {
    int lastDot = classFullName.lastIndexOf('.');
    return (lastDot < 0) ? "" : classFullName.substring(0, lastDot);
  }

  /**
   * Ensures that the given classes are initialized, as described in <a
   * href="http://java.sun.com/docs/books/jls/third_edition/html/execution.html#12.4.2">JLS Section
   * 12.4.2</a>.
   *
   * <p>WARNING: Normally it's a smell if a class needs to be explicitly initialized, because static
   * state hurts system maintainability and testability. In cases when you have no choice while
   * inter-operating with a legacy framework, this method helps to keep the code less ugly.
   *
   * @throws ExceptionInInitializerError if an exception is thrown during initialization of a class
   */
  public static void initialize(Class<?>... classes) {
    for (Class<?> clazz : classes) {
      try {
        Class.forName(clazz.getName(), true, clazz.getClassLoader());
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Returns a proxy instance that implements {@code interfaceType} by dispatching method
   * invocations to {@code handler}. The class loader of {@code interfaceType} will be used to
   * define the proxy class. To implement multiple interfaces or specify a class loader, use {@link
   * Proxy#newProxyInstance}.
   *
   * @throws IllegalArgumentException if {@code interfaceType} does not specify the type of a Java
   *     interface
   */
  public static <T> T newProxy(Class<T> interfaceType, InvocationHandler handler) {
    checkNotNull(handler);
    checkArgument(interfaceType.isInterface(), "%s is not an interface", interfaceType);
    Object object =
        Proxy.newProxyInstance(
            interfaceType.getClassLoader(), new Class<?>[] {interfaceType}, handler);
    return interfaceType.cast(object);
  }

  private Reflection() {}
}
