// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * A class exactly like {@link MapMaker}, except restricted in the types of maps
 * it can build. This type is returned by {@link MapMaker#evictionListener} to
 * prevent the user from trying to build a map that's incompatible with the key
 * and value types of the listener.
 *
 * @param <K0> the base type for all key types of maps built by this map maker
 * @param <V0> the base type for all value types of maps built by this map maker
 * @author Kevin Bourrillion
 * @since 7
 */
@Beta
public abstract class GenericMapMaker<K0, V0> {
  // Set by MapMaker, but sits in this class to preserve the type relationship
  MapEvictionListener<K0, V0> evictionListener;

  // No subclasses but our own
  GenericMapMaker() {}

  /**
   * See {@link MapMaker#initialCapacity}.
   */
  public abstract GenericMapMaker<K0, V0> initialCapacity(int initialCapacity);

  /**
   * See {@link MapMaker#concurrencyLevel}.
   */
  @GwtIncompatible("java.util.concurrent.ConcurrentHashMap concurrencyLevel")
  public abstract GenericMapMaker<K0, V0> concurrencyLevel(
      int concurrencyLevel);

  /**
   * See {@link MapMaker#weakKeys}.
   */
  @GwtIncompatible("java.lang.ref.WeakReference")
  public abstract GenericMapMaker<K0, V0> weakKeys();

  /**
   * See {@link MapMaker#softKeys}.
   */
  @GwtIncompatible("java.lang.ref.SoftReference")
  public abstract GenericMapMaker<K0, V0> softKeys();

  /**
   * See {@link MapMaker#weakValues}.
   */
  @GwtIncompatible("java.lang.ref.WeakReference")
  public abstract GenericMapMaker<K0, V0> weakValues();

  /**
   * See {@link MapMaker#softValues}.
   */
  @GwtIncompatible("java.lang.ref.SoftReference")
  public abstract GenericMapMaker<K0, V0> softValues();

  /**
   * See {@link MapMaker#expiration}.
   */
  public abstract GenericMapMaker<K0, V0> expiration(
      long duration, TimeUnit unit);

  /*
   * Note that MapMaker's evictionListener() is not here, because once you're
   * interacting with a GenericMapMaker you've already called that, and
   * shouldn't be calling it again.
   */

  /**
   * See {@link MapMaker#makeMap}.
   */
  public abstract <K extends K0, V extends V0> ConcurrentMap<K, V> makeMap();

  /**
   * See {@link MapMaker#makeComputingMap}.
   */
  public abstract <K extends K0, V extends V0> ConcurrentMap<K, V>
      makeComputingMap(Function<? super K, ? extends V> computingFunction);
}
