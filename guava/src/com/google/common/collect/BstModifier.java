/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * A specification for a local change to an entry in a binary search tree.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
interface BstModifier<K, N extends BstNode<K, N>> {

  /**
   * Given a target key and the original entry (if any) with the specified key, returns the entry
   * with key {@code key} after this mutation has been performed. The result must either be {@code
   * null} or must have a key that compares as equal to {@code key}. A deletion operation, for
   * example, would always return {@code null}, or an insertion operation would always return a
   * non-null {@code insertedEntry}.
   *
   * <p>If this method returns a non-null entry of type {@code N}, any children it has will be
   * ignored.
   *
   * <p>This method may return {@code originalEntry} itself to indicate that no change is made.
   *
   * @param key The key being targeted for modification.
   * @param originalEntry The original entry in the binary search tree with the specified key, if
   *        any. No guarantees are made about the children of this entry when treated as a node; in
   *        particular, they are not necessarily the children of the corresponding node in the
   *        binary search tree.
   * @return the entry (if any) with the specified key after this modification is performed
   */
  BstModificationResult<N> modify(K key, @Nullable N originalEntry);
}
