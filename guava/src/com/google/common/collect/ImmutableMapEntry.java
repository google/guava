/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;

import com.google.common.annotations.GwtIncompatible;
import javax.annotation.CheckForNull;

/**
 * Implementation of {@code Entry} for {@link ImmutableMap} that adds extra methods to traverse hash
 * buckets for the key and the value. This allows reuse in {@link RegularImmutableMap} and {@link
 * RegularImmutableBiMap}, which don't have to recopy the entries created by their {@code Builder}
 * implementations.
 *
 * <p>This base implementation has no key or value pointers, so instances of ImmutableMapEntry (but
 * not its subclasses) can be reused when copied from one ImmutableMap to another.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible // unnecessary
@ElementTypesAreNonnullByDefault
class ImmutableMapEntry<K, V> extends ImmutableEntry<K, V> {
  /**
   * Creates an {@code ImmutableMapEntry} array to hold parameterized entries. The result must never
   * be upcast back to ImmutableMapEntry[] (or Object[], etc.), or allowed to escape the class.
   *
   * <p>The returned array has all its elements set to their initial null values. However, we don't
   * declare it as {@code @Nullable ImmutableMapEntry[]} because our checker doesn't require newly
   * created arrays to have a {@code @Nullable} element type even when they're created directly with
   * {@code new ImmutableMapEntry[...]}, so it seems silly to insist on that only here.
   */
  @SuppressWarnings("unchecked") // Safe as long as the javadocs are followed
  static <K, V> ImmutableMapEntry<K, V>[] createEntryArray(int size) {
    return new ImmutableMapEntry[size];
  }

  ImmutableMapEntry(K key, V value) {
    super(key, value);
    checkEntryNotNull(key, value);
  }

  ImmutableMapEntry(ImmutableMapEntry<K, V> contents) {
    super(contents.getKey(), contents.getValue());
    // null check would be redundant
  }

  @CheckForNull
  ImmutableMapEntry<K, V> getNextInKeyBucket() {
    return null;
  }

  @CheckForNull
  ImmutableMapEntry<K, V> getNextInValueBucket() {
    return null;
  }

  /**
   * Returns true if this entry has no bucket links and can safely be reused as a terminal entry in
   * a bucket in another map.
   */
  boolean isReusable() {
    return true;
  }

  static class NonTerminalImmutableMapEntry<K, V> extends ImmutableMapEntry<K, V> {
    /*
     * Yes, we sometimes set nextInKeyBucket to null, even for this "non-terminal" entry. We don't
     * do that with a plain NonTerminalImmutableMapEntry, but we do do it with the BiMap-specific
     * subclass below. That's because the Entry might be non-terminal in the key bucket but terminal
     * in the value bucket (or vice versa).
     */
    @CheckForNull private final transient ImmutableMapEntry<K, V> nextInKeyBucket;

    NonTerminalImmutableMapEntry(
        K key, V value, @CheckForNull ImmutableMapEntry<K, V> nextInKeyBucket) {
      super(key, value);
      this.nextInKeyBucket = nextInKeyBucket;
    }

    @Override
    @CheckForNull
    final ImmutableMapEntry<K, V> getNextInKeyBucket() {
      return nextInKeyBucket;
    }

    @Override
    final boolean isReusable() {
      return false;
    }
  }

  static final class NonTerminalImmutableBiMapEntry<K, V>
      extends NonTerminalImmutableMapEntry<K, V> {
    @CheckForNull private final transient ImmutableMapEntry<K, V> nextInValueBucket;

    NonTerminalImmutableBiMapEntry(
        K key,
        V value,
        @CheckForNull ImmutableMapEntry<K, V> nextInKeyBucket,
        @CheckForNull ImmutableMapEntry<K, V> nextInValueBucket) {
      super(key, value, nextInKeyBucket);
      this.nextInValueBucket = nextInValueBucket;
    }

    @Override
    @CheckForNull
    ImmutableMapEntry<K, V> getNextInValueBucket() {
      return nextInValueBucket;
    }
  }
}
