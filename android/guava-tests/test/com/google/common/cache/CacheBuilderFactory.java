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

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.cartesianProduct;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.LocalCache.Strength;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Helper class for creating {@link CacheBuilder} instances with all combinations of several sets of
 * parameters.
 *
 * @author mike nonemacher
 */
@GwtIncompatible
@J2ktIncompatible
@NullUnmarked
class CacheBuilderFactory {
  // Default values contain only 'null', which means don't call the CacheBuilder method (just give
  // the CacheBuilder default).
  private Set<Integer> concurrencyLevels = newHashSet((Integer) null);
  private Set<Integer> initialCapacities = newHashSet((Integer) null);
  private Set<Integer> maximumSizes = newHashSet((Integer) null);
  private Set<DurationSpec> expireAfterWrites = newHashSet((DurationSpec) null);
  private Set<DurationSpec> expireAfterAccesses = newHashSet((DurationSpec) null);
  private Set<DurationSpec> refreshes = newHashSet((DurationSpec) null);
  private Set<Strength> keyStrengths = newHashSet((Strength) null);
  private Set<Strength> valueStrengths = newHashSet((Strength) null);

  @CanIgnoreReturnValue
  CacheBuilderFactory withConcurrencyLevels(Set<Integer> concurrencyLevels) {
    this.concurrencyLevels = new LinkedHashSet<>(concurrencyLevels);
    return this;
  }

  @CanIgnoreReturnValue
  CacheBuilderFactory withInitialCapacities(Set<Integer> initialCapacities) {
    this.initialCapacities = new LinkedHashSet<>(initialCapacities);
    return this;
  }

  @CanIgnoreReturnValue
  CacheBuilderFactory withMaximumSizes(Set<Integer> maximumSizes) {
    this.maximumSizes = new LinkedHashSet<>(maximumSizes);
    return this;
  }

  @CanIgnoreReturnValue
  CacheBuilderFactory withExpireAfterWrites(Set<DurationSpec> durations) {
    this.expireAfterWrites = new LinkedHashSet<>(durations);
    return this;
  }

  @CanIgnoreReturnValue
  CacheBuilderFactory withExpireAfterAccesses(Set<DurationSpec> durations) {
    this.expireAfterAccesses = new LinkedHashSet<>(durations);
    return this;
  }

  @CanIgnoreReturnValue
  CacheBuilderFactory withRefreshes(Set<DurationSpec> durations) {
    this.refreshes = new LinkedHashSet<>(durations);
    return this;
  }

  @CanIgnoreReturnValue
  CacheBuilderFactory withKeyStrengths(Set<Strength> keyStrengths) {
    this.keyStrengths = new LinkedHashSet<>(keyStrengths);
    Preconditions.checkArgument(!this.keyStrengths.contains(Strength.SOFT));
    return this;
  }

  @CanIgnoreReturnValue
  CacheBuilderFactory withValueStrengths(Set<Strength> valueStrengths) {
    this.valueStrengths = new LinkedHashSet<>(valueStrengths);
    return this;
  }

  Iterable<CacheBuilder<Object, Object>> buildAllPermutations() {
    Iterable<List<Object>> combinations =
        buildCartesianProduct(
            concurrencyLevels,
            initialCapacities,
            maximumSizes,
            expireAfterWrites,
            expireAfterAccesses,
            refreshes,
            keyStrengths,
            valueStrengths);
    return transform(
        combinations,
        combination ->
            createCacheBuilder(
                (Integer) combination.get(0),
                (Integer) combination.get(1),
                (Integer) combination.get(2),
                (DurationSpec) combination.get(3),
                (DurationSpec) combination.get(4),
                (DurationSpec) combination.get(5),
                (Strength) combination.get(6),
                (Strength) combination.get(7)));
  }

  /**
   * Sets.cartesianProduct doesn't allow sets that contain null, but we want null to mean "don't
   * call the associated CacheBuilder method" - that is, get the default CacheBuilder behavior. This
   * method wraps the elements in the input sets (which may contain null) as Optionals, calls
   * Sets.cartesianProduct with those, then transforms the result to unwrap the Optionals.
   */
  private static Iterable<List<Object>> buildCartesianProduct(Set<?>... sets) {
    List<Set<Optional<?>>> optionalSets = newArrayListWithExpectedSize(sets.length);
    for (Set<?> set : sets) {
      Set<Optional<?>> optionalSet = newLinkedHashSet(transform(set, Optional::fromNullable));
      optionalSets.add(optionalSet);
    }
    Set<List<Optional<?>>> cartesianProduct = cartesianProduct(optionalSets);
    return transform(cartesianProduct, objs -> transform(objs, Optional::orNull));
  }

  private static CacheBuilder<Object, Object> createCacheBuilder(
      @Nullable Integer concurrencyLevel,
      @Nullable Integer initialCapacity,
      @Nullable Integer maximumSize,
      @Nullable DurationSpec expireAfterWrite,
      @Nullable DurationSpec expireAfterAccess,
      @Nullable DurationSpec refresh,
      @Nullable Strength keyStrength,
      @Nullable Strength valueStrength) {

    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    if (concurrencyLevel != null) {
      builder.concurrencyLevel(concurrencyLevel);
    }
    if (initialCapacity != null) {
      builder.initialCapacity(initialCapacity);
    }
    if (maximumSize != null) {
      builder.maximumSize(maximumSize);
    }
    if (expireAfterWrite != null) {
      builder.expireAfterWrite(expireAfterWrite.duration, expireAfterWrite.unit);
    }
    if (expireAfterAccess != null) {
      builder.expireAfterAccess(expireAfterAccess.duration, expireAfterAccess.unit);
    }
    if (refresh != null) {
      builder.refreshAfterWrite(refresh.duration, refresh.unit);
    }
    if (keyStrength != null) {
      builder.setKeyStrength(keyStrength);
    }
    if (valueStrength != null) {
      builder.setValueStrength(valueStrength);
    }
    return builder;
  }

  static class DurationSpec {
    private final long duration;
    private final TimeUnit unit;

    private DurationSpec(long duration, TimeUnit unit) {
      this.duration = duration;
      this.unit = unit;
    }

    public static DurationSpec of(long duration, TimeUnit unit) {
      return new DurationSpec(duration, unit);
    }

    @Override
    public int hashCode() {
      return Objects.hash(duration, unit);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o instanceof DurationSpec) {
        DurationSpec that = (DurationSpec) o;
        return unit.toNanos(duration) == that.unit.toNanos(that.duration);
      }
      return false;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("duration", duration)
          .add("unit", unit)
          .toString();
    }
  }
}
