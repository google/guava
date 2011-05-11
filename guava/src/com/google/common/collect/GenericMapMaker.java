/*
 * Copyright 2010 The Guava Authors All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.MapMaker.RemovalListener;
import com.google.common.collect.MapMaker.RemovalNotification;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * A class exactly like {@link MapMaker}, except restricted in the types of maps it can build. This
 * type is returned by {@link MapMaker#removalListener} to prevent the user from trying to build a
 * map that's incompatible with the key and value types of the listener.
 *
 * @param <K0> the base type for all key types of maps built by this map maker
 * @param <V0> the base type for all value types of maps built by this map maker
 * @author Kevin Bourrillion
 * @since Guava release 07
 */
@Beta
@GwtCompatible(emulated = true)
public abstract class GenericMapMaker<K0, V0> {
  @GwtIncompatible("To be supported")
  enum NullListener implements RemovalListener<Object, Object> {
    INSTANCE;

    @Override
    public void onRemoval(RemovalNotification<Object, Object> notification) {}
  }

  // Set by MapMaker, but sits in this class to preserve the type relationship
  @GwtIncompatible("To be supported")
  RemovalListener<K0, V0> removalListener;

  // No subclasses but our own
  GenericMapMaker() {}

  /**
   * See {@link MapMaker#initialCapacity}.
   */
  public abstract GenericMapMaker<K0, V0> initialCapacity(int initialCapacity);

  /**
   * See {@link MapMaker#maximumSize}.
   *
   * @since Guava release 08
   */
  @Beta
  public abstract GenericMapMaker<K0, V0> maximumSize(int maximumSize);

  /**
   * See {@link MapMaker#concurrencyLevel}.
   */
  public abstract GenericMapMaker<K0, V0> concurrencyLevel(int concurrencyLevel);

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
  // TODO(user): deprecate
  public abstract GenericMapMaker<K0, V0> expiration(long duration, TimeUnit unit);

  /**
   * See {@link MapMaker#expireAfterWrite}.
   *
   * @since Guava release 08
   */
  public abstract GenericMapMaker<K0, V0> expireAfterWrite(long duration, TimeUnit unit);

  /**
   * See {@link MapMaker#expireAfterAccess}.
   *
   * @since Guava release 08
   */
  @GwtIncompatible("To be supported")
  public abstract GenericMapMaker<K0, V0> expireAfterAccess(long duration, TimeUnit unit);

  /*
   * Note that MapMaker's removalListener() is not here, because once you're interacting with a
   * GenericMapMaker you've already called that, and shouldn't be calling it again.
   */

  @SuppressWarnings("unchecked") // safe covariant cast
  @GwtIncompatible("To be supported")
  <K extends K0, V extends V0> RemovalListener<K, V> getRemovalListener() {
    return (RemovalListener<K, V>) Objects.firstNonNull(removalListener, NullListener.INSTANCE);
  }

  /**
   * See {@link MapMaker#makeMap}.
   */
  public abstract <K extends K0, V extends V0> ConcurrentMap<K, V> makeMap();

  /**
   * See {@link MapMaker#makeComputingMap}.
   */
  public abstract <K extends K0, V extends V0> ConcurrentMap<K, V> makeComputingMap(
      Function<? super K, ? extends V> computingFunction);
}
