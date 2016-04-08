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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread that finalizes referents. All references should implement
 * {@code com.google.common.base.FinalizableReference}.
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
    Thread thread = new Thread(finalizer);
    thread.setName(Finalizer.class.getName());
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

  private static final Field inheritableThreadLocals = getInheritableThreadLocalsField();

  /** Constructs a new finalizer thread. */
  private Finalizer(
      Class<?> finalizableReferenceClass,
      ReferenceQueue<Object> queue,
      PhantomReference<Object> frqReference) {
    this.queue = queue;

    this.finalizableReferenceClassReference =
        new WeakReference<Class<?>>(finalizableReferenceClass);

    // Keep track of the FRQ that started us so we know when to stop.
    this.frqReference = frqReference;
  }

  /**
   * Loops continuously, pulling references off the queue and cleaning them up.
   */
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
   * Cleans up a single reference. Catches and logs all throwables.
   *
   * @return true if the caller should continue, false if the associated FinalizableReferenceQueue
   *     is no longer referenced.
   */
  private boolean cleanUp(Reference<?> reference) {
    Method finalizeReferentMethod = getFinalizeReferentMethod();
    if (finalizeReferentMethod == null) {
      return false;
    }
    do {
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

      /*
       * Loop as long as we have references available so as not to waste CPU looking up the Method
       * over and over again.
       */
    } while ((reference = queue.poll()) != null);
    return true;
  }

  /**
   * Looks up FinalizableReference.finalizeReferent() method.
   */
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

  public static Field getInheritableThreadLocalsField() {
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
}
