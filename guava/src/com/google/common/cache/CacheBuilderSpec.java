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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.cache.LocalCache.Strength;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A specification of a {@link CacheBuilder} configuration.
 *
 * <p>{@code CacheBuilderSpec} supports parsing configuration off of a string, which makes it
 * especially useful for command-line configuration of a {@code CacheBuilder}.
 *
 * <p>The string syntax is a series of comma-separated keys or key-value pairs, each corresponding
 * to a {@code CacheBuilder} method.
 *
 * <ul>
 *   <li>{@code concurrencyLevel=[integer]}: sets {@link CacheBuilder#concurrencyLevel}.
 *   <li>{@code initialCapacity=[integer]}: sets {@link CacheBuilder#initialCapacity}.
 *   <li>{@code maximumSize=[long]}: sets {@link CacheBuilder#maximumSize}.
 *   <li>{@code maximumWeight=[long]}: sets {@link CacheBuilder#maximumWeight}.
 *   <li>{@code expireAfterAccess=[duration]}: sets {@link CacheBuilder#expireAfterAccess}.
 *   <li>{@code expireAfterWrite=[duration]}: sets {@link CacheBuilder#expireAfterWrite}.
 *   <li>{@code refreshAfterWrite=[duration]}: sets {@link CacheBuilder#refreshAfterWrite}.
 *   <li>{@code weakKeys}: sets {@link CacheBuilder#weakKeys}.
 *   <li>{@code softValues}: sets {@link CacheBuilder#softValues}.
 *   <li>{@code weakValues}: sets {@link CacheBuilder#weakValues}.
 *   <li>{@code recordStats}: sets {@link CacheBuilder#recordStats}.
 * </ul>
 *
 * <p>The set of supported keys will grow as {@code CacheBuilder} evolves, but existing keys will
 * never be removed.
 *
 * <p>Durations are represented by an integer, followed by one of "d", "h", "m", or "s",
 * representing days, hours, minutes, or seconds respectively. (There is currently no syntax to
 * request expiration in milliseconds, microseconds, or nanoseconds.)
 *
 * <p>Whitespace before and after commas and equal signs is ignored. Keys may not be repeated; it is
 * also illegal to use the following pairs of keys in a single value:
 *
 * <ul>
 *   <li>{@code maximumSize} and {@code maximumWeight}
 *   <li>{@code softValues} and {@code weakValues}
 * </ul>
 *
 * <p>{@code CacheBuilderSpec} does not support configuring {@code CacheBuilder} methods with
 * non-value parameters. These must be configured in code.
 *
 * <p>A new {@code CacheBuilder} can be instantiated from a {@code CacheBuilderSpec} using {@link
 * CacheBuilder#from(CacheBuilderSpec)} or {@link CacheBuilder#from(String)}.
 *
 * @author Adam Winer
 * @since 12.0
 */
@SuppressWarnings("GoodTime") // lots of violations (nanosecond math)
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public final class CacheBuilderSpec {
  /** Parses a single value. */
  private interface ValueParser {
    void parse(CacheBuilderSpec spec, String key, @CheckForNull String value);
  }

  /** Splits each key-value pair. */
  private static final Splitter KEYS_SPLITTER = Splitter.on(',').trimResults();

  /** Splits the key from the value. */
  private static final Splitter KEY_VALUE_SPLITTER = Splitter.on('=').trimResults();

  /** Map of names to ValueParser. */
  private static final ImmutableMap<String, ValueParser> VALUE_PARSERS =
      ImmutableMap.<String, ValueParser>builder()
          .put("initialCapacity", new InitialCapacityParser())
          .put("maximumSize", new MaximumSizeParser())
          .put("maximumWeight", new MaximumWeightParser())
          .put("concurrencyLevel", new ConcurrencyLevelParser())
          .put("weakKeys", new KeyStrengthParser(Strength.WEAK))
          .put("softValues", new ValueStrengthParser(Strength.SOFT))
          .put("weakValues", new ValueStrengthParser(Strength.WEAK))
          .put("recordStats", new RecordStatsParser())
          .put("expireAfterAccess", new AccessDurationParser())
          .put("expireAfterWrite", new WriteDurationParser())
          .put("refreshAfterWrite", new RefreshDurationParser())
          .put("refreshInterval", new RefreshDurationParser())
          .buildOrThrow();

  @VisibleForTesting @CheckForNull Integer initialCapacity;
  @VisibleForTesting @CheckForNull Long maximumSize;
  @VisibleForTesting @CheckForNull Long maximumWeight;
  @VisibleForTesting @CheckForNull Integer concurrencyLevel;
  @VisibleForTesting @CheckForNull Strength keyStrength;
  @VisibleForTesting @CheckForNull Strength valueStrength;
  @VisibleForTesting @CheckForNull Boolean recordStats;
  @VisibleForTesting long writeExpirationDuration;
  @VisibleForTesting @CheckForNull TimeUnit writeExpirationTimeUnit;
  @VisibleForTesting long accessExpirationDuration;
  @VisibleForTesting @CheckForNull TimeUnit accessExpirationTimeUnit;
  @VisibleForTesting long refreshDuration;
  @VisibleForTesting @CheckForNull TimeUnit refreshTimeUnit;
  /** Specification; used for toParseableString(). */
  private final String specification;

  private CacheBuilderSpec(String specification) {
    this.specification = specification;
  }

  /**
   * Creates a CacheBuilderSpec from a string.
   *
   * @param cacheBuilderSpecification the string form
   */
  public static CacheBuilderSpec parse(String cacheBuilderSpecification) {
    CacheBuilderSpec spec = new CacheBuilderSpec(cacheBuilderSpecification);
    if (!cacheBuilderSpecification.isEmpty()) {
      for (String keyValuePair : KEYS_SPLITTER.split(cacheBuilderSpecification)) {
        List<String> keyAndValue = ImmutableList.copyOf(KEY_VALUE_SPLITTER.split(keyValuePair));
        checkArgument(!keyAndValue.isEmpty(), "blank key-value pair");
        checkArgument(
            keyAndValue.size() <= 2,
            "key-value pair %s with more than one equals sign",
            keyValuePair);

        // Find the ValueParser for the current key.
        String key = keyAndValue.get(0);
        ValueParser valueParser = VALUE_PARSERS.get(key);
        checkArgument(valueParser != null, "unknown key %s", key);

        String value = keyAndValue.size() == 1 ? null : keyAndValue.get(1);
        valueParser.parse(spec, key, value);
      }
    }

    return spec;
  }

  /** Returns a CacheBuilderSpec that will prevent caching. */
  public static CacheBuilderSpec disableCaching() {
    // Maximum size of zero is one way to block caching
    return CacheBuilderSpec.parse("maximumSize=0");
  }

  /** Returns a CacheBuilder configured according to this instance's specification. */
  CacheBuilder<Object, Object> toCacheBuilder() {
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    if (initialCapacity != null) {
      builder.initialCapacity(initialCapacity);
    }
    if (maximumSize != null) {
      builder.maximumSize(maximumSize);
    }
    if (maximumWeight != null) {
      builder.maximumWeight(maximumWeight);
    }
    if (concurrencyLevel != null) {
      builder.concurrencyLevel(concurrencyLevel);
    }
    if (keyStrength != null) {
      switch (keyStrength) {
        case WEAK:
          builder.weakKeys();
          break;
        default:
          throw new AssertionError();
      }
    }
    if (valueStrength != null) {
      switch (valueStrength) {
        case SOFT:
          builder.softValues();
          break;
        case WEAK:
          builder.weakValues();
          break;
        default:
          throw new AssertionError();
      }
    }
    if (recordStats != null && recordStats) {
      builder.recordStats();
    }
    if (writeExpirationTimeUnit != null) {
      builder.expireAfterWrite(writeExpirationDuration, writeExpirationTimeUnit);
    }
    if (accessExpirationTimeUnit != null) {
      builder.expireAfterAccess(accessExpirationDuration, accessExpirationTimeUnit);
    }
    if (refreshTimeUnit != null) {
      builder.refreshAfterWrite(refreshDuration, refreshTimeUnit);
    }

    return builder;
  }

  /**
   * Returns a string that can be used to parse an equivalent {@code CacheBuilderSpec}. The order
   * and form of this representation is not guaranteed, except that reparsing its output will
   * produce a {@code CacheBuilderSpec} equal to this instance.
   */
  public String toParsableString() {
    return specification;
  }

  /**
   * Returns a string representation for this CacheBuilderSpec instance. The form of this
   * representation is not guaranteed.
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).addValue(toParsableString()).toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        initialCapacity,
        maximumSize,
        maximumWeight,
        concurrencyLevel,
        keyStrength,
        valueStrength,
        recordStats,
        durationInNanos(writeExpirationDuration, writeExpirationTimeUnit),
        durationInNanos(accessExpirationDuration, accessExpirationTimeUnit),
        durationInNanos(refreshDuration, refreshTimeUnit));
  }

  @Override
  public boolean equals(@CheckForNull Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof CacheBuilderSpec)) {
      return false;
    }
    CacheBuilderSpec that = (CacheBuilderSpec) obj;
    return Objects.equal(initialCapacity, that.initialCapacity)
        && Objects.equal(maximumSize, that.maximumSize)
        && Objects.equal(maximumWeight, that.maximumWeight)
        && Objects.equal(concurrencyLevel, that.concurrencyLevel)
        && Objects.equal(keyStrength, that.keyStrength)
        && Objects.equal(valueStrength, that.valueStrength)
        && Objects.equal(recordStats, that.recordStats)
        && Objects.equal(
            durationInNanos(writeExpirationDuration, writeExpirationTimeUnit),
            durationInNanos(that.writeExpirationDuration, that.writeExpirationTimeUnit))
        && Objects.equal(
            durationInNanos(accessExpirationDuration, accessExpirationTimeUnit),
            durationInNanos(that.accessExpirationDuration, that.accessExpirationTimeUnit))
        && Objects.equal(
            durationInNanos(refreshDuration, refreshTimeUnit),
            durationInNanos(that.refreshDuration, that.refreshTimeUnit));
  }

  /**
   * Converts an expiration duration/unit pair into a single Long for hashing and equality. Uses
   * nanos to match CacheBuilder implementation.
   */
  @CheckForNull
  private static Long durationInNanos(long duration, @CheckForNull TimeUnit unit) {
    return (unit == null) ? null : unit.toNanos(duration);
  }

  /** Base class for parsing integers. */
  abstract static class IntegerParser implements ValueParser {
    protected abstract void parseInteger(CacheBuilderSpec spec, int value);

    @Override
    public void parse(CacheBuilderSpec spec, String key, @Nullable String value) {
      if (isNullOrEmpty(value)) {
        throw new IllegalArgumentException("value of key " + key + " omitted");
      }
      try {
        parseInteger(spec, Integer.parseInt(value));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            format("key %s value set to %s, must be integer", key, value), e);
      }
    }
  }

  /** Base class for parsing integers. */
  abstract static class LongParser implements ValueParser {
    protected abstract void parseLong(CacheBuilderSpec spec, long value);

    @Override
    public void parse(CacheBuilderSpec spec, String key, @Nullable String value) {
      if (isNullOrEmpty(value)) {
        throw new IllegalArgumentException("value of key " + key + " omitted");
      }
      try {
        parseLong(spec, Long.parseLong(value));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            format("key %s value set to %s, must be integer", key, value), e);
      }
    }
  }

  /** Parse initialCapacity */
  static class InitialCapacityParser extends IntegerParser {
    @Override
    protected void parseInteger(CacheBuilderSpec spec, int value) {
      checkArgument(
          spec.initialCapacity == null,
          "initial capacity was already set to ",
          spec.initialCapacity);
      spec.initialCapacity = value;
    }
  }

  /** Parse maximumSize */
  static class MaximumSizeParser extends LongParser {
    @Override
    protected void parseLong(CacheBuilderSpec spec, long value) {
      checkArgument(spec.maximumSize == null, "maximum size was already set to ", spec.maximumSize);
      checkArgument(
          spec.maximumWeight == null, "maximum weight was already set to ", spec.maximumWeight);
      spec.maximumSize = value;
    }
  }

  /** Parse maximumWeight */
  static class MaximumWeightParser extends LongParser {
    @Override
    protected void parseLong(CacheBuilderSpec spec, long value) {
      checkArgument(
          spec.maximumWeight == null, "maximum weight was already set to ", spec.maximumWeight);
      checkArgument(spec.maximumSize == null, "maximum size was already set to ", spec.maximumSize);
      spec.maximumWeight = value;
    }
  }

  /** Parse concurrencyLevel */
  static class ConcurrencyLevelParser extends IntegerParser {
    @Override
    protected void parseInteger(CacheBuilderSpec spec, int value) {
      checkArgument(
          spec.concurrencyLevel == null,
          "concurrency level was already set to ",
          spec.concurrencyLevel);
      spec.concurrencyLevel = value;
    }
  }

  /** Parse weakKeys */
  static class KeyStrengthParser implements ValueParser {
    private final Strength strength;

    public KeyStrengthParser(Strength strength) {
      this.strength = strength;
    }

    @Override
    public void parse(CacheBuilderSpec spec, String key, @CheckForNull String value) {
      checkArgument(value == null, "key %s does not take values", key);
      checkArgument(spec.keyStrength == null, "%s was already set to %s", key, spec.keyStrength);
      spec.keyStrength = strength;
    }
  }

  /** Parse weakValues and softValues */
  static class ValueStrengthParser implements ValueParser {
    private final Strength strength;

    public ValueStrengthParser(Strength strength) {
      this.strength = strength;
    }

    @Override
    public void parse(CacheBuilderSpec spec, String key, @CheckForNull String value) {
      checkArgument(value == null, "key %s does not take values", key);
      checkArgument(
          spec.valueStrength == null, "%s was already set to %s", key, spec.valueStrength);

      spec.valueStrength = strength;
    }
  }

  /** Parse recordStats */
  static class RecordStatsParser implements ValueParser {

    @Override
    public void parse(CacheBuilderSpec spec, String key, @CheckForNull String value) {
      checkArgument(value == null, "recordStats does not take values");
      checkArgument(spec.recordStats == null, "recordStats already set");
      spec.recordStats = true;
    }
  }

  /** Base class for parsing times with durations */
  abstract static class DurationParser implements ValueParser {
    protected abstract void parseDuration(CacheBuilderSpec spec, long duration, TimeUnit unit);

    @Override
    public void parse(CacheBuilderSpec spec, String key, @CheckForNull String value) {
      if (isNullOrEmpty(value)) {
        throw new IllegalArgumentException("value of key " + key + " omitted");
      }
      try {
        char lastChar = value.charAt(value.length() - 1);
        TimeUnit timeUnit;
        switch (lastChar) {
          case 'd':
            timeUnit = TimeUnit.DAYS;
            break;
          case 'h':
            timeUnit = TimeUnit.HOURS;
            break;
          case 'm':
            timeUnit = TimeUnit.MINUTES;
            break;
          case 's':
            timeUnit = TimeUnit.SECONDS;
            break;
          default:
            throw new IllegalArgumentException(
                format("key %s invalid unit: was %s, must end with one of [dhms]", key, value));
        }

        long duration = Long.parseLong(value.substring(0, value.length() - 1));
        parseDuration(spec, duration, timeUnit);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            format("key %s value set to %s, must be integer", key, value));
      }
    }
  }

  /** Parse expireAfterAccess */
  static class AccessDurationParser extends DurationParser {
    @Override
    protected void parseDuration(CacheBuilderSpec spec, long duration, TimeUnit unit) {
      checkArgument(spec.accessExpirationTimeUnit == null, "expireAfterAccess already set");
      spec.accessExpirationDuration = duration;
      spec.accessExpirationTimeUnit = unit;
    }
  }

  /** Parse expireAfterWrite */
  static class WriteDurationParser extends DurationParser {
    @Override
    protected void parseDuration(CacheBuilderSpec spec, long duration, TimeUnit unit) {
      checkArgument(spec.writeExpirationTimeUnit == null, "expireAfterWrite already set");
      spec.writeExpirationDuration = duration;
      spec.writeExpirationTimeUnit = unit;
    }
  }

  /** Parse refreshAfterWrite */
  static class RefreshDurationParser extends DurationParser {
    @Override
    protected void parseDuration(CacheBuilderSpec spec, long duration, TimeUnit unit) {
      checkArgument(spec.refreshTimeUnit == null, "refreshAfterWrite already set");
      spec.refreshDuration = duration;
      spec.refreshTimeUnit = unit;
    }
  }

  private static String format(String format, Object... args) {
    return String.format(Locale.ROOT, format, args);
  }
}
