/*
 * Copyright (C) 2010 The Guava Authors
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
import static org.junit.Assert.assertThrows;

import com.google.common.testing.NullPointerTester;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import junit.framework.TestCase;

/**
 * Tests for ThreadFactoryBuilder.
 *
 * @author Kurt Alfred Kluever
 * @author Martin Buchholz
 */
public class ThreadFactoryBuilderTest extends TestCase {
  private final Runnable monitoredRunnable =
      new Runnable() {
        @Override
        public void run() {
          completed = true;
        }
      };

  private static final UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER =
      new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
          // No-op
        }
      };

  private ThreadFactoryBuilder builder;
  private volatile boolean completed = false;

  @Override
  public void setUp() {
    builder = new ThreadFactoryBuilder();
  }

  public void testThreadFactoryBuilder_defaults() throws InterruptedException {
    ThreadFactory threadFactory = builder.build();
    Thread thread = threadFactory.newThread(monitoredRunnable);
    checkThreadPoolName(thread, 1);

    Thread defaultThread = Executors.defaultThreadFactory().newThread(monitoredRunnable);
    assertEquals(defaultThread.isDaemon(), thread.isDaemon());
    assertEquals(defaultThread.getPriority(), thread.getPriority());
    assertSame(defaultThread.getThreadGroup(), thread.getThreadGroup());
    assertSame(defaultThread.getUncaughtExceptionHandler(), thread.getUncaughtExceptionHandler());

    assertFalse(completed);
    thread.start();
    thread.join();
    assertTrue(completed);

    // Creating a new thread from the same ThreadFactory will have the same
    // pool ID but a thread ID of 2.
    Thread thread2 = threadFactory.newThread(monitoredRunnable);
    checkThreadPoolName(thread2, 2);
    assertEquals(
        thread.getName().substring(0, thread.getName().lastIndexOf('-')),
        thread2.getName().substring(0, thread.getName().lastIndexOf('-')));

    // Building again should give us a different pool ID.
    ThreadFactory threadFactory2 = builder.build();
    Thread thread3 = threadFactory2.newThread(monitoredRunnable);
    checkThreadPoolName(thread3, 1);
    assertThat(thread2.getName().substring(0, thread.getName().lastIndexOf('-')))
        .isNotEqualTo(thread3.getName().substring(0, thread.getName().lastIndexOf('-')));
  }

  private static void checkThreadPoolName(Thread thread, int threadId) {
    assertThat(thread.getName()).matches("^pool-\\d+-thread-" + threadId + "$");
  }

  public void testNameFormatWithPercentS_custom() {
    String format = "super-duper-thread-%s";
    ThreadFactory factory = builder.setNameFormat(format).build();
    for (int i = 0; i < 11; i++) {
      assertEquals(rootLocaleFormat(format, i), factory.newThread(monitoredRunnable).getName());
    }
  }

  public void testNameFormatWithPercentD_custom() {
    String format = "super-duper-thread-%d";
    ThreadFactory factory = builder.setNameFormat(format).build();
    for (int i = 0; i < 11; i++) {
      assertEquals(rootLocaleFormat(format, i), factory.newThread(monitoredRunnable).getName());
    }
  }

  public void testDaemon_false() {
    ThreadFactory factory = builder.setDaemon(false).build();
    Thread thread = factory.newThread(monitoredRunnable);
    assertFalse(thread.isDaemon());
  }

  public void testDaemon_true() {
    ThreadFactory factory = builder.setDaemon(true).build();
    Thread thread = factory.newThread(monitoredRunnable);
    assertTrue(thread.isDaemon());
  }

  public void testPriority_custom() {
    for (int i = Thread.MIN_PRIORITY; i <= Thread.MAX_PRIORITY; i++) {
      ThreadFactory factory = builder.setPriority(i).build();
      Thread thread = factory.newThread(monitoredRunnable);
      assertEquals(i, thread.getPriority());
    }
  }

  public void testPriority_tooLow() {
    assertThrows(
        IllegalArgumentException.class, () -> builder.setPriority(Thread.MIN_PRIORITY - 1));
  }

  public void testPriority_tooHigh() {
    assertThrows(
        IllegalArgumentException.class, () -> builder.setPriority(Thread.MAX_PRIORITY + 1));
  }

  public void testUncaughtExceptionHandler_custom() {
    assertEquals(
        UNCAUGHT_EXCEPTION_HANDLER,
        builder
            .setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER)
            .build()
            .newThread(monitoredRunnable)
            .getUncaughtExceptionHandler());
  }

  public void testBuildMutateBuild() {
    ThreadFactory factory1 = builder.setPriority(1).build();
    assertEquals(1, factory1.newThread(monitoredRunnable).getPriority());

    ThreadFactory factory2 = builder.setPriority(2).build();
    assertEquals(1, factory1.newThread(monitoredRunnable).getPriority());
    assertEquals(2, factory2.newThread(monitoredRunnable).getPriority());
  }

  public void testBuildTwice() {
    ThreadFactory unused;
    unused = builder.build(); // this is allowed
    unused = builder.build(); // this is *also* allowed
  }

  public void testBuildMutate() {
    ThreadFactory factory1 = builder.setPriority(1).build();
    assertEquals(1, factory1.newThread(monitoredRunnable).getPriority());

    builder.setPriority(2); // change the state of the builder
    assertEquals(1, factory1.newThread(monitoredRunnable).getPriority());
  }

  public void testThreadFactory() throws InterruptedException {
    final String THREAD_NAME = "ludicrous speed";
    final int THREAD_PRIORITY = 1;
    final boolean THREAD_DAEMON = false;
    ThreadFactory backingThreadFactory =
        new ThreadFactory() {
          @Override
          public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(THREAD_NAME);
            thread.setPriority(THREAD_PRIORITY);
            thread.setDaemon(THREAD_DAEMON);
            thread.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
            return thread;
          }
        };

    Thread thread =
        builder.setThreadFactory(backingThreadFactory).build().newThread(monitoredRunnable);

    assertEquals(THREAD_NAME, thread.getName());
    assertEquals(THREAD_PRIORITY, thread.getPriority());
    assertEquals(THREAD_DAEMON, thread.isDaemon());
    assertSame(UNCAUGHT_EXCEPTION_HANDLER, thread.getUncaughtExceptionHandler());
    assertSame(Thread.State.NEW, thread.getState());

    assertFalse(completed);
    thread.start();
    thread.join();
    assertTrue(completed);
  }

  public void testNulls() {
    NullPointerTester npTester = new NullPointerTester();
    npTester.testAllPublicConstructors(ThreadFactoryBuilder.class);
    npTester.testAllPublicStaticMethods(ThreadFactoryBuilder.class);
    npTester.testAllPublicInstanceMethods(builder);
  }

  private static String rootLocaleFormat(String format, Object... args) {
    return String.format(Locale.ROOT, format, args);
  }
}
