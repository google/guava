/*
 * Copyright (C) 2015 The Guava Authors
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

package com.google.common.util.concurrent;

import static com.google.common.truth.Truth.assertThat;

import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import junit.framework.TestCase;

/** Tests for {@link AbstractFuture} with the cancellation cause system property set */

public class AbstractFutureCancellationCauseTest extends TestCase {

  private ClassLoader oldClassLoader;
  private URLClassLoader classReloader;

  @Override
  protected void setUp() throws Exception {
    // Load the "normal" copy of SettableFuture and related classes.
    SettableFuture<?> unused = SettableFuture.create();
    // Hack to load AbstractFuture et. al. in a new classloader so that it re-reads the cancellation
    // cause system property.  This allows us to run with both settings of the property in one jvm
    // without resorting to even crazier hacks to reset static final boolean fields.
    System.setProperty("guava.concurrent.generate_cancellation_cause", "true");
    final String concurrentPackage = SettableFuture.class.getPackage().getName();
    classReloader =
        new URLClassLoader(ClassPathUtil.getClassPathUrls()) {
          @GuardedBy("loadedClasses")
          final Map<String, Class<?>> loadedClasses = new HashMap<>();

          @Override
          public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith(concurrentPackage)
                // Use other classloader for ListenableFuture, so that the objects can interact
                && !ListenableFuture.class.getName().equals(name)) {
              synchronized (loadedClasses) {
                Class<?> toReturn = loadedClasses.get(name);
                if (toReturn == null) {
                  toReturn = super.findClass(name);
                  loadedClasses.put(name, toReturn);
                }
                return toReturn;
              }
            }
            return super.loadClass(name);
          }
        };
    oldClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classReloader);
  }

  @Override
  protected void tearDown() throws Exception {
    classReloader.close();
    Thread.currentThread().setContextClassLoader(oldClassLoader);
    System.clearProperty("guava.concurrent.generate_cancellation_cause");
  }

  public void testCancel_notDoneNoInterrupt() throws Exception {
    Future<?> future = newFutureInstance();
    assertTrue(future.cancel(false));
    assertTrue(future.isCancelled());
    assertTrue(future.isDone());
    try {
      future.get();
      fail("Expected CancellationException");
    } catch (CancellationException e) {
      assertNotNull(e.getCause());
    }
  }

  public void testCancel_notDoneInterrupt() throws Exception {
    Future<?> future = newFutureInstance();
    assertTrue(future.cancel(true));
    assertTrue(future.isCancelled());
    assertTrue(future.isDone());
    try {
      future.get();
      fail("Expected CancellationException");
    } catch (CancellationException e) {
      assertNotNull(e.getCause());
    }
  }

  public void testSetFuture_misbehavingFutureDoesNotThrow() throws Exception {
    ListenableFuture<String> badFuture =
        new ListenableFuture<String>() {
          @Override
          public boolean cancel(boolean interrupt) {
            return false;
          }

          @Override
          public boolean isDone() {
            return true;
          }

          @Override
          public boolean isCancelled() {
            return true; // BAD!!
          }

          @Override
          public String get() {
            return "foo"; // BAD!!
          }

          @Override
          public String get(long time, TimeUnit unit) {
            return "foo"; // BAD!!
          }

          @Override
          public void addListener(Runnable runnable, Executor executor) {
            executor.execute(runnable);
          }
        };
    Future<?> future = newFutureInstance();
    future
        .getClass()
        .getMethod(
            "setFuture",
            future.getClass().getClassLoader().loadClass(ListenableFuture.class.getName()))
        .invoke(future, badFuture);
    try {
      future.get();
      fail();
    } catch (CancellationException expected) {
      assertThat(expected).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
      assertThat(expected).hasCauseThat().hasMessageThat().contains(badFuture.toString());
    }
  }

  private Future<?> newFutureInstance() throws Exception {
    return (Future<?>)
        classReloader.loadClass(SettableFuture.class.getName()).getMethod("create").invoke(null);
  }
}
