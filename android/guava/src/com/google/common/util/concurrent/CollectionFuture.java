/*
 * Copyright (C) 2006 The Guava Authors
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

package com.google.common.util.concurrent;

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static java.util.Collections.unmodifiableList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/** Aggregate future that collects (stores) results of each future. */
@GwtCompatible(emulated = true)
abstract class CollectionFuture<V, C> extends AggregateFuture<V, C> {
  private List<Optional<V>> values;

  CollectionFuture(
      ImmutableCollection<? extends ListenableFuture<? extends V>> futures,
      boolean allMustSucceed) {
    super(futures, allMustSucceed, true);

    this.values =
        futures.isEmpty()
            ? ImmutableList.<Optional<V>>of()
            : Lists.<Optional<V>>newArrayListWithCapacity(futures.size());

    // Populate the results list with null initially.
    for (int i = 0; i < futures.size(); ++i) {
      values.add(null);
    }
  }

  @Override
  final void collectOneValue(int index, @NullableDecl V returnValue) {
    List<Optional<V>> localValues = values;
    if (localValues != null) {
      localValues.set(index, Optional.fromNullable(returnValue));
    }
  }

  @Override
  final void handleAllCompleted() {
    List<Optional<V>> localValues = values;
    if (localValues != null) {
      set(combine(localValues));
    }
  }

  @Override
  void releaseResources(ReleaseResourcesReason reason) {
    super.releaseResources(reason);
    this.values = null;
  }

  abstract C combine(List<Optional<V>> values);

  /** Used for {@link Futures#allAsList} and {@link Futures#successfulAsList}. */
  static final class ListFuture<V> extends CollectionFuture<V, List<V>> {
    ListFuture(
        ImmutableCollection<? extends ListenableFuture<? extends V>> futures,
        boolean allMustSucceed) {
      super(futures, allMustSucceed);
      init();
    }

    @Override
    public List<V> combine(List<Optional<V>> values) {
      List<V> result = newArrayListWithCapacity(values.size());
      for (Optional<V> element : values) {
        result.add(element != null ? element.orNull() : null);
      }
      return unmodifiableList(result);
    }
  }
}
