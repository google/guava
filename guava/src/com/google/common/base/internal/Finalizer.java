/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.base.internal;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;

/**
 * Thread that finalizes referents. All references should implement {@code
 * com.google.common.base.FinalizableReference}.
 *
 * <p>While this class is public, we consider it to be *internal* and not part of our published API.
 * It is public so we can access it reflectively across class loaders in secure environments.
 *
 * <p>This class can't depend on other Guava code. If we were to load this class in the same class
 * loader as the rest of Guava, this thread would keep an indirect strong reference to the class
 * loader and prevent it from being garbage collected. This poses a problem for environments where
 * you want to throw away the class loader. For example, dynamically reloading a web application or
 * unloading an OSGi bundle.
 *
 * <p>{@code com.google.common.base.FinalizableReferenceQueue} loads this class in its own class
 * loader. That way, this class doesn't prevent the main class loader from getting garbage
 * collected, and this class can detect when the main class loader has been garbage collected and
 * stop itself.
 */
// no @ElementTypesAreNonNullByDefault for the reasons discussed above
public class Finalizer implements Runnable {

  private static final Logger logger = Logger.getLogger(Finalizer.class.getName());

  /** Name of FinalizableReference.class. */
  private static final String FINALIZABLE_REFERENCE = "com.google.common.base.FinalizableReference";

  /**
   * Starts the Finalizer thread. FinalizableReferenceQueue calls this method reflectively.
   *
   * @param finalizableReferenceClass FinalizableReference.class.
   * @param queue a reference queue that the thread will poll.
   * @param frqReference a phantom reference to the FinalizableReferenceQueue, which will be queued
   *     either when the FinalizableReferenceQueue is no longer referenced anywhere, or when its
   *     close() method is called.
   */
  public static void startFinalizer(
      Class<?> finalizableReferenceClass,
      ReferenceQueue<Object> queue,
      PhantomReference<Object> frqReference) {
    /*
     * We use FinalizableReference.class for two things:
     *
     * 1) To invoke FinalizableReference.finalizeReferent()
     *
     * 2) To detect when FinalizableReference's class loader has to be garbage collected, at which
     * point, Finalizer can stop running
     */
    if (!finalizableReferenceClass.getName().equals(FINALIZABLE_REFERENCE)) {
      throw new IllegalArgumentException("Expected " + FINALIZABLE_REFERENCE + ".");
    }

    Finalizer finalizer = new Finalizer(finalizableReferenceClass, queue, frqReference);
    String threadName = Finalizer.class.getName();
    Thread thread = null;
    if (bigThreadConstructor != null) {
      try {
        boolean inheritThreadLocals = false;
        long defaultStackSize = 0;
        thread =
            bigThreadConstructor.newInstance(
                (ThreadGroup) null, finalizer, threadName, defaultStackSize, inheritThreadLocals);
      } catch (Throwable t) {
        logger.log(
            Level.INFO, "Failed to create a thread without inherited thread-local values", t);
      }
    }
    if (thread == null) {
      thread = new Thread((ThreadGroup) null, finalizer, threadName);
    }
    thread.setDaemon(true);

    try {
      if (inheritableThreadLocals != null) {
        inheritableThreadLocals.set(thread, null);
      }
    } catch (Throwable t) {
      logger.log(
          Level.INFO,
          "Failed to clear thread local values inherited by reference finalizer thread.",
          t);
    }

    thread.start();
  }

  private final WeakReference<Class<?>> finalizableReferenceClassReference;
  private final PhantomReference<Object> frqReference;
  private final ReferenceQueue<Object> queue;

  // By preference, we will use the Thread constructor that has an `inheritThreadLocals` parameter.
  // But before Java 9, our only way not to inherit ThreadLocals is to zap them after the thread
  // is created, by accessing a private field.
  @CheckForNull
  private static final Constructor<Thread> bigThreadConstructor = getBigThreadConstructor();

