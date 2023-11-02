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

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.LocalCache.Strength;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Helper class for creating {@link CacheBuilder} instances with all combinations of several sets of
 * parameters.
 *
 * @author mike nonemacher
 */
class CacheBuilderFactory {
  // Default values contain only 'null', which means don't call the CacheBuilder method (just give
  // the CacheBuilder default).
  private Set<Integer> concurrencyLevels = Sets.newHashSet((Integer) null);
  private Set<Integer> initialCapacities = Sets.newHashSet((Integer) null);
  private Set<Integer> maximumSizes = Sets.newHashSet((Integer) null);
  private Set<DurationSpec> expireAfterWrites = Sets.newHashSet((DurationSpec) null);
  private Set<DurationSpec> expireAfterAccesses = Sets.newHashSet((DurationSpec) null);
  private Set<DurationSpec> refreshes = Sets.newHashSet((DurationSpec) null);
  private Set<Strength> keyStrengths = Sets.newHashSet((Strength) null);
  private Set<Strength> valueStrengths = Sets.newHashSet((Strength) null);

  CacheBuilderFactory withConcurrencyLevels(Set<Integer> concurrencyLevels) {
    this.concurrencyLevels = Sets.newLinkedHashSet(concurrencyLevels);
    return this;
  }

  CacheBuilderFactory withInitialCapacities(Set<Integer> initialCapacities) {
    this.initialCapacities = Sets.newLinkedHashSet(initialCapacities);
    return this;
  }

  CacheBuilderFactory withMaximumSizes(Set<Integer> maximumSizes) {
    this.maximumSizes = Sets.newLinkedHashSet(maximumSizes);
    return this;
  }

  CacheBuilderFactory withExpireAfterWrites(Set<DurationSpec> durations) {
    this.expireAfterWrites = Sets.newLinkedHashSet(durations);
    return this;
  }

  CacheBuilderFactory withExpireAfterAccesses(Set<DurationSpec> durations) {
    this.expireAfterAccesses = Sets.newLinkedHashSet(durations);
    return this;
  }

  CacheBuilderFactory withRefreshes(Set<DurationSpec> durations) {
    this.refreshes = Sets.newLinkedHashSet(durations);
    return this;
  }

  CacheBuilderFactory withKeyStrengths(Set<Strength> keyStrengths) {
    this.keyStrengths = Sets.newLinkedHashSet(keyStrengths);
    Preconditions.checkArgument(!this.keyStrengths.contains(Strength.SOFT));
    return this;
  }

  CacheBuilderFactory withValueStrengths(Set<Strength> valueStrengths) {
    this.valueStrengths = Sets.newLinkedHashSet(valueStrengths);
    return this;
  }

  Iterable<CacheBuilder<Object, Object>> buildAllPermutations() {
    @SuppressWarnings("unchecked")
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
    return Iterables.transform(
        combinations,
        new Function<List<Object>, CacheBuilder<Object, Object>>() {
          @Override
          public CacheBuilder<Object, Object> apply(List<Object> combination) {
            return createCacheBuilder(
                (Integer) combination.get(0),
                (Integer) combination.get(1),
                (Integer) combination.get(2),
                (DurationSpec) combination.get(3),
                (DurationSpec) combination.get(4),
                (DurationSpec) combination.get(5),
                (Strength) combination.get(6),
                (Strength) combination.get(7));
          }
        });
  }

  private static final Function<Object, Optional<?>> NULLABLE_TO_OPTIONAL =
      new Function<Object, Optional<?>>() {
        @Override
        public Optional<?> apply(@Nullable Object obj) {
          return Optional.fromNullable(obj);
        }
      };

  private static final Function<Optional<?>, @Nullable Object> OPTIONAL_TO_NULLABLE =
      new Function<Optional<?>, @Nullable Object>() {
        @Override
        public @Nullable Object apply(Optional<?> optional) {
          return optional.orNull();
        }
      };

  /**
   * Sets.cartesianProduct doesn't allow sets that contain null, but we want null to mean "don't
   * call the associated CacheBuilder method" - that is, get the default CacheBuilder behavior. This
   * method wraps the elements in the input sets (which may contain null) as Optionals, calls
   * Sets.cartesianProduct with those, then transforms the result to unwrap the Optionals.
   */
  private Iterable<List<Object>> buildCartesianProduct(Set<?>... sets) {
    List<Set<Optional<?>>> optionalSets = Lists.newArrayListWithExpectedSize(sets.length);
    for (Set<?> set : sets) {
      Set<Optional<?>> optionalSet =
          Sets.newLinkedHashSet(Iterables.transform(set, NULLABLE_TO_OPTIONAL));
      optionalSets.add(optionalSet);
    }
    Set<List<Optional<?>>> cartesianProduct = Sets.cartesianProduct(optionalSets);
    return Iterables.transform(
        cartesianProduct,
        new Function<List<Optional<?>>, List<Object>>() {
          @Override
          public List<Object> apply(List<Optional<?>> objs) {
            return Lists.transform(objs, OPTIONAL_TO_NULLABLE);
          }
        });
  }

  private CacheBuilder<Object, Object> createCacheBuilder(
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
      return Objects.hashCode(duration, unit);
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
