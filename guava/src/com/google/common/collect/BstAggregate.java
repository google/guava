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
 * An integer-valued function on binary search tree nodes that adds between nodes.
 * 
 * <p>The value of individual entries must fit into an {@code int}, but the value of an entire
 * tree can require a {@code long}.
 * 
 * @author Louis Wasserman
 */
@GwtCompatible
interface BstAggregate<N extends BstNode<?, N>> {
  /**
   * The total value on an entire subtree. Must be equal to the sum of the {@link #entryValue
   * entryValue} of this node and all its descendants.
   */
  long treeValue(@Nullable N tree);

  /**
   * The value on a single entry, ignoring its descendants.
   */
  int entryValue(N entry);
}
