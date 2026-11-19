/*
 * Copyright (C) 2019 The Guava Authors
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

import com.google.common.testing.AllocationRecordingRule;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ImmutableMapAllocationTest {
  @Rule public final AllocationRecordingRule allocationRule = new AllocationRecordingRule();

  private static final int N = 100;
  private static final Object[] keys = new Object[N];
  private static final Object[] values = new Object[N];
  private static final ImmutableMap<Object, Object> immutableMap;

  static {
    ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builderWithExpectedSize(N);
    for (int i = 0; i < N; i++) {
      builder.put(keys[i] = new Object(), values[i] = new Object());
    }
    immutableMap = builder.buildOrThrow();
  }

  private final Map<Object, Object> map =
      new LinkedHashMap<Object, Object>() {
        @Override
        public Set<Entry<Object, Object>> entrySet() {
          // Ensure entrySet() isn't cached by LinkedHashMap, as in other Map implementations.
          final Set<Entry<Object, Object>> real = super.entrySet();
          return new ForwardingSet<Entry<Object, Object>>() {
            @Override
            protected Set<Entry<Object, Object>> delegate() {
              return real;
            }
          };
        }

        @Override
        public Collection<Object> values() {
          // Ensure values() isn't cached by LinkedHashMap, as in other Map implementations.
          final Collection<Object> real = super.values();
          return new ForwardingCollection<Object>() {
            @Override
            protected Collection<Object> delegate() {
              return real;
            }
          };
        }
      };

  @Test
  public void copyOfMap() {
    ImmutableMap<?, ?> unused = ImmutableMap.copyOf(map);
    allocationRule.expectAllocationCountAtMost(5);
    // entrySet view (if uncached), iterator, alternatingKeysAndValues array, hash table, Map object
  }

  @Test
  public void copyOfEntrySet() {
    ImmutableMap<?, ?> unused = ImmutableMap.copyOf(map.entrySet());
    allocationRule.expectAllocationCountAtMost(5);
    // entrySet view (if uncached), iterator, alternatingKeysAndValues array, hash table, Map object
  }

  @Test
  public void unsizedBuilderPutOneByOne() {
    ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder();
    for (int i = 0; i < N; i++) {
      builder.put(keys[i], values[i]);
    }
    ImmutableMap<?, ?> unused = builder.build();
    allocationRule.expectAllocationCountAtMost(13); // builder + array growths + trim + table + map
  }

  @Test
  public void unsizedBuilderPutMap() {
    ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder().putAll(map);
    ImmutableMap<?, ?> unused = builder.build();
    allocationRule.expectAllocationCountAtMost(
        7); // builder + initial/grown array + trim + iter + table + map
  }

  @Test
  public void unsizedBuilderPutEntrySet() {
    ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder().putAll(map.entrySet());
    ImmutableMap<?, ?> unused = builder.build();
    allocationRule.expectAllocationCountAtMost(
        7); // builder + initial/grown array + trim + iter + table + map
  }

  @Test
  public void sizedBuilderPutOneByOne() {
    ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builderWithExpectedSize(N);
    for (int i = 0; i < N; i++) {
      builder.put(keys[i], values[i]);
    }
    ImmutableMap<?, ?> unused = builder.build();
    allocationRule.expectAllocationCountAtMost(4); // builder + array + table + map
  }

  @Test
  public void sizedBuilderPutMap() {
    ImmutableMap.Builder<Object, Object> builder =
        ImmutableMap.builderWithExpectedSize(N).putAll(map);
    ImmutableMap<?, ?> unused = builder.build();
    allocationRule.expectAllocationCountAtMost(5); // builder + array + iter + table + map
  }

  @Test
  public void sizedBuilderPutEntrySet() {
    ImmutableMap.Builder<Object, Object> builder =
        ImmutableMap.builderWithExpectedSize(N).putAll(map.entrySet());
    ImmutableMap<?, ?> unused = builder.build();
    allocationRule.expectAllocationCountAtMost(5); // builder + array + iter + table + map
  }

  @Test
  public void keySetIterate() {
    int unused = 0;
    for (Object o : immutableMap.keySet()) {
      unused ^= o.hashCode();
    }
    allocationRule.expectAllocationCountAtMost(4);
  }

  @Test
  public void valuesIterate() {
    int unused = 0;
    for (Object o : immutableMap.values()) {
      unused ^= o.hashCode();
    }
    allocationRule.expectAllocationCountAtMost(4);
  }

  @Test
  public void entrySetIterate() {
    int unused = 0;
    for (Entry<Object, Object> o : immutableMap.entrySet()) {
      unused ^= o.hashCode();
    }
    // Flat alternatingKeysAndValues materializes SimpleImmutableEntry on demand (C2 scalar
    // replacement eliminates these in non-instrumented production runs).
    allocationRule.expectAllocationCountAtMost(N + 2);
  }

  @Test
  public void keySetToArray() {
    Object[] unused = immutableMap.keySet().toArray();
    allocationRule.expectAllocationCountAtMost(2); // KeySet + Object[]
  }

  @Test
  public void valuesToArray() {
    Object[] unused = immutableMap.values().toArray();
    allocationRule.expectAllocationCountAtMost(2); // Values + Object[]
  }

  @Test
  public void entrySetToArray() {
    Object[] unused = immutableMap.entrySet().toArray();
    // Flat alternatingKeysAndValues materializes N SimpleImmutableEntry instances for toArray().
    allocationRule.expectAllocationCountAtMost(N + 2); // EntrySet + Object[] + N entries
  }
}
