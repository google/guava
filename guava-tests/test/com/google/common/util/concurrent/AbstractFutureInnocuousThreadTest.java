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

import junit.framework.TestCase;

import javax.annotation.concurrent.GuardedBy;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static com.google.common.truth.Truth.assertThat;

/** Tests for {@link AbstractFuture} using an innocuous thread */

public class AbstractFutureInnocuousThreadTest extends TestCase {

  private ClassLoader oldClassLoader;
  private URLClassLoader classReloader;
  private Class<?> settableFutureClass;
  private Class<?> abstractFutureClass;

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
    abstractFutureClass = classReloader.loadClass(AbstractFuture.class.getName());
    settableFutureClass = classReloader.loadClass(SettableFuture.class.getName());
  }

  @Override
  protected void tearDown() throws Exception {
    classReloader.close();
    Thread.currentThread().setContextClassLoader(oldClassLoader);
    System.clearProperty("guava.concurrent.generate_cancellation_cause");
  }

  public void testAbstractFutureInitializationWithInnocuousThread_doesNotThrow() throws Exception {
    // Set up a SecurityManager with permissive grants.
    File policyFile = File.createTempFile("security_manager", "policy");
    BufferedWriter writer = new BufferedWriter(new FileWriter(policyFile));
    writer.write("grant {\n" +
            "  permission java.io.FilePermission \"/-\", \"read\";\n" +
            "  permission java.util.PropertyPermission \"*\", \"read,write\";\n" +
            "  permission java.lang.RuntimePermission \"*\";\n" +
            "};");
    writer.close();
    System.setProperty("java.security.policy", policyFile.getCanonicalPath());
    System.setSecurityManager(new SecurityManager());
    CountDownLatch latch = new CountDownLatch(1);
    // Setting a security manager causes the common ForkJoinPool to use InnocuousThreads with no permissions.
    // submit()/join() causes this thread to execute the task instead, so we use a CountDownLatch as a barrier to
    // synchronize.
    ForkJoinPool.commonPool().execute(
      () -> {
        try {
          settableFutureClass.getMethod("create").invoke(null);
          latch.countDown();
        } catch (Exception e) {
          fail("Unexpected exception: " + e.toString());
        }
      });
    // In the failure case, await() will timeout.
    assertTrue(latch.await(2, TimeUnit.SECONDS));
    // Reset the SecurityManager so that we can shutdown without issues.
    System.setSecurityManager(null);
  }
}
