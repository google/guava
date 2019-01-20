/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.cache;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import java.util.AbstractMap.SimpleImmutableEntry;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A notification of the removal of a single entry. The key and/or value may be null if they were
 * already garbage collected.
 *
 * <p>Like other {@code Entry} instances associated with {@code CacheBuilder}, this class holds
 * strong references to the key and value, regardless of the type of references the cache may be
 * using.
 *
 * @author Charles Fry
 * @since 10.0
 */
@GwtCompatible
public final class RemovalNotification<K, V> extends SimpleImmutableEntry<K, V> {
  private final RemovalCause cause;

  /**
   * Creates a new {@code RemovalNotification} for the given {@code key}/{@code value} pair, with
   * the given {@code cause} for the removal. The {@code key} and/or {@code value} may be {@code
   * null} if they were already garbage collected.
   *
   * @since 19.0
   */
  public static <K, V> RemovalNotification<K, V> create(
      @Nullable K key, @Nullable V value, RemovalCause cause) {
    return new RemovalNotification(key, value, cause);
  }

  private RemovalNotification(@Nullable K key, @Nullable V value, RemovalCause cause) {
    super(key, value);
    this.cause = checkNotNull(cause);
  }

  /** Returns the cause for which the entry was removed. */
  public RemovalCause getCause() {
    return cause;
  }

  /**
   * Returns {@code true} if there was an automatic removal due to eviction (the cause is neither
   * {@link RemovalCause#EXPLICIT} nor {@link RemovalCause#REPLACED}).
   */
  public boolean wasEvicted() {
    return cause.wasEvicted();
  }

  private static final long serialVersionUID = 0;
}
