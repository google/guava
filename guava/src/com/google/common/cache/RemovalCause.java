/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.cache;

import com.google.common.annotations.Beta;

/**
 * The reason why a cached entry was removed.
 *
 * @author fry@google.com (Charles Fry)
 * @since Guava release 10
 */
@Beta
public enum RemovalCause {
  /**
   * The entry was manually removed by the user. This can result from the user invoking {@link
   * Cache#invalidate}, {@link Map#remove}, {@link ConcurrentMap#remove}, or {@link
   * java.util.Iterator#remove}.
   */
  EXPLICIT {
    @Override
    boolean wasEvicted() {
      return false;
    }
  },

  /**
   * The entry itself was not actually removed, but its value was replaced by the user. This can
   * result from the user invoking {@link Map#put}, {@link Map#putAll},
   * {@link ConcurrentMap#replace(Object, Object)}, or
   * {@link ConcurrentMap#replace(Object, Object, Object)}.
   */
  REPLACED {
    @Override
    boolean wasEvicted() {
      return false;
    }
  },

  /**
   * The entry was removed automatically because its key or value was garbage-collected. This
   * can occur when using {@link #softKeys}, {@link #softValues}, {@link #weakKeys}, or {@link
   * #weakValues}.
   */
  COLLECTED {
    @Override
    boolean wasEvicted() {
      return true;
    }
  },

  /**
   * The entry's expiration timestamp has passed. This can occur when using {@link
   * #expireAfterWrite} or {@link #expireAfterAccess}.
   */
  EXPIRED {
    @Override
    boolean wasEvicted() {
      return true;
    }
  },

  /**
   * The entry was evicted due to size constraints. This can occur when using {@link
   * #maximumSize}.
   */
  SIZE {
    @Override
    boolean wasEvicted() {
      return true;
    }
  };

  /**
   * Returns {@code true} if there was an automatic removal due to eviction (the cause is neither
   * {@link #EXPLICIT} nor {@link #REPLACED}).
   */
  abstract boolean wasEvicted();
}
