/*
 * Copyright (C) 2020 The Guava Authors
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


import java.net.URLClassLoader;
import java.security.Permission;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import junit.framework.TestCase;

/** Tests for {@link AbstractFuture} using an innocuous thread. */

public class AbstractFutureInnocuousThreadTest extends TestCase {
  private ClassLoader oldClassLoader;
  private URLClassLoader classReloader;
  private Class<?> settableFutureClass;
  private SecurityManager oldSecurityManager;

  @Override
  protected void setUp() throws Exception {
    // Load the "normal" copy of SettableFuture and related classes.
    SettableFuture<?> unused = SettableFuture.create();
    // Hack to load AbstractFuture et. al. in a new classloader so that it tries to re-read the
    // cancellation-cause system property. This allows us to test what happens if reading the
    // property is forbidden and then continue running tests normally in one jvm without resorting
    // to even crazier hacks to reset static final boolean fields.
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

    oldSecurityManager = System.getSecurityManager();
    /*
     * TODO(cpovirk): Why couldn't I get this to work with PermissionCollection and implies(), as
     * used by ClassPathTest?
     */
    final PropertyPermission readSystemProperty =
        new PropertyPermission("guava.concurrent.generate_cancellation_cause", "read");
    SecurityManager disallowPropertySecurityManager =
        new SecurityManager() {
          @Override
          public void checkPermission(Permission p) {
            if (readSystemProperty.equals(p)) {
              throw new SecurityException("Disallowed: " + p);
            }
          }
        };
    System.setSecurityManager(disallowPropertySecurityManager);

    settableFutureClass = classReloader.loadClass(SettableFuture.class.getName());

    /*
     * We must keep the SecurityManager installed during the test body: It affects what kind of
     * threads ForkJoinPool.commonPool() creates.
     */
  }

  @Override
  protected void tearDown() throws Exception {
    System.setSecurityManager(oldSecurityManager);
    classReloader.close();
    Thread.currentThread().setContextClassLoader(oldClassLoader);
  }

  public void testAbstractFutureInitializationWithInnocuousThread_doesNotThrow() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    // Setting a security manager causes the common ForkJoinPool to use InnocuousThreads with no
    // permissions.
    // submit()/join() causes this thread to execute the task instead, so we use a CountDownLatch as
    // a barrier to synchronize.
    // TODO(cpovirk): If some other test already initialized commonPool(), this won't work :(
    // Maybe we should just run this test in its own VM.
    ForkJoinPool.commonPool()
        .execute(
            () -> {
              try {
                settableFutureClass.getMethod("create").invoke(null);
                latch.countDown();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
    // In the failure case, await() will timeout.
    assertTrue(latch.await(2, TimeUnit.SECONDS));
  }

  // TODO(cpovirk): Write a similar test that doesn't use ForkJoinPool (to run under Android)?
}
