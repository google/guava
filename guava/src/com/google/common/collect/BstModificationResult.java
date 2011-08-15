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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * The result of a {@code BstModifier}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class BstModificationResult<N extends BstNode<?, N>> {
  enum ModificationType {
    IDENTITY, REBUILDING_CHANGE, REBALANCING_CHANGE;
  }

  static <N extends BstNode<?, N>> BstModificationResult<N> identity(@Nullable N target) {
    return new BstModificationResult<N>(target, target, ModificationType.IDENTITY);
  }

  static <N extends BstNode<?, N>> BstModificationResult<N> rebuildingChange(
      @Nullable N originalTarget, @Nullable N changedTarget) {
    return new BstModificationResult<N>(
        originalTarget, changedTarget, ModificationType.REBUILDING_CHANGE);
  }

  static <N extends BstNode<?, N>> BstModificationResult<N> rebalancingChange(
      @Nullable N originalTarget, @Nullable N changedTarget) {
    return new BstModificationResult<N>(
        originalTarget, changedTarget, ModificationType.REBALANCING_CHANGE);
  }

  @Nullable private final N originalTarget;
  @Nullable private final N changedTarget;
  private final ModificationType type;

  private BstModificationResult(
      @Nullable N originalTarget, @Nullable N changedTarget, ModificationType type) {
    this.originalTarget = originalTarget;
    this.changedTarget = changedTarget;
    this.type = checkNotNull(type);
  }

  @Nullable
  N getOriginalTarget() {
    return originalTarget;
  }

  @Nullable
  N getChangedTarget() {
    return changedTarget;
  }

  ModificationType getType() {
    return type;
  }
}
