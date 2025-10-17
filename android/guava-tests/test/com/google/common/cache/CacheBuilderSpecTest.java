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

import static com.google.common.cache.CacheBuilderSpec.parse;
import static com.google.common.cache.TestingWeighers.constantWeigher;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Suppliers;
import com.google.common.cache.LocalCache.Strength;
import com.google.common.testing.EqualsTester;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests CacheBuilderSpec. TODO(user): tests of a few invalid input conditions, boundary
 * conditions.
 *
 * @author Adam Winer
 */
@NullUnmarked
public class CacheBuilderSpecTest extends TestCase {
  public void testParse_empty() {
    CacheBuilderSpec spec = parse("");
    assertThat(spec.initialCapacity).isNull();
    assertThat(spec.maximumSize).isNull();
    assertThat(spec.maximumWeight).isNull();
    assertThat(spec.concurrencyLevel).isNull();
    assertThat(spec.keyStrength).isNull();
    assertThat(spec.valueStrength).isNull();
    assertThat(spec.writeExpirationTimeUnit).isNull();
    assertThat(spec.accessExpirationTimeUnit).isNull();
    assertCacheBuilderEquivalence(CacheBuilder.newBuilder(), CacheBuilder.from(spec));
  }

  public void testParse_initialCapacity() {
    CacheBuilderSpec spec = parse("initialCapacity=10");
    assertThat(spec.initialCapacity).isEqualTo(10);
    assertThat(spec.maximumSize).isNull();
    assertThat(spec.maximumWeight).isNull();
    assertThat(spec.concurrencyLevel).isNull();
    assertThat(spec.keyStrength).isNull();
    assertThat(spec.valueStrength).isNull();
    assertThat(spec.writeExpirationTimeUnit).isNull();
    assertThat(spec.accessExpirationTimeUnit).isNull();
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().initialCapacity(10), CacheBuilder.from(spec));
  }

  public void testParse_initialCapacityRepeated() {
    assertThrows(
        IllegalArgumentException.class, () -> parse("initialCapacity=10, initialCapacity=20"));
  }

  public void testParse_maximumSize() {
    CacheBuilderSpec spec = parse("maximumSize=9000");
    assertThat(spec.initialCapacity).isNull();
    assertThat(spec.maximumSize).isEqualTo(9000L);
    assertThat(spec.concurrencyLevel).isNull();
    assertThat(spec.keyStrength).isNull();
    assertThat(spec.valueStrength).isNull();
    assertThat(spec.writeExpirationTimeUnit).isNull();
    assertThat(spec.accessExpirationTimeUnit).isNull();
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().maximumSize(9000), CacheBuilder.from(spec));
  }

  public void testParse_maximumSizeRepeated() {
    assertThrows(IllegalArgumentException.class, () -> parse("maximumSize=10, maximumSize=20"));
  }

  public void testParse_maximumWeight() {
    CacheBuilderSpec spec = parse("maximumWeight=9000");
    assertThat(spec.initialCapacity).isNull();
    assertThat(spec.maximumWeight).isEqualTo(9000L);
    assertThat(spec.concurrencyLevel).isNull();
    assertThat(spec.keyStrength).isNull();
    assertThat(spec.valueStrength).isNull();
    assertThat(spec.writeExpirationTimeUnit).isNull();
    assertThat(spec.accessExpirationTimeUnit).isNull();
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().maximumWeight(9000), CacheBuilder.from(spec));
  }

  public void testParse_maximumWeightRepeated() {
    assertThrows(IllegalArgumentException.class, () -> parse("maximumWeight=10, maximumWeight=20"));
  }

  public void testParse_maximumSizeAndMaximumWeight() {
    assertThrows(IllegalArgumentException.class, () -> parse("maximumSize=10, maximumWeight=20"));
  }

  public void testParse_concurrencyLevel() {
    CacheBuilderSpec spec = parse("concurrencyLevel=32");
    assertThat(spec.initialCapacity).isNull();
    assertThat(spec.maximumSize).isNull();
    assertThat(spec.maximumWeight).isNull();
    assertThat(spec.concurrencyLevel).isEqualTo(32);
    assertThat(spec.keyStrength).isNull();
    assertThat(spec.valueStrength).isNull();
    assertThat(spec.writeExpirationTimeUnit).isNull();
    assertThat(spec.accessExpirationTimeUnit).isNull();
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().concurrencyLevel(32), CacheBuilder.from(spec));
  }

  public void testParse_concurrencyLevelRepeated() {
    assertThrows(
        IllegalArgumentException.class, () -> parse("concurrencyLevel=10, concurrencyLevel=20"));
  }

  public void testParse_weakKeys() {
    CacheBuilderSpec spec = parse("weakKeys");
    assertThat(spec.initialCapacity).isNull();
    assertThat(spec.maximumSize).isNull();
    assertThat(spec.maximumWeight).isNull();
    assertThat(spec.concurrencyLevel).isNull();
    assertThat(spec.keyStrength).isEqualTo(Strength.WEAK);
    assertThat(spec.valueStrength).isNull();
    assertThat(spec.writeExpirationTimeUnit).isNull();
    assertThat(spec.accessExpirationTimeUnit).isNull();
    assertCacheBuilderEquivalence(CacheBuilder.newBuilder().weakKeys(), CacheBuilder.from(spec));
  }

  public void testParse_weakKeysCannotHaveValue() {
    assertThrows(IllegalArgumentException.class, () -> parse("weakKeys=true"));
  }

  public void testParse_repeatedKeyStrength() {
    assertThrows(IllegalArgumentException.class, () -> parse("weakKeys, weakKeys"));
  }

  public void testParse_softValues() {
    CacheBuilderSpec spec = parse("softValues");
    assertThat(spec.initialCapacity).isNull();
    assertThat(spec.maximumSize).isNull();
    assertThat(spec.maximumWeight).isNull();
    assertThat(spec.concurrencyLevel).isNull();
    assertThat(spec.keyStrength).isNull();
    assertThat(spec.valueStrength).isEqualTo(Strength.SOFT);
    assertThat(spec.writeExpirationTimeUnit).isNull();
    assertThat(spec.accessExpirationTimeUnit).isNull();
    assertCacheBuilderEquivalence(CacheBuilder.newBuilder().softValues(), CacheBuilder.from(spec));
  }

  public void testParse_softValuesCannotHaveValue() {
    assertThrows(IllegalArgumentException.class, () -> parse("softValues=true"));
  }

  public void testParse_weakValues() {
    CacheBuilderSpec spec = parse("weakValues");
    assertThat(spec.initialCapacity).isNull();
    assertThat(spec.maximumSize).isNull();
    assertThat(spec.maximumWeight).isNull();
    assertThat(spec.concurrencyLevel).isNull();
    assertThat(spec.keyStrength).isNull();
    assertThat(spec.valueStrength).isEqualTo(Strength.WEAK);
    assertThat(spec.writeExpirationTimeUnit).isNull();
    assertThat(spec.accessExpirationTimeUnit).isNull();
    assertCacheBuilderEquivalence(CacheBuilder.newBuilder().weakValues(), CacheBuilder.from(spec));
  }

  public void testParse_weakValuesCannotHaveValue() {
    assertThrows(IllegalArgumentException.class, () -> parse("weakValues=true"));
  }

  public void testParse_repeatedValueStrength() {
    assertThrows(IllegalArgumentException.class, () -> parse("softValues, softValues"));

    assertThrows(IllegalArgumentException.class, () -> parse("softValues, weakValues"));

    assertThrows(IllegalArgumentException.class, () -> parse("weakValues, softValues"));

    assertThrows(IllegalArgumentException.class, () -> parse("weakValues, weakValues"));
  }

  public void testParse_writeExpirationDays() {
    CacheBuilderSpec spec = parse("expireAfterWrite=10d");
    assertThat(spec.initialCapacity).isNull();
    assertThat(spec.maximumSize).isNull();
    assertThat(spec.maximumWeight).isNull();
    assertThat(spec.concurrencyLevel).isNull();
    assertThat(spec.keyStrength).isNull();
    assertThat(spec.valueStrength).isNull();
    assertThat(spec.writeExpirationTimeUnit).isEqualTo(DAYS);
    assertThat(spec.writeExpirationDuration).isEqualTo(10L);
    assertThat(spec.accessExpirationTimeUnit).isNull();
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterWrite(10L, DAYS), CacheBuilder.from(spec));
  }

  public void testParse_writeExpirationHours() {
    CacheBuilderSpec spec = parse("expireAfterWrite=150h");
    assertThat(spec.writeExpirationTimeUnit).isEqualTo(HOURS);
    assertThat(spec.writeExpirationDuration).isEqualTo(150L);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterWrite(150L, HOURS), CacheBuilder.from(spec));
  }

  public void testParse_writeExpirationMinutes() {
    CacheBuilderSpec spec = parse("expireAfterWrite=10m");
    assertThat(spec.writeExpirationTimeUnit).isEqualTo(MINUTES);
    assertThat(spec.writeExpirationDuration).isEqualTo(10L);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterWrite(10L, MINUTES), CacheBuilder.from(spec));
  }

  public void testParse_writeExpirationSeconds() {
    CacheBuilderSpec spec = parse("expireAfterWrite=10s");
    assertThat(spec.writeExpirationTimeUnit).isEqualTo(SECONDS);
    assertThat(spec.writeExpirationDuration).isEqualTo(10L);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterWrite(10L, SECONDS), CacheBuilder.from(spec));
  }

  public void testParse_writeExpirationRepeated() {
    assertThrows(
        IllegalArgumentException.class, () -> parse("expireAfterWrite=10s,expireAfterWrite=10m"));
  }

  public void testParse_accessExpirationDays() {
    CacheBuilderSpec spec = parse("expireAfterAccess=10d");
    assertThat(spec.initialCapacity).isNull();
    assertThat(spec.maximumSize).isNull();
    assertThat(spec.maximumWeight).isNull();
    assertThat(spec.concurrencyLevel).isNull();
    assertThat(spec.keyStrength).isNull();
    assertThat(spec.valueStrength).isNull();
    assertThat(spec.writeExpirationTimeUnit).isNull();
    assertThat(spec.accessExpirationTimeUnit).isEqualTo(DAYS);
    assertThat(spec.accessExpirationDuration).isEqualTo(10L);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterAccess(10L, DAYS), CacheBuilder.from(spec));
  }

  public void testParse_accessExpirationHours() {
    CacheBuilderSpec spec = parse("expireAfterAccess=150h");
    assertThat(spec.accessExpirationTimeUnit).isEqualTo(HOURS);
    assertThat(spec.accessExpirationDuration).isEqualTo(150L);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterAccess(150L, HOURS), CacheBuilder.from(spec));
  }

  public void testParse_accessExpirationMinutes() {
    CacheBuilderSpec spec = parse("expireAfterAccess=10m");
    assertThat(spec.accessExpirationTimeUnit).isEqualTo(MINUTES);
    assertThat(spec.accessExpirationDuration).isEqualTo(10L);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterAccess(10L, MINUTES), CacheBuilder.from(spec));
  }

  public void testParse_accessExpirationSeconds() {
    CacheBuilderSpec spec = parse("expireAfterAccess=10s");
    assertThat(spec.accessExpirationTimeUnit).isEqualTo(SECONDS);
    assertThat(spec.accessExpirationDuration).isEqualTo(10L);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterAccess(10L, SECONDS), CacheBuilder.from(spec));
  }

  public void testParse_accessExpirationRepeated() {
    assertThrows(
        IllegalArgumentException.class, () -> parse("expireAfterAccess=10s,expireAfterAccess=10m"));
  }

  public void testParse_recordStats() {
    CacheBuilderSpec spec = parse("recordStats");
    assertThat(spec.recordStats).isTrue();
    assertCacheBuilderEquivalence(CacheBuilder.newBuilder().recordStats(), CacheBuilder.from(spec));
  }

  public void testParse_recordStatsValueSpecified() {
    assertThrows(IllegalArgumentException.class, () -> parse("recordStats=True"));
  }

  public void testParse_recordStatsRepeated() {
    assertThrows(IllegalArgumentException.class, () -> parse("recordStats,recordStats"));
  }

  public void testParse_accessExpirationAndWriteExpiration() {
    CacheBuilderSpec spec = parse("expireAfterAccess=10s,expireAfterWrite=9m");
    assertThat(spec.writeExpirationTimeUnit).isEqualTo(MINUTES);
    assertThat(spec.writeExpirationDuration).isEqualTo(9L);
    assertThat(spec.accessExpirationTimeUnit).isEqualTo(SECONDS);
    assertThat(spec.accessExpirationDuration).isEqualTo(10L);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterAccess(10L, SECONDS).expireAfterWrite(9L, MINUTES),
        CacheBuilder.from(spec));
  }

  public void testParse_multipleKeys() {
    CacheBuilderSpec spec =
        parse(
            "initialCapacity=10,maximumSize=20,concurrencyLevel=30,"
                + "weakKeys,weakValues,expireAfterAccess=10m,expireAfterWrite=1h");
    assertThat(spec.initialCapacity).isEqualTo(10);
    assertThat(spec.maximumSize).isEqualTo(20);
    assertThat(spec.maximumWeight).isNull();
    assertThat(spec.concurrencyLevel).isEqualTo(30);
    assertThat(spec.keyStrength).isEqualTo(Strength.WEAK);
    assertThat(spec.valueStrength).isEqualTo(Strength.WEAK);
    assertThat(spec.writeExpirationTimeUnit).isEqualTo(HOURS);
    assertThat(spec.accessExpirationTimeUnit).isEqualTo(MINUTES);
    assertThat(spec.writeExpirationDuration).isEqualTo(1L);
    assertThat(spec.accessExpirationDuration).isEqualTo(10L);
    CacheBuilder<?, ?> expected =
        CacheBuilder.newBuilder()
            .initialCapacity(10)
            .maximumSize(20)
            .concurrencyLevel(30)
            .weakKeys()
            .weakValues()
            .expireAfterAccess(10L, MINUTES)
            .expireAfterWrite(1L, HOURS);
    assertCacheBuilderEquivalence(expected, CacheBuilder.from(spec));
  }

  public void testParse_whitespaceAllowed() {
    CacheBuilderSpec spec =
        parse(
            " initialCapacity=10,\nmaximumSize=20,\t\r"
                + "weakKeys \t ,softValues \n , \r  expireAfterWrite \t =  15s\n\n");
    assertThat(spec.initialCapacity).isEqualTo(10);
    assertThat(spec.maximumSize).isEqualTo(20);
    assertThat(spec.maximumWeight).isNull();
    assertThat(spec.concurrencyLevel).isNull();
    assertThat(spec.keyStrength).isEqualTo(Strength.WEAK);
    assertThat(spec.valueStrength).isEqualTo(Strength.SOFT);
    assertThat(spec.writeExpirationTimeUnit).isEqualTo(SECONDS);
    assertThat(spec.writeExpirationDuration).isEqualTo(15L);
    assertThat(spec.accessExpirationTimeUnit).isNull();
    CacheBuilder<?, ?> expected =
        CacheBuilder.newBuilder()
            .initialCapacity(10)
            .maximumSize(20)
            .weakKeys()
            .softValues()
            .expireAfterWrite(15L, SECONDS);
    assertCacheBuilderEquivalence(expected, CacheBuilder.from(spec));
  }

  public void testParse_unknownKey() {
    assertThrows(IllegalArgumentException.class, () -> parse("foo=17"));
  }

  public void testParse_extraCommaIsInvalid() {
    assertThrows(IllegalArgumentException.class, () -> parse("weakKeys,"));

    assertThrows(IllegalArgumentException.class, () -> parse(",weakKeys"));

    assertThrows(IllegalArgumentException.class, () -> parse("weakKeys,,softValues"));
  }

  public void testEqualsAndHashCode() {
    new EqualsTester()
        .addEqualityGroup(parse(""), parse(""))
        .addEqualityGroup(parse("concurrencyLevel=7"), parse("concurrencyLevel=7"))
        .addEqualityGroup(parse("concurrencyLevel=15"), parse("concurrencyLevel=15"))
        .addEqualityGroup(parse("initialCapacity=7"), parse("initialCapacity=7"))
        .addEqualityGroup(parse("initialCapacity=15"), parse("initialCapacity=15"))
        .addEqualityGroup(parse("maximumSize=7"), parse("maximumSize=7"))
        .addEqualityGroup(parse("maximumSize=15"), parse("maximumSize=15"))
        .addEqualityGroup(parse("maximumWeight=7"), parse("maximumWeight=7"))
        .addEqualityGroup(parse("maximumWeight=15"), parse("maximumWeight=15"))
        .addEqualityGroup(parse("expireAfterAccess=60s"), parse("expireAfterAccess=1m"))
        .addEqualityGroup(parse("expireAfterAccess=60m"), parse("expireAfterAccess=1h"))
        .addEqualityGroup(parse("expireAfterWrite=60s"), parse("expireAfterWrite=1m"))
        .addEqualityGroup(parse("expireAfterWrite=60m"), parse("expireAfterWrite=1h"))
        .addEqualityGroup(parse("weakKeys"), parse("weakKeys"))
        .addEqualityGroup(parse("softValues"), parse("softValues"))
        .addEqualityGroup(parse("weakValues"), parse("weakValues"))
        .addEqualityGroup(parse("recordStats"), parse("recordStats"))
        .testEquals();
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testMaximumWeight_withWeigher() {
    CacheBuilder<Object, Object> builder = CacheBuilder.from(parse("maximumWeight=9000"));
    builder.weigher(constantWeigher(42)).build(CacheLoader.from(Suppliers.ofInstance(null)));
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testMaximumWeight_withoutWeigher() {
    CacheBuilder<Object, Object> builder = CacheBuilder.from(parse("maximumWeight=9000"));
    assertThrows(
        IllegalStateException.class,
        () -> builder.build(CacheLoader.from(Suppliers.ofInstance(null))));
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testMaximumSize_withWeigher() {
    CacheBuilder<Object, Object> builder = CacheBuilder.from(parse("maximumSize=9000"));
    builder.weigher(constantWeigher(42)).build(CacheLoader.from(Suppliers.ofInstance(null)));
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testMaximumSize_withoutWeigher() {
    CacheBuilder<Object, Object> builder = CacheBuilder.from(parse("maximumSize=9000"));
    builder.build(CacheLoader.from(Suppliers.ofInstance(null)));
  }

  public void testDisableCaching() {
    // Functional test: assert that CacheBuilderSpec.disableCaching()
    // disables caching.  It's irrelevant how it does so.
    CacheBuilder<Object, Object> builder = CacheBuilder.from(CacheBuilderSpec.disableCaching());
    Object key = new Object();
    Object value = new Object();
    LoadingCache<Object, Object> cache =
        builder.build(CacheLoader.from(Suppliers.ofInstance(value)));
    assertThat(cache.getUnchecked(key)).isSameInstanceAs(value);
    assertThat(cache.size()).isEqualTo(0);
    assertThat(cache.asMap().containsKey(key)).isFalse();
  }

  public void testCacheBuilderFrom_string() {
    CacheBuilder<?, ?> fromString =
        CacheBuilder.from(
            "initialCapacity=10,maximumSize=20,concurrencyLevel=30,"
                + "weakKeys,weakValues,expireAfterAccess=10m");
    CacheBuilder<?, ?> expected =
        CacheBuilder.newBuilder()
            .initialCapacity(10)
            .maximumSize(20)
            .concurrencyLevel(30)
            .weakKeys()
            .weakValues()
            .expireAfterAccess(10L, MINUTES);
    assertCacheBuilderEquivalence(expected, fromString);
  }

  private static void assertCacheBuilderEquivalence(
      CacheBuilder<?, ?> expected, CacheBuilder<?, ?> actual) {
    assertWithMessage("concurrencyLevel")
        .that(actual.concurrencyLevel)
        .isEqualTo(expected.concurrencyLevel);
    assertWithMessage("expireAfterAccessNanos")
        .that(actual.expireAfterAccessNanos)
        .isEqualTo(expected.expireAfterAccessNanos);
    assertWithMessage("expireAfterWriteNanos")
        .that(actual.expireAfterWriteNanos)
        .isEqualTo(expected.expireAfterWriteNanos);
    assertWithMessage("initialCapacity")
        .that(actual.initialCapacity)
        .isEqualTo(expected.initialCapacity);
    assertWithMessage("maximumSize").that(actual.maximumSize).isEqualTo(expected.maximumSize);
    assertWithMessage("maximumWeight").that(actual.maximumWeight).isEqualTo(expected.maximumWeight);
    assertWithMessage("refreshNanos").that(actual.refreshNanos).isEqualTo(expected.refreshNanos);
    assertWithMessage("keyEquivalence")
        .that(actual.keyEquivalence)
        .isEqualTo(expected.keyEquivalence);
    assertWithMessage("keyStrength").that(actual.keyStrength).isEqualTo(expected.keyStrength);
    assertWithMessage("removalListener")
        .that(actual.removalListener)
        .isEqualTo(expected.removalListener);
    assertWithMessage("weigher").that(actual.weigher).isEqualTo(expected.weigher);
    assertWithMessage("valueEquivalence")
        .that(actual.valueEquivalence)
        .isEqualTo(expected.valueEquivalence);
    assertWithMessage("valueStrength").that(actual.valueStrength).isEqualTo(expected.valueStrength);
    assertWithMessage("statsCounterSupplier")
        .that(actual.statsCounterSupplier)
        .isEqualTo(expected.statsCounterSupplier);
    assertWithMessage("ticker").that(actual.ticker).isEqualTo(expected.ticker);
    assertWithMessage("recordStats")
        .that(actual.isRecordingStats())
        .isEqualTo(expected.isRecordingStats());
  }
}
