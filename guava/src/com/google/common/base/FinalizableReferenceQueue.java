/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.base;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;

/**
 * A reference queue with an associated background thread that dequeues references and invokes
 * {@link FinalizableReference#finalizeReferent()} on them.
 *
 * <p>Keep a strong reference to this object until all of the associated referents have been
 * finalized. If this object is garbage collected earlier, the backing thread will not invoke {@code
 * finalizeReferent()} on the remaining references.
 *
 * <p>As an example of how this is used, imagine you have a class {@code MyServer} that creates a
 * {@link java.net.ServerSocket ServerSocket}, and you would like to ensure that the {@code
 * ServerSocket} is closed even if the {@code MyServer} object is garbage-collected without calling
 * its {@code close} method. You <em>could</em> use a finalizer to accomplish this, but that has a
 * number of well-known problems. Here is how you might use this class instead:
 *
 * <pre>{@code
 * public class MyServer implements Closeable {
 *   private static final FinalizableReferenceQueue frq = new FinalizableReferenceQueue();
 *   // You might also share this between several objects.
 *
 *   private static final Set<Reference<?>> references = Sets.newConcurrentHashSet();
 *   // This ensures that the FinalizablePhantomReference itself is not garbage-collected.
 *
 *   private final ServerSocket serverSocket;
 *
 *   private MyServer(...) {
 *     ...
 *     this.serverSocket = new ServerSocket(...);
 *     ...
 *   }
 *
 *   public static MyServer create(...) {
 *     MyServer myServer = new MyServer(...);
 *     final ServerSocket serverSocket = myServer.serverSocket;
 *     Reference<?> reference = new FinalizablePhantomReference<MyServer>(myServer, frq) {
 *       public void finalizeReferent() {
 *         references.remove(this):
 *         if (!serverSocket.isClosed()) {
 *           ...log a message about how nobody called close()...
 *           try {
 *             serverSocket.close();
 *           } catch (IOException e) {
 *             ...
 *           }
 *         }
 *       }
 *     };
 *     references.add(reference);
 *     return myServer;
 *   }
 *
 *   public void close() {
 *     serverSocket.close();
 *   }
 * }
 * }</pre>
 *
 * @author Bob Lee
 * @since 2.0
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public class FinalizableReferenceQueue implements Closeable {
  /*
   * The Finalizer thread keeps a phantom reference to this object. When the client (for example, a
   * map built by MapMaker) no longer has a strong reference to this object, the garbage collector
   * will reclaim it and enqueue the phantom reference. The enqueued reference will trigger the
   * Finalizer to stop.
   *
   * If this library is loaded in the system class loader, FinalizableReferenceQueue can load
   * Finalizer directly with no problems.
   *
   * If this library is loaded in an application class loader, it's important that Finalizer not
   * have a strong reference back to the class loader. Otherwise, you could have a graph like this:
   *
   * Finalizer Thread runs instance of -> Finalizer.class loaded by -> Application class loader
   * which loaded -> ReferenceMap.class which has a static -> FinalizableReferenceQueue instance
   *
   * Even if no other references to classes from the application class loader remain, the Finalizer
   * thread keeps an indirect strong reference to the queue in ReferenceMap, which keeps the
   * Finalizer running, and as a result, the application class loader can never be reclaimed.
   *
   * This means that dynamically loaded web applications and OSGi bundles can't be unloaded.
   *
   * If the library is loaded in an application class loader, we try to break the cycle by loading
   * Finalizer in its own independent class loader:
   *
   * System class loader -> Application class loader -> ReferenceMap -> FinalizableReferenceQueue ->
   * etc. -> Decoupled class loader -> Finalizer
   *
   * Now, Finalizer no longer keeps an indirect strong reference to the static
   * FinalizableReferenceQueue field in ReferenceMap. The application class loader can be reclaimed
   * at which point the Finalizer thread will stop and its decoupled class loader can also be
   * reclaimed.
   *
   * If any of this fails along the way, we fall back to loading Finalizer directly in the
   * application class loader.
   *
   * NOTE: The tests for this behavior (FinalizableReferenceQueueClassLoaderUnloadingTest) fail
   * strangely when run in JDK 9. We are considering this a known issue. Please see
   * https://github.com/google/guava/issues/3086 for more information.
   */

  private static final Logger logger = Logger.getLogger(FinalizableReferenceQueue.class.getName());

  private static final String FINALIZER_CLASS_NAME = "com.google.common.base.internal.Finalizer";

  /** Reference to Finalizer.startFinalizer(). */
  private static final Method startFinalizer;

  static {
    Class<?> finalizer =
        loadFinalizer(new SystemLoader(), new DecoupledLoader(), new DirectLoader());
    startFinalizer = getStartFinalizer(finalizer);
  }

  /** The actual reference queue that our background thread will poll. */
  final ReferenceQueue<Object> queue;

  final PhantomReference<Object> frqRef;

  /** Whether or not the background thread started successfully. */
  final boolean threadStarted;

  /** Constructs a new queue. */
  public FinalizableReferenceQueue() {
    // We could start the finalizer lazily, but I'd rather it blow up early.
    queue = new ReferenceQueue<>();
    frqRef = new PhantomReference<>(this, queue);
    boolean threadStarted = false;
    try {
      startFinalizer.invoke(null, FinalizableReference.class, queue, frqRef);
      threadStarted = true;
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible); // startFinalizer() is public
    } catch (Throwable t) {
      logger.log(
          Level.INFO,
          "Failed to start reference finalizer thread."
              + " Reference cleanup will only occur when new references are created.",
          t);
    }

    this.threadStarted = threadStarted;
  }

  @Override
  public void close() {
    frqRef.enqueue();
    cleanUp();
  }

  /**
   * Repeatedly dequeues references from the queue and invokes {@link
   * FinalizableReference#finalizeReferent()} on them until the queue is empty. This method is a
   * no-op if the background thread was created successfully.
   */
  void cleanUp() {
    if (threadStarted) {
      return;
    }

    Reference<?> reference;
    while ((reference = queue.poll()) != null) {
      /*
       * This is for the benefit of phantom references. Weak and soft references will have already
       * been cleared by this point.
       */
      reference.clear();
      try {
        ((FinalizableReference) reference).finalizeReferent();
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Error cleaning up after reference.", t);
      }
    }
  }

  /**
   * Iterates through the given loaders until it finds one that can load Finalizer.
   *
   * @return Finalizer.class
   */
  private static Class<?> loadFinalizer(FinalizerLoader... loaders) {
    for (FinalizerLoader loader : loaders) {
      Class<?> finalizer = loader.loadFinalizer();
      if (finalizer != null) {
        return finalizer;
      }
    }

    throw new AssertionError();
  }

  /** Loads Finalizer.class. */
  interface FinalizerLoader {

    /**
     * Returns Finalizer.class or null if this loader shouldn't or can't load it.
     *
     * @throws SecurityException if we don't have the appropriate privileges
     */
    @CheckForNull
    Class<?> loadFinalizer();
  }

  /**
   * Tries to load Finalizer from the system class loader. If Finalizer is in the system class path,
   * we needn't create a separate loader.
   */
  static class SystemLoader implements FinalizerLoader {
    // This is used by the ClassLoader-leak test in FinalizableReferenceQueueTest to disable
    // finding Finalizer on the system class path even if it is there.
    @VisibleForTesting static boolean disabled;

    @Override
    @CheckForNull
    public Class<?> loadFinalizer() {
      if (disabled) {
        return null;
      }
      ClassLoader systemLoader;
      try {
        systemLoader = ClassLoader.getSystemClassLoader();
      } catch (SecurityException e) {
        logger.info("Not allowed to access system class loader.");
        return null;
      }
      if (systemLoader != null) {
        try {
          return systemLoader.loadClass(FINALIZER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
          // Ignore. Finalizer is simply in a child class loader.
          return null;
        }
      } else {
        return null;
      }
    }
  }

  /**
   * Try to load Finalizer in its own class loader. If Finalizer's thread had a direct reference to
   * our class loader (which could be that of a dynamically loaded web application or OSGi bundle),
   * it would prevent our class loader from getting garbage collected.
   */
  static class DecoupledLoader implements FinalizerLoader {
    private static final String LOADING_ERROR =
        "Could not load Finalizer in its own class loader. Loading Finalizer in the current class "
            + "loader instead. As a result, you will not be able to garbage collect this class "
            + "loader. To support reclaiming this class loader, either resolve the underlying "
            + "issue, or move Guava to your system class path.";

    @Override
    @CheckForNull
    public Class<?> loadFinalizer() {
      /*
       * We use URLClassLoader because it's the only concrete class loader implementation in the
       * JDK. If we used our own ClassLoader subclass, Finalizer would indirectly reference this
       * class loader:
       *
       * Finalizer.class -> CustomClassLoader -> CustomClassLoader.class -> This class loader
       *
       * System class loader will (and must) be the parent.
       */
      try (URLClassLoader finalizerLoader = newLoader(getBaseUrl())) {
        return finalizerLoader.loadClass(FINALIZER_CLASS_NAME);
      } catch (Exception e) {
        logger.log(Level.WARNING, LOADING_ERROR, e);
        return null;
      }
    }

    /** Gets URL for base of path containing Finalizer.class. */
    URL getBaseUrl() throws IOException {
      // Find URL pointing to Finalizer.class file.
      String finalizerPath = FINALIZER_CLASS_NAME.replace('.', '/') + ".class";
      URL finalizerUrl = getClass().getClassLoader().getResource(finalizerPath);
      if (finalizerUrl == null) {
        throw new FileNotFoundException(finalizerPath);
      }

      // Find URL pointing to base of class path.
      String urlString = finalizerUrl.toString();
      if (!urlString.endsWith(finalizerPath)) {
        throw new IOException("Unsupported path style: " + urlString);
      }
      urlString = urlString.substring(0, urlString.length() - finalizerPath.length());
      return new URL(finalizerUrl, urlString);
    }

    /** Creates a class loader with the given base URL as its classpath. */
    URLClassLoader newLoader(URL base) {
      // We use the bootstrap class loader as the parent because Finalizer by design uses
      // only standard Java classes. That also means that FinalizableReferenceQueueTest
      // doesn't pick up the wrong version of the Finalizer class.
      return new URLClassLoader(new URL[] {base}, null);
    }
  }

  /**
   * Loads Finalizer directly using the current class loader. We won't be able to garbage collect
   * this class loader, but at least the world doesn't end.
   */
  static class DirectLoader implements FinalizerLoader {
    @Override
    public Class<?> loadFinalizer() {
      try {
        return Class.forName(FINALIZER_CLASS_NAME);
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      }
    }
  }

  /** Looks up Finalizer.startFinalizer(). */
  static Method getStartFinalizer(Class<?> finalizer) {
    try {
      return finalizer.getMethod(
          "startFinalizer", Class.class, ReferenceQueue.class, PhantomReference.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }
}