  @CheckForNull
  private static final Field inheritableThreadLocals =
      (bigThreadConstructor == null) ? getInheritableThreadLocalsField() : null;

  /** Constructs a new finalizer thread. */
  private Finalizer(
      Class<?> finalizableReferenceClass,
      ReferenceQueue<Object> queue,
      PhantomReference<Object> frqReference) {
    this.queue = queue;

    this.finalizableReferenceClassReference = new WeakReference<>(finalizableReferenceClass);

    // Keep track of the FRQ that started us so we know when to stop.
    this.frqReference = frqReference;
  }

  /** Loops continuously, pulling references off the queue and cleaning them up. */
  @SuppressWarnings("InfiniteLoopStatement")
  @Override
  public void run() {
    while (true) {
      try {
        if (!cleanUp(queue.remove())) {
          break;
        }
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  /**
   * Cleans up the given reference and any other references already in the queue. Catches and logs
   * all throwables.
   *
   * @return true if the caller should continue to wait for more references to be added to the
   *     queue, false if the associated FinalizableReferenceQueue is no longer referenced.
   */
  private boolean cleanUp(Reference<?> firstReference) {
    Method finalizeReferentMethod = getFinalizeReferentMethod();
    if (finalizeReferentMethod == null) {
      return false;
    }

    if (!finalizeReference(firstReference, finalizeReferentMethod)) {
      return false;
    }

    /*
     * Loop as long as we have references available so as not to waste CPU looking up the Method
     * over and over again.
     */
    while (true) {
      Reference<?> furtherReference = queue.poll();
      if (furtherReference == null) {
        return true;
      }
      if (!finalizeReference(furtherReference, finalizeReferentMethod)) {
        return false;
      }
    }
  }

  /**
   * Cleans up the given reference. Catches and logs all throwables.
   *
   * @return true if the caller should continue to clean up references from the queue, false if the
   *     associated FinalizableReferenceQueue is no longer referenced.
   */
  private boolean finalizeReference(Reference<?> reference, Method finalizeReferentMethod) {
    /*
     * This is for the benefit of phantom references. Weak and soft references will have already
     * been cleared by this point.
     */
    reference.clear();

    if (reference == frqReference) {
      /*
       * The client no longer has a reference to the FinalizableReferenceQueue. We can stop.
       */
      return false;
    }

    try {
      finalizeReferentMethod.invoke(reference);
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "Error cleaning up after reference.", t);
    }
    return true;
  }

  /** Looks up FinalizableReference.finalizeReferent() method. */
  @CheckForNull
  private Method getFinalizeReferentMethod() {
    Class<?> finalizableReferenceClass = finalizableReferenceClassReference.get();
    if (finalizableReferenceClass == null) {
      /*
       * FinalizableReference's class loader was reclaimed. While there's a chance that other
       * finalizable references could be enqueued subsequently (at which point the class loader
       * would be resurrected by virtue of us having a strong reference to it), we should pretty
       * much just shut down and make sure we don't keep it alive any longer than necessary.
       */
      return null;
    }
    try {
      return finalizableReferenceClass.getMethod("finalizeReferent");
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  @CheckForNull
  private static Field getInheritableThreadLocalsField() {
    try {
      Field inheritableThreadLocals = Thread.class.getDeclaredField("inheritableThreadLocals");
      inheritableThreadLocals.setAccessible(true);
      return inheritableThreadLocals;
    } catch (Throwable t) {
      logger.log(
          Level.INFO,
          "Couldn't access Thread.inheritableThreadLocals. Reference finalizer threads will "
              + "inherit thread local values.");
      return null;
    }
  }

  @CheckForNull
  private static Constructor<Thread> getBigThreadConstructor() {
    try {
      return Thread.class.getConstructor(
          ThreadGroup.class, Runnable.class, String.class, long.class, boolean.class);
    } catch (Throwable t) {
      // Probably pre Java 9. We'll fall back to Thread.inheritableThreadLocals.
      return null;
    }
  }
}
